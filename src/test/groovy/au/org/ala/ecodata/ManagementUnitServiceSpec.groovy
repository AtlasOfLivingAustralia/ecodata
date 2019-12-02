package au.org.ala.ecodata


import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import org.springframework.context.MessageSource
import grails.converters.JSON
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller

class ManagementUnitServiceSpec extends MongoSpec implements ServiceUnitTest<ManagementUnitService> {

    CommonService commonService = new CommonService()
    SiteService siteService = Mock(SiteService)

    def setup() {
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())

        commonService.grailsApplication = grailsApplication
        commonService.messageSource = Mock(MessageSource)
        service.commonService = commonService
        service.siteService = siteService

        ManagementUnit.findAll().each { it.delete(flush:true) }
    }

    def cleanup() {
        ManagementUnit.findAll().each { it.delete(flush:true) }
    }


    void "find an existing mu"() {
        setup:
        ManagementUnit p1 = new ManagementUnit(managementUnitId: 'p1', name: 'test 1', description: 'description 1',status:Status.ACTIVE)
        p1.save(flush:true, failOnError: true)

        when:

        ManagementUnit mu = service.get('p1',false)

        then:
        mu.name == 'test 1'

    }

    def "The service can produce a FeatureCollection containing the management unit sites"() {

        setup:
        (1..10).each {setupMu(it)}

        when:
        Map featureCollection = service.managementUnitSiteMap()

        then:
        10 * siteService.get({it.startsWith('muSite')}) >> [siteId:'1']
        10 * siteService.toGeoJson(_) >> squareFeature("1", 0, 0)

        and:
        featureCollection.features.size() == 10
    }


    private void setupMu(int count) {
        ManagementUnit mu = new ManagementUnit(
                managementUnitId: 'm'+count,
                name: 'Management unit '+count,
                description: 'description '+count,
                status:Status.ACTIVE,
                managementUnitSiteId:'muSite'+count)
        mu.save(flush:true)
    }

    private Map squareFeature(String id, int x, int y) {
        Map feature = [
                type:'Feature',
                geometry:[
                        type:'Polygon',
                        coordinates: [[[x, y], [x, y+1], [x+1, y+1], [x+1, y], [x, y]]]
                ],
                properties:[id:id]
        ]
        feature
    }
}
