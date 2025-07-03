package au.org.ala.ecodata.graphql.converters


import au.org.ala.ecodata.graphql.models.TargetMeasure
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException

class TargetMeasureConverter implements Coercing<TargetMeasure, TargetMeasure> {

    protected Optional<TargetMeasure> convert(Object input) {
        if (input instanceof TargetMeasure) {
            Optional.of((TargetMeasure) input)
        }
        else {
            Optional.empty()
        }
    }

    @Override
    TargetMeasure serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to a TargetMeasure")
        })
    }

    @Override
    TargetMeasure parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to a TargetMeasure")
        })
    }

    @Override
    TargetMeasure parseLiteral(Object input) {
        null
    }

}
