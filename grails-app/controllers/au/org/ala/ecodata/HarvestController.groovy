package au.org.ala.ecodata

import grails.converters.JSON
import org.apache.http.HttpStatus

import java.text.SimpleDateFormat

@au.ala.org.ws.security.RequireApiKey(scopesFromProperty=["app.readScope"])
class HarvestController {
    static responseFormats = ['json']

    RecordService recordService
    ProjectService projectService
    ProjectActivityService projectActivityService
    UserService userService

    /**
     * List of supported data resource id available for harvesting.
     * Note: Data Provider must be BioCollect or MERIT
     *
     * @param max = number
     * @param offset = number
     * @param order = lastUpdated
     * @param sort = asc | desc
     *
     */
    def listHarvestDataResource() {
        def result, error
        try {
            if (params.max && !params.max?.isNumber()) {
                error = "Invalid parameter max"
            } else if (params.offset && !params.offset?.isNumber()) {
                error = "Invalid parameter offset"
            } else if (params.sort && params.sort != "asc" && params.sort != "desc") {
                error = "Invalid parameter sort"
            } else if (params.order && params.order != "lastUpdated") {
                error = "Invalid parameter order (Expected: lastUpdated)"
            }

            if (!error) {
                def pagination = [
                        max   : params.max ?: 10,
                        offset: params.offset ?: 0,
                        order : params.order ?: 'asc',
                        sort  : params.sort ?: 'lastUpdated'
                ]

                result = projectService.listProjectForAlaHarvesting(pagination)

            } else {
                response.status = HttpStatus.SC_BAD_REQUEST
                result = [status: 'error', error: error]
            }

        } catch (Exception ex) {
            response.status = HttpStatus.SC_INTERNAL_SERVER_ERROR
            result << [status: 'error', error: "Unexpected error."]
        }

        response.setContentType("application/json")
        render result as JSON
    }

    /**
     * List records associated with the given data resource id
     * Data Provider must be BioCollect or MERIT
     * @param id dataResourceId
     * @param max = number
     * @param offset = number
     * @param order = lastUpdated
     * @param sort = asc | desc | default:asc
     * @param lastUpdated = date | dd/MM/yyyy | default:null
     * @param status = active | deleted | default:active
     * @deprecated ALA's records harvester will use getDarwinCoreArchiveForProject once Events system is setup.
     * To access it use archiveURL property from {@link HarvestController#listHarvestDataResource}.
     */
    @Deprecated
    def listRecordsForDataResourceId () {
        def result = [], error, project
        Date lastUpdated = null
        try {
            if(!params.id) {
                error = "Invalid data resource id"
            } else if (params.max && !params.max.isNumber()) {
                error = "Invalid max parameter vaue"
            } else if (params.offset && !params.offset.isNumber()) {
                error = "Invalid offset parameter vaue"
            } else if (params.order && params.order != "asc" && params.order != "desc") {
                error = "Invalid order parameter value (expected: asc, desc)"
            } else if (params.sort && params.sort != "lastUpdated") {
                error = "Invalid sort parameter value (expected: lastUpdated)"
            } else if (params.status && params.status != "active" && params.status != "deleted") {
                error = "Invalid status parameter value (expected: active or deleted)"
            } else if(params.id){
                project = projectService.getByDataResourceId(params.id, 'active', 'basic')
                error = !project ? 'No valid project found for the given data resource id' : !project.alaHarvest ? "Harvest disabled for data resource id - ${params.id}" : ''
            }

            if (params.lastUpdated) {
                try{
                    def df = new SimpleDateFormat("dd/MM/yyyy")
                    lastUpdated = df.parse(params.lastUpdated)
                } catch (Exception ex) {
                    error = "Invalid lastUpdated format (Expected date format - Example: dd/MM/yyyy"
                }
            }

            if (!error && project) {
                def args = [
                        max     : params.max ?: 10,
                        offset  : params.offset ?: 0,
                        order   : params.order ?: 'asc',
                        sort    : params.sort ?: 'lastUpdated',
                        status  : params.status ?: 'active',
                        projectId: project.projectId
                ]

                List<String> restrictedProjectActivities = projectActivityService.listRestrictedProjectActivityIds(null, params.id)
                log.debug("Retrieving results...")
                result = recordService.listByProjectId(args, lastUpdated, restrictedProjectActivities)
                result?.list?.each {
                    it.projectName = project?.name
                    it.license = recordService.getLicense(it)
                }
            } else {
                response.status = HttpStatus.SC_BAD_REQUEST
                log.error(error.toString())
                result = [status: 'error', error: error]
            }

        } catch (Exception ex) {
            response.status = HttpStatus.SC_INTERNAL_SERVER_ERROR
            log.error(ex.toString())
            result << [status: 'error', error: "Unexpected error."]
        }

        response.setContentType("application/json")
        render result as JSON
    }

    /**
     * Get Darwin Core Archive for a project that has ala harvest enabled.
     * @param projectId
     * @return
     * At the moment, you need to add their IP address to whitelist.
     */
    def getDarwinCoreArchiveForProject (String projectId) {
        if (projectId) {
            Project project = Project.findByProjectId(projectId)
            if(project?.alaHarvest) {
                // This is done to get the correct URL for documents.
                String hostname = project.isMERIT ? grailsApplication.config.getProperty("fieldcapture.baseURL") : grailsApplication.config.getProperty("biocollect.baseURL")
                DocumentHostInterceptor.documentHostUrlPrefix.set(hostname)
                String filename = "darwin-core-${projectId}.zip"
                boolean force = params.getBoolean("force", false)
                response.setContentType("application/zip")
                response.setHeader("Content-Disposition", "attachment; filename=\"${filename}\"");
                recordService.getDarwinCoreArchiveForProjectFromDiskOrOnDemand(response.outputStream, project, force)
            } else
                respond([error: "project not found or ala harvest flag is switched off"], status: HttpStatus.SC_NOT_FOUND)
        } else {
            respond([error: "projectId is required"], status: HttpStatus.SC_BAD_REQUEST)
        }
    }
}