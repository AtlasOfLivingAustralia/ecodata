package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.models.Schema
import au.org.ala.ecodata.graphql.models.Summary
import grails.core.GrailsApplication
import grails.util.Holders
import graphql.schema.DataFetchingEnvironment
import org.apache.commons.lang.WordUtils
import org.elasticsearch.action.search.SearchResponse
import org.springframework.context.MessageSource

class ActivityFetcher implements graphql.schema.DataFetcher<List<Activity>> {

    public ActivityFetcher(ElasticSearchService elasticSearchService, PermissionService permissionService, MetadataService metadataService, MessageSource messageSource, GrailsApplication grailsApplication) {

        this.elasticSearchService = elasticSearchService
        this.permissionService = permissionService
        this.metadataService = metadataService
        this.messageSource = messageSource
        this.grailsApplication = grailsApplication
    }


    PermissionService permissionService
    ElasticSearchService elasticSearchService
    ActivityService activityService
    MetadataService metadataService
    MessageSource messageSource
    GrailsApplication grailsApplication


    @Override
    List<Activity> get(DataFetchingEnvironment environment) throws Exception {

        String userId = environment.context.user?.userId
        String query = environment.arguments.term ?:"*:*"
        SearchResponse searchResponse = elasticSearchService.searchWithSecurity(null, query, [include:'projectId', max:65000], ElasticIndex.HOMEPAGE_INDEX)

        List<String> projectIds = searchResponse.hits.hits.collect{it.source.projectId}


        Activity.findAllByProjectIdInList(projectIds, [max:10])

        // Do we want to restrict API use based on hubs?
//        if (!userId || !environment.context.permissionService.checkUserPermission(userId, environment.fieldDefinition.name, "API", "read")) {
//            throw new GraphQLException("No permission")
//        }

        // Search ES, applying the user role in the process...


        // What should happen if we get a "show me all" type query?

        // Should we return the public view for all public projects (is that all projects?) we have data for?

        // e.g. should the role check only apply during the mapping phase?  In which case we need a bulk query of permissions to determine a list of project ids we can get full resolution data for?
        // Or do we do two queries, one for full resolution, one for the rest (how do we sort/page if we do two queries?)




    }

    List<Summary> getActivitySummaryList(DataFetchingEnvironment environment) {

        List<Summary> activityList = new ArrayList<Summary>()
        def activeActivities = metadataService.buildActivityModel()["activities"].findAll{!it.status || it.status == 'active'}
        if(activeActivities) {
            activeActivities.each() {
                Summary summary = new Summary(name: it.name, definition: messageSource.getMessage("api.${it.name}.description", null, "", Locale.default))
                activityList << summary
            }
        }

        return activityList
    }

    Schema getActivityByName(String name) {

        def activitiesModel = metadataService.activitiesModel()
        def schemaGenerator = new SchemaBuilder(grailsApplication.config, activitiesModel)
        if (!name) {
            return null
        }

        def activityModel = activitiesModel.activities.find{it.name == name}
        def schema = schemaGenerator.schemaForActivity(activityModel)

        Schema schemaEntity = new Schema()
        schemaEntity.type = schema.type
        schemaEntity.id = schema.id
        schemaEntity.propertyList = schema.properties
        return schemaEntity
    }

    List<Activity> getFilteredActivities(List args, String givenProjectId = null, String givenActivityId = null)
    {
        //validate inputs
        if(args){
            List arguments = []
            arguments.add(["activities":args])
            new Helper(Holders.applicationContext.metadataService).validateActivityData(arguments)
        }

        def activityIdList = []
        if(args) {
            activityIdList = Output.where { name in args["output"]["outputType"].flatten() }.findAll()

            args.each { arg ->
                arg["output"].each {
                    if (it["fields"] && !it["fields"].contains(null)) {
                        activityIdList.removeAll { x ->
                            (!x.data || x.data.size() == 0) && x.name == it["outputType"]
                        }
                    }
                }
            }
        }
        def activityList = Activity.where {
            if(givenProjectId) {
                projectId == givenProjectId
            }
            if(givenActivityId) {
                activityId == givenActivityId
            }
            if(args && args["activityType"]) {
                type in args["activityType"].flatten()
            }
            //get the activities with requested output types
            if(activityIdList.size() > 0) {
                activityId in activityIdList.activityId
            }
            status != Status.DELETED
        }.each {
            it.tempArgs = args ?: []
        }.sort { it.type}

        return activityList
    }

    /***
     * This method is used to get the activity output data of a given project
     * @param args
     * @param givenProjectId
     * @param activityName
     * @param outputTypes
     * @param modifiedColumns
     * @return
     */
    def getActivityData(List args, String givenProjectId, String activityName, List outputTypes,List modifiedColumns)
    {
        //get the activity list of a given project
        List<Activity> projectActivities = getFilteredActivities(args, givenProjectId)

        //get the activity Id of the activities of the given activity type
        List activityIds = projectActivities.findAll{ WordUtils.capitalize(it.type).replaceAll("\\W", "") == activityName}.activityId

        //get output data
        List<Output> outputList = new OutputFetcher(Holders.applicationContext.metadataService, Holders.applicationContext.messageSource, Holders.grailsApplication).getFilteredOutput(args)

        //get the output data of the activities of the project
        def outputs = outputList.findAll {it.activityId in activityIds}

        def activities = [:]
        outputTypes.each{
            activities."$it" = []
        }
        //format the output data to match the graphql type format
        outputs.each{
            String name = "OutputType_" + WordUtils.capitalize(it.name).replaceAll("\\W", "")
            if(!(name in outputTypes)){
                name = "OutputType_" + activityName + "_" + WordUtils.capitalize(it.name).replaceAll("\\W", "")
            }
            if(name in outputTypes) {
                def dataList = [:]
                it.data.each { d ->
                    if (d.key in modifiedColumns) {
                        dataList.put(name + "_" + d.key, d.value)
                    }
                    else{
                        dataList.put(d.key, d.value);
                    }
                }
                activities."$name" << dataList
            }
        }

        return activities
    }
}
