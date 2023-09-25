package au.org.ala.ecodata.paratoo


import grails.validation.Validateable

class ParatooCollectionId implements Validateable {

    ParatooSurveyId surveyId

    static constraints = {
        surveyId validator: { val, obj -> val.validate() }
    }
}
