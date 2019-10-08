package au.org.ala.ecodata
import au.org.ala.ecodata.reporting.ShapefileBuilder
import com.mongodb.MongoExecutionTimeoutException
import grails.converters.JSON
import org.apache.http.HttpStatus

class SiteController {

    def siteService, commonService, projectService, webService, projectActivityService
    DocumentService documentService

    static final RICH = "rich"
    static final BRIEF = 'brief'
    static final RAW = 'raw'
    static final SCORES = 'scores'

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
       // response.setContentType("application/json; charset=\"UTF-8\"")
        render model as JSON
    }

    static ignore = ['action','controller','id']

    def index() {
        log.debug "Total sites = ${Site.count()}"
        render "${Site.count()} sites"
    }

    def list() {
        def list = []
        def sites = params.includeDeleted ? Site.list() :
            Site.findAllByStatus('active')
        sites.each { site ->
            list << siteService.toMap(site)
        }
        list.sort {it.name}
        render list as JSON
    }

    def get(String id) {
        def levelOfDetail = []
        if (params.brief || params.view == BRIEF) { levelOfDetail << BRIEF }
        if (params.rich || params.view == RICH) { levelOfDetail << RICH }
        if (params.raw || params.view == RAW) { levelOfDetail << RAW }
        if (params.scores || params.view == SCORES) { levelOfDetail << SCORES }
        if (params.projects || params.view == LevelOfDetail.PROJECTS.name().toLowerCase()) { levelOfDetail << LevelOfDetail.PROJECTS.name()}

        if (!id) {
            def list = []
            def sites = params.includeDeleted ? Site.list() :
                    Site.findAllByStatus('active')
            sites.each { site ->
                list << siteService.toMap(site, levelOfDetail)
            }
            list.sort { it.name }
            asJson([list: list])
        } else {
            def s = siteService.get(id, levelOfDetail, params?.version)
            if (s) {
                withFormat {
                    json {
                        asJson s
                    }
                    shp {
                        asShapefile s
                    }
                    geojson {
                        asJson(asGeoJson(s))
                    }
                }


            } else {
                render (status: 404, text: 'No such id')
            }
        }
    }

    private def asShapefile(site) {
        def name = 'projectSites'
        response.setContentType("application/zip")
        response.setHeader("Content-disposition", "filename=${name}.zip")
        ShapefileBuilder builder = new ShapefileBuilder(projectService, siteService)
        builder.setName(name)
        builder.addSite(site.siteId)
        builder.writeShapefile(response.outputStream)
        response.outputStream.flush()
    }

    private Map asGeoJson(site) {
        siteService.toGeoJson(site)
    }

    @RequireApiKey
    def delete(String id) {
        def s = Site.findBySiteId(id)
        if (s) {
            if (params.destroy) {
                s.delete()
            } else {
                s.status = 'deleted'
                s.save(flush: true)
            }
            render (status: 200, text: 'deleted')
        } else {
            render (status: 404, text: 'No such id')
        }
    }

    /**
     * Update a site.
     *
     * @param id - identifies the resource
     * @return
     */
    @RequireApiKey
    def update(String id) {
        def props = request.JSON
        log.debug "${props}"
        def result
        def message
        if (id) {
            result = siteService.update(props,id)
            message = [message: 'updated']
        }
        else {
            result = siteService.create(props)
            message = [message: 'created', siteId: result.siteId]
        }
        if (result.status == 'ok') {
            asJson(message)
        } else {
            log.error result.error
            render status:400, text: result.error
        }
    }

    @RequireApiKey
    def createOrUpdatePoi(String id) {
        def props = request.JSON

        if (!id) {
            render status:400, text:'Site ID is mandatory'
            return
        }
        def result = siteService.updatePoi(id, props)
        if (result.status == 'ok') {
            def message = [message:'ok', poiId: result.poiId]
            asJson(message)
        }
        else {
            render status:400, text:result.error
        }
    }

    /**
     * Deletes a POI associated with a site.
     * @param id the site id
     * @param poiId the POI to delete.
     */
    @RequireApiKey
    def deletePoi(String id) {
        if (!id) {
            render status:400, text:'Site ID is mandatory'
            return
        }
        def result = siteService.deletePoi(id, params.poiId)
        if (result.status == 'deleted') {
            def message = [message:'deleted', poiId: result.poiId]
            asJson(message)
        }
        else {
            render status:400, text:result.error
        }
    }

    def list2() {
        def sites = Site.list()
        [sites: sites]
    }

    /**
     * get images/documents for a list sites.
     * @required id - comma separated siteIds eg - 142,651,778
     * @return
     */
    @RequireApiKey
    def getImages(){
        String id = params.id
        String role = params.role
        String sort = params.sort?:'dateTaken';
        String order = params.order?:'desc'
        Integer max = (params.max?:5) as Integer
        Integer offset = (params.offset?:0) as Integer
        Long userId = params.long('userId')
        Set<String> siteIds = []
        List result = [];
        Map mongoParams = [:];
        if(role){
            mongoParams.role = role;
        }

        if(id){
            String [] ids = id.split(',');
            siteIds.addAll(ids.toList())

            try{
                result = siteService.getImages(siteIds, mongoParams, userId, sort, order, max, offset)
            } catch (MongoExecutionTimeoutException mete){
                render(text: 'Database timeout exception ' + mete.message, status: HttpStatus.SC_REQUEST_TIMEOUT)
            } catch(Exception e){
                render(text: 'An exception occurred: ' + e.message, status: HttpStatus.SC_INTERNAL_SERVER_ERROR)
            }

            render text: result as JSON, contentType: 'application/json'
        } else {
            response.sendError(HttpStatus.SC_BAD_REQUEST, 'Site id must be provided')
        }
    }

    /**
     * Get images/documents for a point of interest. This function supports pagination.
     * @required siteId, poiId
     * @return
     */
    @RequireApiKey
    def getPoiImages(){
        String siteId = params.siteId
        String poiId = params.poiId
        String role = params.role
        String sort = params.sort?:'dateTaken';
        String order = params.order?:'desc'
        Integer max = (params.max?:5) as Integer
        Integer offset = (params.offset?:0) as Integer
        Long userId = params.long('userId')
        Map result;
        Map mongoParams = [:];
        if(role){
            mongoParams.role = role
        }

        if(siteId && poiId){
            mongoParams.poiId = poiId
            mongoParams.siteId = siteId
            try{
                 result = siteService.getPoiImages(mongoParams, userId, max, offset, sort, order)
            } catch (MongoExecutionTimeoutException mete){
                render(text: 'Database timeout exception ' + mete.message, status: HttpStatus.SC_REQUEST_TIMEOUT)
            } catch(Exception e){
                render(text: 'An exception occurred: ' + e.message, status: HttpStatus.SC_INTERNAL_SERVER_ERROR)
            }

            render text: result as JSON, contentType: 'application/json'
        } else {
            response.sendError(HttpStatus.SC_BAD_REQUEST, 'Site id and Poi id must be provided')
        }
    }

    def lookupLocationMetadataForSite() {
        def site = request.JSON
        render siteService.lookupGeographicFacetsForSite(site) as JSON
    }

    def lookupLocationMetadataForSiteById(String id) {
        Map site = siteService.get(id)
        render siteService.lookupGeographicFacetsForSite(site) as JSON
    }

    def updateGeographicFacetsForSite(String id) {
        Map site = siteService.get(id)
        if (site && site.extent && site.extent.geometry) {
            Map facets = siteService.lookupGeographicFacetsForSite(site, params.getList('fids'))
            site.extent.geometry += facets
            siteService.update([extent:site.extent], id)
            render text:"OK"
        }
        else {
            render text:"Site has no geometry"
        }

    }

    def uniqueName(String id) {
        def name = params.name
        def entityType = params.entityType
        def result = [ value: !siteService.sitesContainsName(id, entityType, name) ]
        respond result
    }

    /**
     * Associate a project to a site
     * @param id site id
     * @param projectId
     * @return
     */
    def addProject(String id){
        String projectId = params.projectId
        if(id && projectId){
            Map result = siteService.addProject(id, projectId)
            asJson result
        } else {
            render status: HttpStatus.SC_BAD_REQUEST, text: "Site id and project id must be provided."
        }
    }

    /**
     * Returns a Map with keys projectId and sites.
     * The value of the sites key is an array of geojson Features that contains all of the sites for the supplied project.
     * (Note that it does not return a FeatureCollection as some sites may themselves be a FeatureCollection)
     *
     */
    @RequireApiKey
    def projectSites(String id) {
        List features = siteService.sitesForProject(id).collect({siteService.toGeoJson(siteService.toMap(it))})
        Map result = [projectId:id, sites: features]
        render result as JSON
    }
}