package au.org.ala.ecodata

import grails.test.spock.IntegrationSpec

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED

class ProjectServiceIntegrationSpec extends IntegrationSpec {

    ProjectService projectService

    def grailsApplication

    def setup() {
        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }
    }

    def "deleteProject should soft delete the project and all related records when destroy = false"() {
        setup:
        Project project = createHierarchy()

        when:
        projectService.delete(project.projectId, false)

        then:
        Project.count() == 1
        Document.count() == 6
        UserPermission.count() == 2
        ProjectActivity.count() == 2
        Activity.count() == 4
        Output.count() == 2
        Record.count() == 2
        Project.findAll().each { assert it.status == DELETED }
        Document.findAll().each { assert it.status == DELETED }
        UserPermission.findAll().each { assert it.status == DELETED }
        ProjectActivity.findAll().each { assert it.status == DELETED }
        Activity.findAll().each { assert it.status == DELETED }
        Output.findAll().each { assert it.status == DELETED }
        Record.findAll().each { assert it.status == DELETED }
    }

    def "deleteProject should hard delete the project and all related records when destroy = true"() {
        setup:
        Project project = createHierarchy()

        when:
        projectService.delete(project.projectId, true)

        then:
        Project.count() == 0
        Document.count() == 0
        UserPermission.count() == 0
        ProjectActivity.count() == 0
        Activity.count() == 0
        Output.count() == 0
        Record.count() == 0
    }

    private static createHierarchy() {
        Project project = new Project(projectId: "project1", name: "project1").save(failOnError: true, flush: true)
        new Document(documentId: "doc3", projectId: project.projectId).save(failOnError: true, flush: true)
        new Document(documentId: "doc4", projectId: project.projectId).save(failOnError: true, flush: true)
        Activity activity1 = new Activity(activityId: "act1", projectId: project.projectId).save(failOnError: true, flush: true)
        Activity activity2 = new Activity(activityId: "act2", projectId: project.projectId).save(failOnError: true, flush: true)
        new Document(documentId: "doc1", activityId: activity1.activityId).save(failOnError: true, flush: true)
        new Document(documentId: "doc2", activityId: activity2.activityId).save(failOnError: true, flush: true)
        new UserPermission(userId: "user1", accessLevel: AccessLevel.admin, entityId: project.projectId, entityType: Project.class.name).save(failOnError: true, flush: true)
        new UserPermission(userId: "user2", accessLevel: AccessLevel.admin, entityId: project.projectId, entityType: Project.class.name).save(failOnError: true, flush: true)
        ProjectActivity projectActivity1 = new ProjectActivity(projectActivityId: "proAct1", projectId: project.projectId, description: "d", name: "n", startDate: new Date(), status: ACTIVE).save(failOnError: true, flush: true)
        ProjectActivity projectActivity2 = new ProjectActivity(projectActivityId: "proAct2", projectId: project.projectId, description: "d", name: "n", startDate: new Date(), status: ACTIVE).save(failOnError: true, flush: true)
        new Document(documentId: "doc5", projectActivityId: projectActivity1.projectActivityId).save(failOnError: true, flush: true)
        new Document(documentId: "doc6", projectActivityId: projectActivity2.projectActivityId).save(failOnError: true, flush: true)
        Activity activity3 = new Activity(activityId: "act3", projectActivityId: projectActivity1.projectActivityId).save(failOnError: true, flush: true)
        Activity activity4 = new Activity(activityId: "act4", projectActivityId: projectActivity2.projectActivityId).save(failOnError: true, flush: true)
        Output output1 = new Output(outputId: "out1", activityId: activity3.activityId).save(failOnError: true, flush: true)
        Output output2 = new Output(outputId: "out2", activityId: activity4.activityId).save(failOnError: true, flush: true)
        new Record(outputId: output1.outputId).save(failOnError: true, flush: true)
        new Record(outputId: output2.outputId).save(failOnError: true, flush: true)

        project
    }
}
