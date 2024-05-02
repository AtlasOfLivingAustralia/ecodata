package au.org.ala.ecodata

import org.apache.http.HttpStatus

class DataSetSummaryController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [update:['POST', 'PUT'], delete:'DELETE']

    ProjectService projectService

    /** Updates a single dataset for a project */
    def update(String projectId) {
        Map dataSet = request.JSON
        projectId = projectId ?: dataSet.projectId

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

    def delete(String projectId, String dataSetId) {
        if (!projectId || !dataSetId) {
            render status: HttpStatus.SC_BAD_REQUEST, text: "projectId and dataSetId are required"
            return
        }
        respond projectService.deleteDataSet(projectId, dataSetId)
    }
}
