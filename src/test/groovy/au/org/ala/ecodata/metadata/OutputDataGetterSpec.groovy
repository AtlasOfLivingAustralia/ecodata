package au.org.ala.ecodata.metadata

import spock.lang.Specification

class OutputDataGetterSpec extends Specification {


    void "Text values will be truncated to fit in an Excel cell"() {
        setup:
        Map node = [name:"textField", dataType:"text"]
        Map dataNode = [textField:"test value"]
        OutputDataGetter outputDataGetter = new OutputDataGetter("textField", node, null, null)

        when:
        String result = outputDataGetter.getFormattedValue(dataNode)

        then:
        result == dataNode.textField

        when:
        char[] longData = new char[OutputDataGetter.getMaxCellLength()]
        Arrays.fill(longData, 'x' as char)
        dataNode.textField = new String(longData)
        result = outputDataGetter.getFormattedValue(dataNode)

        then:
        result == dataNode.textField

        when:
        longData = new char[OutputDataGetter.getMaxCellLength()+1]
        Arrays.fill(longData, 'x' as char)
        dataNode.textField = new String(longData)
        result = outputDataGetter.getFormattedValue(dataNode)

        then:
        result.size() == OutputDataGetter.getMaxCellLength()
        result == dataNode.textField.substring(0, OutputDataGetter.getMaxCellLength())

    }

    void "stringList values will be truncated to fit in an Excel cell"() {
        setup:
        Map node = [name:"stringListField", dataType:"stringList"]
        Map dataNode = [stringListField:["1", "2", '3']]
        OutputDataGetter outputDataGetter = new OutputDataGetter("stringListField", node, null, null)

        when:
        String result = outputDataGetter.getFormattedValue(dataNode)

        then:
        result == "1,2,3"

        when:
        char[] longData = new char[OutputDataGetter.getMaxCellLength()-6]
        Arrays.fill(longData, 'x' as char)
        dataNode.stringListField << new String(longData)
        result = outputDataGetter.getFormattedValue(dataNode)

        then:
        result == "1,2,3,"+dataNode.stringListField[3]

        when:
        longData = new char[OutputDataGetter.getMaxCellLength()-5]
        Arrays.fill(longData, 'x' as char)
        dataNode.stringListField << new String(longData)
        result = outputDataGetter.getFormattedValue(dataNode)

        then:
        result.size() == OutputDataGetter.getMaxCellLength()
        result == ("1,2,3,"+dataNode.stringListField[3]).substring(0, OutputDataGetter.getMaxCellLength())

    }


}
