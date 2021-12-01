package au.org.ala.ecodata

import grails.converters.JSON
import grails.testing.gorm.DataTest
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import org.elasticsearch.action.admin.indices.flush.FlushRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller

import spock.lang.Specification

/**
 * Tests the ElasticSearchService
 */
class ElasticSearchServiceSpec extends Specification implements ServiceUnitTest<ElasticSearchService>, DataTest {

    private static final String PROGRAM_1 = "Program1"
    private static final String SUB_PROGRAM_1 = "SubProgram1"

    private static final String PROGRAM_2 = "Program2"
    private static final String SUB_PROGRAM_2 = "SubProgram2"
    private static final String SUB_PROGRAM_3 = "SubProgram3"

    private static final String THEME1 = "Theme1"
    private static final String THEME2 = "Theme2"

    private static final String INDEX_NAME = ElasticIndex.DEFAULT_INDEX


    private int activityId = 0
    private int projectId = 0
    private int siteId = 0

    Closure doWithConfig() {{ config ->
        config.geoServer.enabled = "false"
    }}

    Closure doWithSpring() {{ ->
        mapService MapService
    }}

    void setupSpec() {
        mockDomain(ActivityForm)
        mockDomain(Project)
        mockDomain(Activity)
        mockDomain(Organisation)
        mockDomain(Site)
    }

    void setup() {

        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())

        CacheService cacheService = new CacheService()
        MetadataService metadataService = new MetadataService()
        metadataService.cacheService = cacheService
        service.cacheService = cacheService
        service.metadataService = metadataService
        service.siteService = Mock(SiteService)
        service.activityService = Mock(ActivityService)
        service.organisationService = Mock(OrganisationService)
        grailsApplication.config.app.facets.geographic.contextual.state='cl927'
        service.initialize()
        service.indexAll() // This will delete then recreate the index as there is no data in the database
        def project1 = createProject(PROGRAM_1, SUB_PROGRAM_1)
        def project2 = createProject(PROGRAM_2, SUB_PROGRAM_2)
        def project3 = createProject(PROGRAM_2, SUB_PROGRAM_3)
        [project1, project2, project3].each {
            service.indexDoc(it, INDEX_NAME)
            service.indexDoc(it, ElasticIndex.HOMEPAGE_INDEX)
        }

        def site1 = createSite("NSW", "NRM1")
        def site2 = createSite("NSW", "NRM2")
        def site3 = createSite("ACT", "NRM3")
        def site4 = createSite("WA", "NRM4")
        def site5 = createSite("QLD", "NRM5")
        def site6 = createSite("VIC", "NRM6")

        def activity1 = createActivity(project1, site1, null, null, THEME1)
        def activity2 = createActivity(project2, site2, null, null, THEME1)
        def activity3 = createActivity(project1, site2, null, null, THEME2)

        def activity4 = createActivity(project1, site3, null, null, THEME1)


        def activity5 = createActivity(project2, site4, null, null, THEME1)
        def activity6 = createActivity(project3, site4, null, null, THEME1)
        def activity7 = createActivity(project3, site4, null, null, THEME1)
        def activity8 = createActivity(project3, site5, null, null, THEME1)
        def activity9 = createActivity(project3, site6, null, null, THEME1)
        def activity10 = createActivity(project3, null, null, null, THEME2)

        [activity1, activity2, activity3, activity4, activity5, activity6, activity7, activity8, activity9, activity10].each  {
            service.indexDoc(it, INDEX_NAME)
        }

        // Ensure results are available for searching
        FlushRequest request = new FlushRequest(INDEX_NAME)
        service.client.indices().flush(request, RequestOptions.DEFAULT)

        request = new FlushRequest(ElasticIndex.HOMEPAGE_INDEX)
        service.client.indices().flush(request, RequestOptions.DEFAULT)

        waitForIndexingToComplete()
    }

    private void waitForIndexingToComplete() {

        int indexCount = 0
        int expectedCount = 13 // 10 activities + 3 projects
        while (indexCount != expectedCount) {

            SearchSourceBuilder builder = new SearchSourceBuilder()
            builder.query(QueryBuilders.matchAllQuery())//.fetchSource(false)

            SearchRequest searchRequest = new SearchRequest()
            searchRequest.indices(INDEX_NAME).source(builder)

            SearchResponse searchResponse = service.client.search(searchRequest, RequestOptions.DEFAULT)
            indexCount = searchResponse.hits.totalHits.value
        }
    }

    /**
     * Tests the facet fields are indexed correctly for activities - this is used in particular by the reporting subsystem.
     */
    void testActivitySearch() {

        when:
        def activityFilters = ["mainThemeFacet:${THEME1}"]
        def results = service.searchActivities(activityFilters, [offset:0, max:10], null, INDEX_NAME)

        then:
        results.hits.totalHits.value == 8

        when:
        activityFilters = ["mainThemeFacet:${THEME1}", "associatedProgramFacet:${PROGRAM_1}"]
        results = service.searchActivities(activityFilters, [offset:0, max:10], null, INDEX_NAME)
        println results

        then:
        results.hits.totalHits.value == 2

        when:
        activityFilters = ["stateFacet:ACT"]
        results = service.searchActivities(activityFilters, [offset:0, max:10], null, INDEX_NAME)

        then:
        assert results.hits.totalHits.value == 1

        when:
        activityFilters = ["mainThemeFacet:${THEME1}", "associatedProgramFacet:${PROGRAM_1}", "stateFacet:ACT"]
        results = service.searchActivities(activityFilters, [offset:0, max:10], null, INDEX_NAME)

        then:
        assert results.hits.totalHits.value == 1

        when:
        activityFilters = ["mainThemeFacet:${THEME1}", "mainThemeFacet:${THEME2}", "stateFacet:ACT", "stateFacet:NSW"]
        results = service.searchActivities(activityFilters, [offset:0, max:10], null, INDEX_NAME)

        then:
        assert results.hits.totalHits.value == 4


    }

    def "The service will accept T/F values when filtering on boolean fields"() {
        when:
        def results = service.search("*:*", [fq:"isExternal:T"], INDEX_NAME)

        then:
        results.hits.totalHits.value > 0

        when:
        results = service.search("*:*", [fq:"isExternal:true"], INDEX_NAME)

        then:
        results.hits.totalHits.value > 0

        when:
        results = service.search("*:*", [fq:"isExternal:F"], INDEX_NAME)

        then:
        results.hits.totalHits.value == 0

        when:
        results = service.search("*:*", [fq:"isExternal:false"], INDEX_NAME)

        then:
        results.hits.totalHits.value == 0

        when:
        results = service.search("*:*", [fq:"isMERIT:T"], INDEX_NAME)

        then:
        results.hits.totalHits.value == 0

        when:
        results = service.search("*:*", [fq:"isMERIT:true"], INDEX_NAME)

        then:
        results.hits.totalHits.value == 0

        when:
        results = service.search("*:*", [fq:"isMERIT:F"], INDEX_NAME)

        then:
        results.hits.totalHits.value > 0

        when:
        results = service.search("*:*", [fq:"isMERIT:false"], INDEX_NAME)

        then:
        results.hits.totalHits.value > 0

    }

    def "The query will search fields other than name, description and organisation name (which are boosted fields)"() {
        when: "We search on a theme in the default index"
        def results = service.search(THEME1, [:], INDEX_NAME)

        then:
        results.hits.totalHits.value > 0

        when: "We search on a theme in the homepage index"
        results = service.search(PROGRAM_1, [:], ElasticIndex.HOMEPAGE_INDEX)

        then:
        results.hits.totalHits.value > 0
    }

    /**
     * Creates a minimal version of an Activity that has just the attributes we will be searching.
     */
    private Map createActivity(project, site, startDate, endDate, theme, status = 'published') {
        def activity = [:]

        activity.putAll(project)
        activity.sites = site?[site]:[]
        activity.putAll([activityId:'activity'+(++activityId), mainTheme:theme, startDate:startDate, endDate:endDate, publicationStatus:status, className:Activity.class.name])

        activity
    }

    private Map createSite(state, nrm) {
        [siteId:'site'+(++siteId), extent:[geometry:[state:state, nrm:nrm]], className:Site.class.name]
    }

    private Map createProject(program, subProgram) {
        [projectId:'project'+(++projectId), associatedProgram:program, associatedSubProgram:subProgram, className:Project.class.name, isExternal:true, isMERIT:false]
    }


}
