package au.org.ala.ecodata

import au.ala.org.ws.security.SkipApiKeyCheck
import au.org.ala.web.AlaSecured
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
/**
 * Responds to requests related to activity forms in ecodata.
 */
@au.ala.org.ws.security.RequireApiKey(scopes=["ecodata/read"])
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
     * Returns ActivityForms that match the supplied search criteria
     */
    List<ActivityForm> search() {
        Map searchCriteria = request.JSON
        if (!searchCriteria) {
            respond ([status:HttpStatus.SC_BAD_REQUEST], [message:"At least one criteria must be supplied"])
            return
        }
        Map options = null
        if (searchCriteria.options) {
            options = searchCriteria.remove('options')
        }
        respond activityFormService.search(searchCriteria, options)
    }

    /**
     * Updates the activity form identified by the name and version in the payload.
     * @return
     */
    @AlaSecured(["ROLE_ADMIN"])
    @SkipApiKeyCheck
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

    @AlaSecured(["ROLE_ADMIN"])
    @SkipApiKeyCheck
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
    @AlaSecured(["ROLE_ADMIN"])
    @SkipApiKeyCheck
    def newDraftForm(String name) {
        respond activityFormService.newDraft(name)
    }

    /**
     * Increments the current form version to create a new draft version.
     * @param name the name of the activity form.
     * @return the new form.
     */
    @AlaSecured(["ROLE_ADMIN"])
    @SkipApiKeyCheck
    def publish(String name, Integer formVersion) {
        respond activityFormService.publish(name, formVersion)
    }

    /**
     * Increments the current form version to create a new draft version.
     * @param name the name of the activity form.
     * @return the new form.
     */
    @AlaSecured(["ROLE_ADMIN"])
    @SkipApiKeyCheck
    def unpublish(String name, Integer formVersion) {
        respond activityFormService.unpublish(name, formVersion)
    }

    @AlaSecured(["ROLE_ADMIN"])
    @SkipApiKeyCheck
    def findUsesOfForm(String name, Integer formVersion) {
        int count = Activity.countByTypeAndFormVersionAndStatusNotEqual(name, formVersion, Status.DELETED)
        Map result = [count:count]
        respond ([status:200], result)
    }
}
