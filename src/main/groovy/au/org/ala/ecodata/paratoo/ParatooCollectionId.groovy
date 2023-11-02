package au.org.ala.ecodata.paratoo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooCollectionId implements Validateable {

    ParatooSurveyId surveyId

    static constraints = {
        surveyId validator: { val, obj -> val.validate() }
    }
}
