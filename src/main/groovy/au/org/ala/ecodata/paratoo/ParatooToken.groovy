package au.org.ala.ecodata.paratoo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable

@JsonIgnoreProperties(['metaClass', 'errors'])
class ParatooToken implements Validateable{
    String token
}
