package au.org.ala.ecodata

class CustomMetadata {
    String category
    String raid
    String indigenousCulturalIP
    String ethicsApproval
    String ethicsApprovalNumber
    String ethicsContactDetails

    static constraints = {
        category nullable: true, blank: true
        raid nullable: true, blank: true
        indigenousCulturalIP nullable: true, blank: true
        ethicsApproval nullable: true, blank: true
        ethicsApprovalNumber nullable: true, blank: true
        ethicsContactDetails nullable: true, blank: true
    }

    Map<String, Object> toMap() {
        [
            category: category,
            raid: raid
        ]
    }
}
