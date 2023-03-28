package au.org.ala.ecodata

import au.org.ala.web.AlaSecured
import grails.converters.JSON
import grails.web.http.HttpHeaders

import javax.servlet.http.HttpServletRequest

class ApiKeyInterceptor {

    ProjectService projectService
    ProjectActivityService projectActivityService
    UserService userService
    PermissionService permissionService
    CommonService commonService
    ActivityService activityService

    int order = -100 // This can go before the ala-ws-security interceptor to do the IP check

    def LOCALHOST_IP = '127.0.0.1'

    public ApiKeyInterceptor() {
        // These controllers use JWT authorization instead
        matchAll().excludes(controller: 'graphql').excludes(controller: 'paratoo')
    }

    boolean before() {
        def controller = grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
        Class controllerClass = controller?.clazz

        // The "excludes" configuration in the constructor isn't working
        if (controllerClass == ParatooController.class) {
            return true
        }

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
                List whiteList = buildWhiteList()
                List clientIp = getClientIP(request)
                boolean ipOk = checkClientIp(clientIp, whiteList)

                // All request without PreAuthorise annotation needs to be secured by IP for backward compatibility
                if (!ipOk) {
                    log.warn("Non-authorised IP address - ${clientIp}" )
                    result.status = 403
                    result.error = "not authorised"
                }

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
    boolean checkClientIp(List clientIps, List whiteList) {
        clientIps.size() > 0 && whiteList.containsAll(clientIps) || (whiteList.size() == 1 && whiteList[0] == LOCALHOST_IP)
    }

    private List buildWhiteList() {
        def whiteList = [LOCALHOST_IP] // allow calls from localhost to make testing easier
        def config = grailsApplication.config.getProperty('app.api.whiteList')
        if (config) {
            whiteList.addAll(config.split(',').collect({it.trim()}))
        }
        whiteList
    }

    private List getClientIP(HttpServletRequest request) {
        // External requests to ecodata are proxied by Apache, which uses X-Forwarded-For to identify the original IP.
        // From grails 5, tomcat started returning duplicate headers as a comma separated list.  When a download
        // request is sent from MERIT to ecodata, ngnix adds a X-Forwarded-For header, then forwards to the
        // reporting server, which adds another header before proxying to tomcat/grails.
        List allIps = []
        Enumeration<String> ips = request.getHeaders(HttpHeaders.X_FORWARDED_FOR)
        while (ips.hasMoreElements()) {
            String ip = ips.nextElement()
            allIps.addAll(ip?.split(',').collect{it?.trim()})
        }
        allIps.add(request.getRemoteHost())
        return allIps
    }

}
