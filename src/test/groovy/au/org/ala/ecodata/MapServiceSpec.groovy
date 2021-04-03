package au.org.ala.ecodata

import grails.converters.JSON
import grails.testing.services.ServiceUnitTest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoGrid
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import spock.lang.Specification

/*
 * Copyright (C) 2020 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Created by Temi on 12/10/20.
 */

class MapServiceSpec extends Specification implements ServiceUnitTest<MapService>  {
    WebService webService = Mock(WebService)
    ElasticSearchService elasticSearchService = Mock(ElasticSearchService)
    CacheService cacheService = Mock(CacheService)

    def setup() {
        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())

        service.resourceResolver = new PathMatchingResourcePatternResolver()
        service.webService = webService
        service.elasticSearchService = elasticSearchService
        service.cacheService = cacheService
        service.enabled = true
    }

    def getMapping() {
        [
                "properties": [
                        "aim": ["type": "string"],
                        "plannedStartDate": ["type": "string"],
                        "plannedEndDate": ["type": "string"],
                        "name": ["type": "string"],
                        "logoAttribution": ["type": "string"],
                        "imageUrl": ["type": "string"],
                        "dateCreated": ["type": "string"],
                        "thumbnailUrl": ["type": "string"],
                        "activityId": ["type": "string"],
                        "projectId": ["type": "string"],
                        "recordNameFacet": ["type": "string"],
                        "projectActivityNameFacet": ["type": "string"],
                        "projectNameFacet": ["type": "string"],
                        "surveyMonthFacet": ["type": "string"],
                        "surveyYearFacet": ["type": "string", fields: [ "surveyYearFacetField": ["type": "string", "index" : "not_analyzed"]]],
                        "projectArea": ["properties": ["geometryType": ["type": "string"], "geoIndex": ["type": "geo_shape"]]],
                        "sites": ["properties": ["geoIndex": ["type": "geo_shape"], "geometryType": ["type": "string"]]]
                ]
        ]
    }

    def "should bind data to template"() {
        when:
        Map workspace = [workspace: "test"]
        String result = service.bindDataToXMLTemplate("classpath:data/templates/workspace.template", workspace)

        then:
        result == "<workspace><name>test</name></workspace>"
    }

    def "should create and delete workspace when called"() {
        when:
        service.createWorkspace()

        then:
        1 * webService.doPost("http://localhost:8081/geoserver/rest/workspaces", _, false, _) >> [resp: "success"]

        when:
        service.deleteWorkspace()

        then:
        1 * webService.doDelete("http://localhost:8081/geoserver/rest/workspaces/ecodata?recurse=true&purge=true", _) >> [resp: "success"]
    }

    def "should create and delete DataStores when called"() {
        int numberOfCalls = service.datastores.size()
        when:
        service.createDatastores()

        then:
        numberOfCalls * webService.doPost("http://localhost:8081/geoserver/rest/workspaces/ecodata/datastores", _, false, _) >> [resp: "success"]

        when:
        service.deleteDatastores()

        then:
        numberOfCalls * webService.doDelete(_, _) >> [resp: "success"]
    }

    def "should create style for both term and range facets"() {
        def field, terms, style, type, dataType, result
        when:
        dataType = 'pa'
        type = 'terms'
        field = 'field1'
        terms = [[
                         "displayName": "term 1",
                         "term"       : "term1"
                 ]]
        style = 'name'

        result = service.createStyleForFacet(field, terms, style, type, dataType)

        then:
        result == "65a961a6ec8439e6db32c491fa394a9d"

        when:
        dataType = 'pa'
        type = 'range'
        field = 'field1'
        terms = [[
                         "displayName": "term 1",
                         "from"       : 1,
                         "to"         : 2
                 ]]
        style = 'name'

        result = service.createStyleForFacet(field, terms, style, type, dataType)

        then:
        result == "37f6c6d26c9d7b14b61cd8794ae40acd"
    }

    def "should get file name from sld file"() {
        def fileName = "point_circle"
        def file = service.resourceResolver.getResources("classpath:data/styles/${fileName}.sld")[0]
        def result
        when:
        result = service.getStyleNameFromFileName(file)

        then:
        result == fileName
    }

    def "should create styles uploaded with application"() {
        def numberOfStyles = service.resourceResolver.getResources("classpath:data/styles/*.sld").size()
        when:
        service.createPredefinedStyles()

        then:
        1 * webService.getJson(_, null, _) >> [styles: [style: [[name: 'test', href: 'test1']]]]
        (numberOfStyles + 1) * webService.doDelete(_, _) >> 200
        numberOfStyles * webService.doPost(_, _, _, _) >> [resp: '']
    }

    def "should delete parameters like viewparams using white list method"() {
        Map params, result
        when:
        params = ['request': 'GetMap', 'service': 'GetMap', 'BBOX': '[Test]', 'viewparams': 'q:{"match": "all"}', 'xyz': "to delete"]
        result = service.whiteListWMSParams(params)

        then:
        result.size() == 3
        result.viewparams == null
        result.xyz == null
        result.request == 'GetMap'
        result.BBOX == "%5BTest%5D"
    }

    def "should create layer based on supplied settings"() {
        def name = 'testName'
        def indices = ['name']
        def dataStore = 'pasearch'
        def enableTimeDimension = false
        def timeSeriesIndex = ''
        def res

        when:
        res = service.createLayer(name, dataStore, indices, enableTimeDimension, timeSeriesIndex)

        then:
        1 * cacheService.get("elastic-search-mapping-with-dynamic-indices-${dataStore}", _) >> ["properties": ["name": ["type": "string"]]]
        2 * cacheService.get("elastic-search-fields-mapping-${dataStore}", _) >> [:]
        1 * webService.doPost(_, _, _, _) >> [headers: [location: 'testLocation']]
        res.success == 'Created'
        res.layerName == name
        res.location == 'testLocation'
    }

    def "should get layer settings"() {
        def name = 'testName'
        def indices = ['name']
        def dataStore = 'pasearch'
        def enableTimeDimension = false
        def timeSeriesIndex = ''
        def res

        when:
        res = service.getLayerSettings(name, indices, dataStore, enableTimeDimension, timeSeriesIndex)

        then:
        1 * cacheService.get("elastic-search-mapping-with-dynamic-indices-${dataStore}", _) >> ["properties": ["name": ["type": "string"]]]
        2 * cacheService.get("elastic-search-fields-mapping-${dataStore}", _) >> [:]
        res.name == name
        res.timeEnabled == enableTimeDimension
        res.timeAttribute == 'dateCreated'
        res.attributes?.collect { it.name } == ['sites.geoIndex', 'name']
    }

    def "should return feature collection from aggregation result and add colour"() {
        setup:
        def name = "heatmap"
        def gridName = "r7h"
        Map features
        Map res = [getAggregations: { ->
            def agg = Mock(GeoGrid)
            def bucket = Mock(GeoGrid.Bucket)
            bucket.getKey() >> gridName
            bucket.getDocCount() >> 1
            agg.getBuckets() >> [bucket]
            ["heatmap": agg]
        }]

        List boundary = [[[151.875, -26.71875], [151.875, -28.125], [153.28125, -28.125], [153.28125, -26.71875], [151.875, -26.71875]]]

        when:
        features = service.getFeatureCollectionFromSearchResult(res, name)
        service.setHeatmapColour(features)

        then:
        features.type == "FeatureCollection"
        features.features.size() == 1
        features.features[0].type == "Feature"
        features.features[0].properties.count == 1
        features.features[0].properties.key == gridName
        features.features[0].properties.colour != null
        features.features[0].geometry.type == "Polygon"
        features.features[0].geometry.coordinates == boundary
    }

    def "should make WMS request with correct parameters"() {
        def params = [dataType: 'project', query: 'difficulty:"Easy"']
        def response = new GrailsMockHttpServletResponse()
        setup:
        service.elasticSearchService = new ElasticSearchService()
        service.elasticSearchService.grailsApplication = grailsApplication

        when:
        service.wms(params, response)

        then:
        //1 * webService.proxyGetRequest(response, "http://localhost:8081/geoserver/ecodata/wms?VIEWPARAMS=q:%7B%22query_string%22%3A%7B%22query%22%3A%22difficulty%3A%5C%5C%5C%22Easy%5C%5C%5C%22%22%5C%2C%22fields%22%3A%5B%22name%5E50.0%22%5C%2C%22description%5E30.0%22%5C%2C%22organisationName%5E30.0%22%5C%2C%22_all%22%5D%7D%7D", false, false, ['Expires', 'Cache-Control', 'Content-Disposition', 'Content-Type'], 600000) >> [image: true]
        params.query == "difficulty:\\\"Easy\\\""

//        when:
//        params = [dataType: 'pa', query: '']
//        service.wms(params, response)
//
//        then:
//        1 * webService.proxyGetRequest(response, "http://localhost:8081/geoserver/ecodata/wms?VIEWPARAMS=q:%7B%22query_string%22%3A%7B%22query%22%3A%22%28docType%3Aactivity+AND+projectActivity.embargoed%3Afalse%29%22%7D%7D", false, false, ['Expires', 'Cache-Control', 'Content-Disposition', 'Content-Type'], 600000) >> [image: true]
    }

    def "should return correct data type for input"() {
        expect:
        service.getClassForElasticSearchDataType(type) == result

        where:
        type        | result
        'string'    | String.class.name
        'integer'   | Integer.class.name
        'long'      | Long.class.name
        'float'     | Float.class.name
        'double'    | Double.class.name
        'boolean'   | Boolean.class.name
        'date'      | Date.class.name
        'geo_shape' | com.vividsolutions.jts.geom.Geometry.class.name
        'geo_point' | com.vividsolutions.jts.geom.Point.class.name
    }

    def "should create layer for various types of requests"() {
        given:
        def res

        when:
        res = service.getLayerNameForType(type, indices, dataType)

        then:
        invocationMapping * cacheService.get("elastic-search-mapping-with-dynamic-indices-${dataStore}", _) >> getMapping()
        invocationFieldMapping * cacheService.get("elastic-search-fields-mapping-${dataStore}", _) >> [:]
        1 * webService.getJson(_, _, _) >> [success: true]
        res == result

        where:
        type                        | dataType                         | dataStore                   | indices                   | invocationMapping | invocationFieldMapping | result
        MapService.GENERAL_LAYER    | MapService.PROJECT_TYPE          | ElasticIndex.HOMEPAGE_INDEX | ['projectArea.geoIndex']     | 1                 | 1                      | 'generalproject'
        MapService.INFO_LAYER       | MapService.PROJECT_TYPE          | ElasticIndex.HOMEPAGE_INDEX | ['projectArea.geoIndex'] | 8                 | 8                      | '8b198034ebae52a478bfe6eaae08b406'
        MapService.INDICES_LAYER    | MapService.PROJECT_TYPE          | ElasticIndex.HOMEPAGE_INDEX | ['projectArea.geoIndex'] | 1                 | 1                      | 'af7159d73d4494025f7d7be876bf489d'
        MapService.GENERAL_LAYER    | MapService.PROJECT_ACTIVITY_TYPE | ElasticIndex.PROJECT_ACTIVITY_INDEX | ['sites.geoIndex']   | 1                 | 1                      | 'general'
        MapService.INFO_LAYER       | MapService.PROJECT_ACTIVITY_TYPE | ElasticIndex.PROJECT_ACTIVITY_INDEX | ['sites.geoIndex']   | 10                | 10                     | '42183e75f05b900fd59f3b545bc8eb2d'
        MapService.INDICES_LAYER    | MapService.PROJECT_ACTIVITY_TYPE | ElasticIndex.PROJECT_ACTIVITY_INDEX | ['sites.geoIndex']   | 1               | 1                      | 'ae01dfca9fda384379f153b7d469d884'
        MapService.TIMESERIES_LAYER | MapService.PROJECT_ACTIVITY_TYPE | ElasticIndex.PROJECT_ACTIVITY_INDEX | ['sites.geoIndex']   | 1                 | 1                      | 'cb971ab32fb5c3400a2090fca3872dbe'
    }

    def "should delete available layers" () {
        when:
        service.deleteLayers()

        then:
        1 * webService.getJson(_, null, _) >> [featureTypes: [featureType: [[name: 'toDelete']]]]
        3 * webService.doDelete(_, _) >> 200
    }

    def "should get field mapping from mappings"() {
        def res
        when:
        res = service.getFieldsMapping(getMapping()["properties"])

        then:
        res["surveyYearFacetField"]  == ["type": "string", "index" : "not_analyzed", path:"surveyYearFacet"]
    }
}
