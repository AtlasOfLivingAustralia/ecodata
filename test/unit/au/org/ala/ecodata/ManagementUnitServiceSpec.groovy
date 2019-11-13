package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import grails.test.mixin.mongodb.MongoDbTestMixin
import org.codehaus.groovy.grails.web.converters.marshaller.json.CollectionMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.json.MapMarshaller
import org.springframework.context.MessageSource
import spock.lang.Specification

@TestMixin(MongoDbTestMixin)
@Domain([ManagementUnit, Report])
@TestFor(ManagementUnitService)
class ManagementUnitServiceSpec extends Specification {

    CommonService commonService = new CommonService()
    SiteService siteService = Mock(SiteService)
    ReportService reportService = Mock(ReportService)

    def setup() {
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())

        commonService.grailsApplication = grailsApplication
        commonService.messageSource = Mock(MessageSource)
        service.commonService = commonService
        service.siteService = siteService
        service.reportService = reportService

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

    def "The service calculates report periods "() {
        setup:
        (1..2).each {setupMu(it)}

        when:
        int[] years = service.getFinancialYearPeriods()

        then:
        1 * reportService.getPeriodOfManagmentUnitReport(['m1','m2']) >> [ new Date(2009,1,1), new Date(2010,8,1)]

        then:
        years.size() == 3

    }




    private void setupMu(int count) {
        ManagementUnit mu = new ManagementUnit(
                managementUnitId: 'm'+count,
                name: 'Management unit '+count,
                description: 'description '+count,
                status:Status.ACTIVE,
                managementUnitSiteId:'muSite'+count)
        mu.save(flush:true)

        Report report = new Report(
                managementUnitId: 'm'+count,
                name: 'report'+count,
                activityId: 'activity'+count
        )

        report.save(flush: true)
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
