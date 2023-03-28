package au.org.ala.ecodata.paratoo

import grails.databinding.BindingFormat
import grails.validation.Validateable

class ParatooSurveyId implements Validateable {
    String surveyType
    @BindingFormat("iso8601")
    Date time // ISO format
    Long randNum
}
