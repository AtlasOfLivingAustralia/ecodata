package au.org.ala.ecodata.caching

import grails.plugin.cache.CustomCacheKeyGenerator
import groovy.transform.CompileStatic
import org.springframework.aop.framework.AopProxyUtils
import java.lang.reflect.Method
@CompileStatic
class EcodataCacheKeyGenerator extends CustomCacheKeyGenerator {
    static final String SEPARATOR = ':'

    @Override
    Object generate(Object target, Method method, Object... params) {
        Class<?> objClass = AopProxyUtils.ultimateTargetClass(target)
        String key =  params.collect { stringify(it) }.join(SEPARATOR)
        return objClass.getName().intern() +
                method.toString().intern() +
                key
    }

    @Override
    Serializable generate(String className, String methodName, int objHashCode, Closure keyGenerator) {
        final Object simpleKey = keyGenerator.call()
        return className + methodName + stringify(simpleKey)
    }

    @Override
    Serializable generate(String className, String methodName, int objHashCode, Map methodParams) {
        String simpleKey = stringify(methodParams)
        return className + methodName + simpleKey
    }

    String stringify(Object obj) {
        if (obj == null) return "null"

        if (obj instanceof Map) {
            return obj.entrySet()
                    .sort { a, b -> a.key.toString() <=> b.key.toString() }
                    .collect { "${stringify(it.key)}=${stringify(it.value)}" }
                    .join(SEPARATOR)
        }

        if (obj instanceof Collection) {
            return obj.collect { stringify(it) }.join(SEPARATOR)
        }

        if (obj.getClass().isArray()) {
                return Arrays.asList((Object[]) obj).collect { stringify(it) }.join(SEPARATOR)
        }

        return obj.toString()
    }
}
