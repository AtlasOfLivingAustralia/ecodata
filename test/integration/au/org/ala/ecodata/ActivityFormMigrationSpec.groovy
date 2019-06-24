package au.org.ala.ecodata

import com.mongodb.BasicDBObject
import grails.converters.JSON
import org.springframework.beans.factory.annotation.Autowired

/**
 * Dual purpose - actually performs the data migration and also ensures the existing API returns the same
 * data as it did before the migration.
 */
class ActivityFormMigrationSpec extends IntegrationTestHelper {

    @Autowired
    MetadataService metadataService

    def setupSpec() {
        ActivityForm.collection.remove(new BasicDBObject())
        // Migrate the activities-model into the database
        migrateActivitiesModel()
    }

    def setup() {
        metadataService.cacheService.clear()
    }

    def cleanupSpec() {
        // The collection is deliberately left in the database at the end of the test so we can use it to
        // dump and load into our target environments.
        //ActivityForm.collection.remove(new BasicDBObject())
    }

    private Map activitiesModel() {
        String filename = "/resources/models/activities-model.json"
        Map activitiesModel = JSON.parse(getClass().getResourceAsStream(filename).text)

        activitiesModel
    }

    private def outputModelTemplate(String templateDir) {
        String filename = "/resources/models/" +templateDir+  '/dataModel.json'
        def templateAsStream = getClass().getResourceAsStream(filename)

        // Using JsonSlurper instead of JSON.parse to avoid nulls being stored as the String "null" in the database.
        templateAsStream ? new groovy.json.JsonSlurper().parseText(templateAsStream.text) : [:]
    }

    private def outputModelTemplateAsJSON(String templateDir) {
        String filename = "/resources/models/" +templateDir+  '/dataModel.json'
        def templateAsStream = getClass().getResourceAsStream(filename)
        templateAsStream ? JSON.parse(templateAsStream.text) : [:]
    }

    /** Parses the activities-model.json and moves all of the data into the ActivityForm collection */
    private void migrateActivitiesModel() {

        Map activitiesModel = activitiesModel()

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
                    Map template = outputModelTemplate(outputDef.template)
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
                println "Error processing: ${activity.name}"
                println form.errors

                throw new RuntimeException(form.errors as String)
            }
        }

    }

    def "the migration preserves the existing activitiesModel() API"() {

        setup:
        Map originalActivitiesModel = activitiesModel()

        when:
        Map model = metadataService.activitiesModel()

        List<String> outputNames = originalActivitiesModel.outputs.collect{it.name}
        // There are 18 outputs that are in the outputs array which aren't referenced by an activity which
        // means they are unused.  These have not been migrated.
        List surplusOutputs = outputNames.findAll{!originalActivitiesModel.activities.find{Map act ->
            act.outputs.find{String outputName -> outputName == it}
        }}

        then: "Every activity in the activities array of the model returns the same data as before the migration"
        model.activities.size() == originalActivitiesModel.activities.size()
        activitiesEqual(model.activities, originalActivitiesModel.activities)

        and: "Every output in the outputs array of the model returns the same data as before the migration"
        model.outputs.size() == (originalActivitiesModel.outputs.size() - surplusOutputs.size())
        outputsEqual(model.outputs, originalActivitiesModel.outputs)

    }

    def "the migration preserves the existing getOutputDataModel() API"() {
        when:
        def originalActivitiesModel = activitiesModel()

        then:
        originalActivitiesModel.outputs.each { def output ->
            def expectedTemplate = outputModelTemplateAsJSON(output.template)
            def template = metadataService.getOutputDataModel(output.template)

            boolean unmatchedOutput = false
            if (!template) {
                // Ensure the template wasn't from a missing or deleted activity.
                def matchingActivity = originalActivitiesModel.activities.find{ Map activity ->
                    activity.status != Status.DELETED && activity.outputs.find{it == output.name}
                }

                if (!matchingActivity) {
                    println "Ignoring template: ${output.template} from output ${output.name} which isn't associated with an activity"
                    unmatchedOutput = true
                }
            }
            if (!unmatchedOutput) {
                println "${output.template}"
                templatesEqual(template, expectedTemplate)
            }

        }
    }

    private void activitiesEqual(List activities, List originalActivities) {
        activities.sort {it.name}
        originalActivities.sort {it.name}

        activities.eachWithIndex { Map activity, int i ->
            def originalActivity = originalActivities[i]

            activityEqual(activity, originalActivity)
        }
    }

    private void activityEqual(Map activity, Map originalActivity) {
        assert activity.name == originalActivity.name
        assert activity.type == originalActivity.type
        assert activity.category == originalActivity.category
        assert (activity.gmsId ?: '') == (originalActivity.gmsId ?: '')
        assert activity.supportsSites == (originalActivity.supportsSites ?: false)
        assert activity.supportsPhotoPoints == (originalActivity.supportsPhotoPoints ?: false)
        assert activity.minOptionalSectionsCompleted == originalActivity.minOptionalSectionsCompleted
        def expectedOutputs = originalActivity.outputs
        // These are outputs that exist in the activities outputs but don't have a corresponding entry in
        // the outputs array.
        ['Photo Points', 'Plant Propagation - Batch Test',  'Indicator 3 - Structural Diversity',
         'Indicator 4 - Regeneration', 'Indicator 5 - Tree and Shrub Health', 'Indicator 6 - Tree Habitat',
         'Indicator 7 - Feral Animals', 'Indicator 8 - Total Grazing Pressure', 'Indicator 9 - Animal Species',
         'Indicator 10 - Bushland Degradation Risk', 'Benchmarks - SA', 'Bushland Condition - Site Health Summary'
        ].each { String excluded ->
            if (expectedOutputs.contains(excluded)) {
                expectedOutputs = expectedOutputs.findAll{it != excluded}
            }
        }

        assert activity.outputs == expectedOutputs
        activity.outputs.each{ String outputName ->
            outputConfigEqual(activity.outputConfig.find{it.outputName == outputName}, originalActivity.outputConfig.find{it.outputName == outputName})
        }
    }

    private void outputConfigEqual(Map outputConfig, Map expectedOutputConfig) {
        assert outputConfig.outputName == expectedOutputConfig.outputName
        assert outputConfig.optional == (expectedOutputConfig.optional ?: false)
        assert outputConfig.collapsedByDefault == (expectedOutputConfig.collapsedByDefault ?: false)
        assert outputConfig.optionalQuestionText == expectedOutputConfig.optionalQuestionText
    }

    private void outputsEqual(List outputs, List expectedOutputs) {
        outputs.each { Map output ->
            def expectedOutput = expectedOutputs.find{it.name == output.name}
            outputEqual(output, expectedOutput)
        }
    }

    private void outputEqual(Map output, Map expectedOutput) {
        assert output.name == expectedOutput.name
        assert output.template == expectedOutput.template
        assert output.title == (expectedOutput.title ?: null)
    }

    private void templatesEqual(def template, def expectedTemplate) {

        if (!template && !expectedTemplate) {
            println "No data"
            return
        }

        assert template.toString() == expectedTemplate.toString()
    }

}
