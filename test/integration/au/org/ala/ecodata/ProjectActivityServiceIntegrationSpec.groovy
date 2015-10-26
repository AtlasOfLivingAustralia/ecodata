package au.org.ala.ecodata

import grails.test.spock.IntegrationSpec

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED


class ProjectActivityServiceIntegrationSpec extends IntegrationSpec {
    ProjectActivityService projectActivityService

    def grailsApplication

    def setup() {
        grailsApplication.domainClasses.each {
            it.clazz.collection.drop()
        }
    }

    def "delete should soft delete the project and all related records when destroy = false"() {
        setup:
        ProjectActivity activity = createHierarchy()

        when:
        projectActivityService.delete(activity.projectActivityId, false)

        then:
        Project.count() == 1
        Document.count() == 4
        ProjectActivity.count() == 1
        Activity.count() == 2
        Output.count() == 2
        Record.count() == 2
        Document.findAll().each { assert it.status == DELETED }
        Activity.findAll().each { assert it.status == DELETED }
        Output.findAll().each { assert it.status == DELETED }
        Record.findAll().each { assert it.status == DELETED }
    }

    def "delete should hard delete the project and all related records when destroy = true"() {
        setup:
        ProjectActivity activity = createHierarchy()

        when:
        projectActivityService.delete(activity.projectActivityId, true)

        then:
        Project.count() == 1
        Document.count() == 0
        Activity.count() == 0
        ProjectActivity.count() == 0
        Output.count() == 0
        Record.count() == 0
    }

    def "update should recalculate the embargoUntil date for all related Records"() {
        setup:
        ProjectActivity activity = createHierarchy()
        new Record(outputId: "UnrelatedId").save(failOnError: true, flush: true)


        when:
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 2)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        projectActivityService.update([embargoOption: EmbargoOption.DATE, embargoUntil: cal.getTime()], activity.projectActivityId)

        then:
        int count = 0
        Record.all.each {
            if (it.projectActivityId == activity.projectActivityId) {
                assert it.embargoUntil == cal.getTime()
                count++
            }
        }
        assert count == 2
    }

    def "update should clear the existing embargoUntil date for all related Records when the embargoOption is NONE"() {
        setup:
        ProjectActivity activity = createHierarchy()
        new Record(outputId: "UnrelatedId").save(failOnError: true, flush: true)
        Record.all.each {
            it.embargoUntil = new Date()
            it.save(flush: true, failOnError: true)
        }

        when:
        projectActivityService.update([embargoOption: EmbargoOption.NONE], activity.projectActivityId)

        then:
        int count = 0
        Record.all.each {
            if (it.projectActivityId == activity.projectActivityId) {
                assert it.embargoUntil == null
                count++
            }
        }
        assert count == 2
    }

    private static createHierarchy() {
        Project project = new Project(projectId: "project1", name: "project1").save(failOnError: true, flush: true)

        ProjectActivity projectActivity1 = new ProjectActivity(projectActivityId: "proAct1", projectId: project.projectId, description: "d", name: "n", startDate: new Date(), status: ACTIVE).save(failOnError: true, flush: true)
        new Document(documentId: "doc5", projectActivityId: projectActivity1.projectActivityId).save(failOnError: true, flush: true)
        new Document(documentId: "doc6", projectActivityId: projectActivity1.projectActivityId).save(failOnError: true, flush: true)
        Activity activity1 = new Activity(activityId: "act3", projectActivityId: projectActivity1.projectActivityId).save(failOnError: true, flush: true)
        Activity activity2 = new Activity(activityId: "act4", projectActivityId: projectActivity1.projectActivityId).save(failOnError: true, flush: true)
        new Document(documentId: "doc1", activityId: activity1.activityId).save(failOnError: true, flush: true)
        new Document(documentId: "doc2", activityId: activity2.activityId).save(failOnError: true, flush: true)
        Output output1 = new Output(outputId: "out1", activityId: activity1.activityId).save(failOnError: true, flush: true)
        Output output2 = new Output(outputId: "out2", activityId: activity2.activityId).save(failOnError: true, flush: true)
        new Record(outputId: output1.outputId, projectActivityId: projectActivity1.projectActivityId).save(failOnError: true, flush: true)
        new Record(outputId: output2.outputId, projectActivityId: projectActivity1.projectActivityId).save(failOnError: true, flush: true)

        projectActivity1
    }
}
