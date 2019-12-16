package au.org.ala.ecodata

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus

import spock.lang.Specification

class ManagementUnitControllerSpec extends Specification implements ControllerUnitTest<ManagementUnitController>, DataTest {
    ManagementUnitService managementUnitService = Mock(ManagementUnitService)

    def setup() {
        controller.managementUnitService = managementUnitService
        mockDomain ManagementUnit
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

    def "The management unit controller can request a geojson FeatureCollection containing all of the management unit sites"() {
        setup:
        Map geojson = [type: "FeatureCollection", features: []]

        when:
        request.method = 'POST'
        controller.managementUnitSiteMap()

        then:
        1 * managementUnitService.managementUnitSiteMap(null) >> geojson

        and:
        response.status == HttpStatus.SC_OK
        response.json == geojson
    }
    def "The management unit controller can request a geojson FeatureCollection containing sites for specific management units"() {

        setup:
        Map geojson = [type: "FeatureCollection", features: []]
        List muIds = ['mu1', 'mu2']

        when:
        request.method = 'POST'
        request.setJson([managementUnitIds:muIds])
        controller.managementUnitSiteMap()

        then:
        1 * managementUnitService.managementUnitSiteMap(muIds) >> geojson

        and:
        response.status == HttpStatus.SC_OK
        response.json == geojson

    }

    def "Not supplying an id to the update method will create a management unit"() {

        setup:
        Map props = [
                name:'Test',
                description:'Test description',
                url:'https://www.mu.org',
                startDate:'2019-01-01T00:00:00Z',
                endDate:'2023-06-30T14:00:00Z',
                priorities:[[category:"Threatened species", priority:"Hibiscus brennanii"]],
                outcomes:[[outcome:"outcome 1", priorities: [category:"Threatened species"]]],
                managementUnitSiteId:'site1',
                config:[config1:'1', config2:'2']

        ]
        ManagementUnit capturedMu = null

        when:
        request.method = 'POST'
        request.json = props
        controller.update('')

        then:
        1 * managementUnitService.create({capturedMu = it})
        compareMu(props, capturedMu)
    }

    def "Extra properties supplied to the controller should not bound to the domain object"() {

        setup:
        Map props = [
                name:'Test',
                description:'Test description',
                url:'https://www.mu.org',
                startDate:'2019-01-01T00:00:00Z',
                endDate:'2023-06-30T14:00:00Z',
                priorities:[[category:"Threatened species", priority:"Hibiscus brennanii"]],
                outcomes:[[outcome:"outcome 1", priorities: [category:"Threatened species"]]],
                managementUnitSiteId:'site1',
                config:[config1:'1', config2:'2']
                ]

        Map unboundProps = [
                dateCreated:'2019-01-01T00:00:00Z',
                lastUpdated:'2019-01-01T00:00:00Z',
                status:'deleted',
                managementUnitId:'mu1',
                id:'test'
            ]
        ManagementUnit capturedMu = null

        when:
        request.method = 'POST'
        request.json = props + unboundProps
        controller.update('')

        then:
        1 * managementUnitService.create({capturedMu = it})
        compareMu(props, capturedMu)
        capturedMu.id == null
        capturedMu.managementUnitId == null
        capturedMu.dateCreated == null
        capturedMu.lastUpdated == null
        capturedMu.status == Status.ACTIVE

    }

    private void compareMu(Map props, ManagementUnit mu) {
        props.each {
            println "${it.key}, ${it.value}"
            if (mu[it.key] instanceof Date) {
                assertEquals(it.value, FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'").format(mu[it.key]))
            }
            else {
                assertEquals(it.value, mu[it.key])
            }
        }
    }


}

