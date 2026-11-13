package au.org.ala.ecodata


import groovy.util.logging.Slf4j

@Slf4j
class ProjectOutcome {

    String description
    String relatedOutcome
    String code
    List<String> assets

    ProjectOutcome(Map outcomeDetails) {
        this.description = outcomeDetails.description
        this.relatedOutcome = outcomeDetails.relatedOutcome
        this.code = outcomeDetails.code
        if (outcomeDetails.assets instanceof String) {
            log.warn("Project outcome assets should be a list, but a string was provided. Converting to a list.")
            this.assets = [outcomeDetails.assets]
        } else if (outcomeDetails.assets instanceof List) {
            this.assets = outcomeDetails.assets
        } else {
            this.assets = []
        }
    }

    static constraints = {
        description nullable: true
        relatedOutcome nullable: true
        code nullable: true
        assets nullable: true
    }
}