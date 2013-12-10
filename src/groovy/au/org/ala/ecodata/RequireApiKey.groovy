package au.org.ala.ecodata

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation to check that a valid api key has been provided.
 */
@Target([ElementType.TYPE, ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireApiKey {
    String projectIdParam() default "id"
    String redirectController() default "project"
    String redirectAction() default "index"
}
