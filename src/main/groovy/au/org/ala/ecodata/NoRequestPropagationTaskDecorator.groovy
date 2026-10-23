package au.org.ala.ecodata

import groovy.transform.CompileStatic
import org.springframework.core.task.TaskDecorator

/**
 * Replaces the Grails 8 GrailsWebRequestTaskDecorator (bean name "grailsWebRequestTaskDecorator") which copies the
 * submitting thread's GrailsWebRequest onto the thread running a Promise task.
 *
 * Ecodata uses fire-and-forget tasks that routinely outlive the HTTP request that started them. Once Tomcat recycles
 * the request, any access to it from the task thread (e.g. the data binding performed by domain map constructors
 * such as new AuditMessage([:])) throws "The request object has been recycled and is no longer associated with this
 * facade".
 */
@CompileStatic
class NoRequestPropagationTaskDecorator implements TaskDecorator {

    @Override
    Runnable decorate(Runnable task) {
        task
    }
}
