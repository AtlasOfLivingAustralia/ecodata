package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * A mu acts as a container for projects, more or less.
 */
class ManagementUnit {

    ObjectId id
    String managementUnitId
    String name
    String description
    String status = Status.ACTIVE
    String url
    Date dateCreated
    Date lastUpdated
    List<Project> projects = []

    List risks
    /** Themes for this mu */
    List themes
    /** Assets managed by this mu (e.g. threatened species, or ecological communities) */
    List assets
    /** Outcomes to be achieved by this mu */
    List outcomes
    /** Priorities for mu outcomes */
    List priorities

    /** Configuration related to the mu */
    Map config

    Date startDate
    Date endDate

    List<AssociatedOrg> associatedOrganisations

    /** (optional) The siteId of a Site that defines the geographic area targeted by this mu */
    String managementUnitSiteId
    /** Allows management unit administrators to publicise and communicate about the management unit */
    List blog


    /** Custom rendering for the mu */
    Map toMap() {
        Map mu = [:]
        mu.managementUnitId = managementUnitId
        mu.name = name
        mu.description = description
        mu.startDate = startDate
        mu.endDate = endDate
        mu.dateCreated = dateCreated
        mu.lastUpdated = lastUpdated
        mu.url = url
        mu.themes = themes
        mu.assets = assets
        mu.outcomes = outcomes
        mu.priorities = priorities
        mu.config = config
        mu.risks = risks
        mu.managementUnitSiteId = managementUnitSiteId
        mu.status = status

        mu.associatedOrganisations = associatedOrganisations
        mu.blog = blog

        mu
    }

    static mapping = {
        managementUnitId index: true
        version false
    }

    static embedded = ['associatedOrganisations']

    static constraints = {
        name unique: true
        description nullable: true
        risks nullable: true
        startDate nullable: true
        endDate nullable: true
        url nullable: true
        config nullable: true
        associatedOrganisations nullable:true
        managementUnitSiteId nullable: true
    }

    public String toString() {
        return "Name: "+name+ ", description: "+description
    }
}
