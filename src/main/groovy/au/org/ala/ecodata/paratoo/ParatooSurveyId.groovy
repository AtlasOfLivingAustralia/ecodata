package au.org.ala.ecodata.paratoo

import grails.validation.Validateable

class ParatooSurveyId implements Validateable {
    String surveyType
    String time // ISO format
    Long randNum
}
