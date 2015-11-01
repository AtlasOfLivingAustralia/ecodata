package au.org.ala.ecodata

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class AssociatedOrg {
    String organisationId
    String name
    String logo
    String url

    // an AssociateOrg can either be another registered Organisation, in which case the organisationId field will be populated,
    // or just a reference to an external body, in which case just the name and an optional logo will be recorded

    static constraints = {
        organisationId nullable: true
        name nullable: true
        logo nullable: true
        url nullable: true
    }

}
