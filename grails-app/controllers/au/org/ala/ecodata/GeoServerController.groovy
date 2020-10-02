package au.org.ala.ecodata

import grails.converters.JSON
import org.springframework.http.HttpStatus

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST

class GeoServerController {
    MapService mapService
    static allowedMethods = [createStyle: 'POST', wms: 'GET']

    def wms() {
        mapService.wms(params, response)
        return null
    }

    def createStyle () {

        def body = request.getJSON()
        def terms = body?.terms ?: []
        def field = body?.field
        def type = body?.type
        def style = body?.style
        def dataType = body?.dataType
        if (field && terms) {
            def name = mapService.createStyleForFacet(field, terms, style, type, dataType)
            render text: [name: name] as JSON , contentType: 'application/json'
        } else {
            render text: "JSON body must have terms and field properties", status: HttpStatus.BAD_REQUEST
        }
    }

    def createPredefinedStyles () {
        def response = mapService.createPredefinedStyles()
        if( response.success ) {
            render( text: [success: "Successfully created styles."] as JSON, contentType: 'application/json')
        } else {
            render( text: [error: "Failed to create styles on GeoServer. Affected files ${response.styles.join(', ')}."] as JSON, contentType: 'application/json', status: HttpStatus.INTERNAL_SERVER_ERROR )
        }
    }

    def getLayerName () {
        def type = params.type ?: ""
        def indices = params.indices ?: ""
        def dataType = params.dataType ?: grailsApplication.config.geoServer.defaultDataType
        List listOfIndex = indices.split(',')
        def name = mapService.getLayerNameForType (type, listOfIndex, dataType)
        if (name) {
            render( text: [success: "Layer resolved.", layerName: name] as JSON, contentType: 'application/json')
        } else {
            render( text: [error: "Failed to resolve layer."] as JSON, contentType: 'application/json', status: HttpStatus.NOT_FOUND )
        }
    }

    def createWorkspace () {
        def result = mapService.createWorkspace()
        render(text: result?.resp?:"")
    }

    def deleteWorkspace() {
        def result = mapService.deleteWorkspace()
        render(text: result)
    }

    def createDatastore() {
        def result = mapService.createDatastores()
        render(text: result?.resp?:"")
    }

    def deleteDatastore() {
        def result = mapService.deleteDatastores()
        render(text: result)
    }

    def deleteStylesFromWorkspace() {
        def result = mapService.deleteWorkspaceStyles() ?: [status: SC_BAD_REQUEST, message: "Could not delete styles."]
        render(text: result as JSON, status: result.status )
    }

    def createStyles() {
        def result = mapService.createPredefinedStyles() ?: [status: SC_BAD_REQUEST, message: "Could not delete styles."]
        render(text: result as JSON, status: result.status )
    }
}
