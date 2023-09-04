package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.DateUtil
import grails.databinding.BindingFormat
import grails.validation.Validateable


class ParatooSurveyId implements Validateable {
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
                randNum: randNum
        ]
    }

    static ParatooSurveyId fromMap(Map map) {
        new ParatooSurveyId(
                surveyType: map.surveyType,
                time: DateUtil.parseWithMilliseconds(map.time),
                randNum: map.randNum
        )
    }

}
