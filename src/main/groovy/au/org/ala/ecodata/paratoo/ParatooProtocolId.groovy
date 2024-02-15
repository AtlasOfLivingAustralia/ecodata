package au.org.ala.ecodata.paratoo

import grails.validation.Validateable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooProtocolId implements Validateable {
    String id
    Integer version
}
