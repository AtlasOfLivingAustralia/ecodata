package au.org.ala.ecodata.metadata

import groovy.util.logging.Slf4j
import org.springframework.context.expression.MapAccessor
import org.springframework.expression.AccessException
import org.springframework.expression.EvaluationContext
import org.springframework.expression.Expression
import org.springframework.expression.TypedValue
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.lang.Nullable
import org.springframework.util.Assert

@Slf4j
class ExpressionUtil {


    /** Evaluates the supplied expression against the data in the supplied Map */
    static def evaluateWithDefault(Expression expression, Map expressionContext, defaultValue) {
        StandardEvaluationContext context = new StandardEvaluationContext(expressionContext)
        context.addPropertyAccessor(new NoExceptionMapAccessor(null))

        def result
        try {
            result = expression.getValue(context)
            if (Double.isNaN(result)) {
                result = defaultValue
            }
        }
        catch (Exception e) {
            log.debug("Error evaluating expression: ${expression.getExpressionString()}", e.getMessage())
            result = defaultValue
        }
        result

    }


    /**
     * Extends the Spring MapAccessor but instead of throwing an Exception if the
     * Map does not have a property with the supplied name just returns null.
     */
    static class NoExceptionMapAccessor extends MapAccessor {

        TypedValue defaultValue
        NoExceptionMapAccessor(defaultValue) {
            this.defaultValue = new TypedValue(defaultValue)
        }
        @Override
        public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
            Assert.state(target instanceof Map, "Target must be of type Map");
            Map<?, ?> map = (Map<?, ?>) target
            Object value = map.get(name)
            if (value == null && !map.containsKey(name)) {
                return defaultValue
            }
            return new TypedValue(value)

        }

        @Override
        public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
            return target instanceof Map
        }
    }
}
