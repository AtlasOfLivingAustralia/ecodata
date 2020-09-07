package au.org.ala.ecodata

import asset.pipeline.AssetHelper
import grails.converters.JSON
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse
import org.elasticsearch.common.xcontent.XContentHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.support.ResourcePatternResolver

import javax.annotation.PostConstruct

import static au.org.ala.ecodata.ElasticIndex.PROJECT_ACTIVITY_INDEX
import static javax.servlet.http.HttpServletResponse.SC_OK

class GeoServerService {
    def grailsApplication
    def webService
    ElasticSearchService elasticSearchService
    HubService hubService
    CacheService cacheService

    boolean enabled = false
    def LAYER_PREFIX = "act",
        GENERAL_LAYER = '_general',
        INFO_LAYER = '_info',
        INDICES_LAYER = '_indices',
        INFO_LAYER_DEFAULT = 'default',
        TIMESERIES_LAYER = '_time'

    @Autowired
    ResourcePatternResolver resourceResolver

    @PostConstruct
    def init() {
        enabled = grailsApplication.config.geoServer.enabled?.toBoolean()

        if (enabled) {
            log.info("GeoServer integration enabled.")
        } else {
            log.info("GeoServer integration disabled.")
        }
    }

    def createWorkspace() {
        if (enabled) {
            String url = "${grailsApplication.config.geoServer.baseURL}/rest/namespaces"
            Map headers = getHeaders()
            String body = "<namespace>\n" +
                    "  <prefix>${grailsApplication.config.geoServer.workspace}</prefix>\n" +
                    "  <uri>${grailsApplication.config.geoServer.workspaceURI}</uri>\n" +
                    "</namespace>"
            webService.doPost(url, body, false, headers)
        }
    }

    def deleteWorkspace() {
        if (enabled) {
            String url = "${grailsApplication.config.geoServer.baseURL}/rest/workspaces/${grailsApplication.config.geoServer.workspace}?recurse=true&purge=true"
            Map headers = getHeaders()
            webService.doDelete(url, headers)
        }
    }

    def createDatastore() {
        if (enabled) {
            String url = "${grailsApplication.config.geoServer.baseURL}/rest/workspaces/${grailsApplication.config.geoServer.workspace}/datastores"
            String datastoreName = grailsApplication.config.geoServer.datastore
            String indexName = grailsApplication.config.geoServer.indexName
            String elasticHome = "localhost"
            String elasticPort = "9300"
            String clusterName = "elasticsearch"
            String body = "<dataStore>\n" +
                    "  <name>${datastoreName}</name>\n" +
                    "  <type>Elasticsearch</type>\n" +
                    "  <enabled>true</enabled>\n" +
                    "  <connectionParameters>\n" +
                    "    <entry key=\"cluster_name\">${clusterName}</entry>\n" +
                    "    <entry key=\"scroll_time\">120</entry>\n" +
                    "    <entry key=\"elasticsearch_port\">${elasticPort}</entry>\n" +
                    "    <entry key=\"store_data\">false</entry>\n" +
                    "    <entry key=\"scroll_enabled\">false</entry>\n" +
                    "    <entry key=\"scroll_size\">20</entry>\n" +
                    "    <entry key=\"index_name\">${indexName}</entry>\n" +
                    "    <entry key=\"elasticsearch_host\">${elasticHome}</entry>\n" +
                    "    <entry key=\"use_local_node\">false</entry>\n" +
                    "  </connectionParameters>\n" +
                    "</dataStore>";

            Map headers = getHeaders()
            webService.doPost(url, body, false, headers)
        }
    }

    def deleteDatastore() {
        if (enabled) {
            String datastore = grailsApplication.config.geoServer.datastore
            String url = "${grailsApplication.config.geoServer.baseURL}/rest/namespaces/${grailsApplication.config.geoServer.workspace}/datastores/${datastore}?recurse=true&purge=true"
            Map headers = getHeaders()
            webService.doDelete(url, headers)
        }
    }

    boolean buildGeoServerDependencies () {
        if (enabled) {
            deleteLayers()
            deleteDatastore()
            deleteWorkspace()

            createWorkspace()
            createDatastore()
            createPredefinedStyles()
        }
    }

    def wms(params, response) {
        if(enabled) {
            elasticSearchService.buildProjectActivityQuery(params)
            def index = PROJECT_ACTIVITY_INDEX
            def request = elasticSearchService.buildSearchRequest(params.query, params, index)
            String requestJSON = XContentHelper.convertToJson(request.source(), true)
            requestJSON = JSON.parse(requestJSON).query.toString()
            requestJSON = requestJSON.replaceAll(',', '\\\\,')
            requestJSON = URLEncoder.encode(requestJSON, "utf-8")
            Map wmsParams = whiteListWMSParams(params)
            if (params.VIEWPARAMS) {
                wmsParams.VIEWPARAMS = params.VIEWPARAMS + ';'
            } else {
                wmsParams.VIEWPARAMS = ""
            }

            wmsParams.VIEWPARAMS += "q:${requestJSON}"

            List requestParams = []
            wmsParams.each { key, value ->
                requestParams << key + "=" + value
            }

            String url = "${grailsApplication.config.geoServer.baseURL}/${grailsApplication.config.geoServer.workspace}/wms?${requestParams.join('&')}"
            webService.proxyGetRequest(response, url, false, false)
        }
    }

    def createStyleForFacet(String field, List terms, String style, String type = 'terms') {
        if (enabled) {
            String sld
            switch (type) {
                case 'terms':
                    sld = buildStyleForTermFacet(field, terms, style)
                    break;
                case 'range':
                    sld = buildStyleForRangeFacet(field, terms, style)
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
                styles[name] = file.getFile().getText()
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
            String url = "${grailsApplication.config.geoServer.baseURL}/rest/workspaces/${grailsApplication.config.geoServer.workspace}/styles.json"
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
            String url = "${grailsApplication.config.geoServer.baseURL}/rest/workspaces/${grailsApplication.config.geoServer.workspace}/styles.sld?name=${name}&raw=true"
            Map headers = getHeaders()
            headers['Content-Type'] = 'application/vnd.ogc.sld+xml'
            webService.doPost(url, style, false, headers)
        }
    }

    def createLayer(name, indices, Boolean enableTimeDimension = false, String timeSeriesIndex = '') {
        if (enabled) {
            String url = "${grailsApplication.config.geoServer.baseURL}/rest/workspaces/${grailsApplication.config.geoServer.workspace}/datastores/${grailsApplication.config.geoServer.datastore}/featuretypes"
            Map layerConfig = getLayerConfiguration(name, indices, enableTimeDimension, timeSeriesIndex)
            String content = getLayerDefinition(layerConfig)
            log.debug("Creating layer (${layerConfig.name}) on GeoServer with content: ${content}")
            Map response = webService.doPost(url, content, false, getHeaders())
            if(!response.error) {
                response.success = 'Created'
                response.layerName = layerConfig.name
                response.location = response.headers.location
            }

            response
        }
    }

    Map getLayerConfiguration (String name, List indices, Boolean enableTimeDimension = false, String timeSeriesIndex = '') {
        // deep copy configuration
        String configSerialized = (grailsApplication.config.geoServer.layerConfiguration as JSON).toString()
        Map config = JSON.parse(configSerialized)
        Map fieldsMapping = getFieldsMapping()
        List attributes = []

        indices?.each { index ->
            Map attributeConfig = getConfigForIndex(index, fieldsMapping)
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
    Map getConfigForIndex(String index, Map fieldsMapping) {
        String dataType = getDataTypeForIndex(index)
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

    String getDataTypeForIndex (String index) {
        Map mapping = getMapping()
        mapping = mapping["properties"]
        getDataTypeFromMapping(index, mapping, getFieldsMapping())
    }

    Map getMapping() {
        cacheService.get('elastic-search-mapping-with-dynamic-indices', {
            String index = grailsApplication.config.geoServer.indexName
            GetMappingsResponse response = elasticSearchService.client.admin().indices().getMappings(new GetMappingsRequest().indices(index)).get()
            response.getMappings().get(index).get(elasticSearchService.DEFAULT_TYPE).getSourceAsMap()
        }) as Map
    }

    Map getFieldsMapping() {
        cacheService.get('elastic-search-fields-mapping', {
            Map mapping = getMapping()
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
                className = com.vividsolutions.jts.geom.Geometry.class.name
                break
            case "geo_point":
                className = com.vividsolutions.jts.geom.Point.class.name
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
    List sanitizeIndices (List indices) {
        List defaultIndices = grailsApplication.config.geoServer.layerConfiguration.attributes.collect {it.name}
        indices?.findAll { !defaultIndices.contains(it) && (getDataTypeForIndex(it) != null) }
    }

    /**
     * Check a layer exists on GeoServer.
     * @param layerName
     * @return
     */
    boolean checkIfLayerExists (String layerName) {
        if (enabled) {
            String url = "${grailsApplication.config.geoServer.baseURL}/rest/workspaces/${grailsApplication.config.geoServer.workspace}/datastores/${grailsApplication.config.geoServer.datastore}/featuretypes/${layerName}.json"
            Map headers = getHeaders()
            def response = webService.getJson(url, null ,headers)
            return !response.error
        }

        return false
    }

    String getLayerForIndices(List indices, String prefix = null, String name = null, Boolean enableTimeDimension = false, String timeSeriesIndex = '') {
        indices = sanitizeIndices(indices)
        name = name ?: getLayerName(indices, prefix)
        boolean isCreated = checkIfLayerExists(name)
        if (!isCreated) {
            Map response = createLayer(name, indices, enableTimeDimension, timeSeriesIndex)
            if (response.error) {
                log.warn("Could not create ${name} on GeoServer.")
                return
            }
        }

        name
    }

    String getLayerNameForType(String type, List indices) {
        if (type && indices) {
            String layerName, timeSeriesIndex
            Map config

            switch (type) {
                case GENERAL_LAYER:
                    config = grailsApplication.config.geoServer.layerNames[GENERAL_LAYER]
                    layerName = getLayerForIndices(config.attributes, null, config.name)
                    break
                case INFO_LAYER:
                    config = grailsApplication.config.geoServer.layerNames[INFO_LAYER]
                    if (indices?.contains(INFO_LAYER_DEFAULT)) {
                        indices = []
                    }

                    indices.addAll(config.attributes)
                    Set uniqueList = new HashSet<String>()
                    uniqueList.addAll(indices)
                    indices = uniqueList.toList()
                    layerName = getLayerForIndices(indices)
                    break
                case INDICES_LAYER:
                    config = grailsApplication.config.geoServer.layerNames[INDICES_LAYER]
                    indices.addAll(config.attributes)
                    layerName = getLayerForIndices(indices, INDICES_LAYER)
                    break
                case TIMESERIES_LAYER:
                    config = grailsApplication.config.geoServer.layerNames[TIMESERIES_LAYER]
                    // assumption is the first index is date index
                    timeSeriesIndex = indices.get(0)
                    indices.addAll(config.attributes)
                    Set uniqueList = new HashSet<String>()
                    uniqueList.addAll(indices)
                    indices = uniqueList.toList()
                    layerName = getLayerForIndices(indices, TIMESERIES_LAYER, null, true, timeSeriesIndex)
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
    String getLayerDefinition (Map layerConfig) {
        def files = resourceResolver.getResources("classpath:data/templates/layer.template")
        def engine = new groovy.text.XmlTemplateEngine()
        engine.setIndentation('')
        String content
        files?.each { Resource file ->
            content = engine.createTemplate(file.getFile()).make(layerConfig).toString()
        }

        content?.replaceAll('\n', '');
    }

    def deleteLayers() {
        if (enabled) {
            String datastore = grailsApplication.config.geoServer.datastore
            String url = "${grailsApplication.config.geoServer.baseURL}/rest/workspaces/${grailsApplication.config.geoServer.workspace}/featuretypes.json"
            Map headers = getHeaders()
            Map layers = webService.getJson(url, null, headers)
            if (layers?.featureTypes){
                layers.featureTypes.featureType?.each { layer ->
                    webService.doDelete(layer.href, headers)
                }
            }
        }
    }

    def deleteStyle (String styleName) {
        String url = "${grailsApplication.config.geoServer.baseURL}/rest/workspaces/${grailsApplication.config.geoServer.workspace}/styles/${styleName}?purge=true"
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
            content = engine.createTemplate(file.getFile()).make(data).toString()
        }

        content?.replaceAll('\n', '');
    }

    def buildStyleForTermFacet(String field, List terms, String style) {
        int cIndex = 0
        List colour = grailsApplication.config.geoserver.facetTermColour
        terms?.eachWithIndex { Map term, index ->
            // reuse last colour in array if number of terms exceed number of colours
            cIndex = index > (colour.size() - 1 ) ? (colour.size() - 1 ) : index
            // add colour value only if not provided
            term.colour = term.colour ?: colour[cIndex]
        }

        cIndex++
        if (cIndex >= colour.size()) {
            cIndex = colour.size() - 1
        }

        Map dataBinding = [
                namespace: grailsApplication.config.geoServer.workspace,
                field: field,
                terms: terms,
                style: style,
                otherColour: colour[cIndex]
        ]

        bindDataToXMLTemplate("classpath:data/templates/colour_by_term.template", dataBinding)
    }

    def buildStyleForRangeFacet(String field, List terms, String style) {
        int cIndex = 0
        List colour = grailsApplication.config.geoserver.facetRangeColour

        terms?.eachWithIndex { Map term, index ->
            // reuse last colour in array if number of terms exceed number of colours
            cIndex = index > (colour.size() - 1 ) ? (colour.size() - 1 ) : index
            // add colour value only if not provided
            term.colour = term.colour ?: colour[cIndex]
            term.displayName = term.displayName?.replaceAll('<', '&lt;')?.replaceAll('>', '&gt;')
        }

        Map dataBinding = [
                namespace: grailsApplication.config.geoServer.workspace,
                field: field,
                terms: terms,
                style: style
        ]

        bindDataToXMLTemplate("classpath:data/templates/colour_by_range.template", dataBinding)
    }

    def whiteListWMSParams(params) {
        List whitelist = [
                'SERVICE', 'VERSION', 'REQUEST', 'FORMAT', 'TRANSPARENT', 'STYLES', 'LAYERS', 'SRS', 'CRS', 'WIDTH', 'LAYER',
                'HEIGHT', 'BBOX', 'QUERY_LAYERS', 'INFO_FORMAT', 'FEATURE_COUNT', 'X', 'Y', 'I', 'J', 'EXCEPTIONS', 'TIME',
                'STYLE', 'LEGEND_OPTIONS', 'RULE', 'SLD_BODY', 'SLD', 'SCALE', 'FEATURETYPE', 'MAXFEATURES', 'PROPERTYNAME',
                'CQL_FILTER', 'TIME', 'ENV'
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

    private Map getHeaders() {
        String encoded = "${grailsApplication.config.geoServer.username}:${grailsApplication.config.geoServer.password}".bytes.encodeBase64().toString()
        [
                "Authorization": "Basic ${encoded}",
                "Content-Type" : "application/xml;charset=utf-8"
        ]
    }
}
