package au.org.ala.ecodata.paratoo

import grails.databinding.BindingFormat
import grails.validation.Validateable

class ParatooCollection implements Validateable {
    String mintedCollectionId
    ParatooProtocolId protocol
    String projectId
    String userId

    @BindingFormat("iso8601")
    Date eventTime

    static constraints = {
        protocol validator: { val, obj -> val.validate() }
    }
}
