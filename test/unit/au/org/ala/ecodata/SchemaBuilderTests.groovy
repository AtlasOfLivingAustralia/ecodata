package au.org.ala.ecodata

import junit.framework.TestCase

/**
 * Tests for the SchemaBuilder class.
 */

class SchemaBuilderTests extends TestCase {

    def schemaGenerator = new SchemaBuilder([grails:[serverURL:'http://localhost:8080/'], app:[external:[api:[version:'test']]]], [])

    void testTextProperty() {

        def input = [dataType:'text', name:'TextProperty']

        def schema = schemaGenerator.textProperty(input)

        assert schema == [type:'string']
    }

    void testConstrainedTextProperty() {
        def input = [dataType:'text', name:'TextProperty', constraints:['1','2','3']]

        def schema = schemaGenerator.constrainedTextProperty(input)

        assert schema == [enum:['1','2','3']]
    }

    void testStringListProperty() {
        def input = [dataType:'stringList', name:'StringListProperty', constraints:['1','2','3']]

        def schema = schemaGenerator.stringListProperty(input)

        assert schema == [type:'array', items:[enum:['1','2','3']]]
    }

    void testSpeciesProperty() {
        def input = [dataType:'species', name:'SpeciesProperty']

        def schema = schemaGenerator.speciesProperty(input)

        assert schema == [type:'object', properties:[name:[type:'string'], guid:[type:'string'], listId:[type:'string']]]
    }

    void testNumberProperty() {
        def input = [dataType:'number', name:'NumberProperty']

        def schema = schemaGenerator.numberProperty(input)

        assert schema == [type:'number']
    }

    void testDateProperty() {
        def input = [dataType:'date', name:'DateProperty']

        def schema = schemaGenerator.dateProperty(input)

        assert schema == [type:'string', format:'date-time']
    }

    def testSchema() {
        def input = [modelName:'Test Output', dataModel:[
                [dataType:'text', name:'TextProperty']
        ]]

        def schema = schemaGenerator.schemaForOutput(input.modelName, input)


        assert schema ==  [
                type:'object',
                properties:[
                    type:[
                        enum:['Test Output']
                    ],
                    data:[
                        type:'object',
                        properties: [
                            TextProperty:[type:'string']
                        ]
                    ]
                ]
            ]

    }

/*
    POST/PUT http:///project/id/activities
    // update activity (do we require unique ids for activities)?
    {
        "projectId":"externalId",
        "activities": [{
            "activityId": "activityExternalId",
            "type":"revegetation",
            "plannedStartDate": "1994-11-05T13:15:30Z",
            "plannedEndDate": "1994-11-05T13:15:30Z",
            "startDate": "1994-11-05T13:15:30Z",
            "endDate": "1994-11-05T13:15:30Z",
            "mainTheme": "Managing invasive species in a connected landscape",
            "status": "started",

            "outputs":[
                {
                    "type":"",
                    "data": {
                        //
                        //
                    }
                }
            ]
        }]
    }
    */

}
