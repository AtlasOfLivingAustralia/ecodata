package au.org.ala.ecodata

import au.ala.org.ws.security.SkipApiKeyCheck
import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooCollectionId
import au.org.ala.ecodata.paratoo.ParatooProject
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.security.SecuritySchemes
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.annotations.servers.ServerVariable
import org.apache.http.HttpStatus
import org.springframework.validation.Errors

import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path

// Requiring these scopes will guarantee we can get a valid userId out of the process.
@au.ala.org.ws.security.RequireApiKey
@OpenAPIDefinition(
        info = @Info(
        title = "Ecodata APIs",
        description = "APIs to query the paratoo org interface in ecodata",
        termsOfService = "https://www.ala.org.au/terms",
        contact = @Contact(name = "Support", email = "support@ala.org.au"),
        license = @License(name = "Mozilla Public License 1.1", url =  "https://www.mozilla.org/en-US/MPL/1.1/"),
        version = "4.1-SNAPSHOT"
        )
)
@Server(url = "https://api.test.ala.org.au/{basePath}",
    variables = [@ServerVariable(name = "basePath", defaultValue = "ecodata")])
@SecuritySchemes([
    @SecurityScheme(name = "openIdConnect",
    type = SecuritySchemeType.OPENIDCONNECT,
    openIdConnectUrl = "https://auth.ala.org.au/cas/oidc/.well-known"),
    @SecurityScheme(name = "oauth",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(clientCredentials = @OAuthFlow(authorizationUrl = "https://auth.ala.org.au/cas/oidc/authorize",
            tokenUrl = "https://auth.ala.org.au/cas/oidc/token",
            refreshUrl = "https://auth.ala.org.au/cas/oidc/refresh")
        )
    ),
    @SecurityScheme(name = "openIdConnect",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT"
    )])
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

    @GET
    @Path("/user-projects")
    @Operation(summary = "Gets all projects for an authenticated user", description = "Gets all projects that a user is assigned to",
    responses = [@ApiResponse(responseCode = "200", description = "Projects assigned to the user", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Project.class)))),
    @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")], tags = ["Org Interface"])
    def userProjects() {
        respond projects:paratooService.userProjects(userService.currentUserDetails.userId)
    }

    @SkipApiKeyCheck
    @POST
    @Path("/validate-token")
    @Operation(summary = "Validates JWT tokens issued by Org", description = "Before Core makes a PDP request via the project membership enforcer, it must check that the JWT it was provided is valid. As Org issues these JWTs, Core must check with Org to ensure the validity.",
    responses = [@ApiResponse(responseCode = "200", description = "", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class)))
    ], tags = ["Org Interface"])
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

    @GET
    @SecurityRequirements([@SecurityRequirement(name = "jwt")])
    @Path("/pdp/{projectId}/{protocolId}/read")
    @Operation(description = "Checks that a user has read permissions for the particular project and protocol", responses = [@ApiResponse(responseCode = "200", description = "Returns if user has read permission for supplied project and protocol", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")], tags = ["Org Interface"], summary = "For authorizing with the PDP which checks read permissions")
    def hasReadAccess(@RequestBody(required = true, content = @Content(schema = @Schema(type = "string"))) String projectId,
                      @RequestBody(required = true, content = @Content(schema = @Schema(type = "integer"))) Integer protocolId) {
        protocolCheck(projectId, protocolId, { String userId, String prjId, Integer proId ->
            paratooService.protocolReadCheck(userId, prjId, proId)
        })
    }

    @GET
    @Path("/pdp/{projectId}/{protocolId}/write")
    @Operation(description = "Checks that a user has write permissions for the particular project and protocol", responses = [@ApiResponse(responseCode = "200", description = "Returns if user has read permission for supplied project and protocol", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")], tags = ["Org Interface"], summary = "For authorizing with the PDP which checks write permissions")
    def hasWriteAccess(@RequestBody(required = true, content = @Content(schema = @Schema(type = "string"))) String projectId,
                       @RequestBody(required = true, content = @Content(schema = @Schema(type = "integer"))) Integer protocolId) {
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

    @POST
    @Path("/mint-identifier")
    @Operation(description = "Creates an identifier that is stored in Org as a cross-reference to Core. Allows a particular survey to be derived from the data contained in the identifier. User's may not have connection to the server when they are performing a collection, so an identifier is created locally. When they are ready to submit the collection (i.e., have access to the server), they hit this endpoint to have the actual identifier minted. The identifier is encrypted using SJCL and is returned to the user as such.",
    responses = [@ApiResponse(responseCode = "200", description = "Returns the encrypted minted identifier", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))), @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")], tags = ["Org Interface"])
    def mintCollectionId(@RequestBody(description = "Note that the survey ID is created by the client application and is arbitrarily defined. It should simply uniquely identify a particular survey",
            required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParatooCollectionId.class))) ParatooCollectionId collectionId) {

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

    @POST
    @Path("/collection")
    @Operation(responses = [@ApiResponse(responseCode = "200", description = "Returns true if Org successfully stored the supplied identifier", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")], tags = ["Org Interface"])
    def submitCollection(@RequestBody(description = "The event time for this request is not the same as the one for minting identifiers. An identifier's event time denotes when the collection was made, this event time denotes when the collection was submitted to the server.",
                        required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParatooCollection.class))) ParatooCollection collection) {

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

    @GET
    @Path("/status/{identifier}")
    @Operation(responses = [@ApiResponse(responseCode = "200", description = "Returns true if Org has stored the supplied identifier", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
    @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")], tags = ["Org Interface"])
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
