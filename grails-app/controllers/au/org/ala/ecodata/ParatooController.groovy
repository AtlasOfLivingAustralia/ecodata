package au.org.ala.ecodata

import au.ala.org.ws.security.SkipApiKeyCheck
import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooCollectionId
import au.org.ala.ecodata.paratoo.ParatooProject
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
             validateToken: 'POST',
             mintCollectionId: 'POST',
             submitCollection: 'POST',
             collectionIdStatus: 'GET'
            ]

    ParatooService paratooService
    UserService userService
    WebService webService

    private error(int status, String message) {
        respond([message:message, code: status], status:status)
    }

    private error(Errors errors) {
        respond errors
    }

    def userProjects() {
        respond projects:paratooService.userProjects(userService.currentUserDetails.userId)
    }

    @SkipApiKeyCheck
    def validateToken() {
        // Possibly an implementation side-effect of the paratoo client but the token is passed in the body here
        // rather than the header.  We extract it and call a protected method to check the token....
        String token = request.JSON?.token
        if (!token) {
            respond([message:"Missing token in body"], status:HttpStatus.SC_BAD_REQUEST)
            return
        }

        // It also has the Bearer string attached according to the implementation so we can use it in the header as is.
        String url = grailsLinkGenerator.link(action:'noop', absolute:true)

        // Make a call to the URL with the token in the header
        Map response = webService.getJson(url, null, [Authorization:token], false)

        boolean valid = (response && response.statusCode == HttpStatus.SC_OK)
        render(valid as String)
    }

    /**
     * This method exists so the validateToken method can call it to get the framework to validate the JWT.
     * If it is reached, it always returns OK
     */
    def noop() {
        respond([statusCode:HttpStatus.SC_OK])
    }

    def hasReadAccess(String projectId, Integer protocolId) {
        protocolCheck(projectId, protocolId, { String userId, String prjId, Integer proId ->
            paratooService.protocolReadCheck(userId, prjId, proId)
        })
    }

    def hasWriteAccess(String projectId, Integer protocolId) {
        protocolCheck(projectId, protocolId, { String userId, String prjId, Integer proId ->
            paratooService.protocolWriteCheck(userId, prjId, proId)
        })
    }

    /**
     * Used for both read and write - if we need to take into account
     * read only users we need to separate these calls
     */
    private void protocolCheck(String projectId, Integer protocolId, Closure checkMethod) {
        if (!projectId || !protocolId) {
            error(HttpStatus.SC_BAD_REQUEST, "Bad request")
            return
        }
        String userId = userService.currentUserDetails.userId
        boolean hasAccessToProtocol = checkMethod(userId, projectId, protocolId)

        respond([isAuthorized:hasAccessToProtocol], status:HttpStatus.SC_OK)
    }

    def mintCollectionId(ParatooCollectionId collectionId) {

        if (collectionId.hasErrors()) {
            error(collectionId.errors)
        }
        else {
            String userId = userService.currentUserDetails.userId
            boolean hasProtocol = paratooService.protocolWriteCheck(userId, collectionId.projectId, collectionId.protocol.id)
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
            error(collection.errors)
        }
        else {
            String userId = userService.currentUserDetails.userId
            boolean hasProtocol = paratooService.protocolWriteCheck(userId, collection.projectId, collection.protocol.id)
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
        if (!collectionId) {
            error(HttpStatus.SC_BAD_REQUEST, "Bad request")
            return
        }
        String userId = userService.currentUserDetails.userId
        ParatooProject projectWithMatchingDataSet = paratooService.findDataSet(userId, collectionId)

        respond([isSubmitted:(projectWithMatchingDataSet != null)])
    }

    def options() {
        respond([statusCode:HttpStatus.SC_NO_CONTENT])
    }

    def handleException(Exception e) {
        log.error("An uncaught error was thrown processing: ${request.uri}", e)
        error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "An error was encountered processing the request.")
    }
}
