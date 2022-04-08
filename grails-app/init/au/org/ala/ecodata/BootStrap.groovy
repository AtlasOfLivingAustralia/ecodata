package au.org.ala.ecodata

import au.org.ala.ecodata.AccessLevel
import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.AuditEventType
import au.org.ala.ecodata.GormEventListener
import au.org.ala.ecodata.Hub
import au.org.ala.ecodata.Program
import au.org.ala.ecodata.ManagementUnit
import au.org.ala.ecodata.customcodec.AccessLevelCodec
import au.org.ala.ecodata.data_migration.ActivityFormMigrator
import grails.converters.JSON
import groovy.json.JsonSlurper
//import net.sf.json.JSONNull
import org.bson.BSON
import org.bson.Transformer
import org.bson.types.ObjectId
import grails.core.ApplicationAttributes
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.web.json.JSONObject
import org.grails.datastore.mapping.core.Datastore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext

import javax.imageio.ImageIO

class BootStrap {

    def elasticSearchService
    def grailsApplication
    def auditService
    def hubService
    @Autowired
    MongoDatastore mongoDatastore

    def init = { servletContext ->

        // Add custom GORM event listener for ES indexing
        def ctx = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)
        ctx.getBeansOfType(Datastore).values().each { Datastore d ->
            log.info "Adding listener for datastore: ${d}"
            ctx.addApplicationListener new GormEventListener(d, elasticSearchService, auditService)
        }

        elasticSearchService.initialize()
        // Index all docs
        if (grailsApplication.config.app.elasticsearch.indexAllOnStartup) {
            elasticSearchService.indexAll()
        }

        // Registering the codec via grails.mongodb.codecs fails to allow this type to be used in dynamic
        // properties (specifically when a UserPermission is stored as the entity in an AuditMessage).
        // This appears to be because, despite being registered, it gets cached as an Optional None.
        // Registering it here seems to resolve the issue.
        mongoDatastore.setCodecs([new AccessLevelCodec()])

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

        JSON.registerObjectMarshaller(ManagementUnit) {
            return it.toMap()
        }

        JSON.registerObjectMarshaller(ObjectId) { ObjectId objId ->
            return objId.toString()
        }

        JSON.registerObjectMarshaller(AuditEventType) { AuditEventType eventType ->
            return eventType.toString()
        }

        JSON.registerObjectMarshaller(ExternalId) {
            return [idType:it.idType, externalId:it.externalId]
        }

      //  JSON.registerObjectMarshaller(JSONNull, {return ""})

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
        elasticSearchService.destroy()
    }
}
