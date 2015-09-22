package au.org.ala.ecodata.converter

import au.org.ala.ecodata.Record
import net.sf.json.JSON
import spock.lang.Specification

class GenericConverterSpec extends Specification {

    def "convert should return a single record with the json attribute set to the JSON representation of the source data"() {
        setup:
        Map data = [field1: "val1", field2: "val2"]

        when:
        List<Record> result = new GenericConverter().convert(data)

        then:
        result.size() == 1
        result[0].json == (data as JSON).toString()
    }
}
