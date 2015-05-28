package au.org.ala.ecodata

import org.grails.datastore.mapping.engine.event.PostInsertEvent

/**
 * Spec for the AuditService.
 */
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
        def event = new PostInsertEvent(datastore, project)
        userServiceStub.getCurrentUserDetails() >> {[userId:userId]}

        when: "a new project is created"
        auditService.logGormEvent(event)
        auditService.flushMessageQueue()

        then: "the details of the create operation should be audited correctly"
        def auditMessage = AuditMessage.findByProjectId(project.projectId)
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
        def event = new PostInsertEvent(datastore, project)

        when: "a new project is created without the user being identifiable"
        auditService.logGormEvent(event)
        auditService.flushMessageQueue()

        then: "the event should be audited against an anonymous userId"
        def auditMessage = AuditMessage.findByProjectId(project.projectId)
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
        def event = new PostInsertEvent(datastore, projectPermission)
        userServiceStub.getCurrentUserDetails() >> {[userId:operatingUser]}

        when:
        auditService.logGormEvent(event)
        auditService.flushMessageQueue()

        then:
        def auditMessage = AuditMessage.findByProjectId(projectId)
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



}
