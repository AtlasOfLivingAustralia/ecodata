package au.org.ala.ecodata.paratoo

import grails.converters.JSON
import grails.validation.Validateable
import org.apache.commons.codec.digest.DigestUtils

class ParatooCollectionId implements Validateable {
    String projectId
    ParatooProtocolId protocol
    ParatooSurveyId surveyId
}
