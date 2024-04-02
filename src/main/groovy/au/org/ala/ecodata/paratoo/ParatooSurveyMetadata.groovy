package au.org.ala.ecodata.paratoo

import grails.validation.Validateable

class ParatooSurveyMetadata implements Validateable {

    static constraints = {
        orgMintedUUID nullable: true
    }

    ParatooSurveyDetails survey_details
    ParatooProvenance provenance

    /** Added by the ParatooService, other fields are supplied as input */
    String orgMintedUUID

    Map toMap() {
        [
            survey_details: survey_details.toMap(),
            provenance: provenance.toMap(),
            orgMintedUUID: orgMintedUUID
        ]
    }

    static ParatooSurveyMetadata fromMap(Map data) {
        ParatooSurveyMetadata surveyMetadata = new ParatooSurveyMetadata([
            survey_details: ParatooSurveyDetails.fromMap(data.survey_details),
            provenance: ParatooProvenance.fromMap(data.provenance),
            orgMintedUUID: data.orgMintedUUID
        ])
    }

}
