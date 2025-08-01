package au.org.ala.ecodata

import grails.gorm.PagedResultList
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.*

/**
 * Spec for the AuditService.
 */
@Integration
@Rollback
class AuditServiceSpec extends IntegrationTestHelper {

    def auditService
    def grailsApplication
    def userService // The original user service
    def userServiceStub = Stub(UserService) // A stub we are using to help test.

    def datastore
    def setup() {
        datastore = grailsApplication.mainContext.mongoDatastore
        auditService.userService = userServiceStub
    }

    def cleanup() {
        auditService.userService = userService // Restore the userService
    }


    def "New projects should be audited"() {

        setup:
        def userId = '1234'
        def project = TestDataHelper.buildProject()
        userServiceStub.getCurrentUserDetails() >> {[userId:userId]}

        when: "a new project is created"
        project.save(flush:true, failOnError: true)
        AuditMessage.withTransaction {
            auditService.flushMessageQueue()
        }
        def auditMessage = null
        AuditMessage.withTransaction {
            auditMessage = AuditMessage.findByProjectId(project.projectId)
        }

        then: "the details of the create operation should be audited correctly"

        auditMessage != null
        auditMessage.eventType == AuditEventType.Insert
        auditMessage.entityType == Project.class.name
        auditMessage.userId == userId
        auditMessage.date != null
        auditMessage.entity.projectId == project.projectId
        auditMessage.entity.name == project.name
    }

    def "Audit messages without a user should be logged with an anonymous userId"() {
        setup:
        userServiceStub.getCurrentUserDetails() >> null
        def project = TestDataHelper.buildProject()

        when: "a new project is created without the user being identifiable"
        project.save(flush:true, failOnError: true)
        AuditMessage.withTransaction {
            auditService.flushMessageQueue()
        }
        def auditMessage
        AuditMessage.withNewTransaction {
            auditMessage = AuditMessage.findByProjectId(project.projectId)
        }

        then: "the event should be audited against an anonymous userId"

        auditMessage != null
        auditMessage.eventType == AuditEventType.Insert
        auditMessage.entityType == Project.class.name
        auditMessage.userId == '<anon>'
        auditMessage.date != null
        auditMessage.entity.projectId == project.projectId
        auditMessage.entity.name == project.name
    }

    def "Project permission changes should be audited against the correct project"() {
        setup:
        def operatingUser = '1234'
        def userToAddToProject = '2345'
        def projectId = 'project1'
        def projectPermission = new UserPermission(entityType:Project.class.name, entityId:projectId, userId:userToAddToProject, accessLevel:AccessLevel.admin)
        userServiceStub.getCurrentUserDetails() >> {[userId:operatingUser]}

        when:
        projectPermission.save(flush:true, failOnError: true)
        AuditMessage.withTransaction {
            auditService.flushMessageQueue()
        }
        def auditMessage
        AuditMessage.withNewTransaction {
            auditMessage = AuditMessage.findByProjectId(projectId)
        }

        then:
        auditMessage != null
        auditMessage.eventType == AuditEventType.Insert
        auditMessage.entityType == UserPermission.class.name
        auditMessage.userId == operatingUser
        auditMessage.date != null
        auditMessage.projectId == projectId
        auditMessage.entity.userId == userToAddToProject
        auditMessage.entity.accessLevel == AccessLevel.admin.name()
        auditMessage.entity.entityId == projectId
    }

    def "Project sites should be returned in the project audit messages"() {
        setup:
        userServiceStub.getCurrentUserDetails() >> {[userId:'1234']}

        Project p = new Project(projectId:'p1', name:'test')
        p.save(flush:true)

        Site s = new Site(siteId:'s1', name:'test site')
        s.projects << 'p1'
        s.save(flush:true)
        AuditMessage.withTransaction {
            auditService.flushMessageQueue()
        }

        when:
        List messages = []
        AuditMessage.withTransaction {
            messages = auditService.getAllMessagesForProject('p1')
        }

        then:
        messages.size() == 2
        messages.find {it.entityId == 'p1'} != null
        messages.find {it.entityId == 's1'} != null

    }

    def "Project sites should be returned in the project audit messages via the paginated query"() {
        setup:
        userServiceStub.getCurrentUserDetails() >> {[userId:'1234']}
        userServiceStub.getUserForUserId(_) >> [displayName:'test']

        Project p = new Project(projectId:'p1', name:'test')
        p.save(flush:true)

        Site s = new Site(siteId:'s1', name:'test site')
        s.projects << 'p1'
        s.save(flush:true)
        AuditMessage.withTransaction {
            auditService.flushMessageQueue()
        }

        when:
        Map result = null
        AuditMessage.withTransaction {
            result = auditService.getAuditMessagesForProjectPerPage('p1', 0, 100, 'date', 'desc', null)
        }

        then:
        result.count == 2
        result.data.find {it.entityId == 'p1'} != null
        result.data.find {it.entityId == 's1'} != null

    }

    def "We can search for audit messages"() {
        setup:
        userServiceStub.getCurrentUserDetails() >> {[userId:'1234']}
        userServiceStub.getUserForUserId(_) >> [displayName:'test']

        Project p = new Project(projectId:'p1', name:'test')
        p.save(flush:true)

        Site s = new Site(siteId:'s1', name:'test site')
        s.projects << 'p1'
        s.save(flush:true)
        AuditMessage.withTransaction {
            auditService.flushMessageQueue()
        }

        when:
        PagedResultList<AuditMessage> result = null
        AuditMessage.withTransaction {
            result = auditService.search([projectId:'p1'])
        }

        then:
        result.totalCount == 1
        result[0].entityId == 'p1'


        when:
        AuditMessage.withTransaction {
            result = auditService.search([entityType:'au.org.ala.ecodata.Site'])
        }

        then:
        result.totalCount == 1
        result[0].entityId == 's1'

    }

}
