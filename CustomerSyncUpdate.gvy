adf.util.O_INT_Info('O_INT_JDE_CustomerSyncUpdate')
adf.util.O_INT_Debug(adf.util.O_INT_JDE_GetLogMsg('0013', 'Current state: ' + O_INT_JDE_Sync_Status_c))

def remoteSystemId = adf.util.O_INT_JDE_GetSysParam('remoteSystemID')

// Get the flag value from Integ Config
def enableAutoModeForSingleMatch = upperCase(adf.util.O_INT_GetIntegConfigParameter(remoteSystemId, 'enableAutoModeForSingleMatch'))

if (O_INT_JDE_Sync_Status_c == null || O_INT_JDE_Sync_Status_c == '' || O_INT_JDE_Sync_Status_c == adf.util.O_INT_JDE_GetSysParam('nsync')) {
	// we have not syncd yet, verify first there's no XREF for this customer
	adf.util.O_INT_JDE_LogWithException('0060', 'Not synchronized. Synchronize first save and then pdate.')
}
def remoteID = adf.util.O_INT_GetXREF(adf.util.O_INT_JDE_GetSysParam('remoteSystemID'), 'Organization', PartyId as String, true)

adf.util.O_INT_Debug('The remote id is ' + remoteID);

if (remoteID == null) {
adf.util.O_INT_JDE_LogWithException('0051', 'Failed to access customer association to JDE! You may not have permission to perform JDE addressUpdate.')

} else if (remoteID?.ObjectID) {
	//adf.util.O_INT_Info(adf.util.O_INT_JDE_GetLogMsg('0052', 'Customer already has XREF but sync status is incorrect. Setting status to syncd.'))
	//setAttribute('O_INT_JDE_Sync_Status_c', adf.util.O_INT_JDE_GetSysParam('sync'))
}
def JDECustomerID = remoteID.ObjectID
def updateResult = O_INT_JDE_UpdateCustomer()



if (updateResult.result == 'UERROR') {
	def messageId = '0057'
	def messageKey = updateResult.e + "***" + updateResult.Resp.toString() + ' Unexpected Error While updating customer record in JDE Address Book #' + JDECustomerID
	adf.util.O_INT_JDE_LogWithException(messageId, messageKey)
}

if (updateResult.result == 'FAIL_UPDATE') {
	def messageId = '0055'
	def messageKey = updateResult.Resp.toString() + ' No customer match found and failed to update customer record in JDE Address Book #' + JDECustomerID
	adf.util.O_INT_JDE_LogWithException(messageId, messageKey)
}

if (updateResult.result == 'NOT_RUN') {
	def messageId = '0058'
	def messageKey = updateResult.Resp.toString() + ' failed to call the Web Service to update customer record in JDE Address Book #' + JDECustomerID
	adf.util.O_INT_JDE_LogWithException(messageId, messageKey)
}

// this is set to log a message on success - debug -mp
if (updateResult.result == 'SUCCESS') {
	// failed to update customer
	// def messageKey = updateResult.Resp.toString() + 'failed to update customer record in JDE Address Book #' + JDECustomerID
	def messageKey = updateResult.Resp.toString() + 'updated customer record in JDE Address Book #' + JDECustomerID
	def messageId = '0060'
	adf.util.O_INT_JDE_LogWithException(messageId, messageKey)
}