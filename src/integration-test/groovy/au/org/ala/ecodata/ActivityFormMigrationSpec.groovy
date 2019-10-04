package au.org.ala.ecodata

import au.org.ala.ecodata.data_migration.ActivityFormMigrator
import com.mongodb.BasicDBObject
import grails.converters.JSON
import grails.testing.mixin.integration.Integration
//import grails.transaction.Rollback
import grails.gorm.transactions.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared


/**
 * Dual purpose - actually performs the data migration and also ensures the existing API returns the same
 * data as it did before the migration.
 */
@Integration
@Rollback
class ActivityFormMigrationSpec extends IntegrationTestHelper {

    @Autowired
    MetadataService metadataService

    ActivityFormMigrator activityFormMigrator

    def setup() {
        ActivityForm.collection.remove(new BasicDBObject())
        String modelsFolder = new File("src/integration-test/resources/models").toString()
        activityFormMigrator = new ActivityFormMigrator(modelsFolder)
        activityFormMigrator.migrateActivitiesModel()
        metadataService.cacheService.clear()
    }

    def cleanup() {
        // The collection is deliberately left in the database at the end of the test so we can use it to
        // dump and load into our target environments.

    }

    private def outputModelTemplateAsJSON(String templateDir) {

        String template = activityFormMigrator.loadOutputModelTemplateToString(templateDir)

        template ? JSON.parse(template) : [:]
    }

    def "the migration preserves the existing activitiesModel() API"() {

        setup:
        Map originalActivitiesModel = activityFormMigrator.loadActivitiesModel()

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
        def originalActivitiesModel = activityFormMigrator.loadActivitiesModel()

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
        assert output.title == expectedOutput.title
    }

    private void templatesEqual(def template, def expectedTemplate) {

        if (!template && !expectedTemplate) {
            println "No data"
            return
        }

        assert template.toString() == expectedTemplate.toString()
    }

}
