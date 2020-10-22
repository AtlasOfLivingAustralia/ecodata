package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import org.bson.types.ObjectId
import org.hsqldb.rights.User
import spock.lang.Specification

class AuditControllerSpec extends Specification implements ControllerUnitTest<AuditController>, DataTest {

    AuditService auditService = Mock(AuditService)
    CommonService commonService = Mock(CommonService)
    UserService userService = Mock(UserService)

    Class[] getDomainClassesToMock() {
        [AuditMessage, Project, Organisation]
    }

    def setup() {
        controller.auditService = auditService
        controller.commonService = commonService
        controller.commonService.grailsApplication = grailsApplication
        controller.userService = userService
    }

    def cleanup() {
    }

    void "Get all by entity id"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1').save(flush:true, failOnError: true)

        when:
        params.entityId = '1'
        def result = controller.entityAuditMessageTableFragment()

        then:
        response.status == HttpStatus.SC_OK
        result.auditMessages.size() == 1
        result.auditMessages[0] == auditMessage
    }

    void "Get audit message by id"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1').save(flush:true, failOnError: true)

        when:
        params.id = '1'
        def result = controller.messageEntityDetails()

        then:
        response.status == HttpStatus.SC_OK
    }

    void "Find all project by name"() {
        setup:
        Project p = new Project(projectId:'1', name:"Project 1")
        p.save()

        when:
        params.q = 'Pro'
        def result = controller.findProjectResultsTableFragment()

        then:
        response.status == HttpStatus.SC_OK
        result.projectList.size() == 1
        result.projectList[0] == p
    }

    void "Audit project"() {
        setup:
        Project p = new Project(projectId:'1', name:"Project 1")
        p.save()
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1', projectId: '1').save(flush:true, failOnError: true)

        when:
        params.projectId = '1'
        def result = controller.auditProject()

        then:
        response.status == HttpStatus.SC_OK
        result.projectInstance == p
        result.auditMessages[0] == auditMessage
    }

    void "Audit project - invalid project"() {
        setup:
        Project p = new Project(projectId:'1', name:"Project 1")
        p.save()
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1').save(flush:true, failOnError: true)

        when:
        params.projectId = '2'
        def result = controller.auditProject()

        then:
        response.status != HttpStatus.SC_OK
    }

    void "Get recent edits"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1', projectId: '1').save(flush:true, failOnError: true)

        when:
        def result = controller.getRecentEdits()

        then:
        response.status == HttpStatus.SC_OK
        response.getJson().totalRecords == 1
        response.getJson().results[0].entityId == '1'
    }

    void "Get recent edits for user"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1', projectId: '1').save(flush:true, failOnError: true)

        when:
        params.id = '1'
        def result = controller.getRecentEditsForUserId()

        then:
        1 * userService.getUserForUserId('1') >> true
        response.status == HttpStatus.SC_OK
        response.getJson()[0].entityId == '1'
    }

    void "Get recent edits for user - invalid userid"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1').save(flush:true, failOnError: true)

        when:
        params.id = '2'
        def result = controller.getRecentEditsForUserId()

        then:
        1 * userService.getUserForUserId('2') >> null
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'User not found for userId: 2'
    }

    void "Get Audit messages for project"() {
        setup:
        Project p = new Project(projectId:'1', name:"Project 1")
        p.save()
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1', projectId: '1',
                entity: [projectId:'1', name:"Project 1"]).save(flush:true, failOnError: true)

        when:
        params.projectId = '1'
        def result = controller.ajaxGetAuditMessagesForProject()

        then:
        1 * auditService.getAllMessagesForProject('1') >> auditMessage
        response.status == HttpStatus.SC_OK
        response.getJson().success == true
        response.getJson().messages.entityId == '1'
    }

    void "Get Audit messages for project - invalid project"() {
        setup:
        Project p = new Project(projectId:'1', name:"Project 1")
        p.save()
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1').save(flush:true, failOnError: true)

        when:
        params.projectId = '2'
        def result = controller.ajaxGetAuditMessagesForProject()

        then:
        response.status == HttpStatus.SC_OK
        response.getJson().success == false
        response.getJson().message == 'Invalid project id 2'
    }


    void "Get Audit messages for organisation"() {
        setup:
        Organisation org = new Organisation(organisationId:'1', name:"Org 1")
        org.save()
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Organisation.class.name, entityId: '1',
                entity: [organisationId:'1', name:"Org 1"]).save(flush:true, failOnError: true)

        when:
        params.organisationId = '1'
        def result = controller.getAuditMessagesForOrganisation()

        then:
        1 * auditService.getAuditMessagesForOrganisation('1') >> [auditMessage]
        response.status == HttpStatus.SC_OK
        response.getJson().success == true
        response.getJson().messages[0].entityId == '1'
    }

    void "Get Audit messages for organisation - invalid organisation"() {
        setup:
        Organisation org = new Organisation(organisationId:'1', name:"Org 1")
        org.save()
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Organisation.class.name, entityId: '1',
                entity: [organisationId:'1', name:"Org 1"]).save(flush:true, failOnError: true)
        when:
        params.organisationId = '2'
        def result = controller.getAuditMessagesForOrganisation()

        then:
        response.status == HttpStatus.SC_OK
        response.getJson().success == false
        response.getJson().message == 'Invalid organisation id 2'
    }

    void "Get Audit messages for settings"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Organisation.class.name, entityId: '1',
                entity: [organisationId:'1', name:"Org 1"]).save(flush:true, failOnError: true)

        when:
        params.projectId = '1'
        def result = controller.getAuditMessagesForSettings()

        then:
        1 * auditService.getAuditMessagesForSettings('') >> [auditMessage]
        response.status == HttpStatus.SC_OK
        response.getJson().success == true
        response.getJson().messages[0].entityId == '1'
    }

    void "Get Auto Compare AuditMessage"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Organisation.class.name, entityId: '1',
                entity: [organisationId:'1', name:"Org 1"]).save(flush:true, failOnError: true)

        when:
        params.auditId = '1'
        controller.getAutoCompareAuditMessage()

        then:
        1 * auditService.getAutoCompareAuditMessage('1') >> auditMessage
        1 * commonService.toBareMap(auditMessage) >> [:]
        response.status == HttpStatus.SC_OK
        response.getJson().message.size() == 0
    }

    void "Get Audit message"() {
        setup:
        ObjectId id = new ObjectId()
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1', projectId: '1', id: id).save(flush:true, failOnError: true)

        when:
        params.id = id
        controller.ajaxGetAuditMessage()

        then:
        response.status == HttpStatus.SC_OK
    }

    void "Get Audit message - invalid id"() {
        setup:
        ObjectId id = new ObjectId()
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1', id: id).save(flush:true, failOnError: true)

        when:
        params.id = id
        controller.ajaxGetAuditMessage()

        then:
        response.status == HttpStatus.SC_OK
        response.getJson().success == false
        response.getJson().errorMessage == 'Could not find audit message with specified id!'
    }

    void "Get user details"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1', projectId: '1').save(flush:true, failOnError: true)

        when:
        params.id = '1'
        controller.ajaxGetUserDetails()

        then:
        1 * userService.getUserForUserId('1') >> true
        response.status == HttpStatus.SC_OK
        response.getJson().success == true
        response.getJson().user != null
    }

    void "Get user details -  invalid id"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1').save(flush:true, failOnError: true)

        when:
        params.id = '2'
        controller.ajaxGetUserDetails()

        then:

        1 * userService.getUserForUserId('2') >> null
        response.status == HttpStatus.SC_OK
        response.getJson().success == false
        response.getJson().errorMessage == "User not found!"
    }

    void "Get user details -  without id"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1').save(flush:true, failOnError: true)

        when:
        controller.ajaxGetUserDetails()

        then:
        response.status == HttpStatus.SC_OK
        response.getJson().success == false
        response.getJson().errorMessage == "You must supply a userId"
    }

    void "Get auditMessages For Project Per Page"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1', projectId: '1').save(flush:true, failOnError: true)

        when:
        params.id = '1'
        controller.getAuditMessagesForProjectPerPage()

        then:
        1 * auditService.getAuditMessagesForProjectPerPage('1',0,10,'date','desc',null) >> [count: 1, data: auditMessage ]
        response.status == HttpStatus.SC_OK
        response.getJson().recordsTotal == 1
        response.getJson().recordsFiltered == 1
        response.getJson().data.entityId == "1"
    }

    void "Get auditMessages For Project Per Page -  without id"() {
        setup:
        AuditMessage auditMessage = new AuditMessage(date: new Date(), userId: '1', eventType: 'Insert', entityType: Project.class.name, entityId: '1', projectId: '1').save(flush:true, failOnError: true)

        when:
        controller.getAuditMessagesForProjectPerPage()

        then:

        response.status == HttpStatus.SC_BAD_REQUEST
        response.errorMessage == "Project id is required"
    }
}
