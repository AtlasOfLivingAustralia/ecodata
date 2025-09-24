package au.org.ala.ecodata.graphql.converters

import au.org.ala.ecodata.graphql.models.OutputData
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException

class OutputDataConverter implements Coercing<Map, Map> {

    protected Optional<Map> convert(Object input) {
        if (input instanceof Map) {
            Optional.of((Map) input)
        }
        else {
            Optional.empty()
        }
    }

    @Override
    Map serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to a OutputData")
        })
    }

    @Override
    Map parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to a OutputData")
        })
    }

    @Override
    Map parseLiteral(Object input) {
        null
    }
}

