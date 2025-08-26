package au.org.ala.ecodata

class ProjectOutcome {

    String description
    String relatedOutcome
    String code
    List<String> assets

    ProjectOutcome(Map outcomeDetails) {
        this.description = outcomeDetails.description
        this.relatedOutcome = outcomeDetails.relatedOutcome
        this.code = outcomeDetails.code
        this.assets = outcomeDetails.assets ?: []
    }

    static constraints = {
        description nullable: true
        relatedOutcome nullable: true
        code nullable: true
        assets nullable: true
    }
}