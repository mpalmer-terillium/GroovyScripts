// 12/18 working but read the bottom
// 
// Function: O_INT_JDE_OpportunitySync
// 
// no params; returns void

adf.util.O_INT_Info('O_INT_JDE_OpportunitySync')


def createNewQuote = {
  setAttribute('O_INT_SalesDocumentStatus_c', '')
  O_INT_JDE_CreateSalesDocument('Quote')
}


def result          = 'NOT_RUN'
def remoteSystemId  = adf.util.O_INT_JDE_GetSysParam('remoteSystemID')
def jdeVersion      = adf.util.O_INT_GetIntegConfigParameter(remoteSystemId, 'jdeCustomerVersion')

def opptyId = adf.source.OptyId
def fusionCustomerId = adf.source.TargetPartyId
def remoteCustomerId = adf.util.O_INT_GetXREF(adf.util.O_INT_JDE_GetSysParam('remoteSystemID'), 'Organization', fusionCustomerId as String, true)

def UPDATE = 2
def ADD = 1
def ACTION_TYPE = (JDEOrderType_c && JDEOrderNumber_c) ? UPDATE : ADD

def CREATED_JDEOrderNumber
def oppSync


// if we are adding a new opportunity, just call createNewQuote() - then store the new documentId from JDE
if(ACTION_TYPE == ADD) {
  
    CREATED_JDEOrderNumber = createNewQuote()
    result = 'SUCCESS'
  
} else {

    def BUSINESS_UNIT = 'M30'
    def DOC_COMPANY = '00200'

    def opptyRevenueItems = nvl(ChildRevenue,'')
    def LINE_NUMBER = 1
    def PRICE_DOMESTIC_UNIT = 5.00

    if (!jdeVersion) {
     adf.util.O_INT_JDE_LogWithException('0018', 'The JDE version integration parameters are not configured properly')
    }

    // we dont know individual line numbers, so we cant update each line
    // this loop will keep updating the same line
    // the request will have all of the individual info, but each one will be the same line number.

    def addProducts = { list, message ->
        while(list?.hasNext()) {

            def opptyRevenue = list.next()
          
            message.header.detail.add(
                [
                    billing:
                    [
                        pricing:
                        [
                            priceUnitDomestic: PRICE_DOMESTIC_UNIT, // find this
                            unitOfMeasureCodePricing: 'EA'
                        ]
                    ],
                    customerPO: 'TEST', //Name,
                    documentLineNumber: LINE_NUMBER,
                    processing:
                    [
                        actionType: ACTION_TYPE
                    ],
                    product:
                    [
                        item:
                        [
                            itemCatalog: 1001, //opptyRevenue.InventoryItemId
                            itemUOMPrimary: 'EA'
                        ]
                    ],
                    quantityOrdered: opptyRevenue.Quantity
                ]
            )
        }
    }

    def addSalesOrderKey = { message ->
        message.header.salesOrderKey =
            [
                documentCompany: DOC_COMPANY,
                documentNumber: nvl(JDEOrderNumber_c,''),
                documentTypeCode: nvl(JDEOrderType_c,'')
            ]
    }

    oppSync = [
        header:
        [
            businessUnit: BUSINESS_UNIT,
            customerPO: 'TEST', //Name,
            detail: [],
           // opportunityId: opptyId,
            dateRequested: nvl(BudgetAvailableDate, ''),
            processing:
            [
                actionType: ACTION_TYPE
            ],
            salesOrderKey: [],
            shipTo:
            [
                customer:
                [
                        entityId: 117866 //remoteCustomerId?.ObjectID
                ]
            ],
            soldTo:
            [
                customer:
                [
                    entityId: 117866 //remoteCustomerId?.ObjectID
                ]
            ]
        ]
    ]

    addProducts(opptyRevenueItems, oppSync)
    addSalesOrderKey(oppSync)
}

// attempt the service call
def resp

if(ACTION_TYPE == UPDATE) {
  
    try {
      
      resp = adf.webServices.O_INT_JDE_SalesOrderManager1.processSalesOrderV5(oppSync)
      result = 'SUCCESS'
      
    } catch (e) {
      
      adf.util.O_INT_Error(adf.util.O_INT_JDE_GetLogMsg('0028', 'Got exception calling SalesOrderManager1.processSalesOrderV5(): ' + e.getMessage()))
      result = 'EXCEPTION ' + e
    }
}

// deal with the result
if (result == 'NOT_RUN') {
  
  def messageId = '0059'
  def messageKey = result + resp?.toString()
  adf.util.O_INT_JDE_LogWithException(messageId, messageKey)
}

if (result =~ /EXCEPTION.*/) {
   
  def messageId = '0061'
  def messageKey = 'Exception Thrown: ' + result
  adf.util.O_INT_JDE_LogWithException(messageId, messageKey)
}

if (result != 'SUCCESS') {
  
  def messageKey = result + ' NOT_WORKING ' + resp?.toString()
  def messageId = '0060'
  adf.util.O_INT_JDE_LogWithException(messageId, messageKey)
}

if (result == 'SUCCESS') {
  
  def messageKey = ''
  def messageId = '0001'
  
  
  if(ACTION_TYPE == UPDATE) {
    
    resp?.e1MessageList?.e1Messages?.each { messageKey += it.toString() }
    messageKey += ' Document Number: '  + resp?.header?.salesOrderKey?.documentNumber?.toString()
    messageKey += ' Document Type: '    + resp?.header?.salesOrderKey?.documentTypeCode?.toString()
    messageKey += ' Document Company: ' + resp?.header?.salesOrderKey?.documentCompany?.toString()
  }
  
  
  if(ACTION_TYPE == ADD) {
    
    setAttribute('JDEOrderNumber_c', CREATED_JDEOrderNumber)
    setAttribute('JDEOrderType_c', 'SQ')
    messageKey += ' Added JDEOrderNumber: ' + CREATED_JDEOrderNumber + ' and JDEOrderType: ' + JDEOrderType_c
  }
  
    adf.util.O_INT_JDE_LogWithException(messageId, messageKey)
}




















