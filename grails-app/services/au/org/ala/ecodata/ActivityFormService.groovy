package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputMetadata

/**
 * Processes requests related to activity forms.
 */
class ActivityFormService {

    private String INVALID_INDEX_KEY = 'activityForm.invalidIndex'
    MetadataService metadataService

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
            List forms = ActivityForm.findAllByNameAndPublicationStatusAndStatusNotEqual(name, PublicationStatus.PUBLISHED, Status.DELETED)
            form = forms.max{it.formVersion}
        }
        addScoreInformationToFormTemplates(form)
        form
    }

    /** Returns a list of all versions of an ActivityForm regardless of publication status. */
    ActivityForm[] findVersionedActivityForm(String name) {
        ActivityForm[] forms = ActivityForm.findAllByNameAndStatusNotEqual(name, PublicationStatus.PUBLISHED, Status.DELETED)
        forms
    }

    /**
     * Publishes an activity form.  This makes it available for selection by the "latest published version"
     * mechanism (findActivityForm with a null formVersion)
     * @param name the name of the form.
     * @param formVersion the version of the form.
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
     * Un-publishes an activity form (returns the publicationStatus to DRAFT).
     * This makes it unavailable for selection by the "latest published version"
     * mechanism (findActivityForm with a null formVersion)
     * @param name the name of the form.
     * @param formVersion the version of the form.
     * @return
     */
    ActivityForm unpublish(String activityFormName, Integer version) {
        ActivityForm form = ActivityForm.findByNameAndFormVersion(activityFormName, version)
        if (form) {
            form.unpublish()
            form.save()
        }
        form
    }

    ActivityForm newDraft(String activityFormName) {
        ActivityForm form = ActivityForm.findAllByNameAndStatusNotEqual(activityFormName, Status.DELETED).max{it.formVersion}
        if (form) {

            if (form.isPublished()) {
                ActivityForm newForm = new ActivityForm(
                        name: form.name,
                        type: form.type,
                        supportsSites: form.supportsSites,
                        supportsPhotoPoints: form.supportsPhotoPoints,
                        category: form.category,
                        formVersion: form.formVersion + 1,
                        gmsId: form.gmsId,
                        minOptionalSectionsCompleted: form.minOptionalSectionsCompleted,
                        sections: form.sections
                )
                newForm.save()
                form = newForm
            } else {
                form.errors.reject("activityForm.latestVersionIsInDraft")
            }
        }
        form
    }

    /**
     * Validates and saves an ActivityForm.
     */
    ActivityForm save(ActivityForm activityForm) {
        activityForm.sections.each { FormSection section ->
            Map validationResult = metadataService.isDataModelValid(section.template)
            if (!validationResult.valid) {
                activityForm.errors.reject(INVALID_INDEX_KEY, [section.name, validationResult.errorInIndex.join(',')] as Object[], "Invalid indicies in template ${section.name}")
            }
        }
        if (!activityForm.hasErrors()) {
            activityForm.save()
        }
        activityForm
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
            order('name', 'asc')
            order('formVersion', 'desc')
        }.list()

        Map grouped = activities.collect{[name:it[0], formVersion:it[1]]}.groupBy{it.name}
        activities = grouped.collect{k, v -> [name:k, formVersions:v.collect{it.formVersion}]}
        // The criteria query sort doesn't seem to be working, but the list isn't that long anyway.
        activities.sort{it.name}
        activities.each{ it.formVersions.sort{-it} }
        activities
    }

    List findScoresThatReferenceForm(ActivityForm form) {
        List scores = []
        Score.findAll().each { Score score ->
            Map referencedFormSections = referencedFormSectionProperties(score.configuration)
            form.sections.each { FormSection section ->
                Map propertiesUsedInScore = referencedFormSections[section.name]
                if (propertiesUsedInScore) {
                    scores << score
                }
            }
        }
        scores
    }

    void addScoreInformationToFormTemplates(ActivityForm form) {
        Score.findAll().each { Score score ->
            Map referencedFormSections = referencedFormSectionProperties(score.configuration)
            form.sections.each { FormSection section ->
                Map propertiesUsedInScore = referencedFormSections[section.name]
                if (propertiesUsedInScore) {
                    mergeScoreIntoTemplate(section.template, propertiesUsedInScore, score)
                }
            }
        }
    }

    private Map mergeScoreIntoTemplate(Map template, Map config, Score score) {

        OutputMetadata metadata = new OutputMetadata(template)
        metadata.dataModelIterator { String path, Map node ->
            if (config[path]) {
                if (!node.scores) {
                    node.scores = []
                }
                node.scores << [scoreId: score.scoreId, label: score.label, config:config[path]]
            }
        }
    }


    Map<String, List> referencedFormSectionProperties(Map scoreConfiguration) {
        referencedPropertiesByFormSection([scoreConfiguration])
    }

    private Map<String, List> referencedPropertiesByFormSection(List<Map> configurations) {
        Map result = [:]
        configurations.each { Map config ->
            // Almost all scores will filter on the output name to avoid having to process
            // every output
            if (configurationMatchesFormSection(config)) {
                // Found a section!
                result[config.filter?.filterValue] = findPropertiesReferencedInSection(config.childAggregations)
            }
            else if (config.childAggregations) {
                result.putAll(referencedPropertiesByFormSection(config.childAggregations))
            }
        }
        result
    }

    private boolean configurationMatchesFormSection(Map config) {
        config.filter?.property == 'name' && config.filter?.filterValue
    }

    private Map findPropertiesReferencedInSection(List<Map> scoreConfigSection) {
        Map<String, List> propertyNameToScore = [:].withDefault{[]}
        scoreConfigSection?.each {
            processProperty(propertyNameToScore, it.filter)
            processProperty(propertyNameToScore, it)

            propertyNameToScore.putAll(findPropertiesReferencedInSection(it.childAggregations))
        }
        propertyNameToScore
    }

    private void processProperty(Map target, Map config) {
        if (config?.property) {
            String prop = filterPropertyName(config.property)
            target[prop] << config
        }
    }

    private static String filterPropertyName(String property) {
        String prefixToRemove = 'data.'
        if (property.startsWith(prefixToRemove)) {
            property = property.substring(prefixToRemove.size())
        }
        property
    }
}
