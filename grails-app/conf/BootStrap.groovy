import au.org.ala.ecodata.GormEventListener
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.grails.datastore.mapping.core.Datastore


class BootStrap {

    def elasticSearchService
    def grailsApplication

    def init = { servletContext ->
        // Add custom GORM event listener for ES indexing
        def ctx = servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT)
        ctx.getBeansOfType(Datastore).values().each { Datastore d ->
            ctx.addApplicationListener new GormEventListener(d)
        }

        // Index all docs
        //elasticSearchService.initialize()
        elasticSearchService.indexAll()
    }

    def destroy = {
        // shutdown ES server
        elasticSearchService.destroy()
    }
}
