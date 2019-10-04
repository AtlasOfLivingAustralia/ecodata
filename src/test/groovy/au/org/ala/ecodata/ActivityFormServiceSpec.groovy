package au.org.ala.ecodata

import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class ActivityFormServiceSpec extends Specification implements ServiceUnitTest<ActivityFormService>, DomainUnitTest<ActivityForm> {

    MetadataService metadataService = Mock(MetadataService)
    void setup() {
        service.metadataService = metadataService

    }

    def "Activity forms cannot be saved without the mandatory fields"() {

        setup:
        ActivityForm form = new ActivityForm()

        when:
        service.save(form)

        then:
        form.hasErrors()

        when:
        form = new ActivityForm(name:'test', formVersion:1, supportsSites:true, supportsPhotoPoints: true, type:'Activity')
        service.save(form)

        then:
        form.hasErrors() == false
    }

    def "Form templates must be validated for correct index fields"() {
        setup:
        ActivityForm form = new ActivityForm(name:'test', formVersion:1, supportsSites:true, supportsPhotoPoints: true, type:'Activity')
        FormSection section = new FormSection(name:'section 1', template:[test:'value'])
        form.sections << section

        when:
        service.save(form)

        then:
        1 * metadataService.isDataModelValid(form.sections[0].template) >> [valid: true]
        form.hasErrors() == false
    }

    def "Index errors will be reported if validation fails"() {
        setup:
        ActivityForm form = new ActivityForm(name:'test', formVersion:1, supportsSites:true, supportsPhotoPoints: true, type:'Activity')
        FormSection section = new FormSection(name:'section 1', template:[test:'value'])
        form.sections << section

        when:
        service.save(form)

        then:
        1 * metadataService.isDataModelValid(form.sections[0].template) >> [valid: false, errorInIndex:['index1']]
        form.hasErrors() == true
    }
}