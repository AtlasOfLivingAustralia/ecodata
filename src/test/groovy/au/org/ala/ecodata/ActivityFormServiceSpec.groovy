package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import groovy.json.JsonSlurper
import spock.lang.Specification

class ActivityFormServiceSpec extends Specification implements ServiceUnitTest<ActivityFormService>, DataTest {

    MetadataService metadataService = Mock(MetadataService)

    void setup() {
        service.metadataService = metadataService
        mockDomain(Score)
        mockDomain(ActivityForm)

    }

    def "Activity forms cannot be saved without the mandatory fields"() {

        setup:
        ActivityForm form = new ActivityForm()

        when:
        service.save(form)

        then:
        form.hasErrors()

        when:
        form = new ActivityForm(name: 'test', formVersion: 1, supportsSites: true, supportsPhotoPoints: true, type: 'Activity')
        service.save(form)

        then:
        form.hasErrors() == false
    }

    def "Form templates must be validated for correct index fields"() {
        setup:
        ActivityForm form = new ActivityForm(name: 'test', formVersion: 1, supportsSites: true, supportsPhotoPoints: true, type: 'Activity')
        FormSection section = new FormSection(name: 'section 1', template: [test: 'value'])
        form.sections << section

        when:
        service.save(form)

        then:
        1 * metadataService.isDataModelValid(form.sections[0].template) >> [valid: true]
        form.hasErrors() == false
    }

    def "Index errors will be reported if validation fails"() {
        setup:
        ActivityForm form = new ActivityForm(name: 'test', formVersion: 1, supportsSites: true, supportsPhotoPoints: true, type: 'Activity')
        FormSection section = new FormSection(name: 'section 1', template: [test: 'value'])
        form.sections << section

        when:
        service.save(form)

        then:
        1 * metadataService.isDataModelValid(form.sections[0].template) >> [valid: false, errorInIndex: ['index1']]
        form.hasErrors() == true
    }

    def "Find activity form by name and formVersion where for is active"() {
        setup:
        ActivityForm form = new ActivityForm(name: 'test', formVersion: 1, status: Status.ACTIVE, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.findActivityForm("test", 1)

        then:
        formRetrieved.name == "test"
        formRetrieved.formVersion == 1
    }

    def "Find activity form by name and formVersion where form is completed"() {
        setup:
        ActivityForm form = new ActivityForm(name: 'test', formVersion: 1, status: Status.COMPLETED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.findActivityForm("test", 1)

        then:
        formRetrieved.name == "test"
        formRetrieved.formVersion == 1
    }

    def "Find activity form by name and formVersion where form is deleted"() {
        setup:
        ActivityForm form = new ActivityForm(name: 'test', formVersion: 1, status: Status.DELETED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.findActivityForm("test", 1)

        then:
        formRetrieved == null
    }

    def "Find activity form by name where status is active and publicationStatus is draft"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.DRAFT, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.findActivityForm("test")

        then:
        formRetrieved == null
    }

    def "Find activity form by name where status is active and publicationStatus is published"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.PUBLISHED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.findActivityForm("test")

        then:
        formRetrieved.name == "test"
    }

    def "Find activity form by name where status is deleted and publicationStatus is published"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.DELETED, publicationStatus: PublicationStatus.PUBLISHED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.findActivityForm("test")

        then:
        formRetrieved == null
    }

    def "Publish activity form"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.DRAFT, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.publish("test", 1)

        then:
        formRetrieved.name == "test"
        formRetrieved.formVersion == 1
        formRetrieved.publicationStatus == PublicationStatus.PUBLISHED
    }

    def "Publish activity form - invalid name"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.DRAFT, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.publish("testInvalid", 1)

        then:
        formRetrieved == null
    }

    def "Publish activity form - invalid version"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.DRAFT, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.publish("test", 2)

        then:
        formRetrieved == null
    }

    def "Unpublish activity form"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.PUBLISHED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.unpublish("test", 1)

        then:
        formRetrieved.name == "test"
        formRetrieved.formVersion == 1
        formRetrieved.publicationStatus == PublicationStatus.DRAFT
    }

    def "Unpublish activity form - invalid name"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.PUBLISHED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.unpublish("testInvalid", 1)

        then:
        formRetrieved == null
    }

    def "Unpublish activity form - invalid version"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.PUBLISHED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.unpublish("test", 2)

        then:
        formRetrieved == null
    }

    def "Create new draft - invalid name"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.PUBLISHED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.newDraft("testInvalid")

        then:
        formRetrieved == null
    }

    def "Create new draft from a deleted form"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.DELETED, publicationStatus: PublicationStatus.PUBLISHED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.newDraft("test")

        then:
        formRetrieved == null
    }

    def "Create new draft from a draft form"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.DRAFT, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.newDraft("test")

        then:
        formRetrieved.errors.hasErrors()
        formRetrieved.errors.allErrors[0].code == 'activityForm.latestVersionIsInDraft'
    }

    def "Create new draft from a published form"() {
        setup:
        ActivityForm form = new ActivityForm(
                name: 'test', formVersion: 1, status: Status.ACTIVE, publicationStatus: PublicationStatus.PUBLISHED, type: 'Activity')
        form.save(flush: true, failOnError: true)

        when:
        ActivityForm formRetrieved = service.newDraft("test")

        then:
        !formRetrieved.errors.hasErrors()
        formRetrieved.name == form.name
        formRetrieved.type == form.type
        formRetrieved.supportsSites == form.supportsSites
        formRetrieved.supportsPhotoPoints == form.supportsPhotoPoints
        formRetrieved.category == form.category
        formRetrieved.formVersion == form.formVersion + 1
        formRetrieved.gmsId == form.gmsId
        formRetrieved.minOptionalSectionsCompleted == form.minOptionalSectionsCompleted
        formRetrieved.sections == form.sections
    }

    def "Get activity form list"() {
        setup:
        ActivityForm form1 = new ActivityForm(name: 'test', formVersion: 1, status: Status.ACTIVE, type: 'Activity')
        form1.save(flush: true, failOnError: true)
        ActivityForm form2 = new ActivityForm(name: 'test', formVersion: 2, status: Status.DELETED, type: 'Activity')
        form2.save(flush: true, failOnError: true)
        ActivityForm form3 = new ActivityForm(name: 'abc', formVersion: 1, status: Status.ACTIVE, type: 'Activity')
        form3.save(flush: true, failOnError: true)
        ActivityForm form4 = new ActivityForm(name: 'abc', formVersion: 2, status: Status.ACTIVE, type: 'Activity')
        form4.save(flush: true, failOnError: true)

        when:
        List<Map> activities = service.activityVersionsByName()

        then:
        activities.size() == 2
        //abc form activity should come first since the list is ordered asc by name
        activities[0].name == 'abc'
        //version 2 should come first since the list is ordered desc by form version
        activities[0].formVersions[0] == 2
        activities[0].formVersions[1] == 1
        activities[1].name == 'test'
        //version 2 should be retrieved since the form is in deleted status
        activities[1].formVersions[0] == 1
    }

    def "The activityFormService can find forms referenced by Score configurations"() {
        when:
        Map scoreConfiguration = new JsonSlurper().parseText(exampleConfig)
        Map scoreToFormMapping = service.referencedFormSectionProperties(scoreConfiguration)

        then:
        scoreToFormMapping.size() == 3
        scoreToFormMapping == [
                "RLP - Output Report Adjustment":[
                        "adjustments.scoreId":[[property:"data.adjustments.scoreId", type:"filter", "filterValue":"a516c78d-740f-463b-a1ce-5b02b8c82dd3"]],
                        "adjustments.adjustment":[[property:"data.adjustments.adjustment", type:"SUM"]]],
                "RLP - Weed treatment":[
                        "weedTreatmentSites.initialOrFollowup":[[property:"data.weedTreatmentSites.initialOrFollowup", type:"filter", filterValue:"Initial"]],
                        "weedTreatmentSites.areaTreatedHa":[[property:"data.weedTreatmentSites.areaTreatedHa", type:"SUM"]]
                ],
                "Weed treatment":[
                        "weedTreatmentSites.initialOrFollowup":[[property:"data.weedTreatmentSites.initialOrFollowup", type:"filter", filterValue:"Initial"]],
                        "weedTreatmentSites.areaTreatedHa":[[property:"data.weedTreatmentSites.areaTreatedHa", type:"SUM"]]
                ]
        ]
    }

    private String exampleConfig = """ 
    {
        "label": "Area (ha) treated for weeds - initial",
        "childAggregations": [
            {
                "filter": {
                    "filterValue": "RLP - Output Report Adjustment",
                    "type": "filter",
                    "property": "name"
                },
                "childAggregations": [
                    {
                        "filter": {
                            "property": "data.adjustments.scoreId",
                            "type": "filter",
                            "filterValue": "a516c78d-740f-463b-a1ce-5b02b8c82dd3"
                        },
                        "childAggregations": [
                            {
                                "property": "data.adjustments.adjustment",
                                "type": "SUM"
                            }
                        ]   
                    }
                ]
            },
            {
                "filter": {
                    "filterValue": "RLP - Weed treatment",
                    "property": "name",
                    "type": "filter"
                },
                "childAggregations": [
                    {
                        "filter": {
                            "property": "data.weedTreatmentSites.initialOrFollowup",
                            "type": "filter",
                            "filterValue": "Initial"
                        },
                        "childAggregations": [
                            {
                                "property": "data.weedTreatmentSites.areaTreatedHa",
                                "type": "SUM"
                            }
                    ]
                    }
                ]
            },
            {
                "filter": {
                    "filterValue": "Weed treatment",
                    "property": "name",
                    "type": "filter"
                },
                "childAggregations": [
                    {
                        "filter": {
                            "property": "data.weedTreatmentSites.initialOrFollowup",
                            "type": "filter",
                            "filterValue": "Initial"
                        },
                        "childAggregations": [
                            {
                                "property": "data.weedTreatmentSites.areaTreatedHa",
                                "type": "SUM"
                            }
                        ]
                    }
                ]
            }
        ]
        }
    }
"""

}