import au.org.ala.ecodata.AccessLevel
import au.org.ala.ecodata.AuditEventType
import au.org.ala.ecodata.GormEventListener
import grails.converters.JSON
import net.sf.json.JSONNull
import org.bson.BSON
import org.bson.Transformer
import org.bson.types.ObjectId
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
            log.info "Adding listener for datastore: ${d}"
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

        /**
         * Custom JSON serializer for {@link AccessLevel} enum
         */
        JSON.registerObjectMarshaller( AccessLevel ) { AccessLevel al ->
            return [
                    class: al.getClass().canonicalName,
                    name : al.name(),
                    code : al.getCode()
            ]
        }

        JSON.registerObjectMarshaller(ObjectId) { ObjectId objId ->
            return objId.toString()
        }

        JSON.registerObjectMarshaller(AuditEventType) { AuditEventType eventType ->
            return eventType.toString()
        }

        JSON.registerObjectMarshaller(JSONNull, {return ""})

    }

    def destroy = {
        // shutdown ES server
        //elasticSearchService.destroy()
    }
}
