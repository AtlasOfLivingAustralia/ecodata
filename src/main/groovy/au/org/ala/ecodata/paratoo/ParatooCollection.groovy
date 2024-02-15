package au.org.ala.ecodata.paratoo

import grails.databinding.BindingFormat
import grails.validation.Validateable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooCollection implements Validateable {
    String orgMintedIdentifier
    ParatooProtocolId protocol
    String projectId
    String userId

    @BindingFormat("iso8601")
    Date eventTime

    static constraints = {
        protocol validator: { val, obj -> val.validate() }
        projectId nullable: true
        userId nullable: true
        eventTime nullable: true
    }
}
