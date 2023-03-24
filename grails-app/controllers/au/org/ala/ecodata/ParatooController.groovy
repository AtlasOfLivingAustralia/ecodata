package au.org.ala.ecodata


import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooCollectionId
import au.org.ala.web.AuthService

import org.apache.http.HttpStatus
import org.springframework.validation.Errors

// Requiring these scopes will guarantee we can get a valid userId out of the process.
@au.ala.org.ws.security.RequireApiKey(scopes = ['ala', 'openid'])
class ParatooController {

    static responseFormats = ['json']
    static allowedMethods =
            [userProjects:'GET',
             protocolReadCheck:'GET',
             protocolWriteCheck:'GET',
             validateToken: 'GET',
             mintCollectionId: 'POST',
             submitCollection: 'POST',
             collectionIdStatus: 'GET'
            ]

    ParatooService paratooService
    AuthService authService

    private error(int status, String message) {
        respond([message:message, code: status], status:status)
    }

    private error(Errors errors) {
        respond errors
    }

    def userProjects() {
        respond projects:paratooService.userProjects(authService.userId)
    }

    def validateToken() {
       respond ([valid:true], status:200)
    }

    /**
     * Used for both read and write - if we need to take into account
     * read only users we need to separate these calls
     */
    def protocolCheck(String projectId, Integer protocolId) {
        if (!projectId || !protocolId) {
            error(HttpStatus.SC_BAD_REQUEST, "Bad request")
            return
        }
        String userId = authService.userId
        boolean hasProtocol = protocolCheck(userId, projectId, protooclId)

        respond([isAuthorized:hasProtocol], status:HttpStatus.SC_OK)
    }

    def mintCollectionId(ParatooCollectionId collectionId) {

        if (collectionId.hasErrors()) {
            error(collectionId.errors)
        }
        else {
            boolean hasProtocol = paratooService.protocolCheck(authService.userId, collectionId.projectId, collectionId.protocol.id)
            if (hasProtocol) {
                respond([orgMintedIdentfier:Identifiers.getNew(true, null)])
            }
            else {
                error(HttpStatus.SC_FORBIDDEN, "Project / protocol combination not available")
            }
        }
    }

    def submitCollection(ParatooCollection collection) {

        if (collection.hasErrors()) {
            error(collection.errors())
        }
        else {
            boolean hasProtocol = paratooService.protocolCheck(authService.userId, collectionId.projectId, collectionId.protocol.id)
            if (hasProtocol) {
                // Create a data set and attach to the project.
                Map result = paratooService.createCollection(collection)
                if (!result.error) {
                    respond([success: true])
                } else {
                    error(HttpStatus.SC_INTERNAL_SERVER_ERROR, result.error)
                }
            }
            else {
                error(HttpStatus.SC_FORBIDDEN, "Project / protocol combination not available")
            }
        }
    }

    def collectionIdStatus(String collectionId) {

        List projects = paratooService.userProjects(authService.userId, false)

        Project projectWithMatchingDataSet = projects?.find{
            it.dataSets?.find{it.dataSetId == collectionId }
        }

        respond([isSubmitted:(projectWithMatchingDataSet != null)])
    }

    def handleException(Exception e) {
        log.error("An uncaught error was thrown processing: ${request.uri}", e)
        error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "An error was encountered processing the request.")
    }
}
