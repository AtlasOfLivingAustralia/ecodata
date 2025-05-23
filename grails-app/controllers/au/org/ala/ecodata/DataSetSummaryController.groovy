package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooProject
import grails.converters.JSON
import org.apache.http.HttpStatus
@au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.readScope"])
class DataSetSummaryController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [update:['POST', 'PUT'], delete:'DELETE', bulkUpdate: 'POST']

    ProjectService projectService
    ParatooService paratooService
    SiteService siteService

    /** Updates a single dataset for a project */
    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
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
    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
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

    @au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.writeScope"])
    def delete(String projectId, String dataSetId) {
        if (!projectId || !dataSetId) {
            render status: HttpStatus.SC_BAD_REQUEST, text: "projectId and dataSetId are required"
            return
        }
        respond projectService.deleteDataSet(projectId, dataSetId)
    }

    /** Monitor data often needs to be re-synced with MERIT/ecodata due to errors/updates. */
    def reImportDataSetFromMonitor(String id, String dataSetId) {
        String userId = userService.currentUserDetails.userId
        List<ParatooProject> projects = paratooService.userProjects(userId)
        ParatooProject project = projects.find {it.project.projectId == id }
        ParatooCollection collection = new ParatooCollection(orgMintedUUID: dataSetId, coreProvenance:  [:])

        if (project) {
            Map dataSet = project.project.custom.dataSets.find {it.dataSetId == dataSetId}
            boolean canModifySite = false
            if (dataSet.siteId) {
                Map site = siteService.get(dataSet.siteId)
                canModifySite = projectService.canModifyDataSetSite(dataSetId, site, project.project)
            }

            paratooService.submitCollection(collection, project, userId, canModifySite)
            render text: [message: "Submitted request to fetch data for dataSet $dataSetId in project $id by user $userId"] as JSON, status: HttpStatus.SC_OK, contentType: 'application/json'
        }
        else {
            render text: [message: "Project not found"] as JSON, status: HttpStatus.SC_NOT_FOUND
        }
    }
}
