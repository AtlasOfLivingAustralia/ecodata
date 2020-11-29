package au.org.ala.ecodata.graphql.converters

import au.org.ala.ecodata.graphql.models.OutputData
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException

class OutputDataConverter implements Coercing<OutputData, OutputData> {

    protected Optional<OutputData> convert(Object input) {
        if (input instanceof OutputData) {
            Optional.of((OutputData) input)
        }
        else {
            Optional.empty()
        }
    }

    @Override
    OutputData serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to a OutputData")
        })
    }

    @Override
    OutputData parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to a OutputData")
        })
    }

    @Override
    OutputData parseLiteral(Object input) {
        null
    }
}

