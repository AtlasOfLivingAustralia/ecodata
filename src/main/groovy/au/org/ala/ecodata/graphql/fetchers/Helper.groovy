package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.FormSection
import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.PublicationStatus
import au.org.ala.ecodata.Status
import grails.util.Holders
import graphql.GraphQLException

class Helper {

    MetadataService metadataService

    Helper(MetadataService metadataService) {
        this.metadataService = metadataService
    }

    Helper() {
    }
    /**
     * This can be used to validate the given activity types and the output types
     */
    def validateActivityData(List args) {

        def metadata = metadataService.buildActivityModel()
        List activities = metadata["activities"].findAll{!it.status || it.status == 'active'}
        List outputs = metadata["outputs"]

        if(!args["activities"].contains(null)) {
            args["activities"].each { activity ->
                activity.each {  prop ->
                    //validate activity type
                    if (prop["activityType"] && !activities.name.contains(prop["activityType"])) {
                        throw new GraphQLException('Invalid Activity Type: ' + prop["activityType"] + ' , suggested values are : ' + activities.name)
                    }
                    if(prop["output"]) {
                        List outputNameList = prop["activityType"] ? activities.find{it.name == prop["activityType"]}.outputs : outputs.name
                        prop["output"].each { output ->
                            //validate output types against the output types of the activity
                            validateOutputs(output, outputs, outputNameList)

                        }
                    }
                }
            }
        }
        else if(!args["outputs"].contains(null)) {
            args["outputs"].each{ output ->
                //validate output types
                output.each {
                    validateOutputs(it, outputs, outputs.name)
                }
            }
        }

    }

    void validateOutputs(def output, List outputs, List outputNameList) {
        //validate output types
        if(output["outputType"] && !outputNameList.contains(output["outputType"])) {
            throw new GraphQLException('Invalid Output Type: ' + output["outputType"] + ' , suggested values are : ' + outputNameList)
        }

        //validate fields
        if(output["fields"] && output["fields"].size() > 0 && !output["fields"].contains(null)) {
            def templateName = outputs.find { it.name == output["outputType"] }.template
            List fieldNames = metadataService.getOutputDataModel(templateName)?.dataModel?.name

            if (!fieldNames.containsAll(output["fields"])) {
                throw new GraphQLException('Invalid Field: ' + output["fields"] + ' , suggested values are : ' + fieldNames)
            }
        }
    }

    /***
     * This method is used to get activity output model
     * @return
     */
    Map getActivityOutputModels(){
        Map activitiesModel = [activities:[]]

        Map maxVersionsByName = [:]
        Map activitiesByName = [:]

        ActivityForm.findAllWhereStatusNotEqualAndPublicationStatusEquals(Status.DELETED, PublicationStatus.PUBLISHED).each { ActivityForm activityForm ->
            Map activityModel = [
                    name: activityForm.name,
                    description: Holders.applicationContext.messageSource.getMessage("api.${activityForm.name}.description", null, "", Locale.default),
                    outputs: []
            ]

            activityForm.sections.unique().each { FormSection section ->
                activityModel.outputs << [
                        name: section.name,
                        title: section.title,
                        fields: section.template.dataModel != null ? section.template.dataModel : []
                ]
            }

            if (!maxVersionsByName[activityForm.name] || (maxVersionsByName[activityForm.name] < activityForm.formVersion)) {
                maxVersionsByName[activityForm.name] = activityForm.formVersion
                activitiesByName[activityForm.name] = activityModel
            }
        }
        // Assemble the latest version of each activity into the model.
        activitiesByName.each { String name, Map activityModel ->
            activitiesModel.activities << activityModel
        }

        activitiesModel
    }
}
