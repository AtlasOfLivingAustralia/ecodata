import au.org.ala.ecodata.GormEventListener
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.grails.datastore.mapping.core.Datastore


class BootStrap {

    def elasticSearchService
    def grailsApplication
    def auditService

    def init = { servletContext ->
        // Add custom GORM event listener for ES indexing
        def ctx = servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT)
        ctx.getBeansOfType(Datastore).values().each { Datastore d ->
            println "Adding listener for datastore: ${d}"
            ctx.addApplicationListener new GormEventListener(d, elasticSearchService, auditService)
        }

        // Index all docs
        //elasticSearchService.initialize()
        if (grailsApplication.config.app.elasticsearch.indexAllOnStartup) {
            elasticSearchService.indexAll()
        }
    }

    def destroy = {
        // shutdown ES server
        //elasticSearchService.destroy()
    }
}
