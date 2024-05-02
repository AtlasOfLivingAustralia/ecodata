package au.org.ala.ecodata

import org.apache.http.HttpStatus

class DataSetSummaryController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [update:['POST', 'PUT']]

    ProjectService projectService

    /** Updates a single dataset for a project */
    def update(String projectId) {
        Map dataSet = request.JSON

        if (!projectId) {
            projectId = dataSet.projectId
        }
        if (!projectId) {
            render status:  HttpStatus.SC_BAD_REQUEST, text: "projectId is required"
            return
        }

        if (dataSet.projectId && dataSet.projectId != projectId) {
            render status: HttpStatus.SC_BAD_REQUEST, text: "projectId must match the data set projectId"
            return
        }

        respond projectService.updateDataSet(projectId, dataSet)
    }
}
