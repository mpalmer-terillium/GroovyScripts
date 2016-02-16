// new Function: O_INT_JDE_UpdateCustomer

// returns Object

// working version 11/19 -mp

// no params

adf.util.O_INT_Info('O_INT_JDE_UpdateCustomer')


def result          = 'NOT_RUN'
def remoteSystemId  = adf.util.O_INT_JDE_GetSysParam('remoteSystemID')
def remoteID        = adf.util.O_INT_GetXREF(adf.util.O_INT_JDE_GetSysParam('remoteSystemID'), 'Organization', PartyId as String, true)
def JDECustomerID   = remoteID.ObjectID
def jdeVersion      = adf.util.O_INT_GetIntegConfigParameter(remoteSystemId, 'jdeCustomerVersion')

if (!jdeVersion) {
 adf.util.O_INT_JDE_LogWithException('0018', 'The JDE version integration parameters are not configured properly')
}

def address_line    = [:]
def entity_line     = [:]
def processing_line = [:]


if(PrimaryAddressLine1 != null) {
    address_line.put('addressLine1', PrimaryAddressLine1)
}
if(PrimaryAddressLine2 != null) {
    address_line.put('addressLine2', PrimaryAddressLine2)
}
if(PrimaryAddressLine3 != null) {
    address_line.put('addressLine3', PrimaryAddressLine3)
}
if(PrimaryAddressCity != null) {
    address_line.put('city', PrimaryAddressCity)
}
if(PrimaryAddressPostalCode != null) {
    address_line.put('postalCode', PrimaryAddressPostalCode)
}
if(PrimaryAddressCountry != null) {
    address_line.put('countryCode', PrimaryAddressCountry)
}
if(PrimaryAddressState != null) {
    address_line.put('stateCode', O_INT_JDE_StateXref(PrimaryAddressState))
}
if(JDECustomerID != null) {
    entity_line.put('entityId', JDECustomerID)
}
if(true) {
    processing_line.put('actionType', 2)
}
//may need to add customer number(?) here -mp
//
// request fields, phoneNumbers:
//
// actionType
// areaCode
// contactId  <<--- ??
// phoneLineNumber
// phoneNumber
// phoneTypeCode
//
def phoneNumbers_line
def PrimaryPhoneLineType_val

// figure out what valid values passed in here are - this is not complete! -mp
if(PrimaryPhoneLineType) {
  PrimaryPhoneLineType_val = "MBL"
} else {
  PrimaryPhoneLineType_val = "HOM"
}


if (PrimaryPhoneNumber != null) {
  phoneNumbers_line = [
    [
      actionType : 2,
      areaCode : PrimaryPhoneAreaCode,
      phoneLineNumber : 4,
      phoneNumber : PrimaryPhoneNumber,
      phoneTypeCode : PrimaryPhoneLineType_val
    ]
  ]
}

// Email fields
//
// actionType = 2 - this has to be a 1 the first time an email is sent regardless of if the customer exists already.
// electronicAddressLineNumber = 1 - required to update after it has been added.  
//                                   We will have to figure out how to sync between the two systems.
// electronicAddress = test@terillium.com
// electronicAddressTypeCode = E
//
def email_line
if(PrimaryEmailAddress != null) {
 email_line = [
     [
       actionType : 2,
       electronicAddressLineNumber : 1,
       electronicAddress : PrimaryEmailAddress,
       electronicAddressTypeCode : "E"
     ]
   ]
}

// organization parameter definition
def organization =
	[
		processAddressBook:
		[
			addressBook:
			[
				address: address_line,
				entity: entity_line,
				entityName : OrganizationName,
				processing: processing_line,
                phoneNumbers: phoneNumbers_line,
                electronicAddresses : email_line
			]
		],
		processing: processing_line
	]


def resp

try {
  resp = adf.webServices.O_INT_JDE_CustomerManager1.processCustomer(organization)
} catch (e) {
  adf.util.O_INT_Error(adf.util.O_INT_JDE_GetLogMsg('0028', 'Got exception calling CustomerManager.processCustomer(): ' + e.getMessage()))
  return [result: 'UERROR', ID: null, e: e.getMessage()]
}

if (!resp || !resp?.confirmProcessAddressBook) {
  adf.util.O_INT_Error(adf.util.O_INT_JDE_GetLogMsg('0029', 'Failed to create Organization: ' + (resp?.toString() + '')))
  return [result: 'FAIL_UPDATE', ID: null]
}

/* Creating cross reference record in fusion */
def remoteSystemID      = adf.util.O_INT_JDE_GetSysParam('remoteSystemID')
def fusionObjectType    = 'Organization'
def fusionRecordID      = PartyId?.toString()
def remoteObjectType    = 'Organization'
def remoteRecordID      = resp.confirmProcessAddressBook[0]?.entity[0]?.entityId?.toString()
def remotePartyNumber   = remoteRecordID

setAttribute('JDEAddressBook_c', remoteRecordID)

if (remoteRecordID == null || !adf.util.O_INT_UpdateXREF(remoteSystemID, fusionObjectType, fusionRecordID, remoteObjectType, remoteRecordID, true)) {
  adf.util.O_INT_Error(adf.util.O_INT_JDE_GetLogMsg('0030', 'Failed to create XREF for OSC Organization ' + fusionRecordID + ' to JDE organization ' + remoteRecordID))
  result = 'FAIL_XREF'
} else result = 'SUCCESS'

return [result: result, ID: remoteRecordID, PartyNumber: remotePartyNumber, Resp: resp]
