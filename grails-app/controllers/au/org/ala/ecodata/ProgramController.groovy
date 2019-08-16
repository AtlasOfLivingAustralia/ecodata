package au.org.ala.ecodata

@RequireApiKey
class ProgramController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [get:'GET', findByName: 'GET', search:'GET', findAllForUser: 'GET', update:['PUT', 'POST'], delete:'DELETE']

    ProgramService programService
    ElasticSearchService elasticSearchService

    def get(String id) {
        respond programService.get(id, false)
    }

    def findByName(String name) {
        respond programService.findByName(name)
    }

    def update(String id) {
        if (!id) {
            respond programService.create(request.JSON)
        }
        else {
            respond programService.update(id, request.JSON)
        }
    }

    def delete(String id) {
        respond programService.delete(id, params.getBoolean('destroy', false))
    }

    def search() {
        elasticSearchService.search(params.query,[:], ElasticIndex.DEFAULT_INDEX)
    }

    def findAllForUser(String id) {
        respond programService.findAllProgramsForUser(id)
    }
}
