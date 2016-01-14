package au.org.ala.ecodata
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.Before
/**
 * Tests the ElasticSearchService
 */
@TestFor(ElasticSearchService)
@TestMixin(ControllerUnitTestMixin) // Used to register JSON converters.
class ElasticSearchServiceTests {

    private static final String PROGRAM_1 = "Program1"
    private static final String SUB_PROGRAM_1 = "SubProgram1"

    private static final String PROGRAM_2 = "Program2"
    private static final String SUB_PROGRAM_2 = "SubProgram2"
    private static final String SUB_PROGRAM_3 = "SubProgram3"

    private static final String THEME1 = "Theme1"
    private static final String THEME2 = "Theme2"

    private static final String INDEX_NAME = "test"


    private int activityId = 0
    private int projectId = 0
    private int siteId = 0


    @Before
    public void indexEntities() {

        grailsApplication.config.app.facets.geographic.contextual.state='cl927'
        service.initialize()
        service.deleteIndex("search") // The elastic search service relies on the search index, this actually forces it to be created.
        service.deleteIndex(INDEX_NAME) // this actually deletes and recreates the index.

        def project1 = createProject(PROGRAM_1, SUB_PROGRAM_1)
        def project2 = createProject(PROGRAM_2, SUB_PROGRAM_2)
        def project3 = createProject(PROGRAM_2, SUB_PROGRAM_3)
        [project1, project2, project3].each {
            service.indexDoc(it, INDEX_NAME)
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
        service.client.admin().indices().prepareFlush().execute().actionGet();

    }

    /**
     * Tests the facet fields are indexed correctly for activities - this is used in particular by the reporting subsystem.
     */
    public void testActivitySearch() {

        def activityFilters = ["mainThemeFacet:${THEME1}"]
        def results = service.searchActivities(activityFilters, [offset:0, max:10], null, INDEX_NAME)
        assert results.hits.totalHits == 8

        activityFilters = ["mainThemeFacet:${THEME1}", "associatedProgramFacet:${PROGRAM_1}"]
        results = service.searchActivities(activityFilters, [offset:0, max:10], null, INDEX_NAME)
        println results
        assert results.hits.totalHits == 2

        activityFilters = ["stateFacet:ACT"]
        results = service.searchActivities(activityFilters, [offset:0, max:10], null, INDEX_NAME)
        assert results.hits.totalHits == 1

        activityFilters = ["mainThemeFacet:${THEME1}", "associatedProgramFacet:${PROGRAM_1}", "stateFacet:ACT"]
        results = service.searchActivities(activityFilters, [offset:0, max:10], null, INDEX_NAME)
        assert results.hits.totalHits == 1

    }

    /**
     * Tests that the home page facets work correctly with activity based facets (in particular, the reporting theme).
     */
    public void testReportingThemeHomepageSearch() {

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
        [projectId:'project'+(++projectId), associatedProgram:program, associatedSubProgram:subProgram, className:Project.class.name]
    }


}
