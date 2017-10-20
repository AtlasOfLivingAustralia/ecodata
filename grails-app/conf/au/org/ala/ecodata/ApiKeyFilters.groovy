package au.org.ala.ecodata

import au.org.ala.web.AlaSecured
import grails.converters.JSON

class ApiKeyFilters {

    def grailsApplication
    ProjectService projectService
    ProjectActivityService projectActivityService
    UserService userService
    PermissionService permissionService
    CommonService commonService
    ActivityService activityService


    def LOCALHOST_IP = '127.0.0.1'

    def filters = {
        apiKeyCheck(controller: '*', action: '*') {
            before = {
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
                        def whiteList = buildWhiteList()
                        def clientIp = getClientIP(request)
                        def ipOk = checkClientIp(clientIp, whiteList)

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
            }
            after = { Map model ->

            }
            afterView = { Exception e ->

            }
        }
    }

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
