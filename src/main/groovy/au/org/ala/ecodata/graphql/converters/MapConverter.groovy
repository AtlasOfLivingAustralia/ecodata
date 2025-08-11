package au.org.ala.ecodata.graphql.converters


import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException

class MapConverter implements Coercing<Object, Map> {

    protected Optional<Map> convert(Object input) {
        if (input instanceof Map) {
            Optional.of((Map) input)
        }
        else if (input) {
            Optional.of(input as Map)
        }
        else {
            Optional.empty()
        }
    }

    @Override
    Map serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to a TargetMeasure")
        })
    }

    @Override
    Map parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to a TargetMeasure")
        })
    }

    @Override
    Map parseLiteral(Object input) {
        null
    }

}

