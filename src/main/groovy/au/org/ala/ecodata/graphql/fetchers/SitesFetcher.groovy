package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.*
import au.org.ala.ecodata.reporting.ShapefileBuilder
import grails.core.GrailsApplication
import graphql.GraphQLException
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.elasticsearch.action.search.SearchResponse
import org.springframework.beans.factory.annotation.Autowired

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SitesFetcher implements DataFetcher<List<Site>> {

    public SitesFetcher(ProjectService projectService, ElasticSearchService elasticSearchService, PermissionService permissionService,
                        SiteService siteService) {
        this.projectService = projectService
        this.elasticSearchService = elasticSearchService
        this.permissionService = permissionService
        this.siteService = siteService
    }


    PermissionService permissionService
    ElasticSearchService elasticSearchService
    SiteService siteService
    ProjectService projectService

    @Autowired
    GrailsApplication grailsApplication

    @Override
    List<Site> get(DataFetchingEnvironment environment) throws Exception {

        String userId = environment.context.user?.userId

        // Do we want to restrict API use based on hubs?
//        if (!userId || !environment.context.permissionService.checkUserPermission(userId, environment.fieldDefinition.name, "API", "read")) {
//            throw new GraphQLException("No permission")
//        }

        // Search ES, applying the user role in the process...


        // What should happen if we get a "show me all" type query?

        // Should we return the public view for all public projects (is that all projects?) we have data for?

        // e.g. should the role check only apply during the mapping phase?  In which case we need a bulk query of permissions to determine a list of project ids we can get full resolution data for?
        // Or do we do two queries, one for full resolution, one for the rest (how do we sort/page if we do two queries?)


        if(environment.arguments.get("siteIds")) {
            return Site.findAllBySiteIdInList(environment.arguments.get("siteIds") as List)
        }
        else {
            return queryElasticSearch(environment)
        }
    }

    private List<Site> queryElasticSearch(DataFetchingEnvironment environment) {
        // Retrieve projectIds only from elasticsearch.

        // Need to only retrieve sites for which we actually have access to.  It's a bit tricky as pagination can mean we
        // can't post process data.  e.g. we want from 100-200 and are post processing we have to query from 0, and post filter,
        // throwing away the first 100.

        // Might have to include an ACL in ES or the database to make it work properly.  Or use projects as ACL - this may not work, as
        // someone with access to all MERIT projects could result in a large project list going to the sites query (e.g. 3500...)
        // I assume we want to be able to query sites based on projects anyway though - e.g. programs.

        // ES limits terms query to ~65,000 by default, which may eventually cause problems that would require building a
        // new index.

        // Another way to deal with this would be to limit site / activity queries to hub based ones and include hub in the
        // index?

        // Another way is to build the query with hub information in the query?
        // e.g. hub in <xyz> or <userId> in ACL?   Do we need a way to give access to all Hubs explicity?

        // Otherwise we need to be adding hub permissions to the ACL, which will require re-indexing a lot of projects when
        // hub permissions change? (maybe that's OK)?

        SearchResponse searchResponse = elasticSearchService.searchWithSecurity(null, "*:*", [include:'projectId', max:65000], ElasticIndex.HOMEPAGE_INDEX)

        List<String> projectIds = searchResponse.hits.hits.collect{it.source.projectId}

        int max = environment.arguments.get("max") ?: 10
        int page = environment.arguments.get("page") ?: 1
        int offset =  (page-1)*max

        Site.findAllByProjectsInList(projectIds, [max:max, offset:offset])
    }

    Map getSitesAsGeojson(String siteId) {
        def site = siteService.get(siteId, siteService.BRIEF, null)
        return siteService.toGeoJson(site)
    }

    String getSiteShapeFileUrl(String siteId) {
        Closure doDownload = {OutputStream outputStream, String id -> downloadSiteShapeFiles(outputStream, id)}
        return createSiteShapeFiles(siteId, doDownload)
    }

    boolean downloadSiteShapeFiles(OutputStream outputStream, String siteId) {
        new ZipOutputStream(outputStream).withStream { zip ->
            try{
                zip.putNextEntry(new ZipEntry("shapefiles/"))
                Site site = Site.findBySiteId(siteId)
                if (site) {
                    zip.putNextEntry(new ZipEntry("shapefiles/${site.name}.zip"))
                    ShapefileBuilder builder = new ShapefileBuilder(projectService, siteService)
                    builder.setName(site.name)
                    builder.addSite(site.siteId)
                    builder.writeShapefile(zip)
                }
            } catch (Exception e){
                throw new GraphQLException("Error creating download archive" + e)
            } finally {
                zip.finish()
                zip.flush()
                zip.close()
            }
        }
        true
    }

    String createSiteShapeFiles(String siteId, Closure downloadAction) {
        String downloadId = UUID.randomUUID().toString()
        File directoryPath = new File("${grailsApplication.config.getProperty('temp.dir')}")
        directoryPath.mkdirs()
        String fileExtension = 'zip'
        FileOutputStream outputStream = new FileOutputStream(new File(directoryPath, "${downloadId}.${fileExtension}"))

        Site.withNewSession {
            downloadAction(outputStream, siteId)
        }

        String urlPrefix = "${grailsApplication.config.getProperty('grails.serverURL')}/download/get/"
        String url = "${urlPrefix}${downloadId}?fileExtension=${fileExtension}"
        if (outputStream) {
            outputStream.flush()
            outputStream.close()
        }
        return url
    }
}
