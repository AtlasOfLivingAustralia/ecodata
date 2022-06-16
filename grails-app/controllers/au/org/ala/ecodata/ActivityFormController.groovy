package au.org.ala.ecodata

import au.org.ala.web.AlaSecured
import grails.converters.JSON
import groovy.json.JsonSlurper

/**
 * Responds to requests related to activity forms in ecodata.
 */
class ActivityFormController {

    static responseFormats = ['json', 'xml']

    ActivityFormService activityFormService

    /**
     * Returns the activity form identified by name and formVersion.  If formVersion is not supplied, the
     * activity form with the highest version that is also published will be returned.
     * @param name the name of the form.
     * @param formVersion (optional) the version of the form.
     * @param includeScoreInformation If true, the form dataModel will be modified to include any scores
     * referenced by the form.
     */
    ActivityForm get(String name, Integer formVersion, boolean includeScoreInformation) {
        ActivityForm form = activityFormService.findActivityForm(name, formVersion)
        if (includeScoreInformation) {
            activityFormService.addScoreInformationToFormConfiguration(form)
        }
        respond form
    }

    /**
     * Updates the activity form identified by the name and version in the payload.
     * @return
     */
    @AlaSecured("ROLE_ADMIN")
    def update() {

        // We are using JsonSlurper instead of request.JSON to avoid JSONObject.Null causing the string
        // "null" to be saved in templates (it will happen in any embedded Maps).
        def formData = new JsonSlurper().parse(request.inputStream)
        ActivityForm form = activityFormService.findActivityForm(formData.name, formData.formVersion)
        if (form) {
            bindData(form, formData, [include:ActivityForm.bindingProperties])
            activityFormService.save(form)
        }

        respond form
    }

    @AlaSecured("ROLE_ADMIN")
    def create() {
        // We are using JsonSlurper instead of request.JSON to avoid JSONObject.Null causing the string
        // "null" to be saved in templates (it will happen in any embedded Maps).
        def formData = new JsonSlurper().parse(request.inputStream)
        ActivityForm form = new ActivityForm(name:formData.name, formVersion: 1)
        if (form) {
            bindData(form, formData, [include:ActivityForm.bindingProperties])
            activityFormService.save(form)
        }

        respond form
    }

    /**
     * Increments the current form version to create a new draft version.
     * @param name the name of the activity form.
     * @return the new form.
     */
    @AlaSecured("ROLE_ADMIN")
    def newDraftForm(String name) {
        respond activityFormService.newDraft(name)
    }

    /**
     * Increments the current form version to create a new draft version.
     * @param name the name of the activity form.
     * @return the new form.
     */
    @AlaSecured("ROLE_ADMIN")
    def publish(String name, Integer formVersion) {
        respond activityFormService.publish(name, formVersion)
    }

    /**
     * Increments the current form version to create a new draft version.
     * @param name the name of the activity form.
     * @return the new form.
     */
    @AlaSecured("ROLE_ADMIN")
    def unpublish(String name, Integer formVersion) {
        respond activityFormService.unpublish(name, formVersion)
    }

    @AlaSecured("ROLE_ADMIN")
    def findUsesOfForm(String name, Integer formVersion) {
        int count = Activity.countByTypeAndFormVersionAndStatusNotEqual(name, formVersion, Status.DELETED)
        Map result = [count:count]
        respond ([status:200], result)
    }
}
