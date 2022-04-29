package au.org.ala.ecodata

import au.org.ala.web.AlaSecured
import grails.converters.JSON
import org.pac4j.core.config.Config
import org.pac4j.core.context.JEEContextFactory
import org.pac4j.core.context.WebContext
import org.pac4j.core.util.FindBest
import org.pac4j.http.client.direct.DirectBearerAuthClient
import org.springframework.beans.factory.annotation.Autowired

class ApiKeyInterceptor {

    ProjectService projectService
    ProjectActivityService projectActivityService
    UserService userService
    PermissionService permissionService
    CommonService commonService
    ActivityService activityService

    @Autowired(required = false)
    Config config
    @Autowired(required = false)
    DirectBearerAuthClient directBearerAuthClient

    public ApiKeyInterceptor() {
        matchAll().excludes(controller: 'graphql')
    }

    boolean before() {
        def controller = grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
        Class controllerClass = controller?.clazz
        def method = controllerClass?.getMethod(actionName?:"index", [] as Class[])
        Map result = [error: '', status : 401]

        if (controllerClass?.isAnnotationPresent(PreAuthorise) || method?.isAnnotationPresent(PreAuthorise)) {
            // What rules needs to be satisfied?
            PreAuthorise pa = method.getAnnotation(PreAuthorise) ?: controllerClass.getAnnotation(PreAuthorise)

            if (pa.basicAuth()) {
                request.userId = userService.authorize(request.getHeader('userName'), request.getHeader('authKey'))
                if(permissionService.isUserAlaAdmin(request.userId)) {
                    /* Don't enforce check for ALA admin.*/
                }
                else if (request.userId) {
                    String accessLevel = pa.accessLevel()
                    String idType = pa.idType()
                    String entityId = params[pa.id()]

                    if (accessLevel && idType) {

                        switch (idType) {
                            case "organisationId":
                                result = permissionService.checkPermission(accessLevel, entityId, Organisation.class.name, request.userId)
                                break
                            case "projectId":
                                result = permissionService.checkPermission(accessLevel, entityId, Project.class.name, request.userId)
                                break
                            case "projectActivityId":
                                def pActivity = projectActivityService.get(entityId)
                                request.projectId = pActivity?.projectId
                                result = permissionService.checkPermission(accessLevel, pActivity?.projectId, Project.class.name, request.userId)
                                break
                            case "activityId":
                                def activity = activityService.get(entityId,'flat')
                                result = permissionService.checkPermission(accessLevel, activity?.projectId, Project.class.name, request.userId)
                                break
                            default:
                                break
                        }
                    }

                } else {
                    result.error = "Access denied"
                    result.status = 401
                }
            }

        } else {

            // Allow migration to the AlaSecured annotation.
            if (!controllerClass?.isAnnotationPresent(AlaSecured) && !method?.isAnnotationPresent(AlaSecured)) {

                // Support RequireJWT
                if(controllerClass?.isAnnotationPresent(RequireJWT) || method?.isAnnotationPresent(RequireJWT)){

                    String authorizationHeader = request.getHeader("Authorization");
                    if (authorizationHeader != null) {
                        if (authorizationHeader.startsWith("Bearer")) {
                            final WebContext context = FindBest.webContextFactory(null, config, JEEContextFactory.INSTANCE).newContext(request, response)
                            def credentials = directBearerAuthClient.getCredentials(context, config.sessionStore)
                            if (!credentials.isPresent()) {
                                result.error = 'Invalid token'
                                result.status = 401
                            }
                        }
                        else {
                            result.error = 'Access denied: No Authorization Bearer token'
                            result.status = 401
                        }
                    }
                    else {
                        result.error = 'Access denied: No Authorization header'
                        result.status = 401
                    }
                }

                //**** This needs to be removed once all RequireApiKey annotations changed to RequireJWT
                // Support RequireApiKey on top of ip restriction.
                if(controllerClass?.isAnnotationPresent(RequireApiKey) || method?.isAnnotationPresent(RequireApiKey)){
                    def keyOk = commonService.checkApiKey(request.getHeader('Authorization')).valid
                    if(!keyOk) {
                        log.warn("No valid api key for ${controllerName}/${actionName}")
                        result.status = 403
                        result.error = "not authorised"
                    }
                }
            }
        }

        if(result.error) {
            response.status = result.status
            render result as JSON
            return false
        }
        true
    }

    boolean after() { true }

    void afterView() { }

    /**
     * Client IP passes if it is in the whitelist of if the whitelist is empty apart from localhost.
     * @param clientIp
     * @return
     */
    def checkClientIp(clientIp, List whiteList) {
        whiteList.contains(clientIp) || (whiteList.size() == 1 && whiteList[0] == LOCALHOST_IP)
    }

    def buildWhiteList() {
        def whiteList = [LOCALHOST_IP] // allow calls from localhost to make testing easier
        def config = grailsApplication.config.app.api.whiteList as String
        if (config) {
            whiteList.addAll(config.split(',').collect({it.trim()}))
        }
        whiteList
    }

    def getClientIP(request) {
        // External requests to ecodata are proxied by Apache, which uses X-Forwarded-For to identify the original IP.
        def ip = request.getHeader("X-Forwarded-For")
        if (!ip) {
            ip = request.getRemoteHost()
        }
        return ip
    }

}
