package au.org.ala.ecodata

import au.org.ala.ecodata.graphql.mappers.OrganisationGraphQLMapper
import au.org.ala.ecodata.graphql.mappers.ProjectGraphQLMapper
import org.bson.types.ObjectId
import org.springframework.validation.Errors

/**
 * Represents an organisation that manages projects in MERIT and BioCollect.
 * Allows some branding as well as grouping / ownership of projects.
 */
class Organisation {

    static graphql = OrganisationGraphQLMapper.graphqlMapping()

    ObjectId id
    /** The hubId of the Hub in which this organisation was created */
    String hubId
    String organisationId
    String acronym
    String name
    String description
    String announcements
    String abn
    String abnStatus // N/A, Active, Cancelled
    String entityName
    String entityType // From ABN register
    List<String> businessNames
    String state
    Integer postcode
    List<ExternalId> externalIds // For financial system vendor codes/reference
    List<String> indigenousOrganisationRegistration
    List<AssociatedOrg> associatedOrgs // e.g. parent organisation such as for NSW LLS group
    List<String> contractNames // When contracts are written for projects with this organisation with a name that doesn't match the organisation name
    String status = 'active'

    String collectoryInstitutionId // Reference to the Collectory

    Date dateCreated
    Date lastUpdated


    static mapping = {
        organisationId index: true
        version false
    }

    static constraints = {
        name unique: true
        acronym nullable: true
        announcements nullable: true
        description nullable: true
        collectoryInstitutionId nullable: true
        abnStatus nullable: true
        entityName nullable: true
        entityType nullable: true
        businessNames nullable: true
        contractNames nullable: true
        state nullable: true
        postcode nullable: true
        indigenousOrganisationRegistration nullable: true
        associatedOrgs nullable: true
        abn nullable: true
        hubId nullable: true, validator: { String hubId, Organisation organisation, Errors errors ->
            GormMongoUtil.validateWriteOnceProperty(organisation, 'organisationId', 'hubId', errors)
        }
    }
}
