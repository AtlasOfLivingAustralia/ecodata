package au.org.ala.ecodata

@RequireApiKey
class ManagementUnitController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [get:'GET', findByName: 'GET', search:'GET', findAllForUser: 'GET', update:['PUT', 'POST'], delete:'DELETE',getManagementUnits: ['POST'],findAllForUser:'GET']

    ManagementUnitService managementUnitService
    ElasticSearchService elasticSearchService

    def get(String id) {
        ManagementUnit mu = managementUnitService.get(id, false)
        respond mu
    }

    /**
     * A list of programIds
     * @return a list of programs
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

    def update(String id) {
        if (!id) {
            respond managementUnitService.create(request.JSON)
        }
        else {
            respond managementUnitService.update(id, request.JSON)
        }
    }

    def delete(String id) {
        respond managementUnitService.delete(id, params.getBoolean('destroy', false))
    }

    def search() {
        elasticSearchService.search(params.query,[:], ElasticIndex.DEFAULT_INDEX)
    }

    def findAllForUser(String id) {
        respond managementUnitService.findAllManagementUnitsForUser(id)
    }
}
