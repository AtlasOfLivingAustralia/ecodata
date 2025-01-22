package au.org.ala.ecodata

import grails.testing.mixin.integration.Integration
import grails.util.GrailsWebMockUtil
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

@Integration
class SpatialControllerIntegrationSpec extends Specification {

    @Autowired
    SpatialController spatialController

    @Autowired
    WebApplicationContext ctx

    def setup() {
        setRequestResponse()
    }

    def cleanup() {
    }

    def setRequestResponse() {
        GrailsMockHttpServletRequest grailsMockHttpServletRequest = new GrailsMockHttpServletRequest()
        GrailsMockHttpServletResponse grailsMockHttpServletResponse = new GrailsMockHttpServletResponse()
        GrailsWebMockUtil.bindMockWebRequest(ctx, grailsMockHttpServletRequest, grailsMockHttpServletResponse)
    }

    void "test uploadShapeFile with resource zip file"() {
        given:
        // Read the zip file from resources
        def zipFileResourceStream = spatialController.class.getResourceAsStream("/projectExtent.zip")
        byte[] zipFileBytes = zipFileResourceStream.bytes

        // Mock the request
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "projectExtent.zip", "application/zip", zipFileBytes)
        spatialController.request.addFile(mockMultipartFile)
        spatialController.request.method = 'POST'

        when:
        // Call the method
        spatialController.uploadShapeFile()

        then:
        // Verify the response
        spatialController.response.status == HttpStatus.SC_OK
        println spatialController.response.contentAsString
        def responseContent = new JsonSlurper().parseText(spatialController.response.contentAsString)
        responseContent.shp_id != null
        responseContent["0"].siteId == "340cfe6a-f230-4bb9-a034-23e9bff125c7"
        responseContent["0"].name == "Project area for Southern Tablelands Koala Habitat Restoration Project"

        when:
        setRequestResponse()
        spatialController.request.method = 'GET'
        spatialController.params.shapeFileId = responseContent.shp_id
        spatialController.params.featureId = "0"
        spatialController.getShapeFileFeatureGeoJson()

        then:
        spatialController.response.status == HttpStatus.SC_OK
        println spatialController.response.contentAsString
        def responseJSON = new JsonSlurper().parseText(spatialController.response.contentAsString)
        responseJSON.geoJson != null
        responseJSON.geoJson.type == "MultiPolygon"
    }
}