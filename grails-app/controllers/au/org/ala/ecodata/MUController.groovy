package au.org.ala.ecodata

@RequireApiKey
class MUController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [get:'GET', findByName: 'GET', search:'GET', findAllForUser: 'GET', update:['PUT', 'POST'], delete:'DELETE']

    MUService muService
    ElasticSearchService elasticSearchService

    def get(String id) {
        respond muService.get(id, false)
    }

    def findByName(String name) {
        respond muService.findByName(name)
    }

    def update(String id) {
        if (!id) {
            respond muService.create(request.JSON)
        }
        else {
            respond muService.update(id, request.JSON)
        }
    }

    def delete(String id) {
        respond muService.delete(id, params.getBoolean('destroy', false))
    }

    def search() {
        elasticSearchService.search(params.query,[:], ElasticIndex.DEFAULT_INDEX)
    }

    def findAllForUser(String id) {
        respond muService.findAllMUsForUser(id)
    }
}
