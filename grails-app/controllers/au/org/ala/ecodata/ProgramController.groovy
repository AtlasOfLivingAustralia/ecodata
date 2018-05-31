package au.org.ala.ecodata

class ProgramController {

    static responseFormats = ['json', 'xml']
    ProgramService programService
    ElasticSearchService elasticSearchService

    def get(String id) {
        respond programService.get(id, false)
    }

    def findByName(String name) {
        respond programService.findByName(name)
    }

    @RequireApiKey
    def update(String id) {
        if (!id) {
            respond programService.create(request.JSON)
        }
        else {
            respond programService.update(id, request.JSON)
        }
    }

    @RequireApiKey
    def delete(String id) {
        respond programService.delete(id, params.getBoolean('destroy', false))
    }

    def search() {
        elasticSearchService.search(params.query,[:], ElasticIndex.DEFAULT_INDEX)
    }
}
