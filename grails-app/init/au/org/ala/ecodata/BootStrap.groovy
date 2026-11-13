package au.org.ala.ecodata


import au.org.ala.ecodata.customcodec.AccessLevelCodec

import grails.converters.JSON
import grails.util.Environment
import groovy.json.JsonSlurper
import jakarta.servlet.ServletContext
import org.bson.types.ObjectId
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext

import javax.imageio.ImageIO

import static grails.async.Promises.task

//import net.sf.json.JSONNull

class BootStrap {

    /** Lower than the default (0) so the type-specific marshallers registered here take precedence. */
    static final int ENUM_FALLBACK_MARSHALLER_PRIORITY = -1

    /**
     * Registers the JSON enum formats returned by the ecodata API.
     *
     * Grails 8 made SimpleEnumMarshaller (enum rendered as a plain string) the default and removed the previous
     * EnumMarshaller, which rendered {"enumType": "<class name>", "name": "<constant>"}. MERIT and BioCollect consume
     * the existing format, so it is registered explicitly as a fallback for all enums.
     */
    static void registerEnumMarshallers() {
        JSON.registerObjectMarshaller(Enum, ENUM_FALLBACK_MARSHALLER_PRIORITY) { Enum value ->
            [enumType: value.getDeclaringClass().name, name: value.name()]
        }

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
    }

    def elasticSearchService
    def grailsApplication
    def auditService
    def hubService
    def grailsCacheManager
    SpatialService spatialService
    MetadataService metadataService
    ServletContext servletContext

    @Autowired
    MongoDatastore mongoDatastore

    def init = {
        // Don't pre-scache spatial layers during development due to frequent restarts
        if (Environment.DEVELOPMENT != Environment.current) {
            initializeSpatialIntersection()
        }


        // Add custom GORM event listener for ES indexing
        def ctx = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)
        ctx.getBeansOfType(Datastore).values().each { Datastore d ->
            log.info "Adding listener for datastore: ${d}"
            ctx.addApplicationListener new GormEventListener(d, elasticSearchService, auditService)
        }

        elasticSearchService.initialize()
        // Index all docs
        if (grailsApplication.config.getProperty('app.elasticsearch.indexAllOnStartup', Boolean, false)) {
            elasticSearchService.indexAll()
        }

        // Registering the codec via grails.mongodb.codecs fails to allow this type to be used in dynamic
        // properties (specifically when a UserPermission is stored as the entity in an AuditMessage).
        // This appears to be because, despite being registered, it gets cached as an Optional None.
        // Registering it here seems to resolve the issue.
        mongoDatastore.setCodecs([new AccessLevelCodec()])

        registerEnumMarshallers()

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

        JSON.registerObjectMarshaller(Service) {
            return it.toMap()
        }

        // Grails 7 now serializes java.util.Date with milliseconds which MERIT and BioCollect do not expect.
        JSON.registerObjectMarshaller(Date) {
            DateUtil.format(it)
        }


      //  JSON.registerObjectMarshaller(JSONNull, {return ""})

        // Setup the default ALA hub if necessary as BioCollect won't load without it.
        Hub alaHub = Hub.findByUrlPath('ala')
        if (!alaHub) {
            Map alaHubData = new JsonSlurper().parseText(getClass().getResourceAsStream("/data/alaHub.json").getText())
            hubService.create(alaHubData)
        }

        //Add a default project for individual sightings (unless disabled)
        def individualSightingsProject = au.org.ala.ecodata.Project.findByProjectId(grailsApplication.config.getProperty('records.default.projectId'))
        if(!individualSightingsProject){
            log.info "Creating individual sightings project"
            def project = new au.org.ala.ecodata.Project(
                    name: "Individual sightings",
                    projectId: grailsApplication.config.getProperty('records.default.projectId'),
                    dataResourceId: grailsApplication.config.getProperty('records.default.dataResourceId'),
                    isCitizenScience: true
            )
            project.save(flush: true)
        }

        ImageIO.scanForPlugins()
    }


    /**
     * Cache intersect of electorates and states layer so that fieldcapture does not have to wait a long time.
     */
    void initializeSpatialIntersection() {
        task {
            Hub meritHub = Hub.findByUrlPath('merit')
            Map config = metadataService.getGeographicConfig(meritHub.hubId)
            String electorateLayerId = config.contextual.elect
            List stateLayerIds = [config.contextual.state]
            spatialService.features(electorateLayerId, stateLayerIds)
        }
    }

    def destroy = {
        // shutdown ES server
        elasticSearchService.destroy()
        log.info "Shutting down - completed destroy method"
    }
}
