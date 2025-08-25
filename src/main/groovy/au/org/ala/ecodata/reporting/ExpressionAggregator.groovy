package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.metadata.ExpressionUtil
import org.springframework.expression.Expression
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser

/**
 * Categorises an activity into a group based on a supplied grouping criteria then delegates to the appropriate
 * Aggregator.
 */
class ExpressionAggregator extends BaseAggregator {

    List<AggregatorIf> aggregators
    int count

    AggregatorFactory factory = new AggregatorFactory()
    Expression expression
    def defaultValue = 0

    ExpressionAggregator(ExpressionAggregationConfig config) {

        ExpressionParser expressionParser = new SpelExpressionParser()
        expression = expressionParser.parseExpression(config.expression)
        defaultValue = config.defaultValue ?: 0

        aggregators = config.childAggregations.collect {
            factory.createAggregator(it)
        }
    }

    PropertyAccessor getPropertyAccessor() {
        return null
    }

    void aggregateSingle(Map output) {
        count++
        aggregators.each {
            it.aggregate(output)
        }

    }

    /**
     * If we have a single childAggregation, return a SingleResult, otherwise a
     * GroupedAggregationResult.
     */
    AggregationResult result() {

        SingleResult result = new SingleResult()
        Map expressionContext = [:]

        aggregators.each {
            AggregationResult childResult = it.result()
            if (childResult instanceof SingleResult) {
                expressionContext[childResult.label] = childResult.result
            }
            else if (childResult instanceof GroupedAggregationResult) {
                expressionContext[childResult.label] = childResult.groups?.collectEntries {
                    [(it.group):it.results[0]?.result]
                }
            }
        }

        result.result = ExpressionUtil.evaluateWithDefault(expression, expressionContext, defaultValue)

        result
    }

}


