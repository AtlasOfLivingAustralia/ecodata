package au.org.ala.ecodata

import asset.pipeline.AssetHelper
import com.spatial4j.core.context.SpatialContext
import com.spatial4j.core.io.GeohashUtils
import com.spatial4j.core.shape.Rectangle
import grails.converters.JSON
import groovy.json.JsonSlurper
import grails.web.http.HttpHeaders
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.common.Strings
import org.elasticsearch.common.io.stream.OutputStreamStreamOutput
import org.elasticsearch.common.xcontent.XContentHelper
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoGrid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.support.ResourcePatternResolver

import javax.annotation.PostConstruct

import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX
import static au.org.ala.ecodata.ElasticIndex.PROJECT_ACTIVITY_INDEX
import static javax.servlet.http.HttpServletResponse.SC_OK

class MapService {
    def grailsApplication
    def webService
    ElasticSearchService elasticSearchService
    CacheService cacheService

    boolean enabled = false
    public static final String LAYER_PREFIX = "act"
    public static final String GENERAL_LAYER = '_general'
    public static final String INFO_LAYER = '_info'
    public static final String INDICES_LAYER = '_indices'
    public static final String INFO_LAYER_DEFAULT = 'default'
    public static final String TIMESERIES_LAYER = '_time'
    public static final String PROJECT_ACTIVITY_TYPE = 'pa'
    public static final String PROJECT_TYPE = 'project'
    List datastores = [PROJECT_ACTIVITY_INDEX, HOMEPAGE_INDEX]

    @Autowired
    ResourcePatternResolver resourceResolver

    @PostConstruct
    def init() {
        enabled = grailsApplication?.config?.getProperty('geoServer.enabled', Boolean)

        if (enabled) {
            log.info("GeoServer integration enabled.")
        } else {
            log.info("GeoServer integration disabled.")
        }
    }

    def createWorkspace() {
        if (enabled) {
            String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces"
            Map headers = getHeaders()
            Map data = [
                    workspace: grailsApplication.config.getProperty('geoServer.workspace')
            ]
            String body = bindDataToXMLTemplate("classpath:data/templates/workspace.template", data)
            webService.doPost(url, body, false, headers)
        }
    }

    def deleteWorkspace() {
        if (enabled) {
            String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces/${grailsApplication.config.getProperty('geoServer.workspace')}?recurse=true&purge=true"
            Map headers = getHeaders()
            webService.doDelete(url, headers)
        }
    }

    def createDatastores() {
        if (enabled) {
            String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces/${grailsApplication.config.getProperty('geoServer.workspace')}/datastores"
            datastores?.each { name ->
                Map data = [
                        datastoreName: name,
                        indexName: name,
                        elasticHome: grailsApplication.config.getProperty('geoServer.elasticHome'),
                        elasticPort: grailsApplication.config.getProperty('geoServer.elasticPort'),
                        clusterName: grailsApplication.config.getProperty('geoServer.clusterName')
                ]

                String body = bindDataToXMLTemplate("classpath:data/templates/datastore.template", data)
                Map headers = getHeaders()
                webService.doPost(url, body, false, headers)
            }
        }
    }

    def deleteDatastores() {
        if (enabled) {
            datastores?.each { datastore ->
                String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/namespaces/${grailsApplication.config.getProperty('geoServer.workspace')}/datastores/${datastore}?recurse=true&purge=true"
                Map headers = getHeaders()
                webService.doDelete(url, headers)
            }
        }
    }

    boolean buildGeoServerDependencies () {
        if (enabled) {
            log.info("Starting to delete GeoServer artifacts.")
            deleteLayers()
            deleteDatastores()
            deleteWorkspace()
            log.info("GeoServer artifacts deleted.")

            log.info("Starting to create GeoServer artifacts.")
            createWorkspace()
            createDatastores()
            createPredefinedStyles()
            log.info("Finished creating GeoServer artifacts.")
        }
    }

    def wms(params, response) {
        if(enabled) {
            def index, geoSearch
            switch (params.dataType) {
                case PROJECT_TYPE:
                    if (params.geoSearchJSON) {
                        geoSearch = new JsonSlurper().parseText(params.geoSearchJSON)
                    }

                    params.includes = "projectArea.geoIndex,projectArea.geometryType"
                    index = HOMEPAGE_INDEX
                    break
                default:
                    params.geoSearchField = "sites.geoIndex"
                    elasticSearchService.buildProjectActivityQuery(params)
                    index = PROJECT_ACTIVITY_INDEX
                    break
            }

            // double quotes need to be escaped to get valid JSON
            if (params.query) {
                params.query = params.query.replaceAll('"', '\\\\"')
            }

            SearchRequest request = elasticSearchService.buildSearchRequest(params.query, params, index, geoSearch)
            QueryBuilder query = request.source().query()
            String requestJSON = Strings.toString(query, true, true)
            requestJSON = requestJSON.replaceAll(',', '\\\\,')
            requestJSON = URLEncoder.encode(requestJSON, "utf-8")
            Map wmsParams = whiteListWMSParams(params)
            wmsParams.VIEWPARAMS = "q:${requestJSON}"
            List requestParams = []
            wmsParams.each { key, value ->
                requestParams << key + "=" + value
            }

            String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/${grailsApplication.config.getProperty('geoServer.workspace')}/wms?${requestParams.join('&')}"
            int readTimeout = grailsApplication.config.getProperty('geoServer.readTimeout', Integer)
            webService.proxyGetRequest(response, url, false, false,  [HttpHeaders.EXPIRES, HttpHeaders.CACHE_CONTROL, HttpHeaders.CONTENT_DISPOSITION, HttpHeaders.CONTENT_TYPE], readTimeout)
        }
    }

    def getDataStoreForDataType (String dataType) {
        String dataStore
        switch (dataType) {
            case PROJECT_TYPE:
                dataStore = HOMEPAGE_INDEX
                break
            default:
                dataStore = PROJECT_ACTIVITY_INDEX
                break
        }

        dataStore
    }

    def createStyleForFacet(String field, List terms, String style, String type = 'terms', String dataType = 'pa') {
        if (enabled) {
            String sld
            String dataStore = getDataStoreForDataType(dataType)
            switch (type) {
                case 'terms':
                    sld = buildStyleForTermFacet(field, terms, style, dataStore)
                    break;
                case 'range':
                    sld = buildStyleForRangeFacet(field, terms, style, dataStore)
                    break;
            }

            String name = AssetHelper.getByteDigest(sld.bytes)
            saveStyle(sld, name)
            name
        }
    }

    /**
     * Creates predefined styles stored in 'grails-app/conf/data/styles' directory on GeoServer.
     */
    def createPredefinedStyles () {
        if (enabled) {
            def response = deleteWorkspaceStyles()
            if (response?.status == SC_OK) {
                def result = [ success: true, status: SC_OK,  styles: [] ]
                def styleFiles = getPredefinedStyles()
                styleFiles?.each { String name , String style ->
                    deleteStyle(name)
                    def resp = saveStyle(style, name)
                    if ( resp.error ) {
                        result.success = false
                        result.styles.add(name)
                    }
                }

                result
            }
        }
    }

    def getPredefinedStyles() {
        def files = resourceResolver.getResources("classpath:data/styles/*.sld")
        Map styles = [:]
        files?.each { file ->
            def name = getStyleNameFromFileName(file)
            if (name) {
                styles[name] = file.getURL().getText()
            }
        }

        styles
    }

    def getStyleNameFromFileName (Resource file) {
        String name = file?.getFilename()
        List parts = name?.split('.sld')
        parts?.size() > 0 ? parts[0] : null
    }

    def getStylesInWorkspace () {
        if (enabled) {
            String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces/${grailsApplication.config.getProperty('geoServer.workspace')}/styles.json"
            Map headers = getHeaders()
            headers.remove('Content-Type')
            Map result = webService.getJson(url, null, headers)
            if (result.error) {
                result = [ styles: []]
            }

            result
        }
    }

    def deleteWorkspaceStyles() {
        if (enabled) {
            Map result = [status: null, message: null]
            List failedDeletes = []
            Map styles = getStylesInWorkspace()
            if (styles?.styles) {
                styles?.styles?.style?.each { Map style ->
                    String url = "${style.href}?purge=true"
                    Map headers = getHeaders()
                    headers.remove("Content-Type")
                    Integer status = webService.doDelete(url, headers)
                    if (status != 200) {
                        failedDeletes.add(style.name)
                    }
                }
            }

            if (failedDeletes.size() >= 0) {
                result.message = "Successfully deleted styles."
            } else {
                result.message = "${failedDeletes.size() } styles failed to delete."
            }

            result.status = SC_OK
            result
        }
    }

    def saveStyle(String style, String name) {
        if (enabled) {
            String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces/${grailsApplication.config.getProperty('geoServer.workspace')}/styles.sld?name=${name}&raw=true"
            Map headers = getHeaders()
            headers['Content-Type'] = 'application/vnd.ogc.sld+xml'
            webService.doPost(url, style, false, headers)
        }
    }

    def createLayer(String name, String dataStore, List indices, Boolean enableTimeDimension = false, String timeSeriesIndex = '') {
        if (enabled) {
            String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces/${grailsApplication.config.getProperty('geoServer.workspace')}/datastores/${dataStore}/featuretypes"
            Map layerSettings = getLayerSettings(name, indices, dataStore, enableTimeDimension, timeSeriesIndex)
            String content = getLayerXMLDefinition(layerSettings)
            log.debug("Creating layer (${layerSettings.name}) on GeoServer with content: ${content}")
            Map response = webService.doPost(url, content, false, getHeaders())
            if(!response.error) {
                response.success = 'Created'
                response.layerName = layerSettings.name
                response.location = response.headers.location
            }

            response
        }
    }

    Map getLayerSettings(String name, List indices, String dataStore, Boolean enableTimeDimension = false, String timeSeriesIndex = '') {
        // deep copy configuration
        String dataStoreConfigProperty = 'geoServer.layerConfiguration.'+dataStore
        Map dataStoreConfig = grailsApplication.config.getProperty(dataStoreConfigProperty, Map)
        String configSerialized = (dataStoreConfig as JSON).toString()
        Map config = JSON.parse(configSerialized)
        Map fieldsMapping = getFieldsMapping(dataStore)
        List attributes = []

        indices?.each { index ->
            Map attributeConfig = getConfigForIndex(index, fieldsMapping, dataStore)
            if (attributeConfig) {
                attributes.add(attributeConfig)
            }
        }

        config.attributes.addAll(attributes)
        config.name = name
        config.nativeName = name
        config.timeEnabled = enableTimeDimension
        if (timeSeriesIndex) {
            config.timeAttribute = timeSeriesIndex
        }
        config
    }

    /**
     * Get configuration for index.
     * @param index
     * @return
     */
    Map getConfigForIndex(String index, Map fieldsMapping, String dataStore) {
        String dataType = getDataTypeForIndex(index, dataStore)
        if (dataType instanceof String) {
            String className = getClassForElasticSearchDataType(dataType)
            String shortName = index
            boolean isUseShortName = fieldsMapping?.get(index) != null
            if (isUseShortName) {
                index = fieldsMapping?.get(index).path
            }

            Map config = [
                    "name": index,
                    "shortName": shortName,
                    "useShortName": isUseShortName,
                    "type": className,
                    "use": true,
                    "defaultGeometry": false,
                    "stored": false,
                    "nested": false,
                    "binding": className,
                    "nillable": true,
                    "minOccurs": 0,
                    "maxOccurs": 1
            ]

            if (className == Date.class.name) {
                config.put("dateFormat", "dateOptionalTime")
            }

            config
        }
    }

    String getDataTypeForIndex (String index, String dataStore) {
        Map mapping = getMapping(dataStore)
        mapping = mapping["properties"]
        getDataTypeFromMapping(index, mapping, getFieldsMapping(dataStore))
    }

    /**
     * Get elasticsearch mapping for a specific dataStore (index) e.g. {@link ElasticIndex#HOMEPAGE_INDEX}
     * @return
     */
    Map getMapping(String dataStore) {
        cacheService.get("elastic-search-mapping-with-dynamic-indices-${dataStore}", {
            String index = dataStore ?: grailsApplication.config.getProperty('geoServer.defaultIndexName')
            GetMappingsResponse response = elasticSearchService.client.admin().indices().getMappings(new GetMappingsRequest().indices(index)).get()
            response.getMappings().get(index).get(elasticSearchService.DEFAULT_TYPE).getSourceAsMap()
        }) as Map
    }

    Map getFieldsMapping(String dataStore) {
        cacheService.get("elastic-search-fields-mapping-${dataStore}", {
            Map mapping = getMapping(dataStore)
            mapping = mapping["properties"]
            getFieldsMapping(mapping)
        }) as Map
    }

    String getDataTypeFromMapping (String index, Map mapping, Map fieldsMapping = null) {
        List path = index?.split('\\.')?.toList()
        Map currentMapping = mapping

        path?.each { element ->
            currentMapping = currentMapping?.get(element)
            if (currentMapping?.properties) {
                currentMapping = currentMapping.properties
            }
        }

        // see if field mapping has definition
        if(!currentMapping) {
            currentMapping = fieldsMapping[path.last()]
        }

        currentMapping?.type
    }

    Map getFieldsMapping(Map mapping, Map fieldsMapping = null, List path = []) {
        fieldsMapping = fieldsMapping ?: [:]
        mapping?.each { field, definition ->
            List fieldPath = path.clone()
            fieldPath.add(field)
            definition.fields?.each {
                fieldsMapping[it.key] = it.value
                fieldsMapping[it.key].path = "${fieldPath.join('.')}"
            }

            if(definition.properties) {
                getFieldsMapping(definition.properties, fieldsMapping, fieldPath)
            }
        }

        fieldsMapping
    }

    /**
     * Maps ElasticSearch data type to Java Class. Class is used by
     * @param type
     * @return
     */
    String getClassForElasticSearchDataType (String type) {
        String className
        switch (type) {
            case "string":
                className = String.class.name
                break;
            case "integer":
                className = Integer.class.name
                break;
            case "long":
                className = Long.class.name
                break;
            case "float":
                className = Float.class.name
                break;
            case "double":
                className = Double.class.name
                break;
            case "boolean":
                className = Boolean.class.name
                break;
            case "date":
                className = Date.class.name
                break;
            case "geo_shape":
                className = org.locationtech.jts.geom.Geometry.class.name
                break
            case "geo_point":
                className = org.locationtech.jts.geom.Point.class.name
                break
        }

        className
    }

    /**
     * Generates a layer name for the provided list of indices. It will generate a unique name
     * for provided indices regardless of their order in the list.
     * @param indices
     * @return
     */
    String getLayerName (List indices, String prefix = LAYER_PREFIX) {
        indices?.sort()
        String layerName = prefix + indices?.join('')
        AssetHelper.getByteDigest(layerName.bytes)
    }

    /**
     * Remove layers already on default list.
     * @param indices
     * @return
     */
    List sanitizeIndices (List indices, String dataStore) {
        String dataStoreConfigProperty = 'geoServer.layerConfiguration.'+dataStore+'.attributes'
        List defaultIndices = grailsApplication.config.getProperty(dataStoreConfigProperty, List).collect {it.name}
        indices?.findAll { !defaultIndices.contains(it) && (getDataTypeForIndex(it, dataStore) != null) }
    }

    /**
     * Check a layer exists on GeoServer.
     * @param layerName
     * @return
     */
    boolean checkIfLayerExists (String layerName, String dataStore) {
        if (enabled) {
            String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces/${grailsApplication.config.getProperty('geoServer.workspace')}/datastores/${dataStore}/featuretypes/${layerName}.json"
            Map headers = getHeaders()
            def response = webService.getJson(url, null ,headers)
            return !response.error
        }

        return false
    }

    String getLayerForIndices(List indices, String dataStore, String prefix = null, String name = null, Boolean enableTimeDimension = false, String timeSeriesIndex = '') {
        indices = sanitizeIndices(indices, dataStore)
        name = name ?: getLayerName(indices, prefix)
        boolean isCreated = checkIfLayerExists(name, dataStore)
        if (!isCreated) {
            Map response = createLayer(name, dataStore, indices, enableTimeDimension, timeSeriesIndex)
            if (!response || response.error) {
                log.warn("Could not create ${name} on GeoServer.")
                return
            }
        }

        name
    }

    String getLayerNameForType(String type, List indices, String dataType) {
        if (type && indices) {
            String layerName, timeSeriesIndex, dataStore
            Map config

            switch (dataType) {
                case PROJECT_TYPE:
                    dataType = PROJECT_TYPE
                    dataStore = HOMEPAGE_INDEX
                    break
                default:
                    dataType = PROJECT_ACTIVITY_TYPE
                    dataStore = PROJECT_ACTIVITY_INDEX
                    break
            }

            switch (type) {
                case GENERAL_LAYER:
                    config = grailsApplication.config.getProperty('geoServer.layerNames.'+GENERAL_LAYER+'.'+dataType, Map)
                    layerName = getLayerForIndices(config.attributes, dataStore,null, config.name)
                    break
                case INFO_LAYER:
                    config = grailsApplication.config.getProperty('geoServer.layerNames.'+INFO_LAYER+'.'+dataType, Map)
                    if (indices?.contains(INFO_LAYER_DEFAULT)) {
                        indices = []
                    }

                    indices.addAll(config.attributes)
                    Set uniqueList = new HashSet<String>()
                    uniqueList.addAll(indices)
                    indices = uniqueList.toList()
                    layerName = getLayerForIndices(indices, dataStore)
                    break
                case INDICES_LAYER:
                    config = grailsApplication.config.getProperty('geoServer.layerNames.'+INDICES_LAYER+'.'+dataType, Map)
                    indices.addAll(config.attributes)
                    layerName = getLayerForIndices(indices, dataStore, INDICES_LAYER)
                    break
                case TIMESERIES_LAYER:
                    config = grailsApplication.config.getProperty('geoServer.layerNames.'+TIMESERIES_LAYER+'.'+dataType, Map)
                    // assumption is the first index is date index
                    timeSeriesIndex = indices.get(0)
                    indices.addAll(config.attributes)
                    Set uniqueList = new HashSet<String>()
                    uniqueList.addAll(indices)
                    indices = uniqueList.toList()
                    layerName = getLayerForIndices(indices, dataStore, TIMESERIES_LAYER, null, true, timeSeriesIndex)
                    break
            }

            layerName
        }
    }

    /**
     * Create an XML representation of the layer. Output is used to create layer on GeoServer.
     * @param layerConfig
     * @return
     */
    String getLayerXMLDefinition(Map layerConfig) {
        def files = resourceResolver.getResources("classpath:data/templates/layer.template")
        def engine = new groovy.text.XmlTemplateEngine()
        engine.setIndentation('')
        String content
        files?.each { Resource file ->
            content = engine.createTemplate(file.getURL()).make(layerConfig).toString()
        }

        content?.replaceAll('\n', '');
    }

    def deleteLayers() {
        if (enabled) {
            String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces/${grailsApplication.config.getProperty('geoServer.workspace')}/featuretypes.json"
            Map headers = getHeaders()
            Map layers = webService.getJson(url, null, headers)
            if (layers?.featureTypes){
                layers.featureTypes.featureType?.each { layer ->
                    String layerURL = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/layers/${layer.name}"
                    webService.doDelete(layerURL, headers)
//                    Feature type needs data store for deletion. Don't know which data store this feature type is
//                    associated with. Therefore, try all data stores.
                    datastores?.each { String store ->
                        String featureTypeURL = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces/${grailsApplication.config.getProperty('geoServer.workspace')}/datastores/${store}/layers/${layer.name}"
                        webService.doDelete(featureTypeURL, headers)
                    }
                }
            }
        }
    }

    def deleteStyle (String styleName) {
        String url = "${grailsApplication.config.getProperty('geoServer.baseURL')}/rest/workspaces/${grailsApplication.config.getProperty('geoServer.workspace')}/styles/${styleName}?purge=true"
        Map headers = getHeaders()
        headers.remove('Content-Type')
        webService.doDelete(url, headers)
    }

    String bindDataToXMLTemplate (String fileClassPath, Map data) {
        def files = resourceResolver.getResources(fileClassPath)
        def engine = new groovy.text.XmlTemplateEngine()
        engine.setIndentation('')
        String content
        files?.each { Resource file ->
            content = engine.createTemplate(file.getURL()).make(data).toString()
        }

        content?.replaceAll('\n', '');
    }

    def buildStyleForTermFacet(String field, List terms, String style, String dataStore) {
        int cIndex = 0
        List colour = grailsApplication.config.getProperty('geoserver.facetTermColour', List)
        terms?.eachWithIndex { Map term, index ->
            // reuse last colour in array if number of terms exceed number of colours
            cIndex = index > (colour.size() - 1 ) ? (colour.size() - 1 ) : index
            // add colour value only if not provided
            term.colour = term.colour ?: colour[cIndex]
            // escape entities like &, <, > etc. in XML
            term.term = groovy.xml.XmlUtil.escapeXml(term.term)
            term.displayName = groovy.xml.XmlUtil.escapeXml(term.displayName)
        }

        cIndex++
        if (cIndex >= colour.size()) {
            cIndex = colour.size() - 1
        }

        Map dataBinding = [
                namespace: grailsApplication.config.getProperty('geoServer.workspace'),
                field: field,
                terms: terms,
                style: style,
                geometryTypeField: grailsApplication.config.getProperty('geoServer.'+dataStore+'.geometryTypeField'),
                otherColour: colour[cIndex]
        ]

        bindDataToXMLTemplate("classpath:data/templates/colour_by_term.template", dataBinding)
    }

    def buildStyleForRangeFacet(String field, List terms, String style, String dataStore) {
        int cIndex = 0
        List colour = grailsApplication.config.getProperty('geoserver.facetRangeColour', List)

        terms?.eachWithIndex { Map term, index ->
            // reuse last colour in array if number of terms exceed number of colours
            cIndex = index > (colour.size() - 1 ) ? (colour.size() - 1 ) : index
            // add colour value only if not provided
            term.colour = term.colour ?: colour[cIndex]
            term.displayName = term.displayName?.replaceAll('<', '&lt;')?.replaceAll('>', '&gt;')
        }

        Map dataBinding = [
                namespace: grailsApplication.config.getProperty('geoServer.workspace'),
                field: field,
                terms: terms,
                style: style,
                geometryTypeField: grailsApplication.config.getProperty('geoServer.'+dataStore+'.geometryTypeField')
        ]

        bindDataToXMLTemplate("classpath:data/templates/colour_by_range.template", dataBinding)
    }

    /**
     * Filter out parameters in request like VIEWPARAMS. VIEWPARAMS is used to send query to ES.
     * @param params
     * @return
     */
    def whiteListWMSParams(params) {
        List whitelist = [
                'SERVICE', 'VERSION', 'REQUEST', 'FORMAT', 'TRANSPARENT', 'STYLES', 'LAYERS', 'SRS', 'CRS', 'WIDTH', 'LAYER',
                'HEIGHT', 'BBOX', 'QUERY_LAYERS', 'INFO_FORMAT', 'FEATURE_COUNT', 'X', 'Y', 'I', 'J', 'EXCEPTIONS', 'TIME',
                'STYLE', 'LEGEND_OPTIONS', 'RULE', 'SLD_BODY', 'SLD', 'SCALE', 'FEATURETYPE', 'MAXFEATURES', 'PROPERTYNAME',
                'CQL_FILTER', 'TIME', 'ENV', 'TILED'
        ]
        Map legitimateParams = [:]
        params = params ?: [:]

        params.each { key, value ->
            if (whitelist.contains(key.toUpperCase()) && value) {
                legitimateParams[key] = URLEncoder.encode(value, "utf-8")
            }
        }

        legitimateParams
    }

    def getFeatureCollectionFromSearchResult(res, String aggName = "heatmap") {
        Map features = [type: "FeatureCollection", features: []]
        SpatialContext sc = new SpatialContext(true)
        GeoGrid geoHashGridAgg = res.getAggregations().get(aggName)

        for (GeoGrid.Bucket entry : geoHashGridAgg.getBuckets()) {
            Map properties = [:]
            properties.key = entry.getKey()
            properties.count = entry.getDocCount()
            Rectangle rect = GeohashUtils.decodeBoundary(properties.key, sc)
            Map bottomRight = [lat: rect.minY, lon: rect.maxX]
            Map topLeft = [lat: rect.maxY, lon: rect.minX]
            features.features.add([type: "Feature", properties: properties, geometry: getGeoJSONPolygonFromPoints(topLeft, bottomRight)])
        }

        features
    }

    Map getGeoJSONPolygonFromPoints (Map topLeft, Map bottomRight) {
        List first, second, third, fourth, last
        first = last = [topLeft.lon, topLeft.lat]
        second = [topLeft.lon, bottomRight.lat]
        third = [bottomRight.lon, bottomRight.lat]
        fourth = [bottomRight.lon, topLeft.lat]

        [
                type: "Polygon",
                coordinates: [[first, second, third, fourth, last]]
        ]
    }

    Map setHeatmapColour(Map features) {
        int maxCount= 0,
            minCount = Integer.MAX_VALUE,
            numberOfBuckets = grailsApplication.config.getProperty('geoserver.facetRangeColour', List).size()

        features?.features?.each { Map feature ->
            Map properties = feature.properties
            if (properties.count > maxCount) {
                maxCount = properties.count
            }

            if (properties.count < minCount) {
                minCount = properties.count
            }
        }

        int stepSize = 1
        if (stepSize*numberOfBuckets < maxCount) {
            stepSize = Math.ceil((maxCount - minCount) / numberOfBuckets)
        }

        // upper bound is exclusive
        maxCount ++
        List buckets = []
        int minRange, maxRange = minCount - 1
        for ( int i = 0; i < numberOfBuckets; i++) {
            minRange = maxRange
            maxRange = (minCount + (i + 1) * stepSize)

            if (i == (numberOfBuckets - 1)) {
                maxRange = maxCount
            }

            buckets.add([label: "${minRange} - ${maxRange}", colour: grailsApplication.config.getProperty('geoserver.facetRangeColour', List)[numberOfBuckets - i - 1], min: minRange, max: maxRange])
        }

        features?.features?.each { Map feature ->
            Map properties = feature.properties
            int index = (properties.count - minCount) / stepSize
            if (index >= buckets.size()) {
                index = buckets.size() - 1
            }

            Map bucket = buckets[index]
            if(!((properties.count >= bucket.min) && (properties.count < bucket.max))) {
                if ((properties.count >= bucket.max) && ((index + 1) < numberOfBuckets)) {
                    bucket = buckets[index + 1]
                }
                else if ((properties.count < bucket.min) && ((index - 1) >= 0)) {
                    bucket = buckets[index - 1]
                }
            }

            properties.putAll (bucket)
        }

        features
    }

    private Map getHeaders() {
        String encoded = "${grailsApplication.config.getProperty('geoServer.username')}:${grailsApplication.config.getProperty('geoServer.password')}".bytes.encodeBase64().toString()
        [
                "Authorization": "Basic ${encoded}",
                "Content-Type" : "application/xml;charset=utf-8"
        ]
    }
}
