package au.org.ala.ecodata

import grails.converters.JSON
import org.springframework.http.HttpStatus

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST

class GeoServerController {
    GeoServerService geoServerService
    static allowedMethods = [createStyle: 'POST', wms: 'GET']

    def wms() {
        geoServerService.wms(params, response)
        return null
    }

    def createStyle () {
        def body = request.getJSON()
        def terms = body?.terms ?: []
        def field = body?.field
        def type = body?.type
        def style = body?.style
        if (field && terms) {
            def name = geoServerService.createStyleForFacet(field, terms, style, type)
            render text: [name: name] as JSON , contentType: 'application/json'
        } else {
            render text: "JSON body must have terms and field properties", status: HttpStatus.BAD_REQUEST
        }
    }

    def createPredefinedStyles () {
        def response = geoServerService.createPredefinedStyles()
        if( response.success ) {
            render( text: [success: "Successfully created styles."] as JSON, contentType: 'application/json')
        } else {
            render( text: [error: "Failed to create styles on GeoServer. Affected files ${response.styles.join(', ')}."] as JSON, contentType: 'application/json', status: HttpStatus.INTERNAL_SERVER_ERROR )
        }
    }

    def getLayerName () {
        def type = params.type ?: ""
        def indices = params.indices ?: ""
        List listOfIndex = indices.split(',')
        def name = geoServerService.getLayerNameForType (type, listOfIndex)
        if (name) {
            render( text: [success: "Layer resolved.", layerName: name] as JSON, contentType: 'application/json')
        } else {
            render( text: [error: "Failed to resolve layer."] as JSON, contentType: 'application/json', status: HttpStatus.NOT_FOUND )
        }
    }

    def createWorkspace () {
        def result = geoServerService.createWorkspace()
        render(text: result?.resp?:"")
    }

    def deleteWorkspace() {
        def result = geoServerService.deleteWorkspace()
        render(text: result)
    }

    def createDatastore() {
        def result = geoServerService.createDatastore()
        render(text: result?.resp?:"")
    }

    def deleteDatastore() {
        def result = geoServerService.deleteDatastore()
        render(text: result)
    }

    def deleteStylesFromWorkspace() {
        def result = geoServerService.deleteWorkspaceStyles() ?: [status: SC_BAD_REQUEST, message: "Could not delete styles."]
        render(text: result as JSON, status: result.status )
    }

    def createStyles() {
        def result = geoServerService.createPredefinedStyles() ?: [ status: SC_BAD_REQUEST, message: "Could not delete styles."]
        render(text: result as JSON, status: result.status )
    }
}
