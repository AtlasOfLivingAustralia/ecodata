package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.EcodataGraphQLContextBuilder
import au.org.ala.ecodata.graphql.enums.ProjectStatus
import grails.util.Holders
import graphql.GraphQLException
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingFieldSelectionSet
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.text.SimpleDateFormat

import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX

@Component
class ProjectsFetcher implements DataFetcher<Map<Integer, List<Project>>> {

    @Autowired
    PermissionService permissionService
    @Autowired
    ElasticSearchService elasticSearchService
    @Autowired
    ReportService reportService
    @Autowired
    ProjectService projectService
    @Autowired
    CacheService cacheService
    @Autowired
    HubService hubService
    @Autowired
    EcodataGraphQLContextBuilder ecodataGraphQLContextBuilder

    static String meritFacets = "status,organisationFacet,associatedProgramFacet,associatedSubProgramFacet,mainThemeFacet,stateFacet,nrmFacet,lgaFacet,mvgFacet,ibraFacet,imcra4_pbFacet,otherFacet,electFacet,meriPlanAssetFacet," +
            "cmzFacet,partnerOrganisationTypeFacet,promoteOnHomepage,custom.details.caseStudy,primaryOutcomeFacet,secondaryOutcomesFacet,muFacet,tags,fundingSourceFacet,grantId.keyword,grantManagerNominatedProject"
    static Map meritParams = [hubFq:"isMERIT:true", flimit:1500, fsort:"term", query:"docType:project", facets:meritFacets, format:null, max:20]

    static  Map paramList = [flimit:1500, fsort:"term", query:"docType: project", format:null, offset:0, max:20, skipDefaultFilters:false,
                             hubFq:null, facets: null]

    @Override
    Map<Integer, List<Project>> get(DataFetchingEnvironment environment) throws Exception {

        String query = environment.arguments.term ?:"*:*"
        return queryElasticSearch(environment, query, [include:'projectId'])
    }

    private Map<Integer, List<Project>> queryElasticSearch(DataFetchingEnvironment environment, String queryString, Map params) {
        // Retrieve projectIds only from elasticsearch.
        EcodataGraphQLContextBuilder.EcodataGraphQLContext context = ecodataGraphQLContextBuilder.buildContext(null)
        environment.graphQlContext.put("securityContext", context)
        String query = queryString ?:"*:*"
        SearchResponse searchResponse

        params.put("include", ["projectId", "hubId", "name", "description"])
        searchResponse = elasticSearchService.search(query, params, HOMEPAGE_INDEX)

        List<Map> restrictedAccessProjects = new ArrayList()
        List<String> fullAccessProjectIds = new ArrayList()
        List<String> projectIds = new ArrayList(searchResponse.hits?.hits?.size() ?: 0)
        searchResponse.hits?.hits?.each { SearchHit hit ->
            Map projectInfo = hit.sourceAsMap
            projectIds.add(projectInfo.projectId)

            if (context.hasPermission(projectInfo)) {
                fullAccessProjectIds << projectInfo.projectId
            }
            else {
                restrictedAccessProjects << new Project(projectId:projectInfo.projectId, name:projectInfo.name, description:projectInfo.description)
            }
        }

        List fullAccessProjects = Project.createCriteria().list {
            inList('projectId', fullAccessProjectIds)
            order('lastUpdated', 'desc')
        }

        List<Project> results = new ArrayList(projectIds.size())
        projectIds.each { String projectId ->
            results << (fullAccessProjects.find{it.projectId == projectId} ?: restrictedAccessProjects.find{it.projectId == projectId})
        }

        [totalCount: searchResponse.hits?.totalHits?.value ?: 0, results: results]
    }

    Map<Integer, List<Project>> searchMeritProject (DataFetchingEnvironment environment) {

        def fqList = mapFq(environment)

        //validate the query
        validateSearchQuery(environment, fqList, meritParams, "docType: project", ["dateRange", "grantManagerNominatedProject"])

        Map params = meritParams
        params["fq"] = fqList

        int max = Math.min(environment.arguments.get("max"), 50)
        params["max"] = max
        int page = Math.max(1, environment.arguments.get("page") as Integer ?: 1)
        params["offset"] = max*(page-1)

        params["sort"] = environment.arguments.get("sort") ?: "dateCreated"
        params["order"] = environment.arguments.get("order") ?: "desc"

        if(environment.arguments.get("fromDate")) {
            params["fromDate"] = environment.arguments.get("fromDate").toString()
        }

        if(environment.arguments.get("toDate")) {
            params["toDate"] = environment.arguments.get("toDate").toString()
        }
        // Special last updated query handling
        if (environment.arguments.get("updatedAfter")) {
            String updatedAfter = environment.arguments.get("updatedAfter")
            List lastUpdatedFields = ["lastUpdated"]
            DataFetchingFieldSelectionSet selectionSet = environment.getSelectionSet()
            if (selectionSet.contains("results/reports")) {
                lastUpdatedFields << "reports.lastUpdated"
            }
            println selectionSet
            if ( selectionSet.contains("results/sites")) {
                lastUpdatedFields << "sites.lastUpdated"
            }
            String lastUpdatedQuery = lastUpdatedFields.collect {
                "${it}:[${updatedAfter} TO *]"
            }.join(" OR ")

            fqList << "_query:(${lastUpdatedQuery})"

        }

        String query = "docType: project" + (environment.arguments.get("projectId") ? " AND projectId:" + environment.arguments.get("projectId") : "")
        Map<Integer, List<Project>> results =  queryElasticSearch(environment, query, params)

        return results
    }

    void validateSearchQuery (DataFetchingEnvironment environment, List fqList, Map params, String query, List enumList) {

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

    }

    def mapFq(DataFetchingEnvironment environment) {

        def fqList = []
        def facetMappings = [
                "managementArea": "nrmFacet:",
                "majorVegetationGroup": "mvgFacet:",
                "biogeographicRegion": "ibraFacet:",
                "marineRegion": "imcra4_pbFacet:",
                "otherRegion": "otherFacet:",
                "grantManagerNominatedProject":"promoteOnHomepage:",
                "federalElectorate": "electFacet:",
                "assetsAddressed": "meriPlanAssetFacet:",
                "userNominatedProject": "custom.details.caseStudy:",
                "managementUnit": "muFacet:",
                "meritProjectID": "grantId.keyword:"
        ]

        environment.arguments.each {
            if(it.key in ["fromDate", "toDate", "dateRange", "activities", "projectId", "activityOutputs", "programs", "reports",
                          "outputTargetMeasures", "projectStartFromDate", "projectStartToDate", "isWorldWide", "page", "max", "myProjects", "hub", "updatedAfter"]) {
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
                    case "meritProjectID"  :
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

    List<Project> searchBioCollectProject (DataFetchingEnvironment environment) {

        def fqList = mapFq(environment)

        Map hub = hubService.findByUrlPath(environment.arguments.get("hub"))
        if(!hub) {
            List hubList = Hub.findAll().collect{ it.urlPath}.unique()
            throw new GraphQLException('Invalid hub, suggested values are : ' + hubList)
        }

        paramList.hubFq = hub.defaultFacetQuery
        paramList.facets = hub.availableFacets?.join(",")
        paramList.hub = environment.arguments.get("hub").toString()

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
            throw new GraphQLException('The searchBioCollectProject query is not available for MERIT projects')
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
