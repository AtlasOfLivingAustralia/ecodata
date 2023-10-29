package au.org.ala.ecodata.graphql.converters

import au.org.ala.ecodata.graphql.models.MeriPlan
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException

class MeriPlanConverter implements Coercing<MeriPlan, MeriPlan> {

    protected Optional<MeriPlan> convert(Object input) {
        if (input instanceof MeriPlan) {
            Optional.of((MeriPlan) input)
        }
        else {
            Optional.empty()
        }
    }

    @Override
    MeriPlan serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to a MeriPlan")
        })
    }

    @Override
    MeriPlan parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to a MeriPlan")
        })
    }

    @Override
    MeriPlan parseLiteral(Object input) {
        null
    }
}

