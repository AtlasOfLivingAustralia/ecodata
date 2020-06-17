package au.org.ala.ecodata

import grails.converters.JSON
import org.springframework.http.HttpStatus

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
        if (field && terms) {
            def name = geoServerService.createStyleForFacet(field, terms, type)
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
}
