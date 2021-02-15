package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.enums.ProjectStatus
import au.org.ala.ecodata.graphql.models.KeyValue
import au.org.ala.ecodata.graphql.models.OutputData
import com.mongodb.client.FindIterable
import com.mongodb.client.model.Filters
import com.sun.xml.internal.bind.v2.TODO
import grails.util.Holders
import graphql.GraphQLException
import graphql.schema.DataFetchingEnvironment
import org.elasticsearch.action.search.SearchResponse

import java.text.SimpleDateFormat

import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX
import static au.org.ala.ecodata.Status.DELETED

class ProjectsFetcher implements graphql.schema.DataFetcher<List<Project>> {

    public ProjectsFetcher(ProjectService projectService, ElasticSearchService elasticSearchService, PermissionService permissionService,
                           ReportService reportService, CacheService cacheService, HubService hubService) {
        this.projectService = projectService
        this.elasticSearchService = elasticSearchService
        this.permissionService = permissionService
        this.reportService  = reportService
        this.cacheService = cacheService
        this.hubService = hubService
    }


    PermissionService permissionService
    ElasticSearchService elasticSearchService
    ReportService reportService
    ProjectService projectService
    CacheService cacheService
    HubService hubService

    static String meritFacets = "status,organisationFacet,associatedProgramFacet,associatedSubProgramFacet,mainThemeFacet,stateFacet,nrmFacet,lgaFacet,mvgFacet,ibraFacet,imcra4_pbFacet,otherFacet,electFacet,meriPlanAssetFacet," +
            "cmzFacet,partnerOrganisationTypeFacet,promoteOnHomepage,custom.details.caseStudy,primaryOutcomeFacet,secondaryOutcomesFacet,muFacet,tags,fundingSourceFacet"
    static Map meritParams = [hubFq:"isMERIT:true", flimit:1500, fsort:"term", query:"docType:project", facets:meritFacets, format:null, max:20]

    static  Map paramList = [flimit:1500, fsort:"term", query:"docType: project", format:null, offset:0, max:20, skipDefaultFilters:false,
                             hubFq:null, facets: null]

    @Override
    List<Project> get(DataFetchingEnvironment environment) throws Exception {


        // Search ES, applying the user role in the process...


        // What should happen if we get a "show me all" type query?

        // Should we return the public view for all public projects (is that all projects?) we have data for?

        // e.g. should the role check only apply during the mapping phase?  In which case we need a bulk query of permissions to determine a list of project ids we can get full resolution data for?
        // Or do we do two queries, one for full resolution, one for the rest (how do we sort/page if we do two queries?)

        String query = environment.arguments.term ?:"*:*"

        return queryElasticSearch(environment, query, [include:'projectId'])
    }

    private List<Project> queryElasticSearch(DataFetchingEnvironment environment, String queryString, Map params) {
        // Retrieve projectIds only from elasticsearch.


        // add pagination results.
        String userId = environment.context.user?.userId ?: '1493'
        String query = queryString ?:"*:*"
        Boolean myProjects = environment.arguments.get("myProjects") != null ? environment.arguments.get("myProjects") : true
        SearchResponse searchResponse

        if(myProjects) {
            searchResponse = elasticSearchService.searchWithSecurity(userId, query, params, HOMEPAGE_INDEX)
        }
        else {
            searchResponse = elasticSearchService.search(query, params, HOMEPAGE_INDEX)
        }

        List<String> projectIds = searchResponse.hits.hits.collect{it.source.projectId}

        // Split projects into those the user has full read permission & those they don't
        List publicProjectIds = []
        List fullProjectIds = []

        if(myProjects) {
            fullProjectIds = projectIds
        }
        else {
            publicProjectIds = projectIds
        }


        // Alternative here is to also return the userIds from the ES query and see if the user is in the result.
        // we could also map directly from the ES, but this would require a different approach (maybe creating
        // and binding the domain objects from the ES data?)
//        projectIds.each {
//            boolean readable = permissionService.checkUserPermission(userId, it, Project.name, "read")
//            readable ? publicProjectIds << it : publicProjectIds << it
//        }

        Map publicView = [name:true, description:true, projectId:true]

        Map publicProjects = [:]
        FindIterable findIterable = Project.find(Filters.in("projectId", publicProjectIds))
        findIterable.projection(publicView).each { Project project ->
            publicProjects.put(project.projectId, project)
        }

        Map fullProjects = [:]
        findIterable = Project.find(Filters.in("projectId", fullProjectIds))
        findIterable.each { Project project ->
            fullProjects.put(project.projectId, project)
        }

        List projects = projectIds.collect {
            !myProjects ? publicProjects[it] : fullProjects[it]
        }

        projects
    }

    List<Project> searchMeritProject (DataFetchingEnvironment environment) {

        def fqList = mapFq(environment)

        //validate the query
        validateSearchQuery(environment, fqList, meritParams, "docType: project", ["dateRange", "grantManagerNominatedProject"])

        Map params = meritParams
        params["fq"] = fqList

        //offset for elastic search
        if(environment.arguments.get("page")) {
            params["offset"] = 20*(Integer.parseInt(environment.arguments.get("page") as String)-1)
        }
        else{
            params["offset"] = 0
        }

        if(environment.arguments.get("max")) {
            params["max"] = environment.arguments.get("max")
        }
        else{
            params["max"] = 20
        }

        if(environment.arguments.get("fromDate")) {
            params["fromDate"] = environment.arguments.get("fromDate").toString()
        }

        if(environment.arguments.get("toDate")) {
            params["toDate"] = environment.arguments.get("toDate").toString()
        }

        if(environment.arguments.get("dateRange")) {

            params["fromDate"] = environment.arguments.get("dateRange").from
            params["toDate"] = environment.arguments.get("dateRange").to
        }

        String query = "docType: project" + (environment.arguments.get("projectId") ? " AND projectId:" + environment.arguments.get("projectId") : "")
        List<Project> projects =  queryElasticSearch(environment, query, params)

        if(environment.arguments.get("activities")) {
            List projectIdList = projects.projectId

            List activities = new ActivityFetcher(Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService, Holders.applicationContext.metadataService,
                    Holders.applicationContext.messageSource, Holders.grailsApplication).getFilteredActivities(environment.arguments.get("activities") as List)

            //get projects with requested activity output types
            List projectIds = activities.findAll { it.projectId in projectIdList }.projectId.unique()

            projects =  projects.findAll{ it.projectId in projectIds}
        }

        return projects
    }

    void validateSearchQuery (DataFetchingEnvironment environment, List fqList, Map params, String query, List enumList) {

        def searchDetails = elasticSearchService.search(query, params, HOMEPAGE_INDEX)

        fqList.each {
            List fq = it.toString().split(":")
            if(!enumList.contains(fq.first())) {
                List<String> lookUps = searchDetails.facets.getFacets().get(fq.first()).entries.term as String[]
                if (!lookUps.contains(fq.last())) {
                    throw new GraphQLException('Invalid ' + fq.first() + ' : suggested values are : ' + lookUps)
                }
            }
        }

        def datePattern = /\d{4}\-\d{2}\-\d{2}/

        //validate the format of the from and to dates
        if(environment.arguments.get("fromDate")) {
            if(!(environment.arguments.get("fromDate") ==~ datePattern)){
                throw new GraphQLException('Invalid fromDate: fromDate should match yyyy-mm-dd')
            }
        }

        if(environment.arguments.get("toDate")) {
            if(!(environment.arguments.get("fromDate") ==~ datePattern)){
                throw new GraphQLException('Invalid toDate: toDate should match yyyy-mm-dd')
            }
        }

        if(environment.arguments.get("projectStartFromDate")) {
            if(!(environment.arguments.get("projectStartFromDate") ==~ datePattern)){
                throw new GraphQLException('Invalid projectStartFromDate: projectStartFromDate should match yyyy-mm-dd')
            }
        }

        if(environment.arguments.get("projectStartToDate")) {
            if(!(environment.arguments.get("projectStartToDate") ==~ datePattern)){
                throw new GraphQLException('Invalid projectStartToDate: projectStartToDate should match yyyy-mm-dd')
            }
        }

        //validate activity types and output types
        if(environment.arguments.get("activities")){
            List args = []
            args.add(["activities":environment.arguments.get("activities")])

            new Helper(Holders.applicationContext.metadataService).validateActivityData(args)
        }

    }

    def searchActivityOutput (DataFetchingEnvironment environment) {

        if(environment.arguments.get("activityOutputs")) {
            validateActivityOutputInput(environment)
        }

        def fqList = mapFq(environment)

        validateSearchQuery(environment, fqList, meritParams, "docType: project", ["dateRange", "grantManagerNominatedProject"])

        List<Score> scores = Score.findAll()
        def results = getActivityOutputs(fqList, scores)

        List outputs = results.outputData
        //filter the output based on the filtering values
        if(environment.arguments.get("activityOutputs")) {
            def filteredOutputs = []
            results.outputData.each {
                if (it.category in environment.arguments.get("activityOutputs").getAt("category")) {
                    filteredOutputs.add(it)
                }
            }
            environment.arguments.get("activityOutputs").each { activityOutput ->
                if(activityOutput.outputs) {
                    def unWanted = []
                    filteredOutputs.each {
                        if(activityOutput.category == it.category) {
                            if (!(it.outputType in activityOutput.outputs.outputType)) {
                                unWanted.add(it)
                            }

                            if (activityOutput.outputs.labels[0] && activityOutput.outputs.labels[0].size() != 0 && !activityOutput.outputs.labels.contains(null)) {
                                if (it.outputType in activityOutput.outputs.outputType && !(it.label in activityOutput.outputs.labels[0])) {
                                    unWanted.add(it)
                                }
                            }
                        }
                    }
                    filteredOutputs = filteredOutputs.minus(unWanted)
                }
            }
            outputs = filteredOutputs
        }

        outputs.each {
            if(it["result"]["result"] != null && !it["result"]["result"].toString().isNumber()){
                OutputData outputData = new OutputData(dataList: new ArrayList<KeyValue>())
                it["result"]["result"].each{ list ->
                    outputData.dataList.add(new KeyValue(key: list.key, value: list.value))
                }
                it["result"]["resultList"] = outputData
                it["result"]["result"] = null
            }
        }
        return  [outputData : outputs ]
    }

    def searchOutputTargetsByProgram (DataFetchingEnvironment environment) {

        def fqList = mapFq(environment)

        validateSearchQuery(environment, fqList, meritParams, "docType: project", ["dateRange", "grantManagerNominatedProject"])

        Map params = [hubFq:"isMERIT:true", controller:"search", showOrganisations:true, report:"outputTargets", action:"targetsReport", fq:fqList, format:null]

        def targets = reportService.outputTargetsBySubProgram(params)
        //def scores = reportService.outputTargetReport(fqList, "*:*")

        def targetList = []

        targets.each {
            def target = [:]
            //remove null values
            if(it.value != null && it.value.entrySet().key.contains(null)) {
                it.value = it.value.remove(it.value.get(null))
            }

            if(!environment.arguments.get("programs") || environment.arguments.get("programs").contains(it.key)) {
                target["program"] = it.key
                target["outputTargetMeasure"] = []
                it.value.each { x ->
                    if(!environment.arguments.get("outputTargetMeasures") || environment.arguments.get("outputTargetMeasures").contains(x.key)) {
                        def targetMeasure = [:]
                        targetMeasure["outputTarget"] = x.key
                        targetMeasure["count"] = x.value.count
                        targetMeasure["total"] = x.value.total
                        target["outputTargetMeasure"] << targetMeasure
                    }
                }
                targetList.add(target)
            }
        }

        return [targets:targetList]
    }

    def mapFq(DataFetchingEnvironment environment) {

        def fqList = []
        def facetMappings = ["managementArea": "nrmFacet:", "majorVegetationGroup": "mvgFacet:", "biogeographicRegion": "ibraFacet:", "marineRegion": "imcra4_pbFacet:", "otherRegion": "otherFacet:", "grantManagerNominatedProject":"promoteOnHomepage:",
                             "federalElectorate": "electFacet:", "assetsAddressed": "meriPlanAssetFacet:", "userNominatedProject": "custom.details.caseStudy:", "managementUnit": "muFacet:"]

        environment.arguments.each {
            if(it.key in ["fromDate", "toDate", "dateRange", "activities", "projectId", "activityOutputs", "programs",
                          "outputTargetMeasures", "projectStartFromDate", "projectStartToDate", "isWorldWide", "page", "max", "myProjects", "hub"]) {
                return
            }

            String key

            it.value.each { val ->
                switch (it.key) {
                    case "status":
                    case "tags":
                    case "scienceType":
                    case "countries":
                    case "ecoScienceType":
                    case "difficulty":
                    case "origin":
                    case "isBushfire":
                    case "typeOfProject":
                        key = it.key + ":"
                        break;
                    case "managementArea" :
                    case "majorVegetationGroup" :
                    case "biogeographicRegion" :
                    case "marineRegion" :
                    case "otherRegion" :
                    case "grantManagerNominatedProject" :
                    case "federalElectorate" :
                    case "assetsAddressed" :
                    case "userNominatedProject" :
                    case "managementUnit" :
                        key = facetMappings.get(it.key)
                        break;
                    default:
                        key = it.key + "Facet:"
                        break;
                }
                fqList << key + val;
            }
        }

        return fqList
    }

    def getActivityOutputs(List fqList, List scores) {
        return cacheService.get("dashboard-activityOutput-"+fqList, {
            reportService.aggregate(fqList, "docType:project", scores)
        })
    }

    void validateActivityOutputInput (DataFetchingEnvironment environment) {

        def categories = []
        List<Score> scores = Score.findAllWhereStatusNotEqual(DELETED)

        scores.each { score ->
            def cat = score.category?.trim()
            if (cat && !categories.contains(cat)) {
                categories << cat
            }
        }

        environment.arguments.get("activityOutputs").each {
            if(!(it.category in categories)) {
                throw new GraphQLException('Invalid category ' +  it.category +' : suggested values are : ' + categories)
            }
            if(it.outputs) {
                it.outputs.each{ outputs ->
                    def outputTypes = scores.findAll { score -> score.category == it.category}.outputType.unique()
                    if(outputs.outputType && !(outputs.outputType in  outputTypes)){
                        throw new GraphQLException('Invalid outputType ' +  outputs.outputType +' : suggested values are : ' + outputTypes)
                    }
                }

                if (it.outputs.labels[0] && it.outputs.labels[0].size() != 0 && !it.outputs.labels.contains(null)) {
                    def labels = scores.findAll { score -> score.category == it.category && it.outputs.outputType.contains(score.outputType)}?.label.unique()

                    it.outputs.labels[0].each { label ->
                        if (!(label in labels)) {
                            throw new GraphQLException('Invalid label ' + label + ' : suggested values are : ' + labels)
                        }
                    }
                }
            }
        }
    }

    List<Project> searchBioCollectProject (DataFetchingEnvironment environment) {

        def fqList = mapFq(environment)

        Map hub = hubService.findByUrlPath(environment.arguments.get("hub"))
        if(!hub) {
            List hubList = Hub.findAll().collect{ it.urlPath}.unique()
            throw new GraphQLException('Invalid hub, suggested values are : ' + hubList)
        }

        paramList.hubFq = hub.defaultFacetQuery
        paramList.facets = hub.availableFacets.join(",")

        //validate the query
        validateSearchQuery(environment, fqList, paramList, "docType: project", ["status"])

        Map queryParams =  buildBioCollectProjectSearchQuery(environment.arguments, fqList)
        List<Project> projects =  queryElasticSearch(environment, queryParams["query"] as String, queryParams)

        if(environment.arguments.get("activities")) {
            List projectIdList = projects.projectId

            List activities = new ActivityFetcher(Holders.applicationContext.elasticSearchService, Holders.applicationContext.permissionService, Holders.applicationContext.metadataService,
                    Holders.applicationContext.messageSource, Holders.grailsApplication).getFilteredActivities(environment.arguments.get("activities") as List)

            //get projects with requested activity output types
            List projectIds = activities.findAll { it.projectId in projectIdList }.projectId.unique()

            projects =  projects.findAll{ it.projectId in projectIds}
        }

        return projects
    }

    static Map buildBioCollectProjectSearchQuery(Map params, List fqList){

        List difficulty = [], status =[]
        Boolean isMerit
        Map trimmedParams = [:]
        trimmedParams = paramList
        trimmedParams.query = "docType:project" + (params.get("projectId") ? " AND projectId:" + params.get("projectId") : "")
        trimmedParams.difficulty = params.get('difficulty')
        trimmedParams.isWorldWide = params.get('isWorldWide') ?: false

        List fq = [], projectType = []
        List immutableFq = fqList
        immutableFq.each {
            if(!it?.startsWith('status:')) {
                it ? fq.push(it) : null
            }
        }

        if(params.projectStartFromDate) {
            if(params.projectStartToDate) {
                fq.push("plannedStartDate:[" + params.projectStartFromDate + " TO " + params.projectStartToDate + "}")
            }
            else {
                fq.push("plannedStartDate:[" + params.projectStartFromDate + " TO *}")
            }
        }
        else{
            if(params.projectStartToDate) {
                fq.push("plannedStartDate:[* TO " + params.projectStartToDate + "}")
            }
        }

        trimmedParams.fq = fq;

        String hubFq = paramList.hubFq

        if(hubFq.contains("isCitizenScience:true")) {
            projectType.push('isCitizenScience:true') }
        else if(hubFq.contains("isWorks:true")) {
            projectType.push('(projectType:works AND isMERIT:false)')}
        else if(hubFq.contains("isEcoScience:true")) {
            projectType.push('(projectType:ecoScience)')}
        else if(hubFq.contains("isMERIT:true")) {
            projectType.push('isMERIT:true')
            isMerit = true
        }

        if(trimmedParams.difficulty){
            trimmedParams.difficulty.each{
                difficulty.push("difficulty:${it}")
            }
            trimmedParams.query += " AND (${difficulty.join(' OR ')})"
            trimmedParams.difficulty = null
        }

        if (projectType) {
            trimmedParams.query += ' AND (' + projectType.join(' OR ') + ')'
        }

        if(params.status){
            SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd');
            // Do not execute when both active and completed facets are checked.
            if(params.status.unique().size() == 1){
                params.status.unique().each{
                    switch (it){
                        case ProjectStatus.Active:
                            status.push("-(plannedEndDate:[* TO *] AND -plannedEndDate:>=${sdf.format( new Date())})")
                            break;
                        case ProjectStatus.Completed:
                            status.push("(plannedEndDate:<${sdf.format( new Date())})")
                            break;
                    }
                }
                trimmedParams.query += " AND (${status.join(' OR ')})";
            }
            else if(params.status.unique().size() == 2 && params.status.unique() == [ProjectStatus.Active, ProjectStatus.Completed]){

                status.push("status:(\"active\")");
                status.push("(plannedEndDate:<${sdf.format( new Date())})");

                trimmedParams.query += " AND ${status.join(' AND ')}";
            }
        }

        if(!isMerit) {
            if (trimmedParams.isWorldWide) {
                trimmedParams.isWorldWide = null
            } else if (trimmedParams.isWorldWide == false) {
                trimmedParams.query += " AND countries:(Australia OR Worldwide)"
                trimmedParams.isWorldWide = null
            }
        }

        //offset for elastic search
        if(params.get("page")) {
            trimmedParams["offset"] = 20*(Integer.parseInt(params.get("page") as String)-1)
        }
        else{
            trimmedParams["offset"] = 0
        }

        if(params.get("max")) {
            trimmedParams["max"] = params.get("max")
        }
        else{
            trimmedParams["max"] = 20
        }


        Map queryParams = [:]
        trimmedParams.each { key, value ->
            if (value != null) {
                queryParams.put(key, value)
            }
        }
        queryParams
    }
}
