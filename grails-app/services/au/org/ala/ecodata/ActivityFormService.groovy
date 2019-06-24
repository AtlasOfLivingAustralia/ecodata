package au.org.ala.ecodata

/**
 * Processes requests related to activity forms.
 */
class ActivityFormService {

    /**
     * Returns the activity form identified by name and formVersion.  If formVersion is not supplied, the
     * activity form with the highest version that is also published will be returned.
     * @param name the name of the form.
     * @param formVersion (optional) the version of the form.
     * @return
     */
    ActivityForm findActivityForm(String name, Integer formVersion = null) {

        ActivityForm form
        if (formVersion != null) {
            form = ActivityForm.findByNameAndFormVersionAndStatusNotEqual(name, formVersion, Status.DELETED)
        }
        else {
            List forms = ActivityForm.findAllByNameAndStatusNotEqual(name, Status.DELETED)
            form = forms.max{it.formVersion}
        }
        form
    }

    /**
     * Publishes an activity form.  This makes it available for selection by the "latest published version"
     * mechanism (findActivityForm with a null formVersion)
     * @param name the name of the form.
     * @param formVersion (optional) the version of the form.
     * @return
     */
    ActivityForm publish(String activityFormName, Integer version) {
        ActivityForm form = ActivityForm.findByNameAndFormVersion(activityFormName, version)
        if (form) {
            form.publish()
            form.save()
        }
        form
    }

    /**
     * Returns a List of Maps of the form [name:<activity name>, formVersions:[<array of available versions>]
     * Used for activity form selection in the administration interface.
     */
    List<Map> activityVersionsByName() {

        List activities = ActivityForm.where {
            status != Status.DELETED
            projections {
                property('name')
                property('formVersion')
            }
        }.list()

        Map grouped = activities.collect{[name:it[0], formVersion:it[1]]}.groupBy{it.name}
        activities = grouped.collect{k, v -> [name:k, formVersions:v.collect{it.formVersion}]}
        activities.sort{it.name}
        activities
    }

}
