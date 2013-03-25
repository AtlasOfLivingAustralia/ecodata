package au.org.ala.ecodata



import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(SiteController)
@Mock(Site)
class SiteControllerTests {

    def testInsert() {
        def id = 'test1'
        Site s = new Site(siteId: id, name: 'Test site1',
                location: [new Coordinate(decimalLatitude: '-35.4', decimalLongitude: '145.3')])
        s.save(flush: true)
        if (s.hasErrors()) {
            s.errors.each { println it }
        }
        def r = Site.findBySiteId(id)
        assertNotNull(r)
        //println r.location
        assertEquals(1,r.location.size())
        assertEquals('-35.4',r.location[0].decimalLatitude)
    }
}
