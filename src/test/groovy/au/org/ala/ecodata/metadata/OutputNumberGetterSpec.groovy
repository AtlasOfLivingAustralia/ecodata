package au.org.ala.ecodata.metadata

import spock.lang.Specification

class OutputNumberGetterSpec extends Specification {
    void "String integer should be converted to Integer object"() {
        setup:
        Map node = [name:"integerField", dataType:"integer"]
        Map dataNode = [integerField:"1"]
        OutputNumberGetter outputNumberGetter = new OutputNumberGetter("integerField", node, [:], TimeZone.default)

        when:
        Number result = outputNumberGetter.getFormattedValue(dataNode)

        then:
        result instanceof Integer
        result == 1

        when:
        dataNode.integerField = "a"
        result = outputNumberGetter.getFormattedValue(dataNode)

        then:
        result == null
    }

    void "Number should be converted to Double object"() {
        setup:
        Map node = [name:"numberField", dataType:"number"]
        Map dataNode = [numberField:"1.1"]
        OutputNumberGetter outputNumberGetter = new OutputNumberGetter("numberField", node, [:], TimeZone.default)

        when:
        Number result = outputNumberGetter.getFormattedValue(dataNode)

        then:
        result instanceof Double
        result == 1.1

        when:
        dataNode.numberField = "1.a"
        result = outputNumberGetter.getFormattedValue(dataNode)

        then:
        result == null
    }
}

