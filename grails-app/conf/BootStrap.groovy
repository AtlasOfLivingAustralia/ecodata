import au.org.ala.ecodata.AccessLevel
import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.AuditEventType
import au.org.ala.ecodata.GormEventListener
import au.org.ala.ecodata.Hub
import au.org.ala.ecodata.Program
import au.org.ala.ecodata.data_migration.ActivityFormMigrator
import grails.converters.JSON
import groovy.json.JsonSlurper
import net.sf.json.JSONNull
import org.bson.BSON
import org.bson.Transformer
import org.bson.types.ObjectId
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.codehaus.groovy.grails.web.json.JSONObject
import org.grails.datastore.mapping.core.Datastore

import javax.imageio.ImageIO

class BootStrap {

    def elasticSearchService
    def grailsApplication
    def auditService
    def hubService

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

        // Allow GStrings to be saved to mongodb
        BSON.addEncodingHook(GString.class, new Transformer() {
            @Override
            Object transform(Object o) {
                return o?o.toString():null
            }
        })

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

        JSON.registerObjectMarshaller(Program) {
            return it.toMap()
        }

        JSON.registerObjectMarshaller(ObjectId) { ObjectId objId ->
            return objId.toString()
        }

        JSON.registerObjectMarshaller(AuditEventType) { AuditEventType eventType ->
            return eventType.toString()
        }

        JSON.registerObjectMarshaller(JSONNull, {return ""})

        // Setup the default ALA hub if necessary as BioCollect won't load without it.
        Hub alaHub = Hub.findByUrlPath('ala')
        if (!alaHub) {
            Map alaHubData = new JsonSlurper().parseText(getClass().getResourceAsStream("/data/alaHub.json").getText())
            hubService.create(alaHubData)
        }

        //Add a default project for individual sightings (unless disabled)
        def individualSightingsProject = au.org.ala.ecodata.Project.findByProjectId(grailsApplication.config.records.default.projectId)
        if(!individualSightingsProject){
            log.info "Creating individual sightings project"
            def project = new au.org.ala.ecodata.Project(
                    name: "Individual sightings",
                    projectId: grailsApplication.config.records.default.projectId,
                    dataResourceId: grailsApplication.config.records.default.dataResourceId,
                    isCitizenScience: true
            )
            project.save(flush: true)
        }

        ImageIO.scanForPlugins()


        // Data migration of activities-model.json
        int formCount = ActivityForm.count()
        if (formCount == 0) {
            new ActivityFormMigrator(grailsApplication.config.app.external.model.dir).migrateActivitiesModel()
        }
    }

    def destroy = {
        // shutdown ES server
        //elasticSearchService.destroy()
    }
}
