package au.org.ala.ecodata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString


@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class AssociatedOrg {

    /** Reference to the Organisation entity if ecodata has a record of the Organisation */
    String organisationId

    /** The name of the organisation as referenced via the organisationId */
    String organisationName
    /**
     * The name of the organisation in the context of the relationship.  e.g. it could be a name used
     * in a contract with a project that is different from the current business name of the organisation
     */
    String name
    String logo
    String url

    /**
     * The date the association started.  A null date indicates the relationship started at the same
     * time as the related entity. e.g. the start of a Project
     */
    Date fromDate

    /**
     * The date the association e ended.  A null date indicates the relationship ended at the same
     * time as the related entity. e.g. the end of a Project
     */
    Date toDate

    /** A description of the association - e.g. Service Provider, Grantee, Sponsor */
    String description

    // an AssociateOrg can either be another registered Organisation, in which case the organisationId field will be populated,
    // or just a reference to an external body, in which case just the name and an optional logo will be recorded

    static constraints = {
        organisationId nullable: true
        name nullable: true
        logo nullable: true

        url nullable: true
        description nullable: true
        fromDate nullable: true
        toDate nullable: true
        organisationName nullable: true
    }

}
