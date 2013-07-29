import au.org.ala.ecodata.GormEventListener
import org.codehaus.groovy.grails.commons.ApplicationAttributes


class BootStrap {

    def elasticSearchService
    def grailsApplication

    def init = { servletContext ->
        // Add custom GORM event listener for ES indexing
        def applicationContext = servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT)
        applicationContext.addApplicationListener new GormEventListener(applicationContext.mongoDatastore)

        // Index all docs
        elasticSearchService.initialize()
        elasticSearchService.indexAll()
    }

    def destroy = {
        // shutdown ES server
        elasticSearchService.destroy()
    }
}
