package au.org.ala.ecodata

import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooCollectionId
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
    UserService userService

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

    /**
     * Monitor data often needs to be re-synced with MERIT/ecodata due to errors/updates.
     * We are requiring the user token and using the paratoo authorization check because this token
     * will be passed to Monitor to perform the resync.
     */
    @au.ala.org.ws.security.RequireApiKey(scopes = ["profile", "openid"])
    def resync(String projectId, String dataSetId) {
        String userId = userService.currentUserDetails.userId
        Project project = Project.findByProjectId(projectId)

        Map dataSet = project?.custom?.dataSets?.find { it.dataSetId == dataSetId }

        if (project && dataSet) {
            ParatooCollectionId paratooCollectionId = ParatooCollectionId.fromMap(dataSet.surveyId)
            String protocolId = paratooCollectionId.getProtocolId()
            if (!protocolId || !paratooService.protocolWriteCheck(userId, projectId, protocolId)) {
                render status: HttpStatus.SC_UNAUTHORIZED
                return
            }

            log.info("Resyncing data set $dataSetId in project $projectId by user $userId")

            ParatooCollection collection = new ParatooCollection(orgMintedUUID: dataSetId, coreProvenance:  [:])
            // The access level is not required for a resync and has been checked in MERIT.  This is to avoid
            // requiring the user to add themselves to the project ACL before being able to resync as it will
            // be generally be done by high level users such as site admins.
            ParatooProject paratooProject = paratooService.paratooProjectFromProject(project, null)
            boolean canModifySite = false
            if (dataSet.siteId) {
                Map site = siteService.get(dataSet.siteId)
                canModifySite = projectService.canModifyDataSetSite(site, project)
            }

            paratooService.submitCollection(collection, paratooProject, userId, canModifySite)
            render text: [message: "Submitted request to fetch data for dataSet $dataSetId in project $projectId by user $userId"] as JSON, status: HttpStatus.SC_OK, contentType: 'application/json'
        }
        else {
            render text: [message: "Project not found"] as JSON, status: HttpStatus.SC_NOT_FOUND
        }
    }
}
