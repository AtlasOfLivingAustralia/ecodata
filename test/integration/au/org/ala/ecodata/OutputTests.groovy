package au.org.ala.ecodata

import static org.junit.Assert.*
import org.junit.*

class OutputIntegrationTests {

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
     void testDynamicProperty() {
         def o = new Output(activityId: '20', outputId: '30')
         o.save(flush: true)
         assertEquals '20', o.activityId
         // add a dynamic prop
         o['testProp'] = 'test'
         assertEquals 'test', o.testProp
     }
}
