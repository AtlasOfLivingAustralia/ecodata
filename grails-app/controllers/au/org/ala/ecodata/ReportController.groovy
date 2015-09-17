package au.org.ala.ecodata

import grails.rest.RestfulController

class ReportController {

    static responseFormats = ['json', 'xml']
    def reportingService

    def get(String id) {
        reportingService.get(id, false)
    }

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
