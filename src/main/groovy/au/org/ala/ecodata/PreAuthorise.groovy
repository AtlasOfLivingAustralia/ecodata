package au.org.ala.ecodata

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation to check if user has valid project/organisation permissions
 */
@Target([ElementType.TYPE, ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PreAuthorise {
    String id() default "id"
    boolean basicAuth() default true      // Check username against authKey?
    String accessLevel() default "editor" // What is the minimum access level needed to access the method?
    String idType() default ""
}
