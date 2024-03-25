package au.org.ala.ecodata.paratoo

import grails.databinding.BindingFormat
import grails.validation.Validateable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooCollection implements Validateable {

    String orgMintedUUID
    Map coreProvenance
}
