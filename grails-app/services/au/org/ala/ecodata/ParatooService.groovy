package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.PropertyAccessor
import au.org.ala.ecodata.paratoo.*
import au.org.ala.ws.tokens.TokenService
import grails.async.Promise
import grails.converters.JSON
import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import javassist.NotFoundException

import java.util.regex.Matcher
import java.util.regex.Pattern

import static grails.async.Promises.task
/**
 * Supports the implementation of the paratoo "org" interface
 */
@Slf4j
class ParatooService {
    static final String DATASET_DATABASE_TABLE = 'Database Table'
    static final int PARATOO_MAX_RETRIES = 3
    static final String PARATOO_PROTOCOL_PATH = '/protocols'
    static final String PARATOO_DATA_PATH = '/protocols/reverse-lookup'
    static final String PARATOO_PROTOCOL_FORM_TYPE = 'EMSA'
    static final String PARATOO_PROTOCOLS_KEY = 'paratoo.protocols'
    static final String PARATOO_PROTOCOL_DATA_MAPPING_KEY = 'paratoo.surveyData.mapping'
    static final String PROGRAM_CONFIG_PARATOO_ITEM = 'supportsParatoo'
    static final String PARATOO_APP_NAME = "Monitor"
    static final String MONITOR_AUTH_HEADER = "Authorization"
    static final List DEFAULT_MODULES =
            ['Plot Selection and Layout', 'Plot Description', 'Opportune']
    static final List ADMIN_ONLY_PROTOCOLS = ['Plot Selection']
    static final String INTERVENTION_PROTOCOL_TAG = 'intervention'
    static final String PARATOO_UNIT_FIELD_NAME = "x-paratoo-unit"
    static final String PARATOO_HINT = "x-paratoo-hint"
    static final String PARATOO_MODEL_REF = "x-model-ref"
    static final int PARATOO_FLOAT_DECIMAL_PLACES = 6
    static final String PARATOO_SCHEMA_POSTFIX = "Request"
    static final String PARATOO_FILE_SCHEMA_NAME = "UploadFile"
    static final String PARATOO_FILE_MODEL_NAME = "file"
    static final String PARATOO_LUT_REF = "x-lut-ref"
    static final String PARATOO_FILE_TYPE = "x-paratoo-file-type"
    static final String PARATOO_IMAGE_FILE_TYPE = "images"
    static final String PARATOO_SPECIES_TYPE = "x-paratoo-csv-list-taxa"
    static final String PARATOO_FIELD_LABEL = "x-paratoo-rename"
    static final List PARATOO_IGNORE_MODEL_LIST = [
            'created_at', 'createdAt', 'updated_at', 'updatedAt', 'created_by', 'createdBy', 'updated_by', 'updatedBy',
            'published_at', 'publishedAt', 'x-paratoo-file-type', PARATOO_DATAMODEL_PLOT_LAYOUT, PARATOO_DATAMODEL_PLOT_SELECTION,
            PARATOO_DATAMODEL_PLOT_VISIT, 'plot-visit', 'plot-selection', 'plot-layout', 'survey_metadata'
    ]
    static final List PARATOO_IGNORE_X_MODEL_REF_LIST_MINIMUM = [
            'file', 'admin::user'
    ]
    static final List PARATOO_IGNORE_X_MODEL_REF_LIST = [
            'plot-visit', 'plot-selection', 'plot-layout'
    ] + PARATOO_IGNORE_X_MODEL_REF_LIST_MINIMUM
    static final String PARATOO_WORKFLOW_PLOT_LAYOUT = "plot-layout"
    static final String PARATOO_COMPONENT = "x-paratoo-component"
    static  final String PARATOO_TYPE_ARRAY = "array"
    static final String PARATOO_LOCATION_COMPONENT = "location.location"
    static final String PARATOO_LOCATION_COMPONENT_STARTS_WITH = "location."
    static final String PARATOO_DATAMODEL_PLOT_LAYOUT = "plot_layout"
    static final String PARATOO_DATAMODEL_PLOT_SELECTION = "plot_selection"
    static final String PARATOO_DATAMODEL_PLOT_VISIT = "plot_visit"

    GrailsApplication grailsApplication
    SettingService settingService
    WebService webService
    ProjectService projectService
    SiteService siteService
    PermissionService permissionService
    TokenService tokenService
    CacheService cacheService
    ActivityService activityService
    RecordService recordService
    MetadataService metadataService
    UserService userService

    /**
     * The rules we use to find projects eligible for use by paratoo are:
     * 1. The project is under a program that has flagged eligibility for paratoo
     * 2. The project is currently active (status = active)
     * 3. The project has protocols selected.  (The current way this is implemented is via a mapping
     * to project services.  A hypothetical future implementation for BioCollect could include finding
     * a ProjectActivity with a compatible activity type)
     *
     * @param userId The user of interest
     * @param includeProtocols
     * @return
     */
    List<ParatooProject> userProjects(String userId) {

        List<ParatooProject> projects = findUserProjects(userId)

        projects.each { ParatooProject project ->
            project.protocols = findProjectProtocols(project)
        }

        projects.findAll { it.protocols }
    }

    private List findProjectProtocols(ParatooProject project) {
        log.debug "Finding protocols for ${project.id} ${project.name}"
        List<ActivityForm> protocols = []

        List monitoringProtocolCategories = project.getMonitoringProtocolCategories()
        if (monitoringProtocolCategories) {
            List categoriesWithDefaults = monitoringProtocolCategories + DEFAULT_MODULES
            protocols += findProtocolsByCategories(categoriesWithDefaults.unique())
            if (!project.isParaooAdmin()) {
                protocols = protocols.findAll { !(it.name in ADMIN_ONLY_PROTOCOLS) }
            }
            // Temporarily exclude intervention protocols until they are ready
            if (grailsApplication.config.getProperty('paratoo.excludeInterventionProtocols', Boolean.class, true)) {
                protocols = protocols.findAll { !(INTERVENTION_PROTOCOL_TAG in it.tags) }
            }

        }
        protocols
    }

    private static List findProtocolsByCategories(List categories) {
        List<ActivityForm> forms = ActivityForm.findAllByCategoryInListAndExternalAndStatusNotEqual(categories, true, Status.DELETED)
        forms
    }

    private List<ParatooProject> findUserProjects(String userId) {
        List<UserPermission> permissions = UserPermission.findAllByUserIdAndEntityTypeAndStatusNotEqual(userId, Project.class.name, Status.DELETED)


        // If the permission has been set as a favourite then delegate to the Hub permission.
        Map projectAccessLevels = [:]
        permissions?.each { UserPermission permission ->
            String projectId = permission.entityId
            // Don't override an existing permission with a starred permission
            if (permission.accessLevel == AccessLevel.starred) {
                if (!projectAccessLevels[projectId]) {
                    permission = permissionService.findParentPermission(permission)
                    projectAccessLevels[projectId] = permission?.accessLevel
                }
            } else {
                // Update the map of projectId to accessLevel
                projectAccessLevels[projectId] = permission.accessLevel
            }
        }

        List projects = Project.findAllByProjectIdInListAndStatus(new ArrayList(projectAccessLevels.keySet()), Status.ACTIVE)

        // Filter projects that aren't in a program configured to support paratoo or don't have permission
        projects = projects.findAll { Project project ->
            if (!project.programId) {
                return false
            }

            Program program = Program.findByProgramId(project.programId)
            Map config = program.getInheritedConfig()
            // The Monitor/Paratoo app is "write only" (i.e. there is no view mode for the data), so we don't support
            // the read only role
            config?.get(PROGRAM_CONFIG_PARATOO_ITEM) && projectAccessLevels[project.projectId] && projectAccessLevels[project.projectId] != AccessLevel.readOnly
        }

        List paratooProjects = projects.collect { Project project ->
            List<Site> sites = siteService.sitesForProjectWithTypes(project.projectId, [Site.TYPE_PROJECT_AREA, Site.TYPE_SURVEY_AREA])
            AccessLevel accessLevel = projectAccessLevels[project.projectId]
            mapProject(project, accessLevel, sites)
        }
        paratooProjects

    }

    Map mintCollectionId(String userId, ParatooCollectionId paratooCollectionId) {
        String projectId = paratooCollectionId.projectId
        Project project = Project.findByProjectId(projectId)

        // Update the identifier properties as per the org contract and save the data to the data set
        String dataSetId = Identifiers.getNew(true, '')
        paratooCollectionId.eventTime = new Date()
        paratooCollectionId.survey_metadata.orgMintedUUID = dataSetId
        paratooCollectionId.userId = project.organisationId // using the organisation as the owner of the data
        Map dataSet = mapParatooCollectionId(paratooCollectionId, project)

        dataSet.surveyId = paratooCollectionId.toMap() // No codec to save this to mongo

        if (!project.custom) {
            project.custom = [:]
        }
        if (!project.custom.dataSets) {
            project.custom.dataSets = []
        }

        dataSet.orgMintedIdentifier = paratooCollectionId.encodeAsOrgMintedIdentifier()

        log.info "Minting identifier for Monitor collection: ${paratooCollectionId}: ${dataSet.orgMintedIdentifier}"
        project.custom.dataSets << dataSet
        Map result = projectService.update([custom: project.custom], projectId, false)

        if (!result.error) {
            result.orgMintedIdentifier = dataSet.orgMintedIdentifier
        }
        result
    }

    private static String buildName(String protocolId, String displayDate, Project project) {
        ActivityForm protocolForm = ActivityForm.findByExternalId(protocolId)
        String dataSetName = protocolForm?.name + " - " + displayDate + " (" + project.name + ")"
        dataSetName
    }

    /**
     * Stores a collection created by monitor. This method will create a site, activity and output.
     * If species are recorded, record of it are automatically generated.
     * @param collection
     * @param project
     * @param userId - user making the data submission
     * @return
     */
    Map submitCollection(ParatooCollection collection, ParatooProject project, String userId = null) {
        userId = userId ?: userService.currentUserDetails?.userId
        Map dataSet = project.project.custom?.dataSets?.find{it.dataSetId == collection.orgMintedUUID}

        if (!dataSet) {
            throw new RuntimeException("Unable to find data set with orgMintedUUID: "+collection.orgMintedUUID)
        }
        dataSet.progress = Activity.STARTED
        dataSet.surveyId.coreSubmitTime = new Date()
        dataSet.surveyId.survey_metadata.provenance.putAll(collection.coreProvenance)

        Map authHeader = getAuthHeader()
        Promise promise = task {
            asyncFetchCollection(collection, authHeader, userId, project)
        }
        promise.onError { Throwable e ->
            log.error("An error occurred feching ${collection.orgMintedUUID}: ${e.message}", e)
        }
        def result = projectService.update([custom: project.project.custom], project.id, false)
        [updateResult: result, promise: promise]
    }

    Map asyncFetchCollection(ParatooCollection collection, Map authHeader, String userId, ParatooProject project) {
        Activity.withSession { session ->
            int counter = 0
            Map surveyDataAndObservations = null
            Map response = null
            Map dataSet = project.project.custom?.dataSets?.find{it.dataSetId == collection.orgMintedUUID}

            if (!dataSet) {
                throw new RuntimeException("Unable to find data set with orgMintedUUID: "+collection.orgMintedUUID)
            }

            // wait for 5 seconds before fetching data
            while(response == null && counter < PARATOO_MAX_RETRIES) {
                sleep(5 * 1000)
                try {
                    response = retrieveSurveyAndObservations(collection, authHeader)
                    surveyDataAndObservations = response?.collections
                } catch (Exception e) {
                    log.error("Error fetching collection data for ${collection.orgMintedUUID}: ${e.message}")
                }

                counter++
            }

            if (surveyDataAndObservations == null) {
                log.error("Unable to fetch collection data for ${collection.orgMintedUUID}")
                return
            } else {
                ParatooCollectionId surveyId = ParatooCollectionId.fromMap(dataSet.surveyId)
                ParatooProtocolConfig config = getProtocolConfig(surveyId.protocolId)
                config.surveyId = surveyId
                ActivityForm form = ActivityForm.findByExternalId(surveyId.protocolId)
                // add plot data to survey observations
                addPlotDataToObservations(surveyDataAndObservations, config)
                rearrangeSurveyData(surveyDataAndObservations, surveyDataAndObservations, form.sections[0].template.relationships.ecodata, form.sections[0].template.relationships.apiOutput)
                // transform data to make it compatible with data model
                surveyDataAndObservations = recursivelyTransformData(form.sections[0].template.dataModel, surveyDataAndObservations, form.name, 1, config)
                // If we are unable to create a site, null will be returned - assigning a null siteId is valid.

                if (!dataSet.siteId) {
                    dataSet.siteId = createSiteFromSurveyData(surveyDataAndObservations, collection, surveyId, project.project, config, form)
                }

                // plot layout is of type geoMap. Therefore, expects a site id.
                if (surveyDataAndObservations.containsKey(PARATOO_DATAMODEL_PLOT_LAYOUT) && dataSet.siteId) {
                    surveyDataAndObservations[PARATOO_DATAMODEL_PLOT_LAYOUT] = dataSet.siteId
                }

                // Delete previously created activity so that duplicate species records are not created.
                // Updating existing activity will also create duplicates since it relies on outputSpeciesId to determine
                // if a record is new and new ones are created by code.
                if (dataSet.activityId) {
                    activityService.delete(dataSet.activityId, true)
                }

                String activityId = createActivityFromSurveyData(form, surveyDataAndObservations, surveyId, dataSet.siteId, userId)
                List records = recordService.getAllByActivity(activityId)
                dataSet.areSpeciesRecorded = records?.size() > 0
                dataSet.activityId = activityId

                dataSet.startDate = config.getStartDate(surveyDataAndObservations)
                dataSet.endDate = config.getEndDate(surveyDataAndObservations)
                dataSet.format = DATASET_DATABASE_TABLE
                dataSet.sizeUnknown = true

                projectService.update([custom: project.project.custom], project.id, false)
            }
        }
    }

    /**
     * Rearrange survey data to match the data model.
     * e.g. [a: [b: [c: 1, d: 2], d: 1], b: [c: 1, d: 2]] => [b: [c: 1, d: 2, a: [d: 1]]]
     * where relationship [b: [a: [:]]]
     * @param properties
     * @param rootProperties
     * @param relationship
     * @param nodesToRemove
     * @param ancestors
     * @param isChild
     * @return
     */
    Map rearrangeSurveyData (Map properties, Map rootProperties, Map relationship, Map apiOutputRelationship, List nodesToRemove = [], List ancestors = [] , Boolean isChild = false) {
        if (relationship instanceof Map) {
            relationship.each { String nodeName, Map children ->
                ancestors.add(nodeName)
                def nodeObject = rootProperties[nodeName]
                if (nodeObject != null) {
                    properties[nodeName] = nodeObject
                    // don't add root properties to remove list
                    if (properties != rootProperties) {
                        nodesToRemove.add(nodeName)
                    }
                }
                else {
                    def result = findObservationDataFromAPIOutput(nodeName, apiOutputRelationship, rootProperties)
                    if (result.data instanceof List && (result.data.size() > 0)) {
                        nodeObject = result.data.first()
                    }

                    if (nodeObject != null) {
                        properties[nodeName] = nodeObject
                        nodesToRemove.add(result.path)
                    }
                }


                if (children) {
                    if (nodeObject instanceof Map) {
                        rearrangeSurveyData(nodeObject, rootProperties, children, apiOutputRelationship, nodesToRemove, ancestors, true )
                    }
                    else if (nodeObject instanceof List) {
                        nodeObject.each { Map node ->
                            rearrangeSurveyData(node, rootProperties, children, apiOutputRelationship, nodesToRemove, ancestors, true )
                        }
                    }
                }
                ancestors.removeLast()
            }
        }

        // remove nodes that have been rearranged. removing during iteration will cause exception.
        if (!isChild) {
            // sort based on depth of nesting so that child nodes are removed first before ancestors.
            nodesToRemove = nodesToRemove.sort { a, b -> b.split('\\.').size() <=> a.split('\\.').size() }
            nodesToRemove.each { String path ->
                removeProperty(properties, path)
            }

            nodesToRemove.clear()
        }

        properties
    }

    /**
     * Extract plot selection, layout and visit data from survey and copy it to the observations
     * @param surveyData
     * @param surveyDataAndObservations
     * @param config
     */
    static void addPlotDataToObservations(Map surveyDataAndObservations, ParatooProtocolConfig config) {
        if (surveyDataAndObservations && config.usesPlotLayout) {
            Map plotSelection = config.getPlotSelection(surveyDataAndObservations)
            Map plotLayout = config.getPlotLayout(surveyDataAndObservations)
            Map plotVisit = config.getPlotVisit(surveyDataAndObservations)

            if (plotSelection) {
                surveyDataAndObservations[PARATOO_DATAMODEL_PLOT_SELECTION] = plotSelection
                surveyDataAndObservations[PARATOO_DATAMODEL_PLOT_LAYOUT] = plotLayout
                surveyDataAndObservations[PARATOO_DATAMODEL_PLOT_VISIT] = plotVisit
            }
        }
    }

    /**
     * Get the protocol config for the given protocol id.
     * @param protocolId
     * @return
     */
    ParatooProtocolConfig getProtocolConfig(String protocolId) {
        String result = settingService.getSetting(PARATOO_PROTOCOL_DATA_MAPPING_KEY)
        Map protocolDataConfig = JSON.parse(result ?: '{}')
        Map config = protocolDataConfig[protocolId]
        new ParatooProtocolConfig(config ?: [:])
    }

    boolean protocolReadCheck(String userId, String projectId, String protocolId) {
        protocolCheck(userId, projectId, protocolId, true)
    }

    boolean protocolWriteCheck(String userId, String projectId, String protocolId) {
        protocolCheck(userId, projectId, protocolId, false)
    }

    private boolean protocolCheck(String userId, String projectId, String protocolId, boolean read) {
        List projects = userProjects(userId)
        ParatooProject project = projects.find { it.id == projectId }
        boolean protocol = project?.protocols?.find { it.externalIds.find { it.externalId == protocolId } }
        int minimumAccess = read ? AccessLevel.projectParticipant.code : AccessLevel.editor.code
        protocol && project.accessLevel.code >= minimumAccess
    }

    Map findDataSet(String userId, String orgMintedUUID) {
        List projects = findUserProjects(userId)

        Map dataSet = null
        ParatooProject project = projects?.find {
            dataSet = it.dataSets?.find { it.dataSetId == orgMintedUUID }
            dataSet
        }
        [dataSet: dataSet, project: project]
    }

    /**
     * Create an activity from survey data.
     * @param activityForm
     * @param surveyObservations
     * @param collection
     * @param siteId
     * @return
     */
    private String createActivityFromSurveyData(ActivityForm activityForm, Map surveyObservations, ParatooCollectionId collection, String siteId, String userId) {
        Map activityProps = [
                type             : activityForm.name,
                formVersion      : activityForm.formVersion,
                description      : "Activity submitted by monitor",
                projectId        : collection.projectId,
                publicationStatus: "published",
                siteId           : siteId,
                userId           : userId,
                outputs          : [[
                                            data: surveyObservations,
                                            name: activityForm.name
                                    ]]
        ]

        Map result = activityService.create(activityProps)
        result.activityId
    }

    /**
     * Converts species, feature, document, image and list to appropriate formats.
     * @param dataModel
     * @param output
     * @param path
     * @return
     */
    def recursivelyTransformData(List dataModel, Map output, String formName = "", int featureId = 1, ParatooProtocolConfig config = null) {
        dataModel?.each { Map model ->
            switch (model.dataType) {
                case "list":
                    String updatedPath = model.name
                    def rows =[]
                    try {
                        rows = getProperty(output, updatedPath, true, false)
                    }
                    catch (Exception e) {
                        log.info("Error getting list for ${model.name}: ${e.message}")
                    }

                    if (rows instanceof Map) {
                        output[updatedPath] = rows = [rows]
                    }

                    rows?.each { row ->
                        if (row != null) {
                            recursivelyTransformData(model.columns, row, formName, featureId, config)
                        }
                    }
                    break
                case "species":
                    String speciesName
                    try {
                        if(model.containsKey(PARATOO_LUT_REF)) {
                            speciesName = getProperty(output, model.name)?.label?.first()
                        } else {
                            speciesName = getProperty(output, model.name)?.first()
                        }

                        output[model.name] = transformSpeciesName(speciesName)
                    } catch (Exception e) {
                        log.info("Error getting species name for ${model.name}: ${e.message}")
                    }
                    break
                case "feature":
                    // used by protocols like bird survey where a point represents a sight a bird has been observed in a
                    // bird survey plot
                    def location = output[model.name]
                    if (location instanceof Map) {
                        output[model.name] = [
                                type      : 'Feature',
                                geometry  : [
                                        type       : 'Point',
                                        coordinates: [location.lng, location.lat]
                                ],
                                properties: [
                                        name      : "Point ${formName}-${featureId}",
                                        externalId: location.id,
                                        id: "${formName}-${featureId}"
                                ]
                        ]
                    }
                    else if (location instanceof List) {
                        String name
                        switch (config?.geometryType) {
                            case "LineString":
                                name = "LineString ${formName}-${featureId}"
                                output[model.name] = ParatooProtocolConfig.createLineStringFeatureFromGeoJSON (location, name, null, name)
                                break
                            default:
                                name = "Polygon ${formName}-${featureId}"
                                output[model.name] = ParatooProtocolConfig.createFeatureFromGeoJSON (location, name, null, name)
                                break
                        }
                    }

                    featureId ++
                    break
                case "image":
                case "document":
                    // backup a copy of multimedia to another attribute and remove it from existing attribute since it interferes with existing logic
                    String backupAttributeName = "${model.name}_backup"
                    output[backupAttributeName] = output[model.name]
                    output.remove(model.name)
                    break
            }
        }

        output
    }

    private String createSiteFromSurveyData(Map observation, ParatooCollection collection, ParatooCollectionId surveyId, Project project, ParatooProtocolConfig config, ActivityForm form) {
        String siteId = null
        // Create a site representing the location of the collection
        Map geoJson = config.getGeoJson(observation, form)
        if (geoJson) {
            Map siteProps = siteService.propertiesFromGeoJson(geoJson, 'upload')
            List features = geoJson?.features ?: []
            geoJson.remove('features')
            siteProps.features = features
            siteProps.type = Site.TYPE_SURVEY_AREA
            siteProps.publicationStatus = PublicationStatus.PUBLISHED
            siteProps.projects = [project.projectId]
            String externalId = geoJson.properties?.externalId
            if (externalId) {
                siteProps.externalIds = [new ExternalId(idType: ExternalId.IdType.MONITOR_PLOT_GUID, externalId: externalId)]
            }
            Site site
            // create new site for every non-plot submission
            if (config.usesPlotLayout) {
                site = Site.findByExternalId(ExternalId.IdType.MONITOR_PLOT_GUID, externalId)
                if (site?.features) {
                    siteProps.features?.addAll(site.features)
                }
            }

            Map result
            if (!site) {
                result = siteService.create(siteProps)
            } else {
                result = [siteId: site.siteId]
            }
            if (result.error) {
                // Don't treat this as a fatal error for the purposes of responding to the paratoo request
                log.error("Error creating a site for survey " + collection.orgMintedUUID + ", project " + project.projectId + ": " + result.error)
            }
            siteId = result.siteId
        }
        siteId
    }

    private Map syncParatooProtocols(List<Map> protocols) {
        Map result = [errors: [], messages: []]
        List guids = []
        protocols.each { Map protocol ->
            String message
            String id = protocol.id
            String guid = protocol.attributes.identifier
            guids << guid
            String name = protocol.attributes.name
            if (guid) {
                ParatooProtocolConfig protocolConfig = getProtocolConfig(guid)
                ActivityForm form = ActivityForm.findByExternalId(guid)
                if (!form) {
                    form = new ActivityForm()
                    form.externalIds = []
                    form.externalIds << new ExternalId(idType: ExternalId.IdType.MONITOR_PROTOCOL_INTERNAL_ID, externalId: id)
                    form.externalIds << new ExternalId(idType: ExternalId.IdType.MONITOR_PROTOCOL_GUID, externalId: guid)

                    message = "Creating form with id: " + id + ", name: " + name
                    result.messages << message
                    log.info message
                } else {
                    ExternalId paratooInternalId = form.externalIds.find { it.idType == ExternalId.IdType.MONITOR_PROTOCOL_INTERNAL_ID }

                    // Paratoo internal protocol ids are not stable so if we match the guid, we may need to update
                    // the id as that is used in other API methods.
                    if (paratooInternalId) {
                        message = "Updating form with id: " + paratooInternalId.externalId + ", guid: " + guid + ", name: " + name + ", new id: " + id
                        paratooInternalId.externalId = id
                        result.messages << message
                        log.info message
                    } else {
                        String error = "Error: Missing internal id for form with id: " + id + ", name: " + name
                        result.errors << error
                        log.error error
                    }

                }

                try {
                    mapProtocolToActivityForm(protocol, form, protocolConfig)
                    form.save()

                    if (form.hasErrors()) {
                        result.errors << form.errors
                        log.warn "Error saving form with id: " + id + ", name: " + name
                    }
                }
                catch (NotFoundException e) {
                    String error = "Error: No protocol definition found in swagger documentation for protocol: " + name
                    result.errors << error
                    log.error error
                }
                catch (Exception e) {
                    String error = "Error: Unable to save form for protocol: " + name
                    result.errors << error
                    log.error error
                }

            } else {
                String error = "Error: No valid guid found for protocol: " + name
                result.errors << error
                log.error error
            }
        }

        List allProtocolForms = ActivityForm.findAll {
            externalIds {
                idType == ExternalId.IdType.MONITOR_PROTOCOL_GUID
            }
            status != Status.DELETED
        }

        List deletions = allProtocolForms.findAll { it.externalIds.find { it.idType == ExternalId.IdType.MONITOR_PROTOCOL_GUID && !(it.externalId in guids) } }
        deletions.each { ActivityForm activityForm ->
            result.messages << "Form ${activityForm.name} with guid: ${activityForm.externalIds.find { it.idType == ExternalId.IdType.MONITOR_PROTOCOL_GUID }.externalId} has been deleted"
        }

        log.debug("Completed syncing paratoo protocols")
        result

    }

    /** This is a backup method in case the protocols aren't available online */
    Map syncProtocolsFromSettings() {
        List protocols = JSON.parse(settingService.getSetting(PARATOO_PROTOCOLS_KEY))
        syncParatooProtocols(protocols)
    }

    private String getParatooBaseUrl() {
        grailsApplication.config.getProperty('paratoo.core.baseUrl')
    }

    private String getDocumentationEndpoint() {
        grailsApplication.config.getProperty('paratoo.core.documentationUrl')
    }

    List getProtocolsFromParatoo() {
        (List) cacheService.get("paratoo-protocols", { ->
            String url = paratooBaseUrl + PARATOO_PROTOCOL_PATH
            Map authHeader = getAuthHeader()
            webService.getJson(url, null, authHeader, false)?.data
        })
    }

    Map getAuthHeader() {
        String accessToken = tokenService.getAuthToken(true)
        if (!accessToken?.startsWith('Bearer')) {
            accessToken = 'Bearer ' + accessToken
        }

        if (!accessToken) {
            throw new RuntimeException("Unable to get access token")
        }

        [(MONITOR_AUTH_HEADER): accessToken]
    }

    Map syncProtocolsFromParatoo() {
        List protocols = getProtocolsFromParatoo()
        syncParatooProtocols(protocols)
    }

    private void mapProtocolToActivityForm(Map protocol, ActivityForm form, ParatooProtocolConfig config) {
        form.name = protocol.attributes.name
        form.formVersion = protocol.attributes.version
        form.type = PARATOO_PROTOCOL_FORM_TYPE
        form.category = protocol.attributes.module
        form.external = true
        form.publicationStatus = PublicationStatus.PUBLISHED
        form.description = protocol.attributes.description
        form.tags = config.tags
        form.externalIds
        form.sections = [getFormSectionForProtocol(protocol, config)]
    }

    ParatooProject mapProject(Project project, AccessLevel accessLevel, List<Site> sites) {
        Site projectArea = sites.find { it.type == Site.TYPE_PROJECT_AREA }
        Map projectAreaGeoJson = null
        if (projectArea) {
            projectAreaGeoJson = siteService.geometryAsGeoJson(projectArea)
        }

        // Monitor has users selecting a point as an approximate survey location then
        // laying out the plot using GPS when at the site.  We only want to return the approximate planning
        // sites from this call
        List<Site> plotSelections = sites.findAll{it.type == Site.TYPE_SURVEY_AREA && it.extent?.geometry?.type == 'Point'}

        Map attributes = [
                id:project.projectId,
                name:project.name,
                grantID:project.grantId,
                accessLevel: accessLevel,
                project:project,
                projectArea: projectAreaGeoJson,
                projectAreaSite: projectArea,
                plots          : plotSelections]
        new ParatooProject(attributes)

    }

    private static Map mapParatooCollectionId(ParatooCollectionId paratooCollectionId, Project project) {
        Map dataSet = [:]
        dataSet.dataSetId = paratooCollectionId.survey_metadata.orgMintedUUID
        dataSet.protocol = paratooCollectionId.protocolId
        dataSet.grantId = project.grantId
        dataSet.collectionApp = PARATOO_APP_NAME
        dataSet.dateCreated = DateUtil.format(new Date())
        dataSet.lastUpdated = DateUtil.format(new Date())

        dataSet.progress = Activity.PLANNED
        String dataSetName = buildName(
                paratooCollectionId.protocolId,
                DateUtil.formatAsDisplayDateTime(paratooCollectionId.eventTime), project)
        dataSet.name = dataSetName

        dataSet
    }

    private static String buildSurveyQueryString(int start, int limit, String createdAt) {
        "?populate=deep&sort=updatedAt&pagination[start]=$start&pagination[limit]=$limit&filters[createdAt][\$eq]=$createdAt"
    }

    Map retrieveSurveyAndObservations(ParatooCollection collection, Map authHeader = null) {
        String apiEndpoint = PARATOO_DATA_PATH
        Map payload = [
                org_minted_uuid: collection.orgMintedUUID
        ]

        if (!authHeader) {
            authHeader = getAuthHeader()
        }

        String url = paratooBaseUrl + apiEndpoint
        Map response = webService.doPost(url, payload, false, authHeader)
        log.debug((response as JSON).toString())

        response?.resp
    }

    Map addOrUpdatePlotSelections(String userId, ParatooPlotSelectionData plotSelectionData) {

        List projects = userProjects(userId)
        if (!projects) {
            return [error: 'User has no projects eligible for Monitor site data']
        }

        Map siteData = mapPlotSelection(plotSelectionData)
        // The project/s for the site will be specified by a subsequent call to /projects
        siteData.projects = []

        Site site = Site.findByExternalId(ExternalId.IdType.MONITOR_PLOT_GUID, siteData.externalId)
        Map result
        if (site) {
            result = siteService.update(siteData, site.siteId)
        } else {
            result = siteService.create(siteData)
        }

        result
    }

    private static Map mapPlotSelection(ParatooPlotSelectionData plotSelectionData) {
        Map geoJson = ParatooProtocolConfig.plotSelectionToGeoJson(plotSelectionData)
        Map site = SiteService.propertiesFromGeoJson(geoJson, 'point')
        site.projects = []
        // get all projects for the user I suppose - not sure why this isn't in the payload as it's in the UI...
        site.type = Site.TYPE_SURVEY_AREA
        site.externalIds = [new ExternalId(idType: ExternalId.IdType.MONITOR_PLOT_GUID, externalId: geoJson.properties.externalId)]
        site.publicationStatus = PublicationStatus.PUBLISHED
        // Mark the plot as read only as it is managed by the Monitor app

        site
    }

    Map updateProjectSites(ParatooProject project, Map siteData, List<ParatooProject> userProjects) {
        if (siteData.plot_selections) {
            List siteExternalIds = siteData.plot_selections
            siteExternalIds = siteExternalIds.findAll { it } // Remove null / empty ids
            if (siteExternalIds) {
                linkProjectToSites(project, siteExternalIds, userProjects)
            }

        }
        if (siteData.project_area_type && siteData.project_area_coordinates) {
            updateProjectArea(project, siteData.project_area_type, siteData.project_area_coordinates)
        }
    }


    private static Map linkProjectToSites(ParatooProject project, List siteExternalIds, List<ParatooProject> userProjects) {
        List errors = []

        siteExternalIds.each { String siteExternalId ->

            Site site = Site.findByExternalId(ExternalId.IdType.MONITOR_PLOT_GUID, siteExternalId)
            if (site) {
                site.projects = site.projects ?: []
                if (!site.projects.contains(project.id)) {
                    // Validate that the user has permission to link the site to the project by checking
                    // if the user has permission on any other projects this site is linked to.
                    if (site.projects) {
                        if (!userProjects.collect { it.id }.containsAll(site.projects)) {
                            errors << "User does not have permission to link site ${site.externalId} to project ${project.id}"
                            return
                        }
                    }
                    site.projects << project.id
                    site.save()
                    if (site.hasErrors()) {
                        errors << site.errors
                    }
                }
            } else {
                errors << "No site exists with externalId = ${siteExternalId}"
            }
        }
        [success: !errors, error: errors]
    }

    private Map updateProjectArea(ParatooProject project, String type, List coordinates) {
        Map geometry = ParatooProtocolConfig.toGeometry(coordinates)
        Site projectArea = project.projectAreaSite
        if (projectArea) {
            projectArea.extent.geometry.type = geometry.type
            projectArea.extent.geometry.coordinates = geometry.coordinates
            siteService.update(projectArea.extent, projectArea.siteId)
        } else {

            Map site = [
                    name:'Monitor Project Extent',
                    type:Site.TYPE_PROJECT_AREA,
                    extent: [
                            source:'drawn',
                            geometry:geometry
                    ],
                    projects: [project.id]
            ]
            siteService.create(site)
        }
    }

    Map getParatooSwaggerDocumentation() {
        (Map) cacheService.get('paratoo-swagger-documentation', {
            String url = "${getParatooBaseUrl()}${getDocumentationEndpoint()}"
            webService.getJson(url, null, null, false)
        })
    }

    FormSection getFormSectionForProtocol(Map protocol, ParatooProtocolConfig config) {
        Map documentation = getParatooSwaggerDocumentation()
        Map template = buildTemplateForProtocol(protocol, documentation, config)

        new FormSection(template: template, templateName: template.modelName, name: protocol.attributes.name)
    }

    Map buildPathToModel(Map properties) {
        Map relationship = buildChildParentRelationship(properties)
        buildTreeFrom2DRelationship(relationship)
    }

    def getModelStructureFromDefinition(def definition) {
        if (definition instanceof Map)
            definition?.properties?.data?.properties ?: definition?.items?.properties?.data?.properties ?: definition?.items?.properties ?: definition?.properties ?: definition?.items
    }

    def getRequiredModels(Map properties) {
        properties.properties?.data?.required ?: properties.items?.properties?.data?.required ?: properties.items?.required ?: properties.properties?.required ?: properties?.required ?: []
    }

    Map simplifyModelStructure(Map definition) {
        Map simplifiedDefinition = [:]
        if ((definition.type == PARATOO_TYPE_ARRAY) && definition.items) {
            definition << definition.items
        }

        Map properties = getModelStructureFromDefinition(definition)
        List required = getRequiredModels(definition)
        String componentName = definition[PARATOO_COMPONENT]
        if (properties) {
            simplifiedDefinition.type = definition.type ?: "object"
            simplifiedDefinition.properties = properties
        } else {
            simplifiedDefinition << definition
        }

        if (required) {
            simplifiedDefinition.required = required
        }

        if (componentName) {
            simplifiedDefinition[PARATOO_COMPONENT] = componentName
        }

        simplifiedDefinition
    }

    def cleanSwaggerDefinition(def definition) {
        def cleanedDefinition
        if (definition instanceof Map) {
            cleanedDefinition = simplifyModelStructure(definition)
            if (cleanedDefinition.properties) {
                cleanedDefinition.properties?.each { String name, def value ->
                    cleanedDefinition.properties[name] = cleanSwaggerDefinition(value)
                }
            } else {
                cleanedDefinition?.each { String name, def value ->
                    def cleanedValue = value instanceof Map ? simplifyModelStructure(value) : value
                    cleanedDefinition[name] = cleanSwaggerDefinition(cleanedValue)
                }
            }
        } else if (definition instanceof List) {
            cleanedDefinition = []
            definition.each { def value ->
                cleanedDefinition << cleanSwaggerDefinition(value)
            }
        } else {
            try {
                cleanedDefinition = definition?.clone()
            }
            catch (CloneNotSupportedException e) {
                // if not cloneable, then it is a primitive type
                cleanedDefinition = definition
            }
        }

        cleanedDefinition
    }

    Map buildChildParentRelationship(def definition, ArrayDeque<String> currentNodes = null, Map<String, List> relationship = null) {
        currentNodes = currentNodes ?: new ArrayDeque<String>()
        relationship = relationship != null ? relationship : [:].withDefault { [] } as Map<String, List>
        if (definition instanceof Map) {
            // model references are usually has the following representation
            // {
            //                "type": "integer",
            //                "x-model-ref": "bird-survey"
            // }
            if (definition[PARATOO_MODEL_REF] && (definition["type"] == "integer")) {
                String modelName = definition[PARATOO_MODEL_REF]
                String parentNode = currentNodes.first()
                String grandParentNode = currentNodes.size() > 1 ? currentNodes.getAt(1) : null
                if (!PARATOO_IGNORE_X_MODEL_REF_LIST_MINIMUM.contains(modelName)) {
                    // no circular references
                    List nodes = [grandParentNode, parentNode, modelName].unique()
                    if ((nodes.size() == 3) &&
                            !currentNodes.contains(modelName) &&
                            !(relationship.containsKey(grandParentNode) && relationship[grandParentNode].contains(modelName))) {
                        String nodeListToParent = parentNode ? currentNodes.toList().reverse().join(".") : null
                        // make sure there are no duplicate child entries
                        if (nodeListToParent && (!relationship.containsKey(nodeListToParent)
                                || !relationship[nodeListToParent].contains(modelName))) {
                            relationship[nodeListToParent].add(modelName)
                        }
                    }
                }
            } else if (definition.properties && definition.properties[PARATOO_MODEL_REF]) {
                // For representation like
                //   {
                //         "quad" : {
                //         "type" : "array",
                //         "properties" : {
                //              "type" : "integer"
                //              "x-model-ref" : "quadrant"
                //          }
                //      }
                //   }
                buildChildParentRelationship(definition.properties, currentNodes, relationship)
            } else {
                if (definition instanceof Map) {
                    definition?.each { String name, model ->
                        currentNodes.push(name)
                        buildChildParentRelationship(model, currentNodes, relationship)
                        currentNodes.pop()
                    }
                }
            }
        }

        relationship
    }

    Map buildTreeRelationshipOfModels(Map properties) {
        def twoDRelationships = buildParentChildRelationship(properties)
        buildTreeFrom2DRelationship(twoDRelationships)
    }

    def buildParentChildRelationship(def definition, ArrayDeque<String> ancestorNodes = null, Map<String, List> relationships = null) {
        ancestorNodes = ancestorNodes ?: new ArrayDeque<String>()
        relationships = relationships != null ? relationships : [:].withDefault { [] }
        if (definition instanceof Map) {
            // model references are usually has the following representation
            // {
            //                "type": "integer",
            //                "x-model-ref": "bird-survey"
            // }
            if (definition[PARATOO_MODEL_REF] && (definition["type"] == "integer")) {
                String modelName = definition[PARATOO_MODEL_REF]
                String currentNode = ancestorNodes.size() ? ancestorNodes.last() : name
                if (!PARATOO_IGNORE_X_MODEL_REF_LIST.contains(modelName)) {
                    if (!relationships.containsKey(modelName) || !relationships[modelName].contains(currentNode)) {
                        // prevent circular references
                        if (modelName != currentNode
                                && !ancestorNodes.contains(modelName)
                                && (!relationships.containsKey(currentNode) || !relationships[currentNode].contains(modelName))) {
                            relationships[modelName].add(currentNode)
                        }
                    }
                }
            } else if (definition.properties && definition.properties[PARATOO_MODEL_REF]) {
                // For representation like
                //   {
                //         "quad" : {
                //         "type" : "array",
                //         "properties" : {
                //              "type" : "integer"
                //              "x-model-ref" : "quadrant"
                //          }
                //      }
                //  }
                buildParentChildRelationship(definition.properties, ancestorNodes, relationships)
            } else {
                def iteratingObject = definition.properties ?: definition
                if (iteratingObject instanceof Map) {
                    iteratingObject?.each { String modelName, model ->
                        ancestorNodes.push(modelName)
                        buildParentChildRelationship(model, ancestorNodes, relationships)
                        ancestorNodes.pop()
                    }
                }
            }
        }

        relationships
    }

    /**
     * Builds a tree representation from a list of two dimensional relationships.
     * @param twoDRelationships
     * e.g.
     * [
     *  "bird-survey": ["bird-observation"],
     *  "fauna-survey": ["fauna-observation"],
     *  "plot-visit": ["bird-survey", "fauna-survey"]
     * ]
     * @return
     */
    Map buildTreeFrom2DRelationship(Map twoDRelationships) {
        Map<String, Map> treeRepresentation = [:]

        twoDRelationships.each { String name, List children ->
            treeRepresentation[name] = addChildrenToNode(children)
        }

        List nodesToRemove = iterateTreeAndAddChildren(treeRepresentation)

        nodesToRemove?.each {
            treeRepresentation.remove(it)
        }

        treeRepresentation
    }

    List iterateTreeAndAddChildren(Map treeRepresentation, Map root = null, HashSet nodesToRemove = null, depth = 0, ArrayDeque<String> visited = null) {
        nodesToRemove = nodesToRemove ?: new HashSet<String>()
        root = root ?: treeRepresentation
        visited = visited ?: new ArrayDeque<String>()
        treeRepresentation.each { String parent, Map children ->
            visited.push(parent)
            children?.each { String child, Map grandChildren ->
                if (!visited.contains(child)) {
                    visited.push(child)
                    if (root.containsKey(child)) {
                        grandChildren << deepCopy(root[child])
                        nodesToRemove.add(child)
                    }

                    iterateTreeAndAddChildren(children, root, nodesToRemove, depth + 1, visited)
                    visited.pop()
                }
            }
            visited.pop()
        }

        nodesToRemove.toList()
    }

    Map addChildrenToNode(List children) {
        Map childrenNode = [:]
        children?.each { String child ->
            if (!childrenNode.containsKey(child))
                childrenNode.put(child, [:])
        }

        childrenNode
    }

    static def deepCopy(def original) {
        def copy

        if (original instanceof Map) {
            copy = [:]
            original.each { String key, Object value ->
                copy.put(key, deepCopy(value))
            }
        } else if (original instanceof List) {
            copy = original.collect { deepCopy(it) }
        } else {
            copy = original
        }

        copy
    }

    Map buildTemplateForProtocol(Map protocol, Map documentation, ParatooProtocolConfig config) {
        ArrayDeque<String> modelVisitStack = new ArrayDeque<>()
        documentation = deepCopy(documentation)
        Map components = deepCopy(getComponents(documentation))

        Map template = [dataModel: [], viewModel: [], modelName: capitalizeModelName(protocol.attributes.name), record: true, relationships: [ecodata: [:], apiOutput: [:]]]
        Map properties = deepCopy(findProtocolEndpointDefinition(protocol, documentation))
        if (properties == null) {
            throw new NotFoundException("No protocol endpoint found for ${protocol.attributes.endpointPrefix}/bulk")
        }

        resolveReferences(properties, components)
        Map cleanedProperties = cleanSwaggerDefinition(properties)
        cleanedProperties = deepCopy(cleanedProperties)
//        rearrange models not working for protocols multiple relationship between models. Disabling it for now.
//        template.relationships.ecodata = buildTreeRelationshipOfModels(cleanedProperties)
//        template.relationships.apiOutput = buildPathToModel(cleanedProperties)
//        println((template.relationships.apiOutput as JSON).toString(true))
//        println((template.relationships.ecodata as JSON).toString(true))
        resolveModelReferences(cleanedProperties, components)
//        cleanedProperties = rearrangePropertiesAccordingToModelRelationship(cleanedProperties, template.relationships.apiOutput, template.relationships.ecodata)
        cleanedProperties = deepCopy(cleanedProperties)
        log.debug((properties as JSON).toString())

        if (isPlotLayoutNeededByProtocol(protocol)) {
            template.dataModel.addAll(grailsApplication.config.getProperty("paratoo.defaultPlotLayoutDataModels", List))
            template.viewModel.addAll(grailsApplication.config.getProperty("paratoo.defaultPlotLayoutViewModels", List))
        }
        cleanedProperties.each { String name, def definition ->
            if (definition instanceof Map) {
                modelVisitStack.push(name)
                Map result = convertToDataModelAndViewModel(definition, documentation, name, null, modelVisitStack, 0, name, config)
                modelVisitStack.pop()
                if (result) {
                    template.dataModel.addAll(result.dataModel)
                    template.viewModel.addAll(result.viewModel)
                }
            }
        }

        template
    }

    def resolveModelReferences(def model, Map components, ArrayDeque<String> modelVisitStack = null) {
        modelVisitStack = modelVisitStack ?: new ArrayDeque<String>()
        boolean modelNameStacked = false
        if (model instanceof Map) {
            if (model.containsKey(PARATOO_MODEL_REF)) {
                String modelName = model[PARATOO_MODEL_REF]
                String componentName = getSchemaNameFromModelName(modelName)
                Map referencedComponent = components[componentName]

                if (referencedComponent) {
                    if (modelVisitStack.contains(modelName))
                        log.error("Circular dependency - ignoring model resolution ${modelName}")
                    else {
                        modelNameStacked = true
                        modelVisitStack.push(modelName)
                        model << cleanSwaggerDefinition(components[componentName])
                    }
                }
            }

            model.each { String modelName, def value ->
                resolveModelReferences(value, components, modelVisitStack)
            }
        } else if (model instanceof List) {
            model.each { def value ->
                resolveModelReferences(value, components, modelVisitStack)
            }
        }

        if (modelNameStacked) {
            modelVisitStack.pop()
        }

        model
    }

    List findPathFromRelationship(String nameToFind, def relationship, List ancestors = null) {
        ancestors = ancestors ?: []
        List results = []
        relationship?.each { String path, def models ->
            if (path == nameToFind) {
                results << ancestors.join('.')
            } else if (models instanceof Map) {
                ancestors.add(path)
                results.addAll(findPathFromRelationship(nameToFind, models, ancestors))
                ancestors.removeLast()
            }
        }

        results
    }

    /**
     * Check if protocol requires a plot
     * @param protocol
     * @return boolean
     */
    boolean isPlotLayoutNeededByProtocol(Map protocol) {
        List modelNames = protocol.attributes.workflow?.collect { it.modelName }
        modelNames.contains(PARATOO_WORKFLOW_PLOT_LAYOUT)
    }

    /**
     * Find the models associated with a protocol. Endpoint is of the format Protocol's END_POINT_PREFIX/bulk.
     *
     * @param protocol
     * @param documentation
     * @return
     */
    Map findProtocolEndpointDefinition (Map protocol, Map documentation) {
        (Map) documentation.paths.findResult { String pathName, Map path ->
            if (pathName == "${protocol.attributes.endpointPrefix}/bulk") {
                return path.post.requestBody.content['application/json'].schema.properties
                        .data.properties.collections.items.properties
            }
        }
    }

    Map getComponents(Map documentation) {
        documentation.components.schemas
    }

    Map resolveReferences(Map schema, Map components) {
        String componentName
        schema.each { String model, def value ->
            if (!(value instanceof Map))
                return

            if (value['$ref']) {
                componentName = getModelNameFromRef(value['$ref'])
                if (components[componentName])
                    value.putAll(components[componentName])
                else
                    log.debug("No component definition found for ${componentName}")
                value.remove('$ref')
                resolveReferences(value, components)
            } else if (value.items) {
                resolveReferences(value, components)
            } else if (value.properties && value.properties.data && value.properties.data.properties) {
                resolveReferences(value.properties.data, components)
            } else if (value.anyOf) {
                def definition = value.anyOf.find {
                    it['$ref'] !== null
                }


                value.putAll(definition ?: [:])
                value.remove('anyOf')
                resolveReferences(value, components)
            } else {
                resolveReferences(value, components)
            }

        }

        schema
    }

    String getModelNameFromRef(String ref) {
        ref.replace("#/components/schemas/", "")
    }

    Map convertToDataModelAndViewModel(Map component, Map documentation, String name, List required = null, Deque<String> modelVisitStack = null, int depth = 0, String path = "", ParatooProtocolConfig config = null) {
        boolean modelNameStacked = false
        Map model = [dataModel: [], viewModel: []], dataType, viewModel, template
        modelVisitStack = modelVisitStack ?: new ArrayDeque<String>()
        String componentName, modelName = component[PARATOO_MODEL_REF]

        /**
         * Some time component definition can be like
         {
             "type": "array",
             "items": {
                 "type": "integer",
                 "x-paratoo-file-type": ["images"],
                 "x-model-ref": "file"
             }
         }
         */
        if (!modelName && (component.properties?.getAt(PARATOO_MODEL_REF) == PARATOO_FILE_MODEL_NAME)) {
            component = component.properties
            modelName = component[PARATOO_MODEL_REF]
        }

        if (PARATOO_IGNORE_MODEL_LIST.contains(name))
            return
        else if (component[PARATOO_SPECIES_TYPE]) {
            component.type = "species"
        } else if (modelName == PARATOO_FILE_MODEL_NAME) {
            if (component[PARATOO_FILE_TYPE] == [PARATOO_IMAGE_FILE_TYPE]) {
                component.type = "image"
            } else {
                component.type = "document"
            }
        }

        switch (component.type) {
            case "object":
                if (isLocationObject(component)) {
                    // complex object here represents a point with lat,lng attributes
                    dataType = getFeatureDataType(component, documentation, name)
                    viewModel = getFeatureViewModel(dataType, component, documentation, name)
                } else {
                    template = getColumns(component, documentation, modelVisitStack, depth, path, config)
                    dataType = getListDataType(component, documentation, name, template.dataModel, true)
                    viewModel = getListViewModel(dataType, component, documentation, name, template.viewModel)
                }
                break
            case "array":
                template = getColumns(component, documentation, modelVisitStack, depth, path, config)
                dataType = getListDataType(component, documentation, name, template.dataModel)
                viewModel = getListViewModel(dataType, component, documentation, name, template.viewModel)
                break
            case "integer":
                dataType = getIntegerDataType(component, documentation, name)
                viewModel = getIntegerViewModel(dataType, component, documentation, name)
                break
            case "number":
                dataType = getNumberDataType(component, documentation, name)
                viewModel = getNumberViewModel(dataType, component, documentation, name)
                break
            case "string":
                dataType = getStringDataType(component, documentation, name)
                viewModel = getStringViewModel(dataType, component, documentation, name)
                break
            case "boolean":
                dataType = getBooleanDataType(component, documentation, name)
                viewModel = getBooleanViewModel(dataType, component, documentation, name)
                break
            case "document":
                dataType = getDocumentDataType(component, documentation, name)
                viewModel = getDocumentViewModel(dataType, component, documentation, name)
                break
            case "image":
                dataType = getImageDataType(component, documentation, name)
                viewModel = getImageViewModel(dataType, component, documentation, name)
                break
            case "species":
                dataType = getSpeciesDataType(component, documentation, name)
                viewModel = getSpeciesViewModel(dataType, component, documentation, name)
                break
            default:
                log.error("Cannot convert Paratoo component to dataModel - ${component.type}")
                break
        }

        addOverrides(dataType, viewModel, path, config)

        if (dataType) {
            addRequiredFlag(component, dataType, required)
            model.dataModel.add(dataType)
        }

        if (viewModel) {
            model.viewModel.add(viewModel)
        }

        if (modelNameStacked) {
            modelVisitStack.pop()
        }

        model
    }

    Map addOverrides(Map dataModel, Map viewModel, String path, ParatooProtocolConfig config) {
        Map overrides = config.overrides
        if (overrides?.dataModel?.containsKey(path)) {
            Map override = overrides?.dataModel[path]
            if (override) {
                dataModel.putAll(override)
            }
        }

        if (overrides?.viewModel?.containsKey(path)) {
            Map override = overrides?.viewModel[path]
            if (override) {
                viewModel.putAll(override)
            }
        }
    }

    boolean isLocationObject(Map input) {
        ((input[PARATOO_COMPONENT] == PARATOO_LOCATION_COMPONENT) ||
                input[PARATOO_COMPONENT]?.startsWith(PARATOO_LOCATION_COMPONENT_STARTS_WITH)) &&
                !grailsApplication.config.getProperty("paratoo.location.excluded", List)?.contains(input[PARATOO_COMPONENT])
    }

    static Map addRequiredFlag(Map component, Map dataType, List required) {
        if (required?.contains(component.name)) {
            dataType["validate"] = "required"
        }

        dataType
    }

    Map getStringDataType(Map component, Map documentation, String name) {
        Map dataType
        switch (component.format) {
            case "date-time":
                dataType = addUnitsAndDescription([
                        "dataType"    : "date",
                        "name"        : name,
                        "dwcAttribute": "eventDate"
                ], component)
                break
            default:
                dataType = addUnitsAndDescription([
                        "dataType": "text",
                        "name"    : name
                ], component)

                if (component[PARATOO_LUT_REF]) {
                    List items = getLutValues(component[PARATOO_LUT_REF])
                    if (items)
                        dataType << transFormLutValuesToDataModel(items)
                    else
                        dataType.constraints = component.enum
                    dataType[PARATOO_LUT_REF] = component[PARATOO_LUT_REF]
                }
                break
        }

        dataType
    }

    Map transFormLutValuesToDataModel(List items) {
        Map dataModel = [
                constraints     : [
                        "textProperty" : "label",
                        "type"         : "literal",
                        "valueProperty": "value"
                ],
                "displayOptions": [
                        "placeholder": "Select an option",
                        "tags"       : true
                ]
        ]

        dataModel.constraints.literal = items.collect { Map item ->
            [label: item.attributes.label, value: item.attributes.symbol]
        }

        dataModel
    }

    /**
     * Get display names for symbols. Swagger documentation does not have display names but only list of symbols.
     * @param lutRef
     * @return [
     *      [
     *        "id": 1,
     *        "attributes": [
     *        "symbol": "E",
     *        "label": "Estuary",
     *        "description": "",
     *        "uri": "",
     *        "createdAt": "2024-03-05T07:21:46.070Z",
     *        "updatedAt": "2024-03-12T07:30:12.903Z"
     *        ]
     *       ]
     *      ]
     */
    List getLutValues(String lutRef) {
        cacheService.get("paratoo-$lutRef", {
            String url = "${getParatooBaseUrl()}/${getPluralizedName(lutRef)}"
            int start = 0
            int limit = 20
            String query = "?" + buildPaginationQuery(start, limit)
            Map authHeader = getAuthHeader()
            Map response = webService.getJson(url + query, null, authHeader, false)
            List items = response.data ?: []
            if (!response.error) {
                int total = response.meta?.pagination?.total ?: 0
                while (items && start + limit < total) {
                    start += limit
                    query = "?" + buildPaginationQuery(start, limit)
                    response = webService.getJson(url + query, null, authHeader, false)
                    if (!response.error) {
                        items.addAll(response.data)
                    }
                }
            }

            items
        })
    }

    String buildPaginationQuery(int start, int limit) {
        "pagination[start]=$start&pagination[limit]=$limit"
    }

    /**
     * Get pluralized name of an enumeration. This is used to get the correct endpoint to get display name of an enumeration.
     * @param name
     * @return
     */
    String getPluralizedName(String name) {
        Map documentation = getParatooSwaggerDocumentation()
        // strapi uses plural names for endpoints
        List suffixes = ["s", "es", "ies"]
        String winner = name
        suffixes.each { String suffix ->
            String tempName
            if (suffix.equals("ies") && name.endsWith("y")) {
                // Apply special rule for words ending with 'y'
                tempName = name.substring(0, name.length() - 1) + "ies"
            } else {
                tempName = "${name}${suffix}"
            }

            // check if the endpoint exists in swagger documentation to decide if the plural value is correct.
            if (documentation.paths["/${tempName}"] != null)
                winner = tempName
        }

        winner
    }

    /**
     * Get model definitions according to model relationship so that parent will come first and children will come under it.
     * If relationship is [b:[a:[:]] then rearranged output will be
     * [ type: "object",
     *      properties: [
     *          b: [
     *          type: "object",
     *          properties: [
     *              a: [...]
     *        ]
     *     ]
     *   ]
     * ]
     * @param properties
     * @param apiOutput
     * @param relationship
     * @param newOrder
     * @return
     */
    Map rearrangePropertiesAccordingToModelRelationship(properties, apiOutput, relationship, Map newOrder = null) {
        newOrder = newOrder ?: [type: "object", properties: [:]]
        if (relationship instanceof Map) {
            relationship.each { String parent, Map children ->
                List paths = findPathFromRelationship(parent, apiOutput)
                String path = paths.size() ? paths.first() : null
                if (path) {
                    newOrder["type"] = "object"
                    // get model definition for the parent
                    def value = [:]
                    try {
                        value = getProperty(properties, path)?.first() ?: [:]
                        // remove parent from children
                        paths?.each { String propertyPath ->
                            removeProperty(properties, propertyPath)
                        }

                        value = deepCopy(value)

                    }
                    catch (Exception e) {
                        log.info("Error getting property for path: ${path}")
                    }

                    // reorder
                    newOrder.properties = newOrder.properties ?: [:]
                    newOrder.properties[parent] = newOrder.properties[parent] ?: [:]
                    newOrder.properties[parent].putAll(value ?: [:])
                    removeProperty(properties, path)
                } else {
                    // if path is not found, then check if parent is in root model
                    newOrder.properties = newOrder.properties ?: [:]
                    newOrder.properties[parent] = newOrder.properties[parent] ?: [:]
                    properties = properties ?: [:]
                    newOrder.properties[parent].putAll((properties[parent] ?: [:]))
                }

                if (children) {
                    // if children are present, then recurse the process through each children
                    newOrder.properties[parent] = newOrder.properties[parent] ?: [type: "object", properties: [:]]
                    newOrder.properties[parent].properties.putAll(rearrangePropertiesAccordingToModelRelationship(properties, apiOutput, children, newOrder.properties[parent]).properties)
                }
            }
        }

        newOrder
    }

    def findObservationDataFromAPIOutput(String modelToFind, Map apiOutputRelationship, Map data) {
        List paths = findPathFromRelationship(modelToFind, apiOutputRelationship)
        String path = paths.size() ? paths.first() : null
        if (path) {
            path = path.replaceAll(".properties", "")
            // get model definition for the parent
            try {
                def result = getProperty(data, path)
                return [path: path, data: result]
            }
            catch (Exception e) {
                log.info("Error getting property for path: ${path}")
            }
        }
        else {
            return [path: modelToFind, data: [data[modelToFind]]]
        }
    }

    /**
     * Remove a property at a given path.
     * i.e. if path is a.b.c and object is [a: [b: [c: [:]]]] then after removing the property, properties will be [a: [b: [:]]]
     * @param object
     * @param key
     * @param parts
     */
    void removeProperty(def object, String key, List parts = null) {
        parts = parts ?: key.split(/\./)
        String part = parts.remove(0)
        if (parts.size() == 0) {
            if (object instanceof Map)
                object.remove(part)
            else if (object instanceof List) {
                object.each { def item ->
                    item.remove(part)
                }
            }
        } else if (object instanceof Map)
            removeProperty(object[part], key, parts)
        else if (object instanceof List) {
            object.each { def item ->
                removeProperty(item[part], key, parts.clone())
            }
        }
    }

    Map getStringViewModel(Map dataModel, Map component, Map documentation, String name) {
        Map viewModel
        switch (component.format) {
            case "date-time":
                viewModel = addLabel([
                        "type"  : "date",
                        "source": name
                ], component, name)
                break
            default:
                if (dataModel.constraints) {
                    viewModel = addLabel([
                            "type"  : "selectOne",
                            "source": name
                    ], component, name)
                } else {
                    viewModel = addLabel([
                            "type"  : "text",
                            "source": name
                    ], component, name)
                }

                break
        }

        viewModel
    }

    Map getTimeDataType(Map component, Map documentation, String name) {
        addUnitsAndDescription([
                "dataType"    : "time",
                "name"        : name,
                "dwcAttribute": "eventTime"
        ], component)
    }

    Map getTimeViewModel(Map dataModel, Map component, Map documentation, String name) {
        addUnitsAndDescription([
                "dataType"    : "time",
                "name"        : name,
                "dwcAttribute": "eventTime"
        ], component)
    }

    Map getListDataType(Map component, Map documentation, String name, List columns, Boolean isObject = false) {
        addUnitsAndDescription([
                "dataType": "list",
                "name"    : name,
                "isObject": isObject,
                "columns" : columns
        ], component)
    }

    Map getListViewModel(Map dataModel, Map component, Map documentation, String name, List columns) {
        Map viewModel = addLabel([
                type : "section",
                title: getLabel(component, name),
                boxed: true,
                items: [[
                        type  : "repeat",
                        source: dataModel.name,
                        items : [[
                                type   : "row",
                                "class": "output-section",
                                items  : [[
                                        type : "col",
                                        items: columns
                                ]]
                        ]]
                ]]
        ], component, name)

        if (dataModel.isObject && viewModel.items) {
            viewModel.items.first().userAddedRows = false
        }

        viewModel
    }

    Map getSpeciesDataType(Map component, Map documentation, String name) {
        addUnitsAndDescription([
                "dataType"    : "species",
                "name"        : name,
                "dwcAttribute": "scientificName"
        ], component)
    }

    Map getSpeciesViewModel(Map dataModel, Map component, Map documentation, String name) {
        addLabel([
                type  : "speciesSelect",
                source: dataModel.name
        ], component, name)
    }

    Map getImageDataType(Map component, Map documentation, String name) {
        addUnitsAndDescription([
                "dataType": "image",
                "name"    : name
        ], component)
    }

    Map getImageViewModel(Map dataModel, Map component, Map documentation, String name) {
        addLabel([
                type  : "image",
                source: dataModel.name
        ], component, name)
    }

    Map getDocumentDataType(Map component, Map documentation, String name) {
        addUnitsAndDescription([
                "dataType": "boolean",
                "name"    : name
        ], component)
    }

    Map getDocumentViewModel(Map dataModel, Map component, Map documentation, String name) {
        addLabel([
                type  : "document",
                source: dataModel.name
        ], component, name)
    }

    Map getBooleanDataType(Map component, Map documentation, String name) {
        addUnitsAndDescription([
                "dataType": "boolean",
                "name"    : name
        ], component)
    }

    Map getBooleanViewModel(Map dataModel, Map component, Map documentation, String name) {
        addLabel([
                type  : "boolean",
                source: dataModel.name
        ], component, name)
    }

    Map getFeatureDataType(Map component, Map documentation, String name) {
        addUnitsAndDescription([
                "dataType": "feature",
                "name"    : name
        ], component)
    }

    Map getFeatureViewModel(Map dataModel, Map component, Map documentation, String name) {
        addLabel([
                type  : "feature",
                source: dataModel.name
        ], component, name)
    }

    Map getIntegerDataType(Map component, Map documentation, String name) {
        addUnitsAndDescription([
                "dataType"     : "number",
                "name"         : name,
                "decimalPlaces": 0
        ], component)
    }

    Map getIntegerViewModel(Map dataModel, Map component, Map documentation, String name) {
        addLabel([
                type  : "number",
                source: dataModel.name
        ], component, name)
    }

    Map getNumberDataType(Map component, Map documentation, String name) {
        addUnitsAndDescription([
                "dataType"     : "number",
                "name"         : name,
                "decimalPlaces": PARATOO_FLOAT_DECIMAL_PLACES
        ], component)
    }

    Map getNumberViewModel(Map dataModel, Map component, Map documentation, String name) {
        addLabel([
                type  : "number",
                source: dataModel.name
        ], component, name)
    }

    Map addLabel(Map viewModel, Map component, String name) {
        viewModel.preLabel = getLabel(component, name)
        viewModel
    }

    /**
     * Get label from swagger definition or from property name
     * @param component
     * @param name
     * @return
     */
    String getLabel(Map component, String name) {
        String label
        if (component[PARATOO_FIELD_LABEL]) {
            label = component[PARATOO_FIELD_LABEL]
        } else {
            label = getLabelFromPropertyName(name)
        }

        label
    }

    /**
     * Get label from property name.
     * e.g. "bird-survey" will be converted to "Bird Survey"
     * @param name
     * @return
     */
    String getLabelFromPropertyName(String name) {
        String out = ''
        out = name.replaceAll(/(-|_)/, '-')
                .split('-')
                .findAll { it }
                .collect { it.capitalize() }
                .join(' ')

        return out.trim()
    }

    Map addUnitsAndDescription(Map dataType, Map component) {
        if (component[PARATOO_UNIT_FIELD_NAME]) {
            dataType.units = component[PARATOO_UNIT_FIELD_NAME]
        }

        if (component[PARATOO_HINT]) {
            dataType.description = component[PARATOO_HINT]
        }

        dataType
    }

    Map getColumns(Map component, Map documentation, Deque<String> modelVisitStack, int depth = 0, String path, ParatooProtocolConfig config = null) {
        Map template = [dataModel: [], viewModel: []]
        Map properties = getModelStructureFromDefinition(component)
        List required = getRequiredModels(component)
        properties?.each { String name, def model ->
            if (!model)
                return

            if (PARATOO_IGNORE_MODEL_LIST.contains(name))
                return

            if(model instanceof Map) {
                Map sections = convertToDataModelAndViewModel(model, documentation, name, required, modelVisitStack, depth, "${path}.${name}", config)
                if (sections?.dataModel)
                    template.dataModel.addAll(sections.dataModel)

                if (sections?.viewModel)
                    template.viewModel.addAll(sections.viewModel)
            }
        }

        template
    }

    /**
     * Converts a model name to a name used in swagger documentation.
     * e.g. a-model-name will be converted to AModelNameRequest
     * @param modelName
     * @return
     */
    String getSchemaNameFromModelName(String modelName) {
        capitalizeModelName(modelName) + PARATOO_SCHEMA_POSTFIX
    }

    /**
     * Capitalize a model name.
     * e.g. a-model-name will be converted to AModelName
     * @param modelName
     * @return
     */
    String capitalizeModelName(String modelName) {
        modelName?.toLowerCase()?.replaceAll("[^a-zA-Z0-9]+", ' ')?.tokenize(' ')?.collect { it.capitalize() }?.join()
    }

    def getProperty(def surveyData, String path, boolean useAccessor = false, boolean isDeepCopy = true) {
        if (!path || surveyData == null) {
            return null
        }

        def result
        if (useAccessor) {
            result = new PropertyAccessor(path).get(surveyData)
        }
        else {
            List parts = path.split(/\./)
            result = ElasticSearchService.getDataFromPath(surveyData, parts)
        }

        return isDeepCopy ? deepCopy(result) : result
    }

    /**
     * Transforms a species name to species object used by ecodata.
     * e.g. Acacia glauca [Species] (scientific: Acacia glauca Willd.)
     * [name: "Acacia glauca Willd.", scientificName: "Acacia glauca Willd.", guid: "A_GUID"]
     * Guid is necessary to generate species occurrence record. Guid is found by searching the species name with BIE. If not found, then a default value is added.
     * @param name
     * @return
     */
    Map transformSpeciesName(String name) {
        if (!name) {
            return null
        }

        String regex = "([^\\[\\(]*)(?:\\[(.*)\\])?\\s*(?:\\(scientific:\\s*(.*?)\\))?"
        Pattern pattern = Pattern.compile(regex)
        Matcher matcher = pattern.matcher(name)
        Map result = [name: name, scientificName: name, commonName: name, outputSpeciesId: UUID.randomUUID().toString()]

        if (matcher.find()) {
            String commonName = matcher.group(1)?.trim()
            String scientificName = matcher.group(3)?.trim()
            result.commonName = commonName ?: result.commonName
            result.taxonRank = matcher.group(2)?.trim()
            result.scientificName = scientificName ?: commonName ?: result.scientificName
            result.name = scientificName ?: commonName ?: result.name
        }

        metadataService.autoPopulateSpeciesData(result)
        // try again with common name
        if ((result.guid == null) && result.commonName) {
            def speciesObject = [scientificName: result.commonName]
            metadataService.autoPopulateSpeciesData(speciesObject)
            result.guid = speciesObject.guid
            result.scientificName = result.scientificName ?: speciesObject.scientificName
        }

        // record is only created if guid is present
        result.guid = result.guid ?: "A_GUID"
        result
    }
}

