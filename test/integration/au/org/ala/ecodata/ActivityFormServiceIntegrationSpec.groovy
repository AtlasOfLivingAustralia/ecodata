package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import org.springframework.beans.factory.annotation.Autowired

/**
 * Integration tests for the ActivityFormService
 */
class ActivityFormServiceIntegrationSpec extends IntegrationTestHelper {

    @Autowired
    ActivityFormService activityFormService

    def setupSpec() {
        ActivityForm.collection.remove(new BasicDBObject())
    }

    private List forms

    def setup() {
        // Create some activity forms to test with.
        forms = [[name:'test', formVersions:[2,1]], [name:'test 2', formVersions:[1]], [name:'test 3', formVersions:[1]], [name:'test 4', formVersions:[4,3,2,1]]]
        forms.each { Map form ->
            form.formVersions.each { int version ->
                createActivityForm(form.name, version, version == 4 ? PublicationStatus.DRAFT : PublicationStatus.PUBLISHED)
            }
        }
    }

    def cleanup() {
        ActivityForm.collection.remove(new BasicDBObject())
    }

    void "the service can retrieve an activity form by name and version"() {

        when:
        ActivityForm form = activityFormService.findActivityForm("test", 2)

        then:
        form.name == "test"
        form.formVersion == 2

        when:
        form = activityFormService.findActivityForm("test 4", 3)

        then:
        form.name == "test 4"
        form.formVersion == 3
    }

    void "if no version is supplied, the service will return the most recent published version"() {

        when:
        ActivityForm form = activityFormService.findActivityForm("test")

        then:
        form.name == "test"
        form.formVersion == 2

        when:
        form = activityFormService.findActivityForm("test 4")

        then:
        form.name == "test 4"
        form.formVersion == 3
    }

    void "the service can return a list of activity form names and versions"() {

        when:
        List formVersionsByName = activityFormService.activityVersionsByName()

        then:
        formVersionsByName == forms
    }

    private void createActivityForm(String name, int version, String publicationStatus) {
        ActivityForm form = new ActivityForm(name:name, formVersion: version, publicationStatus:publicationStatus, type:'Works', category: 'Testing', supportsSites:true, supportsPhotoPoints: true)
        form.save(failOnError: true, flush:true)
    }

}
