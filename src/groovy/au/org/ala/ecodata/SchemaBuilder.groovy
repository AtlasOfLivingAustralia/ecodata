package au.org.ala.ecodata
/**
 * Builds a JSON schema from a data model definition.
 * @link http://json-schema.org/
 */

class SchemaBuilder {

    /** Builds URLs for various schema */
    SchemaUrlBuilder urlBuilder

    def activitiesModel


    public SchemaBuilder(config, activitiesModel) {
        this.activitiesModel = activitiesModel
        this.urlBuilder = new SchemaUrlBuilder(config, activitiesModel)
    }

    /**
     * Temp storage for properties that reference nested object structures.  Used to separate those structures into
     * references in the schema (instead of embedded/nested objects) to simplify producing the documentation
     */
    def referencedDefinitions = [:]


    def projectActivitiesSchema(programsModel) {

        // TODO To constrain program / subprogram definitions we would need to provide a nested set of definitions
        // per program.

        def activities = activitiesModel.activities.collect {[$ref:urlBuilder.activitySchemaUrl(it.name)]}

        def programs = programsModel.programs.collect{it.name}

        def schema = [
            id:urlBuilder.projectActivitiesSchemaUrl()+"#",
            $schema:'http://json-schema.org/draft-04/schema#',
            type:'object',
            properties: [
                projectId:[
                        type:'object',
                        description:"Identifies the project by one of its unique properties (grantId, externalId, internal guid).  <pre>{type:string, value:string}</pre> Type must be one of ['grantId','externalId','guid'].",
                        properties:[
                                type:constrainedTextProperty([constraints:['grantId', 'externalId', 'guid']]),
                                id:textProperty(null)
                        ],
                        required: ['type', 'id']
                ],
                //startDate:dateProperty(null),
                //endDate:dateProperty(null),
                //program:constrainedTextProperty([constraints: programs]),
                //subprogram:textProperty(null),
                //outputTargets:[type:'array', items:[type:'object', anyOf:allowedOutputs]],
                activities:[type:'array', items:[type:'object', anyOf:activities]]
            ],
            required:['projectId']
        ]

        schema

    }

    def schemaForActivity(activity) {

        def allowedOutputs = activity.outputs.collect {[$ref:urlBuilder.outputSchemaUrl(it)]}

        def schema = [
            id:urlBuilder.activitySchemaUrl(activity.name)+'#',
            $schema:'http://json-schema.org/draft-04/schema#',
            type:'object',
            properties: [
                projectExternalId:[type:'string', description:'Must match the externalId property of an existing project entity'],
                type:[enum:[activity.name], description: ''],
                plannedStartDate:dateProperty(null),
                plannedEndDate:dateProperty(null),
                startDate:dateProperty(null),
                endDate:dateProperty(null),
                mainTheme:textProperty(null),
                progress:constrainedTextProperty([constraints:['planned','started','finished']])
            ]
        ]

        if (allowedOutputs.size() > 0) {
            schema.properties << [outputs:[type:'array', items:[type:'object', anyOf:allowedOutputs]]]
        }
        schema
    }

    /**
     * This method is not threadsafe.
     * @param output
     * @return
     */
    def schemaForOutput(name, output) {

        def outputProperties = [:]
        outputProperties << [name:[enum:[name]]]
        outputProperties << [data:objectSchema(output.dataModel)]
        def schema = [id:urlBuilder.outputSchemaUrl(name), $schema:'http://json-schema.org/draft-04/schema#', type:'object', properties: outputProperties]

        def definitions = [:]

        if (referencedDefinitions.size() > 0) {
            referencedDefinitions.each { key, value ->
                definitions << [(key):objectSchema(value)]
            }
            schema << [definitions:definitions]
        }
        schema
    }

    def objectSchema(objectProps) {
        def properties = [:]
        def required = []
        objectProps.each {
            if (!isComputed(it)) {  // TODO what to do about computed values?

                def validationRules = new ValidationRules(it)
                if (validationRules.mandatory) {
                    required << it.name
                }
                def props = generatorFor(it).schemaFor(it)
                def validationProps = validationRules.validationProperties()
                if (validationProps) {
                    props.putAll(validationProps)
                }
                properties << [(it.name):props]

            }
        }
        def schema = [type:'object', properties:properties]
        if (required) {
            schema << [required:required]
        }
        schema
    }

    def isComputed(property) {
        return property.computed || (property.dataType == 'lookupByDiscreteValues') || (property.dataType == 'lookupRange')
    }


   PropertySchemaGenerator generatorFor(property) {
       if (property.constraints && property.dataType == 'text') {
           return new PropertySchemaGenerator(this.&constrainedTextProperty)
       }
       else {
           def typeGenerator
           switch (property.dataType) {
               case 'text':
                   typeGenerator = this.&textProperty
                   break
               case 'stringList':
                   typeGenerator = this.&stringListProperty
                   break
               case 'list':
                   typeGenerator = this.&listProperty
                   break
               case 'matrix':
                   typeGenerator = this.&matrixProperty
                   break
               case 'species':
                   typeGenerator = this.&speciesProperty
                   break
               case 'number':
                   typeGenerator = this.&numberProperty
                   break
               case 'date':
               case 'simpleDate':
                   typeGenerator = this.&dateProperty
                   break
               case 'image':
                   typeGenerator = this.&imageProperty
                   break
               case 'photoPoints':
                   typeGenerator = this.&photoPointProperty
                   break
               case 'boolean':
                   typeGenerator = this.&booleanProperty
                   break

               default:
                   //typeGenerator = this.&error
                   //break
                   throw new IllegalArgumentException("Unsupported dataType: ${property.dataType} for property: ${property}")
           }

           return new PropertySchemaGenerator(typeGenerator)
       }
   }

    class PropertySchemaGenerator {

        def typeSpecificProperties

        public PropertySchemaGenerator(Closure typeSpecificProperties) {
            this.typeSpecificProperties = typeSpecificProperties
        }

        def schemaFor(property) {
            def properties = [:]
            if (property.title) {
                properties << [title:property.title]
            }
            if (property.description) {
                properties << [description:property.description]
            }

            def extraProperties = typeSpecificProperties(property)
            properties.putAll(extraProperties)
            properties
        }
    }



    def textProperty(property) {
        return [type:'string']
    }

    def constrainedTextProperty(property) {
        return [enum:property.constraints]
    }

    def stringListProperty(property) {
        return [type:'array', items:[enum:property.constraints]]
    }

    def listProperty(property) {
        referencedDefinitions << [(property.name):property.columns]
        return [type:'array', items:[type:'object', oneOf:[[$ref:"#/definitions/${property.name}"]]]]
    }

    def matrixProperty(property) {
        referencedDefinitions << [(property.name):property.rows]

        def columnProperties = [:]
        property.columns.each {
            columnProperties << [(it.name):[type:'object', oneOf:[[$ref:"#/definitions/${property.name}"]]]]
        }
        return [type:'object', properties: columnProperties]
    }

    def speciesProperty(property) {
        return [type:'object', properties:[name:[type:'string'], guid:[type:'string'], listId:[type:'string']]]
    }

    def numberProperty(property) {
        return [type:'number']
    }

    def dateProperty(property) {
        [type:'string', format:'date-time']
    }

    def imageProperty(property) {
        return [type:'object', properties:[filename:[type:'string'], data:[type:'base64']]]
    }

    def photoPointProperty(property) {
        return [type:'array', items:[type:'object', properties:[filename:[type:'string'], data:[type:'string', format:'base64'], photoPointId:[type:'string']]]]
    }

    def booleanProperty(property) {
        return [type:'boolean']
    }

    def error(property) {
        return [type:'unsupported']
    }

    def validation(property) {
        if (!property.validate) {
            return
        }
        def criteria = property.validate.tokenize(',')
        criteria = criteria.collect { it.trim() }

        def values = []
        criteria.each {
            switch (it) {
                case 'required':
                    if (model.type == 'selectMany') {
                        values << 'minCheckbox[1]'
                    }
                    else {
                        values << it
                    }
                    break
                case 'number':
                    values << 'custom[number]'
                    break
                case it.startsWith('min:'):
                    values << it
                    break
                default:
                    values << it
            }
        }
    }

    class ValidationRules {

        def validationRules = []
        public ValidationRules(property) {
            if (property.validate) {
                def criteria = property.validate.tokenize(',')
                validationRules = criteria.collect { it.trim() }
            }

        }


        public boolean isMandatory() {
            return validationRules.contains('required')
        }

        def validationProperties() {
            def props = [:]
            validationRules.each {
                if (it.startsWith('min')) {
                    props << minProperty(it)
                }
                else if (it.startsWith('max')) {
                    props << maxProperty(it)
                }
            }
            return props
        }

        def valueInBrackets(rule) {
            def matcher = rule =~ /.*\[(.*)\]/
            if (matcher.matches()) {
                return matcher[0][1]
            }
            return null
        }

        def minProperty(rule) {
            def min = valueInBrackets(rule)
            if (min) {
                return [minimum: min as BigDecimal]
            }
            throw new IllegalArgumentException("Invalid validation rule: "+rule)
        }

        def maxProperty(rule) {
            def max = valueInBrackets(rule)
            if (max) {
                return [maximum: max as BigDecimal]
            }
            throw new IllegalArgumentException("Invalid validation rule: "+rule)
        }

    }
}
