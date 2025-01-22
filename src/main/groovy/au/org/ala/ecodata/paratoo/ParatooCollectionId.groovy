package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.DateUtil
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.converters.JSON
import grails.databinding.BindingFormat
import grails.validation.Validateable

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooCollectionId implements Validateable {

    ParatooSurveyMetadata survey_metadata
    String userId
    @BindingFormat("iso8601")
    Date eventTime

    Date coreSubmitTime

    static constraints = {
        userId nullable: true
        eventTime nullable: true
        coreSubmitTime nullable: true
        survey_metadata validator: { val, obj -> val.validate() }
    }

    String getProjectId() {
        survey_metadata?.survey_details?.project_id
    }

    String getProtocolId() {
        survey_metadata?.survey_details?.protocol_id
    }

    Map toMap() {
        [
            survey_metadata: survey_metadata.toMap(),
            userId: userId,
            eventTime: eventTimeAsISOString()
        ]
    }

    String eventTimeAsISOString() {
        eventTime ? DateUtil.formatWithMilliseconds(eventTime) : null
    }

    String encodeAsOrgMintedIdentifier() {
        Map data = toMap()
        String jsonString = (data as JSON).toString()
        jsonString.encodeAsBase64()
    }

    static ParatooCollectionId fromMap(Map map) {

        Date eventTime = map.eventTime ? DateUtil.parseWithMilliseconds(map.eventTime) : null
        new ParatooCollectionId(
                eventTime: eventTime,
                survey_metadata: ParatooSurveyMetadata.fromMap(map.survey_metadata),
        )
    }
}
