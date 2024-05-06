package au.org.ala.ecodata

import org.apache.http.HttpStatus

class DataSetSummaryController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [update:['POST', 'PUT'], delete:'DELETE', bulkUpdate: 'POST']

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

    /**
     * Updates multiple data sets for a project.
     * This endpoint exists to support the use case of associating multiple data sets with a
     * report and updating their publicationStatus when the report is submitted/approved.
     *
     * This method expects the projectId to be supplied via the URL and the data sets to be supplied in the request
     * body as a JSON object with key="dataSets" and value=List of data sets.
     */
    def bulkUpdate(String projectId) {
        Map postBody =  request.JSON
        List dataSets = postBody?.dataSets

        if (!projectId) {
            render status:  HttpStatus.SC_BAD_REQUEST, text: "projectId is required"
            return
        }

        for (Map dataSet in dataSets) {
            if (dataSet.projectId && dataSet.projectId != projectId) {
                render status: HttpStatus.SC_BAD_REQUEST, text: "projectId must match the projectId in all supplied data sets"
                return
            }
        }

        respond projectService.updateDataSets(projectId, dataSets)
    }

    def delete(String projectId, String dataSetId) {
        if (!projectId || !dataSetId) {
            render status: HttpStatus.SC_BAD_REQUEST, text: "projectId and dataSetId are required"
            return
        }
        respond projectService.deleteDataSet(projectId, dataSetId)
    }
}
