package au.org.ala.ecodata

import org.bson.types.ObjectId
import org.springframework.validation.Errors

/**
 * A mu acts as a container for projects, more or less.
 */
class ManagementUnit {

    static bindingProperties = ['managementUnitSiteId', 'name', 'description', 'url', 'outcomes', 'priorities',
                                'startDate', 'endDate', 'associatedOrganisations', 'config', 'shortName']

    ObjectId id
    /** The hubId of the Hub in which this ManagementUnit was created */
    String hubId
    String status = Status.ACTIVE
    Date dateCreated
    Date lastUpdated
    String managementUnitId

    String name
    String description
    String url
    /** The date this management unit was established */
    Date startDate
    Date endDate
    String shortName

    /** Outcomes to be achieved in this mu (probably should only be defined by programs) */
    List<Map> outcomes

    /** Priority assets managed within the boundary of this mu (e.g. threatened species, or ecological communities) */
    List<Map> priorities

    /** (optional) The siteId of a Site that defines the geographic area targeted by this mu */
    String managementUnitSiteId

    /** Configuration related to the mu, reporting frequencies and types of reports */
    Map config

    /**
     * Organisations that have a relationship of some kind with this management unit.  Currently the only
     * relationship is a service provider.
     */
    List<AssociatedOrg> associatedOrganisations

    //Management units which have the same service provider
    List relevantManagementUnits = []

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
        mu.outcomes = outcomes
        mu.priorities = priorities
        mu.config = config
        mu.managementUnitSiteId = managementUnitSiteId
        mu.status = status
        mu.associatedOrganisations = associatedOrganisations
        mu.relevantManagementUnits = relevantManagementUnits
        mu.shortName = shortName

        mu
    }

    static mapping = {
        managementUnitId index: true
        version false
    }

    static embedded = ['associatedOrganisations']

    static transients = ['relevantManagementUnits']

    static constraints = {
        name unique: true
        description nullable: true
        startDate nullable: true
        endDate nullable: true
        url nullable: true
        config nullable: true
        associatedOrganisations nullable:true
        managementUnitSiteId nullable: true
        priorities nullable: true
        outcomes nullable:true
        shortName nullable: true
        hubId nullable: true, validator: { String hubId, ManagementUnit managementUnit, Errors errors ->
            GormMongoUtil.validateWriteOnceProperty(managementUnit, 'managementUnitId', 'hubId', errors)
        }
    }

    String toString() {
        return "Name: "+name+ ", description: "+description
    }
}
