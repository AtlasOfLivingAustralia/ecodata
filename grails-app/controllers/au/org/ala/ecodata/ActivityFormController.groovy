package au.org.ala.ecodata

import au.org.ala.ws.security.RequireAuth
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
     * @return
     */
    ActivityForm get(String name, Integer formVersion) {
        respond activityFormService.findActivityForm(name, formVersion)
    }

    /**
     * Updates the activity form identified by the name and version in the payload.
     * @return
     */
    @RequireAuth(["ROLE_ADMIN"])
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

    @RequireAuth(["ROLE_ADMIN"])
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
    @RequireAuth(["ROLE_ADMIN"])
    def newDraftForm(String name) {
        respond activityFormService.newDraft(name)
    }

    /**
     * Increments the current form version to create a new draft version.
     * @param name the name of the activity form.
     * @return the new form.
     */
    @RequireAuth(["ROLE_ADMIN"])
    def publish(String name, Integer formVersion) {
        respond activityFormService.publish(name, formVersion)
    }

    /**
     * Increments the current form version to create a new draft version.
     * @param name the name of the activity form.
     * @return the new form.
     */
    @RequireAuth(["ROLE_ADMIN"])
    def unpublish(String name, Integer formVersion) {
        respond activityFormService.unpublish(name, formVersion)
    }

    @RequireAuth(["ROLE_ADMIN"])
    def findUsesOfForm(String name, Integer formVersion) {
        int count = Activity.countByTypeAndFormVersionAndStatusNotEqual(name, formVersion, Status.DELETED)
        Map result = [count:count]
        respond ([status:200], result)
    }
}
