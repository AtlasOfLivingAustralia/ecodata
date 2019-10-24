package au.org.ala.ecodata

import grails.test.mixin.TestFor
import org.apache.http.HttpStatus

import spock.lang.Specification

@TestFor(ManagementUnitController)
class ManagementUnitControllerSpec extends Specification {
    ManagementUnitService managementUnitService = Mock(ManagementUnitService)

    def setup() {
        controller.managementUnitService = managementUnitService
    }

    def "find a management unit"() {
        setup:
        String id = 'p1'
        ManagementUnit mu = new ManagementUnit(managementUnitId:'p1', name:'test 1')

        when:
        params.id = id
        controller.get()

        then:
        1 * managementUnitService.get(_,false) >> mu

        response.status == HttpStatus.SC_OK
        Map responseData = response.json
        responseData.managementUnitId == 'p1'
    }



    def "A user can retrieve a list of MUs they have a role assigned to"() {
        setup:
        String id = 'u1'
        List mus = [new ManagementUnit(managementUnitId:'p1', name:'test 1'), new ManagementUnit(managementUnitId:'p2', name:'test 2')]

        when:
        params.id = id
        controller.findAllForUser()

        then:
        1 * managementUnitService.findAllManagementUnitsForUser(id) >> mus

        response.status == HttpStatus.SC_OK
        List responseData = response.json
        responseData.size() == 2
        responseData[0].managementUnitId == 'p1'
    }

    def "The management unit controller can request a geojson FeatureCollection containing the management unit sites"() {

        setup:
        Map geojson = [type:"FeatureCollection", features:[]]
        List muIds = ['mu1', 'mu2']

        when:
        request.method = 'POST'
        controller.managementUnitSiteMap()

        then:
        1 * managementUnitService.managementUnitSiteMap(null) >> geojson

        and:
        response.status == HttpStatus.SC_OK
        response.json == geojson


        when:
        request.JSON = [managementUnitIds:muIds]
        controller.managementUnitSiteMap()

        then:
        1 * managementUnitService.managementUnitSiteMap(muIds) >> geojson

        and:
        response.status == HttpStatus.SC_OK
        response.json == geojson

    }

}

