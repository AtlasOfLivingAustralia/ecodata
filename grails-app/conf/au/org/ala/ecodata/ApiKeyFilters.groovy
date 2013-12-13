package au.org.ala.ecodata

import grails.converters.JSON

class ApiKeyFilters {

    def grailsApplication, commonService

    def LOCALHOST_IP = '127.0.0.1'

    def filters = {
        apiKeyCheck(controller: '*', action: '*') {
            before = {
                def controller = grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
                Class controllerClass = controller?.clazz
                def method = controllerClass?.getMethod(actionName?:"index", [] as Class[])

                if (controllerClass?.isAnnotationPresent(RequireApiKey) || method?.isAnnotationPresent(RequireApiKey)) {
                    def whiteList = buildWhiteList()
                    def clientIp = getClientIP(request)
                    def ipOk = checkClientIp(clientIp, whiteList)
                    def keyOk = commonService.checkApiKey(request.getHeader('Authorization')).valid
                    //log.debug "IP ${clientIp} ${ipOk ? 'is' : 'is not'} ok. Key ${keyOk ? 'is' : 'is not'} ok."

                    if (!ipOk || !keyOk) {
                        log.warn(ipOk ? "No valid api key for ${controllerName}/${actionName}" :
                            "Non-authorised IP address - ${clientIp}" )
                        response.status = 403
                        def error = [error:'not authorised']
                        render error as JSON
                        return false
                    }
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
        log.debug whiteList
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
