package au.org.ala.ecodata.graphql.converters

import au.org.ala.ecodata.graphql.models.SectionTemplate
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException

class SectionTemplateConverter implements Coercing<SectionTemplate, SectionTemplate> {

    protected Optional<SectionTemplate> convert(Object input) {
        if (input instanceof SectionTemplate) {
            Optional.of((SectionTemplate) input)
        }
        else {
            Optional.empty()
        }
    }

    @Override
    SectionTemplate serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to a SectionTemplate")
        })
    }

    @Override
    SectionTemplate parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to a SectionTemplate")
        })
    }

    @Override
    SectionTemplate parseLiteral(Object input) {
        null
    }
}

