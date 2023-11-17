package au.org.ala.ecodata

import au.ala.org.ws.security.SkipApiKeyCheck
import au.org.ala.ecodata.paratoo.ParatooCollection
import au.org.ala.ecodata.paratoo.ParatooCollectionId
import au.org.ala.ecodata.paratoo.ParatooPlotSelection
import au.org.ala.ecodata.paratoo.ParatooPlotSelectionData
import au.org.ala.ecodata.paratoo.ParatooProject
import au.org.ala.ecodata.paratoo.ParatooToken
import groovy.util.logging.Slf4j
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.*
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.annotations.servers.ServerVariable
import org.apache.http.HttpStatus
import org.springframework.validation.Errors

import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path

// Requiring these scopes will guarantee we can get a valid userId out of the process.
@au.ala.org.ws.security.RequireApiKey
@Slf4j
@OpenAPIDefinition(
        info = @Info(
                title = "Ecodata APIs",
                description = "APIs to query the paratoo org interface in ecodata",
                termsOfService = "https://www.ala.org.au/terms",
                contact = @Contact(name = "Support", email = "support@ala.org.au"),
                license = @License(name = "Mozilla Public License 1.1", url = "https://www.mozilla.org/en-US/MPL/1.1/"),
                version = "4.1-SNAPSHOT"
        )
)
@Server(url = "https://api.test.ala.org.au/{basePath}",
        variables = [@ServerVariable(name = "basePath", defaultValue = "ecodata")])
@SecuritySchemes([
        @SecurityScheme(name = "openIdConnect",
                type = SecuritySchemeType.OPENIDCONNECT,
                openIdConnectUrl = "https://auth-test.ala.org.au/cas/oidc/.well-known"
        ),
        @SecurityScheme(name = "oauth",
                type = SecuritySchemeType.OAUTH2,
                flows = @OAuthFlows(
                        clientCredentials = @OAuthFlow(
                            authorizationUrl = "https://auth-test.ala.org.au/cas/oidc/authorize",
                            tokenUrl = "https://auth-test.ala.org.au/cas/oidc/token",
                            refreshUrl = "https://auth-test.ala.org.au/cas/oidc/refresh",
                            scopes = [
                                    @OAuthScope(name="openid"),
                                    @OAuthScope(name="profile"),
                                    @OAuthScope(name="ala", description = "CAS scope"),
                                    @OAuthScope(name="roles", description = "CAS scope"),
                                    @OAuthScope(name="ala/attrs", description = "Cognito scope"),
                                    @OAuthScope(name="ala/roles", description = "Cognito scope")
                            ]
                        )
                ),
                scheme = "bearer"
        ),
        @SecurityScheme(
                name = "jwt",
                type = SecuritySchemeType.HTTP,
                bearerFormat = "JWT",
                scheme = "bearer"
        )])
@Path("/ws/paratoo")
class ParatooController {

    static responseFormats = ['json']
    static allowedMethods = [
            userProjects       : 'GET',
            protocolReadCheck  : 'GET',
            protocolWriteCheck : 'GET',
            validateToken      : 'POST',
            mintCollectionId   : 'POST',
            submitCollection   : 'POST',
            collectionIdStatus : 'GET',
            addPlotSelection   : ['POST', 'PUT'],
            updatePlotSelection: ['POST', 'PUT'],
            updateProjectSites : ['PUT']
    ]

    ParatooService paratooService
    UserService userService
    WebService webService

    private error(int status, String message) {
        respond([message: message, code: status], status: status)
    }

    private error(Errors errors) {
        log.warn("Validation errors encountered on ${request.requestURL}: " + errors)
        respond errors
    }

    @GET
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Path("/user-projects")
    @Operation(
            method = "GET",
            summary = "Gets all projects for an authenticated user",
            description = "Gets all projects that a user is assigned to",
            responses = [
                    @ApiResponse(responseCode = "200", description = "Projects assigned to the user", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Project.class)))),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            ],
            tags = "Org Interface"
    )
    def userProjects() {
        respond projects: paratooService.userProjects(userService.currentUserDetails.userId)
    }

    @SkipApiKeyCheck
    @POST
    @Path("/validate-token")
    @Operation(
            method = "POST",
            summary = "Validates JWT tokens issued by Org",
            description = "Before Core makes a PDP request via the project membership enforcer, it must check that the JWT it was provided is valid. As Org issues these JWTs, Core must check with Org to ensure the validity.",
            responses = [
                    @ApiResponse(responseCode = "200", description = "The token is valid or invalid", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class)))
            ],
            tags = "Org Interface"
    )
    def validateToken(@RequestBody(description = "The JWT token", required = true, content = @Content(schema = @Schema(implementation = ParatooToken.class))) ParatooToken body) {
        // Possibly an implementation side-effect of the paratoo client but the token is passed in the body here
        // rather than the header.  We extract it and call a protected method to check the token....
        String token = body?.token
        if (!token) {
            respond([message: "Missing token in body"], status: HttpStatus.SC_BAD_REQUEST)
            return
        }

        // It also has the Bearer string attached according to the implementation so we can use it in the header as is.
        String url = grailsLinkGenerator.link(action: 'noop', absolute: true)

        // Make a call to the URL with the token in the header
        Map response = webService.getJson(url, null, [Authorization: token], false)

        boolean valid = (response && response.statusCode == HttpStatus.SC_OK)
        if (valid) {
            respond([valid: true], status: HttpStatus.SC_OK)
        } else {
            respond([valid: false], status: HttpStatus.SC_UNAUTHORIZED)
        }
    }

    /**
     * This method exists so the validateToken method can call it to get the framework to validate the JWT.
     * If it is reached, it always returns OK
     */
    def noop() {
        respond([statusCode: HttpStatus.SC_OK])
    }

    @GET
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Path("/pdp/{projectId}/{protocolId}/read")
    @Operation(
            method = "GET",
            description = "Checks that a user has read permissions for the particular project and protocol",
            parameters = [
                    @Parameter(name = "projectId", description = "The project id", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "protocolId", description = "The protocol id", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string"))
            ],
            responses = [
                    @ApiResponse(responseCode = "200", description = "Returns if user has read permission for supplied project and protocol", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden"), @ApiResponse(responseCode = "404", description = "Not found")
            ],
            tags = "Org Interface",
            summary = "For authorizing with the PDP which checks read permissions"
    )
    def hasReadAccess(@RequestBody(required = true, content = @Content(schema = @Schema(type = "string"))) String projectId,
                      @RequestBody(required = true, content = @Content(schema = @Schema(type = "string"))) String protocolId) {
        protocolCheck(projectId, protocolId, { String userId, String prjId, String proId ->
            paratooService.protocolReadCheck(userId, prjId, proId)
        })
    }

    @GET
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Path("/pdp/{projectId}/{protocolId}/write")
    @Operation(
            method = "GET",
            description = "Checks that a user has write permissions for the particular project and protocol",
            parameters = [
                    @Parameter(name = "projectId", description = "The project id", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")),
                    @Parameter(name = "protocolId", description = "The protocol id", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string"))
            ],
            responses = [
                    @ApiResponse(responseCode = "200", description = "Returns if user has read permission for supplied project and protocol", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            ],
            tags = "Org Interface",
            summary = "For authorizing with the PDP which checks write permissions"
    )
    def hasWriteAccess(@RequestBody(required = true, content = @Content(schema = @Schema(type = "string"))) String projectId,
                       @RequestBody(required = true, content = @Content(schema = @Schema(type = "string"))) String protocolId) {
        protocolCheck(projectId, protocolId, { String userId, String prjId, String proId ->
            paratooService.protocolWriteCheck(userId, prjId, proId)
        })
    }

    /**
     * Used for both read and write - if we need to take into account
     * read only users we need to separate these calls
     */
    private void protocolCheck(String projectId, String protocolId, Closure checkMethod) {
        if (!projectId || !protocolId) {
            error(HttpStatus.SC_BAD_REQUEST, "Bad request")
            return
        }
        String userId = userService.currentUserDetails.userId
        boolean hasAccessToProtocol = checkMethod(userId, projectId, protocolId)

        respond([isAuthorised: hasAccessToProtocol], status: HttpStatus.SC_OK)
    }

    @POST
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Path("/mint-identifier")
    @Operation(
            method = "POST",
            description = "Creates an identifier that is stored in Org as a cross-reference to Core. Allows a particular survey to be derived from the data contained in the identifier. User's may not have connection to the server when they are performing a collection, so an identifier is created locally. When they are ready to submit the collection (i.e., have access to the server), they hit this endpoint to have the actual identifier minted. The identifier is encrypted using SJCL and is returned to the user as such.",
            responses = [
                    @ApiResponse(responseCode = "200", description = "Returns the encrypted minted identifier", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            ],
            tags = "Org Interface")
    def mintCollectionId(@RequestBody(description = "Note that the survey ID is created by the client application and is arbitrarily defined. It should simply uniquely identify a particular survey",
            required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParatooCollectionId.class))) ParatooCollectionId collectionId) {

        if (collectionId.hasErrors()) {
            error(collectionId.errors)
        } else {
            String userId = userService.currentUserDetails.userId
            boolean hasProtocol = paratooService.protocolWriteCheck(userId, collectionId.surveyId.projectId, collectionId.surveyId.protocol.id)
            if (hasProtocol) {
                Map mintResults = paratooService.mintCollectionId(userId, collectionId)
                if (mintResults.error) {
                    error(mintResults.error)
                } else {
                    respond([orgMintedIdentifier: mintResults.orgMintedIdentifier])
                }

            } else {
                error(HttpStatus.SC_FORBIDDEN, "Project / protocol combination not available")
            }
        }
    }

    @POST
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Path("/collection")
    @Operation(
            method = "POST",
            responses = [
                    @ApiResponse(responseCode = "200", description = "Returns true if Org successfully stored the supplied identifier", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            ],
            tags = "Org Interface"
    )
    def submitCollection(@RequestBody(description = "The event time for this request is not the same as the one for minting identifiers. An identifier's event time denotes when the collection was made, this event time denotes when the collection was submitted to the server.",
            required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParatooCollection.class))) ParatooCollection collection) {

        if (log.isDebugEnabled()) {
            log.debug("ParatooController::submitCollection")
            log.debug(request.JSON.toString())
        }
        if (collection.hasErrors()) {
            error(collection.errors)
        } else {
            String userId = userService.currentUserDetails.userId
            Map dataSet = paratooService.findDataSet(userId, collection.orgMintedIdentifier)

            boolean hasProtocol = paratooService.protocolWriteCheck(userId, dataSet.project.id, collection.protocol.id)
            if (hasProtocol) {
                Map result = paratooService.submitCollection(collection, dataSet.project)
                if (!result.error) {
                    respond([success: true])
                } else {
                    error(HttpStatus.SC_INTERNAL_SERVER_ERROR, result.error)
                }
            } else {
                error(HttpStatus.SC_FORBIDDEN, "Project / protocol combination not available")
            }
        }
    }

    @GET
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Path("/status/{id}")
    @Operation(
            method = "GET",
            responses = [
                    @ApiResponse(responseCode = "200", description = "Returns true if Org has stored the supplied identifier", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            ],
            tags = "Org Interface"
    )
    def collectionIdStatus(@Parameter(name = "id", description = "The value for mintedCollectionId", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string")) String id) {
        if (!id) {
            error(HttpStatus.SC_BAD_REQUEST, "Bad request")
            return
        }
        String userId = userService.currentUserDetails.userId
        Map matchingDataSet = paratooService.findDataSet(userId, id)

        if (!matchingDataSet.dataSet) {
            error(HttpStatus.SC_NOT_FOUND, "No data set found with mintedCollectionId=$id")
            return
        }

        respond([isSubmitted: (matchingDataSet.dataSet.progress == Activity.STARTED)])
    }

    @POST
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Path("/plot-selections")
    @Operation(
            method = "POST",
            responses = [
                    @ApiResponse(responseCode = "200", description = "Plot selection added", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            ],
            tags = "Org Interface"
    )
    def addPlotSelection(@RequestBody(description = "Plot selection data. Make sure all fields are provided.",
            required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParatooPlotSelection.class))) ParatooPlotSelection plotSelection) {
        addOrUpdatePlotSelection(plotSelection)
    }

    @PUT
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Path("/plot-selections")
    @Operation(
            method = "PUT",
            responses = [
                    @ApiResponse(responseCode = "200", description = "Plot selection updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            ],
            tags = "Org Interface"
    )
    def updatePlotSelection(@RequestBody(description = "Plot selection data. Make sure all fields are provided.",
            required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParatooPlotSelection.class))) ParatooPlotSelection plotSelection) {
        addOrUpdatePlotSelection(plotSelection)
    }

    @GET
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Path("/plot-selections")
    @Operation(
            method = "GET",
            responses = [
                    @ApiResponse(responseCode = "200", description = "All plots the user has permission for", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "400", description = "Bad request"),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "404", description = "Not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            ],
            tags = "Org Interface"
    )
    def getPlotSelections() {
        String userId = userService.currentUserDetails.userId
        List<ParatooProject> projects = paratooService.userProjects(userId)
        // Plots can be reused between projects so we need to ensure they are unique
        List plotSelections = []
        projects.each {
            if (it.plots) {
                plotSelections.addAll(it.plots)
            }
        }
        plotSelections = plotSelections.unique {it.siteId} ?: []
        respond plots:plotSelections
    }

    private def addOrUpdatePlotSelection(ParatooPlotSelection plotSelection) {

        String userId = userService.currentUserDetails.userId
        Map result = paratooService.addOrUpdatePlotSelections(userId, plotSelection.data)

        if (result.error) {
            respond([message: result.error], status: HttpStatus.SC_INTERNAL_SERVER_ERROR)
        } else {
            respond(buildPlotSelectionsResponse(plotSelection.data), status: HttpStatus.SC_OK)
        }
    }

    private static Map buildPlotSelectionsResponse(ParatooPlotSelectionData data) {
        [
                "data": [
                        "id"        : data.uuid,
                        "attributes": data
                ],
                meta  : [:]
        ]
    }

    @PUT
    @Path("/projects/{id}")
    @SecurityRequirements([@SecurityRequirement(name = "jwt"), @SecurityRequirement(name = "openIdConnect"), @SecurityRequirement(name = "oauth")])
    @Operation(
            method = "PUT",
            responses = [
                    @ApiResponse(responseCode = "200", description = "Updated project", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            ],
            requestBody = @RequestBody(
                    description = "Project sites to update",
                    required = true,
                    content = @Content(
                            mediaType = 'application/json',
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            tags = "Org Interface"
    )
    def updateProjectSites(@Parameter(name = "id", description = "Project id", required = true, in = ParameterIn.PATH, schema = @Schema(type = "string"))String id) {
        String userId = userService.currentUserDetails.userId
        List projects = paratooService.userProjects(userId)
        ParatooProject project = projects?.find { it.id == id }
        if (!project) {
            error(HttpStatus.SC_FORBIDDEN, "Project not available")
            return
        }
        Map data = request.JSON

        Map result = paratooService.updateProjectSites(project, data.data, projects)

        if (result?.error) {
            respond([message:result.error], status:HttpStatus.SC_INTERNAL_SERVER_ERROR)
        }
        else {
            respond(buildUpdateProjectSitesResponse(id, data.data), status:HttpStatus.SC_OK)
        }
    }

    private static Map buildUpdateProjectSitesResponse(String id, Map data) {
        [
            "data": [
                    "id": id,
                    "attributes": data
            ],
            meta: [:]
        ]
    }

    def options() {
        respond([statusCode: HttpStatus.SC_NO_CONTENT])
    }

    def handleException(Exception e) {
        log.error("An uncaught error was thrown processing: ${request.uri}", e)
        error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "An error was encountered processing the request.")
    }
}
