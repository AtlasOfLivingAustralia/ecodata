package au.org.ala.ecodata.converter

import au.org.ala.ecodata.*
import groovy.json.JsonSlurper
import spock.lang.Specification

class GenericConverterSpec extends Specification {

    def "convert should return a single record"() {
        setup:
        Map data =  [field1: "val1", field2: "val2", userId: "user1"]

        when:
        List<Map> result = new GenericFieldConverter().convert(data)

        then:
        result.size() == 1
    }


    def "convert should return latitude and longitude from location values"() {
        setup:
        Map data = [locationLatitude : "2.1", locationLongitude: "3.1"]

        when:
        List<Map> result = new GenericFieldConverter().convert(data)

        then:
        result.size() == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
    }

    def "convert should return latitude and longitude from decimal values"() {
        setup:
        Map data = [decimalLatitude : "2.1", decimalLongitude: "3.1"]

        when:
        List<Map> result = new GenericFieldConverter().convert(data)

        then:
        result.size() == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
    }

    def "convert should handle numeric values of locationLatitude and locationLongitude"() {
        setup:
        String data = """{
                "locationLatitude" : 2.1,
                "locationLongitude": 3.1
        }"""

        when:
        List<Map> result = new GenericFieldConverter().convert(new JsonSlurper().parseText(data))

        then:
        result.size() == 1
        result[0].decimalLatitude == 2.1
        result[0].decimalLongitude == 3.1
    }

    def "convert should handle integer values of locationLatitude and locationLongitude"() {
        setup:
        String data = """{
            "locationLatitude" : 2,
            "locationLongitude": 3
        }"""

        when:
        List<Map> result = new GenericFieldConverter().convert(new JsonSlurper().parseText(data))

        then:
        result.size() == 1
        result[0].decimalLatitude == 2.0
        result[0].decimalLongitude == 3.0
    }

    def "convert should handle expression to evaluate value of dwc attribute"() {
        setup:
        Map data = [
            "juvenile" : 2,
            "adult": 3
        ]

        Map metadata = [
                name: "juvenile",
                dwcAttribute: "individualCount",
                dwcExpression: "context.metadata.description + ' ' + (juvenile + adult)",
                dwcDefault: 0,
                description: "Total number of individuals"
        ]
        GenericFieldConverter converter = new GenericFieldConverter()
        Map context = [:]

        when:
        List<Map> result = converter.convert(data, metadata)

        then:
        result.size() == 1
        result[0].individualCount == "Total number of individuals 5"
    }

    def "convert should handle expressions in groovy, access information from context and add multiple darwin core attributes"() {
        setup:
        Map data = [
                "juvenile" : 2,
                "adult": 3
        ]

        Map otherMetadata = [
                name: "adult",
                dwcAttribute: "adultCount",
                description: "Total number of adult individuals"
        ]
        Map metadata = [
                name: "juvenile",
                dataType: "number",
                dwcAttribute: "individualCount",
                dwcExpression: "context.record.projectId = context.project.projectId; context.record.organisationId = context.organisation.organisationId; context.record.individualCount = juvenile + adult;",
                dwcDefault: 0,
                description: "Total number of individuals"
        ]

        GenericFieldConverter converter = new GenericFieldConverter()
        Map context = [
                'project': new Project(projectId: 'project1'),
                'organisation': new Organisation(organisationId: 'org1'),
                'site': new Site(siteId: 'site1'),
                'projectActivity': new ProjectActivity(projectActivityId: 'pa1'),
                'activity':new Activity(activityId: 'activity1', siteId: 'site1'),
                'output': new Output(outputId: 'output1', activityId: 'activity1'),
                outputMetadata: [dataModel: [metadata,otherMetadata]],
                rootData: data
        ]

        when:
        List<Map> result = converter.convert(data, metadata, context)

        then:
        result.size() == 1
        result[0].individualCount == 5
        result[0].projectId == "project1"
        result[0].organisationId == "org1"
    }

    def "convert should return expression if binding not found"() {
        setup:
        Map data = [
                "juvenile" : 2
        ]

        Map metadata = [
                name: "juvenile",
                dwcAttribute: "individualCount",
                dwcExpression: "juvenile + adult",
                dwcDefault: 0,
                description: "Total number of individuals"
        ]
        GenericFieldConverter converter = new GenericFieldConverter()

        when:
        List<Map> result = converter.convert(data, metadata)

        then:
        result.size() == 1
        result[0].individualCount == "juvenile + adult"
    }

    def "convert should return default value for all other exceptions"() {
        setup:
        Map data = [
                "juvenile" : 2,
                adult: null
        ]

        Map metadata = [
                name: "juvenile",
                dwcAttribute: "individualCount",
                dwcExpression: "juvenile + adult",
                dwcDefault: 0,
                description: "Total number of individuals"
        ]
        GenericFieldConverter converter = new GenericFieldConverter()

        when:
        List<Map> result = converter.convert(data, metadata)

        then:
        result.size() == 1
        result[0].individualCount == 0
    }
}
