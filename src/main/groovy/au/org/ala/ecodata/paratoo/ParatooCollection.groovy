package au.org.ala.ecodata.paratoo

import grails.validation.Validateable

class ParatooCollection implements Validateable {
    String mintedCollectionId
    ParatooProtocolId protocolId
    String projectId
    String userId
    Date eventTime
}
