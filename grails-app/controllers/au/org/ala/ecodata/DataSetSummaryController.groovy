package au.org.ala.ecodata

import org.apache.http.HttpStatus

class DataSetSummaryController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [update:['POST', 'PUT']]

    ProjectService projectService

    /** Updates a single dataset for a project */
    def update(String projectId) {
        Map dataSet = request.JSON

        if (!projectId && !dataSet.projectId) {
            respond status: 400, message: "projectId is required"
            return
        }

        projectId = projectId || dataSet.projectId

        if (dataSet.projectId && dataSet.projectId != projectId) {
            respond status: HttpStatus.SC_BAD_REQUEST, message: "projectId must match the data set projectId"
            return
        }

        respond projectService.updateDataSet(projectId, dataSet)
    }
}
