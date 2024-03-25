package au.org.ala.ecodata.paratoo

class ParatooSurveyDetails {
    String survey_model
    /** ISO time, stored as a String as we don't need to use it as a Date */
    String time
    String uuid
    String project_id
    String protocol_id
    Integer protocol_version

    Map toMap() {
        [
            survey_model: survey_model,
            time: time,
            uuid: uuid,
            project_id: project_id,
            protocol_id: protocol_id,
            protocol_version: protocol_version
        ]
    }
}
