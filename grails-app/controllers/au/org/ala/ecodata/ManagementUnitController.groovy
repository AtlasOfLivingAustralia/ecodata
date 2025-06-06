package au.org.ala.ecodata

@au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.readScope"])
class ManagementUnitController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [get:'GET', findByName: 'GET', search:'GET', findAllForUser: 'GET', update:['PUT', 'POST'], delete:'DELETE',getManagementUnits: ['POST'], findAllForUser:'GET', managementUnitSiteMap: ['GET', 'POST']]

    ManagementUnitService managementUnitService
    ElasticSearchService elasticSearchService
    ActivityService activityService
    DownloadService downloadService
    UserService userService

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json;charset=UTF-8")
        model
    }

    def get(String id) {
        ManagementUnit mu = managementUnitService.get(id, false)
        respond mu
    }

    /**
     * @param The request body should contain a JSON object with a single attribute: ids which is an array
     * of managementUnitIds to retrieve.
     * @return a list of management units with the ids matching the request.
     */
    def getManagementUnits() {
        String[] ids =  request.getJSON()?.managementUnitIds
        if(ids){
            List mues =  managementUnitService.get(ids)
            respond mues
        }
        else{
            respond []
        }
    }

    def findByName(String name) {
        respond managementUnitService.findByName(name)
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def update(String id) {
        if (!id) {
            ManagementUnit mu = new ManagementUnit()
            bindData(mu, request.JSON, [include:ManagementUnit.bindingProperties])
            respond managementUnitService.create(mu)
        }
        else {
            ManagementUnit mu = managementUnitService.get(id)
            if (!mu) {
                respond null
            }
            else {
                String originalName = mu.name
                bindData(mu, request.JSON, [include:ManagementUnit.bindingProperties])
                ManagementUnit savedMu = managementUnitService.save(mu)
                // Update the project search index if the program name has changed.
                if (originalName != savedMu.name) {
                    elasticSearchService.reindexProjectsWithCriteriaAsync(managementUnitId:id)
                }
                respond savedMu
            }
        }
    }

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def delete(String id) {
        respond managementUnitService.delete(id, params.getBoolean('destroy', false))
    }

    def search() {
        elasticSearchService.search(params.query,[:], ElasticIndex.DEFAULT_INDEX)
    }

    def findAllForUser(String id) {
        respond managementUnitService.findAllManagementUnitsForUser(id)
    }

    /**
     *
     * The POST body can optionally contain a JSON formatted attribute "managementUnitIds" which supplies a list
     * of management unit ids to query.  Otherwise all ManagementUnit sites will be returned.
     * @return a geojson FeatureCollection containing the management unit boundaries including a geojson property
     * "type" which can be used to colour a map produced from the collection.
     */
    def managementUnitSiteMap() {
        List ids = null
        if (request.method == 'POST') {
            ids = request.JSON?.managementUnitIds
        }
        respond managementUnitService.managementUnitSiteMap(ids)
    }

    /**
     * Get financial years of managment unit reports cover
     * @return
     */
    def getReportPeriods(){
        List financialYears = managementUnitService.getFinancialYearPeriods()
        response.setContentType("application/json")
        respond financialYears
    }
}
