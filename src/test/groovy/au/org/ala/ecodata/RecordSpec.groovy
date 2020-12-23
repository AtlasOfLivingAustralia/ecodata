package au.org.ala.ecodata

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class RecordSpec extends Specification implements DomainUnitTest<Record> {

    void "The sightings URL can be passed in as a parameter"() {
        setup:
        Record r = new Record(outputId:"o1", activityId:"a1")
        String url = "/sightings"
        expect:
        r.getRecordNumber(url) == url+"/bioActivity/index/${r.activityId}"
    }
}
