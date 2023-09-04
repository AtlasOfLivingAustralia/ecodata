package au.org.ala.ecodata.paratoo

import grails.converters.JSON
import grails.validation.Validateable

class ParatooCollectionId implements Validateable {
    String projectId
    ParatooProtocolId protocol
    ParatooSurveyId surveyId

    static constraints = {
        protocol validator: { val, obj -> val.validate() }
        surveyId validator: { val, obj -> val.validate() }
    }

    String encodeAsMintedCollectionId() {
        Map jsonObject = [
                projectId: projectId,
                protocol: [id:protocol.id, version:protocol.version],
                surveyId: [surveyType:surveyId.surveyType, time:surveyId.timeAsISOString(), randNum:surveyId.randNum]
        ]
        String jsonString = (jsonObject as JSON).toString()

        jsonString.encodeAsBase64()

    }
}
