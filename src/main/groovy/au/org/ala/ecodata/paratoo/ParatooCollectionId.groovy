package au.org.ala.ecodata.paratoo


import grails.converters.JSON
import grails.validation.Validateable

class ParatooCollectionId implements Validateable {

    ParatooSurveyId surveyId

    static constraints = {
        surveyId validator: { val, obj -> val.validate() }
    }

    String encodeAsMintedCollectionId() {
        String jsonString = ([surveyId:surveyId.toMap()] as JSON).toString()
        jsonString.encodeAsBase64()
    }
}
