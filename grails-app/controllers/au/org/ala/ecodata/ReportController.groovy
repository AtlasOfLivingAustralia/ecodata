package au.org.ala.ecodata

class ReportController {

    static responseFormats = ['json', 'xml']
    def reportingService

    def get(String id) {
        respond reportingService.get(id)
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

    def submit(String id) {
        reportingService.submit(id)
    }

    def approve(String id) {
        reportingService.approve(id)
    }

    def returnForRework(String id) {
        reportingService.returnForRework(id)
    }
}
