package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.DateUtil
import grails.converters.JSON
import grails.databinding.BindingFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooMintedIdentifier {
    ParatooSurveyId surveyId
    @BindingFormat("iso8601")
    Date eventTime
    String userId
    String projectId
    String system
    String version

    String eventTimeAsISOString() {
        DateUtil.formatWithMilliseconds(eventTime)
    }

    String encodeAsMintedCollectionId() {
        Map data = [
                surveyId: surveyId.toMap(),
                eventTime: eventTimeAsISOString(),
                userId: userId,
                projectId: projectId,
                protocolId: surveyId.protocol.id,
                protocolVersion: surveyId.protocol.version,
                org_provenance: [
                        system: system,
                        version: version
                ]
        ]
        String jsonString = (data as JSON).toString()
        jsonString.encodeAsBase64()
    }

}
