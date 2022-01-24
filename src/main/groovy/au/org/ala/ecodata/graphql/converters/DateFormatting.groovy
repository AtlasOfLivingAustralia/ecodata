package au.org.ala.ecodata.graphql.converters

import graphql.schema.Coercing
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException;

/***
 * This class is used to convert the ISO date string to yyyy-MM-dd format
 */
class DateFormatting implements Coercing<String, String>{

        protected Optional<String> convert(Object input) {
            if (input instanceof Date) {
                Optional.of(input.format( 'yyyy-MM-dd' ))
            }
            else {
                Optional.empty()
            }
        }

        @Override
        String serialize(Object input) {
            convert(input).orElseThrow( {
                throw new CoercingSerializeException("Could not convert ${input.class.name} to a Date")
            })
        }

        @Override
        String parseValue(Object input) {
            convert(input).orElseThrow( {
                throw new CoercingParseValueException("Could not convert ${input.class.name} to a Date")
            })
        }

        @Override
        String parseLiteral(Object input) {
            null
        }
    }
