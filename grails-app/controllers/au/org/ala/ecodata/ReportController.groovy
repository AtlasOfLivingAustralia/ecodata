package au.org.ala.ecodata


class ReportController {

    static responseFormats = ['json', 'xml']
    def reportingService

    def get(String id) {
        reportingService.get(id, false)
    }

    @RequireApiKey
    def update(String id) {
        if (!id) {
            respond reportingService.create(request.JSON)
        }
        else {
            respond reportingService.update(id, request.JSON)
        }
    }

    def find(String entity, String id) {
        respond reportingService.findAllByOwner(entity + 'Id', id)
    }

    def findByUserId(String id) {
        respond reportingService.findAllForUser(id)
    }

    def search() {
        def searchCriteria = request.JSON

        def reportList = reportingService.search(searchCriteria)
        respond reportList
    }

    @RequireApiKey
    def submit(String id) {
        respond reportingService.submit(id)
    }

    @RequireApiKey
    def approve(String id) {
        respond reportingService.approve(id)
    }

    @RequireApiKey
    def returnForRework(String id) {
        respond reportingService.returnForRework(id)
    }

}
