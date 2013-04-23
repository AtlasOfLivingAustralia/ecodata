package au.org.ala.ecodata



import grails.test.mixin.*

import java.text.SimpleDateFormat

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(ProjectController)
class ProjectControllerTests {

    void testDateParsing() {
        def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        //def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ")

        Date date = dateFormat.parse('2013-03-12T14:00:00Z')
        //Date date = dateFormat.parse('2013-12-03T14:00:00Z')
        println date
    }
}
