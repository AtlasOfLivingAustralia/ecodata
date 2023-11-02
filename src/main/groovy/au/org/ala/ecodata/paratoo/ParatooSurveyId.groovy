package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.DateUtil
import grails.databinding.BindingFormat
import grails.validation.Validateable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooSurveyId implements Validateable {

    String projectId
    ParatooProtocolId protocol
    String surveyType
    @BindingFormat("iso8601")
    Date time // ISO format
    Long randNum

    String timeAsISOString() {
        DateUtil.formatWithMilliseconds(time) // We preserve the milliseconds here because of Monitor unlike other times ecodata
    }

    String timeAsDisplayDate() {
        DateUtil.formatAsDisplayDate(time)
    }

    Map toMap() {
        [
                surveyType: surveyType,
                time: timeAsISOString(),
                randNum: randNum,
                projectId: projectId,
                protocol: [id:protocol.id, version:protocol.version]
        ]
    }

    static constraints = {
        protocol validator: { val, obj -> val.validate() }
    }

    static ParatooSurveyId fromMap(Map map) {
        new ParatooSurveyId(
                surveyType: map.surveyType,
                time: DateUtil.parseWithMilliseconds(map.time),
                randNum: map.randNum,
                projectId: map.projectId,
                protocol: new ParatooProtocolId(id: map.protocol.id, version: map.protocol.version)
        )
    }

}
