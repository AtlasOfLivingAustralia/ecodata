package au.org.ala.ecodata

import grails.converters.JSON
import org.springframework.http.HttpStatus

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

    def getLayerName () {
        def type = params.type ?: ""
        def indices = params.indices ?: ""
        def dataType = params.dataType ?: grailsApplication.config.getProperty('geoServer.defaultDataType')
        List listOfIndex = indices.split(',')
        def name = mapService.getLayerNameForType (type, listOfIndex, dataType)
        if (name) {
            render( text: [success: "Layer resolved.", layerName: name] as JSON, contentType: 'application/json')
        } else {
            render( text: [error: "Failed to resolve layer."] as JSON, contentType: 'application/json', status: HttpStatus.NOT_FOUND )
        }
    }
}
