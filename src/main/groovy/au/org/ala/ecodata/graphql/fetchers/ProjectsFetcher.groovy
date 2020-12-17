package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.*
import com.mongodb.client.FindIterable
import com.mongodb.client.model.Filters
import grails.util.Holders
import graphql.GraphQLException
import graphql.schema.DataFetchingEnvironment
import org.elasticsearch.action.search.SearchResponse

import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX

class ProjectsFetcher implements graphql.schema.DataFetcher<List<Project>> {

    public ProjectsFetcher(ProjectService projectService, ElasticSearchService elasticSearchService, PermissionService permissionService) {
        this.projectService = projectService
        this.elasticSearchService = elasticSearchService
        this.permissionService = permissionService
    }


    PermissionService permissionService
    ElasticSearchService elasticSearchService

    ProjectService projectService

    static String meritFacets = "status,organisationFacet,associatedProgramFacet,associatedSubProgramFacet,mainThemeFacet,stateFacet,nrmFacet,lgaFacet,mvgFacet,ibraFacet,imcra4_pbFacet,otherFacet,electFacet,meriPlanAssetFacet," +
            "cmzFacet,partnerOrganisationTypeFacet,promoteOnHomepage,custom.details.caseStudy,primaryOutcomeFacet,secondaryOutcomesFacet,muFacet,tags,fundingSourceFacet"
    static Map meritParams = [hubFq:"isMERIT:true", controller:"search", flimit:1500, fsort:"term", query:"docType:project", action:"elasticHome", facets:meritFacets, format:null]

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
        String userId = environment.context.userId ?: '1493'
        String query = queryString ?:"*:*"
        //SearchResponse searchResponse = elasticSearchService.searchWithSecurity(userId, query, params, HOMEPAGE_INDEX)
        SearchResponse searchResponse = elasticSearchService.search(query, params, HOMEPAGE_INDEX)

        List<String> projectIds = searchResponse.hits.hits.collect{it.source.projectId}

        // Split projects into those the user has full read permission & those they don't
        List publicProjectIds = []
        List fullProjectIds = []


        // Alternative here is to also return the userIds from the ES query and see if the user is in the result.
        // we could also map directly from the ES, but this would require a different approach (maybe creating
        // and binding the domain objects from the ES data?)
        projectIds.each {
            boolean readable = permissionService.checkUserPermission(userId, it, Project.name, "read")
            readable ? publicProjectIds << it : publicProjectIds << it
        }

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
            publicProjects.containsKey(it) ? publicProjects[it] : fullProjects[it]
        }

        projects
    }

    List<Project> searchMeritProject (DataFetchingEnvironment environment) {

        def fqList = []
        def facetMappings = ["managementArea": "nrmFacet:", "majorVegetationGroup": "mvgFacet:", "biogeographicRegion": "ibraFacet:", "marineRegion": "imcra4_pbFacet:", "otherRegion": "otherFacet:", "grantManagerNominatedProject":"promoteOnHomepage:",
                             "federalElectorate": "electFacet:", "assetsAddressed": "meriPlanAssetFacet:", "userNominatedProject": "custom.details.caseStudy:", "managementUnit": "muFacet:"]

        environment.arguments.each {
            if(it.key in ["fromDate", "toDate", "dateRange", "activities", "projectId"]) {
                return
            }

            String key

            it.value.each { val ->
                switch (it.key) {
                    case "status":
                    case "tags":
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

        //validate the query
        validateSearchQuery(environment, fqList)

        Map params = meritParams
        params["fq"] = fqList

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

        projects.each {
            if(environment.arguments.get("activities")) {
                it.tempArgs = environment.arguments.get("activities") as List
            }
        }

        return projects
    }

    void validateSearchQuery (DataFetchingEnvironment environment, List fqList) {

        def searchDetails = elasticSearchService.search("docType: project", meritParams, HOMEPAGE_INDEX)

        fqList.each {
            List fq = it.toString().split(":")
            List<String> lookUps = searchDetails.facets.getFacets().get(fq.first()).entries.term as String[]
            if(!lookUps.contains(fq.last())) {
                throw new GraphQLException('Invalid ' +  fq.first() +' : suggested values are : ' + lookUps)
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

        //validate activity types and output types
        if(environment.arguments.get("activities")){
            List args = []
            args.add(["activities":environment.arguments.get("activities")])

            new Helper(Holders.applicationContext.metadataService).validateActivityData(args)
        }

    }
}
