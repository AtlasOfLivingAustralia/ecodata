package au.org.ala.ecodata

class ProjectOutcome {

    static graphql = {
        operations.get.enabled false
        operations.list.enabled false
        operations.count.enabled false
        operations.create.enabled false
        operations.update.enabled false
        operations.delete.enabled false
        exclude('details', 'errors', 'id')
    }
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