package au.org.ala.ecodata

import java.lang.annotation.*

/**
 * Annotation to check that a valid api key has been provided.
 */
@Target([ElementType.TYPE, ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireJWT {
}
