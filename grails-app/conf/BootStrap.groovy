import au.org.ala.ecodata.GormEventListener
import org.bson.BSON
import org.bson.Transformer
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.web.json.JSONObject
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

        // Allow groovy JSONObject$NULL to be saved (as null) to mongodb
        BSON.addEncodingHook(JSONObject.NULL.class, new Transformer() {
            public Object transform(Object o) {
                return null;
            }
        });

    }

    def destroy = {
        // shutdown ES server
        //elasticSearchService.destroy()
    }
}
