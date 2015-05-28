package au.org.ala.ecodata

import spock.lang.Specification

/**
 * Tests the IdentifierHelper
 */
class IdentifierHelperSpec extends Specification {

    def "the associated projectId for a domain object should be obtained if possible"() {

        setup:
        def projectId = 'project1'

        when:
        def domainObject = new Project(projectId: projectId, name: 'project 1')
        then:
        IdentifierHelper.getProjectId(domainObject) == projectId

        when: "the domain object is type Activity"
        domainObject = new Activity(projectId: projectId, activityId: 'activity 1', type: 'An Activity')
        then: "the projectId should be returned"
        IdentifierHelper.getProjectId(domainObject) == projectId

        when: "the domain object is an Output"
        domainObject = new Output(activityId:'An activity', outputId:'An output')
        then: "the projectId need not be returned, as the UI assembles the Outputs via the related Activity"
        IdentifierHelper.getProjectId(domainObject) == null

        when: "the domain object is a Site"
        domainObject = new Site(siteId:"A site", projects:[projectId, 'another project'])
        then: "the projectId need not be returned as a site can be associated with multiple projects.  The UI assembles sites into each Project being interrogated."
        IdentifierHelper.getProjectId(domainObject) == null

        when: "the domain object is an Organisation"
        domainObject = new Organisation(organisationId: "An organisation")
        then: "the projectId should not be returned as the project to organisation association is one way"
        IdentifierHelper.getProjectId(domainObject) == null

        when: "the domain object is a UserPermission relating to a Project permission"
        domainObject = new UserPermission(entityType: 'au.org.ala.ecodata.Project', entityId: projectId)
        then: "the projectId should be returned"
        IdentifierHelper.getProjectId(domainObject) == projectId
        when: "the domain object is a UserPermission relating on an Organisation"
        domainObject = new UserPermission(entityType: 'au.org.ala.ecodata.Organisation', entityId: 'An organisation')
        then: "Null should be returned"
        IdentifierHelper.getProjectId(domainObject) == null

        when: "The domain object is a project Document"
        domainObject = new Document(projectId:projectId)
        then: "the projectId should be returned"
        IdentifierHelper.getProjectId(domainObject) == projectId

        when: "The domain object is a Document not relating to a project"
        domainObject = new Document(siteId:'A site')
        then: "Null should be returned"
        IdentifierHelper.getProjectId(domainObject) == null
    }
}
