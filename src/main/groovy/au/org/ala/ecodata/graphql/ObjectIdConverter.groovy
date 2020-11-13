package au.org.ala.ecodata.graphql

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import org.bson.types.ObjectId

class ObjectIdConverter implements Coercing<ObjectId, ObjectId> {

    protected Optional<ObjectId> convert(Object input) {
        if (input instanceof ObjectId) {
            Optional.of((ObjectId) input)
        }
        else if (input instanceof String) {
            parseObjectId((String) input)
        }
        else {
            Optional.empty()
        }
    }

    @Override
    ObjectId serialize(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingSerializeException("Could not convert ${input.class.name} to an ObjectId")
        })
    }

    @Override
    ObjectId parseValue(Object input) {
        convert(input).orElseThrow( {
            throw new CoercingParseValueException("Could not convert ${input.class.name} to an ObjectId")
        })
    }

    @Override
    ObjectId parseLiteral(Object input) {
        if (input instanceof StringValue) {
            parseObjectId(((StringValue) input).value).orElse(null)
        }
        else {
            null
        }
    }

    protected Optional<ObjectId> parseObjectId(String input) {
        if (ObjectId.isValid(input)) {
            Optional.of(new ObjectId(input))
        }
        else {
            Optional.empty()
        }
    }

}
