package au.org.ala.ecodata.graphql.converters

import au.org.ala.ecodata.graphql.models.Summary
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException

class SummaryConverter implements Coercing<Summary, Summary> {

    protected Optional<Summary> convert(Object input) {
        if (input instanceof Summary) {
            Optional.of((Summary) input)
        }
        else {
            Optional.empty()
        }
    }

    @Override
    Summary serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to a Summary")
        })
    }

    @Override
    Summary parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to a Summary")
        })
    }

    @Override
    Summary parseLiteral(Object input) {
        null
    }
}

