package au.org.ala.ecodata.metadata

import org.springframework.context.expression.MapAccessor
import org.springframework.expression.AccessException
import org.springframework.expression.EvaluationContext
import org.springframework.expression.Expression
import org.springframework.expression.TypedValue
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.lang.Nullable
import org.springframework.util.Assert

class ExpressionUtil {


    /** Evaluates the supplied expression against the data in the supplied Map */
    static def evaluate(Expression expression, Map expressionContext) {
        StandardEvaluationContext context = new StandardEvaluationContext(expressionContext)
        context.addPropertyAccessor(new NoExceptionMapAccessor())
        expression.getValue(context)
    }


    /**
     * Extends the Spring MapAccessor but instead of throwing an Exception if the
     * Map does not have a property with the supplied name just returns null.
     */
    static class NoExceptionMapAccessor extends MapAccessor {

        @Override
        public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
            Assert.state(target instanceof Map, "Target must be of type Map");
            Map<?, ?> map = (Map<?, ?>) target
            Object value = map.get(name)
            if (value == null && !map.containsKey(name)) {
                return TypedValue.NULL
            }
            return new TypedValue(value)

        }
    }
}
