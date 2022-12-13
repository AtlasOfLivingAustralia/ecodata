package au.org.ala.ecodata.graphql.converters

import au.org.ala.ecodata.graphql.models.Schema
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException

class SchemaConverter implements Coercing<Schema, Schema> {

    protected Optional<Schema> convert(Object input) {
        if (input instanceof Schema) {
            Optional.of((Schema) input)
        }
        else {
            Optional.empty()
        }
    }

    @Override
    Schema serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to a Schema")
        })
    }

    @Override
    Schema parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to a Schema")
        })
    }

    @Override
    Schema parseLiteral(Object input) {
        null
    }
}
