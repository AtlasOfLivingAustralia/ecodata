package au.org.ala.ecodata.graphql.converters

import au.org.ala.ecodata.DateUtil
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import org.jetbrains.annotations.NotNull

/***
 * This class is used to convert the ISO date string to yyyy-MM-dd format
 */
class DateFormatting implements Coercing<Date, String>{

    protected Date parse(String dateStr ) {
        DateUtil.parseDisplayDate(dateStr)
    }

    protected String format(Date date) {
        DateUtil.formatAsDisplayDate(date)
    }

    @Override
    String serialize(@NotNull Object dataFetcherResult, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingSerializeException {

        Date date = null
        if (dataFetcherResult instanceof Date) {
            date = dataFetcherResult
        }
        else if (dataFetcherResult instanceof String) {
            date = parse(dataFetcherResult)
        }
        if (!date) {
            throw new CoercingSerializeException("Invalid date value: ${dataFetcherResult}")
        }
        format(date)
    }

    @Override
    Date parseValue(@NotNull Object input, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseValueException {
        Date result = null
        if (input instanceof Date) {
            result = input
        }
        else if (input instanceof String) {
            result = parse(input)
        }
        if (!result) {
            throw new CoercingParseValueException("Invalid date value: ${input}")
        }
        result
    }

    @Override
    Date parseLiteral(@NotNull Value<?> input, @NotNull CoercedVariables variables, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            parseValue(((StringValue) input).getValue(), graphQLContext, locale)
        }
        else {
            throw new CoercingParseLiteralException("Invalid date value: ${input}")
        }

    }

    @Override
    Value<?> valueToLiteral(@NotNull Object input, @NotNull GraphQLContext graphQLContext, @NotNull Locale locale) {
        if (input instanceof Date) {
            return new StringValue(format(input))
        }
        else if (input instanceof String) {
            return new StringValue(input)
        }
        throw new CoercingParseLiteralException("Invalid date value: ${input}")
    }
}
