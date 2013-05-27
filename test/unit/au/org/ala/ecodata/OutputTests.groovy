package au.org.ala.ecodata



import grails.test.mixin.*
import org.junit.*

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(Output)
class OutputTests {

    // dyn props don't work in unit tests without some fiddlin'
   /* void testDynamicProperty() {
        def o = new Output(activityId: '20', outputId: '30')
        //o.save(flush: true)
        assertEquals '20', o.activityId
        // add a dynamic prop
        o['testProp'] = 'test'
        assertEquals 'test', o.testProp
    }*/
}
