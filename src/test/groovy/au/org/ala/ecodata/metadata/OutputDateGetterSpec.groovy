package au.org.ala.ecodata.metadata

import spock.lang.Specification

class OutputDateGetterSpec extends Specification {
    void "Date should be returned serialized date is passed"() {
        setup:
        Map node = [name:"dateField", dataType:"date"]
        Map dataNode = [dateField:"2021-06-09T14:00:00Z"]
        OutputDateGetter outputDateGetter = new OutputDateGetter("dateField", node, [:], TimeZone.default)

        when:
        Date result = outputDateGetter.getFormattedValue(dataNode)

        then:
        result instanceof Date
        println(outputDateGetter.getDateTimeParser().DATE_FORMATS?.toString())
        result == outputDateGetter.getDateTimeParser().parse(dataNode.dateField)?.getTime()

        when:
        dataNode.dateField = ""
        result = outputDateGetter.getFormattedValue(dataNode)

        then:
        result == null

        when:
        dataNode.dateField = "xyz"
        result = outputDateGetter.getFormattedValue(dataNode)

        then:
        result == null

        when:
        dataNode.dateField = null
        result = outputDateGetter.getFormattedValue(dataNode)

        then:
        result == null

        when:
        node.dataType = "text"
        dataNode.dateField = "abc"
        result = outputDateGetter.getFormattedValue(dataNode)

        then:
        result == null

    }

}
