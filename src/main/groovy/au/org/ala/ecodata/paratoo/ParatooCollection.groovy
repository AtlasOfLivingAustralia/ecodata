package au.org.ala.ecodata.paratoo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooCollection implements Validateable {

    String orgMintedUUID
    Map coreProvenance
}
