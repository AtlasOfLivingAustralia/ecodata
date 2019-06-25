package au.org.ala.ecodata.data_migration

import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.FormSection
import au.org.ala.ecodata.PublicationStatus
import au.org.ala.ecodata.Status
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Takes the contents of the activities-model.json and associated form templates dataModel.json and
 * inserts them into the ActivityForm collection.
 */
class ActivityFormMigrator {

    static Log log = LogFactory.getLog(ActivityFormMigrator.class)
    private String basePath
    ActivityFormMigrator(String basePath) {
        this.basePath = basePath
    }

    Map loadActivitiesModel() {
        File modelFile = new File(basePath, "activities-model.json")
        if (!modelFile.exists()) {
            throw new IllegalArgumentException("No activities-model.json at ${modelFile.path}")
        }
        Map activitiesModel = null
        modelFile.withInputStream {
            activitiesModel = new groovy.json.JsonSlurper().parse(it)
        }
        activitiesModel
    }

    Map loadOutputModelTemplate(String templateDir) {

        String template = loadOutputModelTemplateToString(templateDir)

        // Using JsonSlurper instead of JSON.parse to avoid nulls being stored as the String "null" in the database.
        Map result = [:]
        try {
            result = template ? new groovy.json.JsonSlurper().parseText(template) : [:]
        }
        catch (Exception e) {
            log.error("Failed to parse template: ${templateDir}", e)
        }
        result
    }

    String loadOutputModelTemplateToString(String templateDir) {
        File template = new File(new File(basePath, templateDir), 'dataModel.json')
        def templateAsStream = null
        if (template.exists()) {
            templateAsStream = new FileInputStream(template)
        }
        templateAsStream ? templateAsStream.text : null
    }

    /** Parses the activities-model.json and moves all of the data into the ActivityForm collection */
    void migrateActivitiesModel() {
        Map activitiesModel = loadActivitiesModel()
        activitiesModel.activities.each { Map activity ->

            ActivityForm form = new ActivityForm(
                    name:activity.name,
                    type:activity.type,
                    status:activity.status == Status.DELETED ? Status.DELETED : Status.ACTIVE,
                    category: activity.category,
                    gmsId: activity.gmsId,
                    minOptionalSectionsCompleted: activity.minOptionalSectionsCompleted,
                    supportsSites: activity.supportsSites ?: false,
                    supportsPhotoPoints: activity.supportsPhotoPoints ?: false,
                    publicationStatus: PublicationStatus.PUBLISHED
            )

            activity.outputs.each { outputName ->

                Map config = activity.outputConfig.find{it.outputName == outputName}
                Map outputDef = activitiesModel.outputs.find{it.name == outputName}


                if (!outputDef) {
                    println "Cannot find output ${outputName} for activity ${activity.name}"
                }
                else {
                    Map template = loadOutputModelTemplate(outputDef.template)
                    if (!template) {
                        println "No template found: ${activity.name} ${outputName} ${outputDef.template}"
                    }
                    FormSection section = new FormSection(
                            name:outputName,
                            optional:config.optional,
                            collapsedByDefault: config.collapsedByDefault,
                            optionalQuestionText: config.optionalQuestionText,
                            title: outputDef.title,
                            template:template,
                            templateName: outputDef.template
                    )

                    form.sections << section
                }
            }

            form.save()

            if (form.hasErrors()) {
                log.error("Error processing: ${activity.name}")
                log.error(form.errors.toString())

            }
        }

    }
}
