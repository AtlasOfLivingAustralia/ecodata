package au.org.ala.ecodata.graphql.converters


import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import org.bson.types.ObjectId

class ObjectConverter implements Coercing<Object, Object> {

    protected Optional<Object> convert(Object input) {

            Optional.empty()

    }

    @Override
    Object serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to an Object")
        })
    }

    @Override
    Object parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to an Object")
        })
    }

    @Override
    ObjectId parseLiteral(Object input) {
        null
    }


}
