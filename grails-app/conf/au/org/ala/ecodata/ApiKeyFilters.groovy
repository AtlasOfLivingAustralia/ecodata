package au.org.ala.ecodata

import grails.converters.JSON

class ApiKeyFilters {

    def grailsApplication, commonService

    def filters = {
        apiKeyCheck(controller: '*', action: '*') {
            before = {
                def controller = grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
                Class controllerClass = controller?.clazz
                def method = controllerClass?.getMethod(actionName?:"index", [] as Class[])

                if (controllerClass?.isAnnotationPresent(RequireApiKey) || method?.isAnnotationPresent(RequireApiKey)) {
                    if (!commonService.checkApiKey(request.getHeader('Authorization')).valid) {
                        log.debug "No valid api key for ${controllerName}/${actionName}"
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
}
