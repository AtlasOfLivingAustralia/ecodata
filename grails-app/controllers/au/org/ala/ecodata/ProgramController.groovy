package au.org.ala.ecodata

@au.ala.org.ws.security.RequireApiKey(scopes=["ecodata/read"])
class ProgramController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [get:'GET', findByName: 'GET', search:'GET', listOfAllPrograms:'GET', findAllForUser: 'GET', update:['PUT', 'POST'], delete:'DELETE', getPrograms: ['POST']]

    ProgramService programService
    ElasticSearchService elasticSearchService

    def get(String id) {
        respond programService.get(id, false)
    }

    /**
     * A list of programIds
     * @return a list of programs
     */
    def getPrograms() {
        String[] ids =  request.getJSON()?.programIds
        if(ids){
            Program[] programs =  programService.get(ids)
            respond programs
        }
        else
            respond []
    }

    def findByName(String name) {
        respond programService.findByName(name)
    }

    @au.ala.org.ws.security.RequireApiKey(scopes=["ecodata/write"])
    def update(String id) {
        if (!id) {
            respond programService.create(request.JSON)
        }
        else {
            respond programService.update(id, request.JSON)
        }
    }

    @au.ala.org.ws.security.RequireApiKey(scopes=["ecodata/write"])
    def delete(String id) {
        respond programService.delete(id, params.getBoolean('destroy', false))
    }

    def search() {
        elasticSearchService.search(params.query,[:], ElasticIndex.DEFAULT_INDEX)
    }

    def findAllForUser(String id) {
        respond programService.findAllProgramsForUser(id)
    }

    def listOfAllPrograms(){
        respond programService.findAllProgramList()
    }

}
