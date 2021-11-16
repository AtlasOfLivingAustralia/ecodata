package au.org.ala.ecodata

import au.com.bytecode.opencsv.CSVReader
import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest
import org.apache.lucene.search.TotalHitCountCollector
import org.apache.lucene.search.TotalHits
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.SearchHits
import spock.lang.Specification


/**
 * Specification for the ReportService.
 */
class ReportServiceSpec extends MongoSpec implements ServiceUnitTest<ReportService> {

    MetadataService metadataService = Stub(MetadataService)
    ElasticSearchService elasticSearchService = Stub(ElasticSearchService)
    OutputService outputService = Stub(OutputService)
    UserService userService = Mock(UserService)
    ActivityService activityService = Mock(ActivityService)


    def setupSpec() {}

    def setup() {

        service.elasticSearchService = elasticSearchService
        service.metadataService = metadataService
        service.outputService = outputService
        service.activityService = activityService
        service.userService = userService
        outputService.toMap(_) >> {Map output -> output}

        deleteAll()
    }

    def cleanup() {
        deleteAll()
    }

    private void deleteAll() {
        User.findAll().each{it.delete()}
        Project.findAll().each{it.delete()}
        ManagementUnit.findAll().each{it.delete()}
        Program.findAll().each{it.delete()}
    }

    def setupInputs(outputs, activities, outputData) {
        Map model = [outputs:outputs]
        Map dataModel = [dataModel:[[type:'string', name:'test'], [type:'list', name:'nested', columns:[]]]]

        // By default we mostly only deal with published activities.
        activities = activities.collect {it+[publicationStatus:Report.REPORT_APPROVED]}

        metadataService.activitiesModel() >> model
        metadataService.getOutputDataModel(_) >> dataModel

        Set projectIds = new HashSet(activities.collect { it.projectId ?: 'defaultProjectId' })
        List projectDocs = projectIds.collect {
            [projectId:it, activities: activities.findAll{activity -> (it == 'defaultProjectId' && !activity.projectId) || activity.projectId == it}]
        }

        SearchHit[] hits = projectDocs.collect{
            SearchHit searchHit = GroovyMock(SearchHit)
            searchHit.getSourceAsMap() >> it
            searchHit
        }

        SearchResponse searchResponse = GroovyMock(SearchResponse)
        TotalHits totalHits = new TotalHits(projectDocs.size(), TotalHits.Relation.EQUAL_TO)
        searchResponse.getHits() >> new SearchHits(hits, totalHits, 1.0)
        elasticSearchService.search(_, _, _) >> searchResponse


        Output.metaClass.static.findAllByActivityIdInListAndStatusNotEqual = {activityIds, status -> activityIds.collect{outputData[it]}.flatten().findAll()}
    }

    def "the sum of a single property can be reported"() {
        given:
        def output = "output"
        def property = "prop"

        Map config = [type:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM.name(), property:'data.'+property, label:property]
        Score score = new Score([outputType:output, label:property, configuration:config])

        def values = [1:1,2:2,3:3,4:4,5:5]
        def activities = values.collect{[activityId:it.key]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, property, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [score])

        then:
        results.outputData[0].result.result == values.values().sum()
        results.metadata.activities == 5
        results.metadata.projects.size() == 1

    }

    def "the sum of a property grouped by another property can be reported"() {
        given:
        def output = "output"
        def property = "prop"
        Map config = [label:property, childAggregations:[[type:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM.name(), property:"data."+property]], groups:[type:'discrete', property:"activity.mainTheme"]]
        Score score = new Score([outputType:output, label:property, configuration:config])

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:1,2:2,3:3,4:4,5:5]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, property, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [score])

        then:

        results.outputData[0].result.groups[0].group == "theme1"
        results.outputData[0].result.groups[0].count == 3
        results.outputData[0].result.groups[0].results[0].result == 1+2+5

        results.outputData[0].result.groups[1].results[0].result == 3+4
        results.outputData[0].result.groups[1].group == "theme2"
        results.outputData[0].result.groups[1].count == 2

        results.metadata.activities == 5
        results.metadata.projects.size() == 1
    }

    def "the filtered sum of a property can be reported"() {

        given:
        def output = "output"
        def property = "prop"
        Map config = [childAggregations: [[type:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM.name(), property:'data.'+property]], label:property, filter:[type:'discrete', property:"activity.mainTheme", filterValue:'theme1']]
        Score score = new Score([outputType:output, label:property, configuration:config])

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:1,2:2,3:3,4:4,5:5]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, property, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [score])

        then:
        results.outputData.size() == 1

        def expected = 1+2+5

        results.outputData[0].result.result == expected

        5 == results.metadata.activities
        1 == results.metadata.projects.size()
    }

    def "test nested group sum aggregation"() {

        setup:
        def output = "output"
        def property = "prop"
        Map config = [childAggregations: [[type:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM.name(), property:'data.nested.'+property]], label:property, groups:[property:"data.nested.group", type:'discrete']]
        Score score = new Score([outputType:output, label:property, configuration:config])

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:"group1"], [prop:2, group:"group2"]],
                      2:[[prop:10, group:"group1"], [prop:12, group:"group2"]],
                      3:[[prop:3, group:"group3"]]
        ]

        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, "nested", it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [score])

        then:
        results.outputData.size() == 1
        results.outputData[0].result.groups.size() == 3

        results.outputData[0].result.groups[0].results[0].result == 11
        results.outputData[0].result.groups[0].group == "group1"
        results.outputData[0].result.groups[0].count == 2

        results.outputData[0].result.groups[1].results[0].result == 14
        results.outputData[0].result.groups[1].group == "group2"
        results.outputData[0].result.groups[1].count == 2

        results.outputData[0].result.groups[2].results[0].result == 3
        results.outputData[0].result.groups[2].group == "group3"
        results.outputData[0].result.groups[2].count == 1

        5 == results.metadata.activities
        1 == results.metadata.projects.size()
        //assertEquals 5, results.metadata.activities
        //assertEquals 1, results.metadata.projects.size()

    }

    def "testFilteredSumAggregationOfListAttribute"() {

        setup:
        def output = "output"
        def property = "prop"
        def list = 'nested'
        Map config = [childAggregations: [[type:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM.name(), property:'data.'+list+'.'+property]], label:property, filter:[property:"data."+list+".group", filterValue:'group1', type:'discrete']]
        Score score = new Score([outputType:output, label:property, configuration:config])

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [score])

        then:
        results.outputData.size() == 1


        def expected = 1+3+2+3
        results.outputData[0].result.result == expected
        results.outputData[0].result.count == 4

        results.metadata.activities == 5
        results.metadata.projects.size() == 1
    }

    def "testGroupByMultiValueField"() {
        setup:
        def output = "output"
        def property = "prop"
        Map config = [childAggregations: [[ type:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM.name(), property:'data.nested.'+property]], label:property, groups:[property:"data.nested.group", type:'discrete']]
        Score score = new Score([outputType:output, label:property, configuration:config])

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:["group1", "group2"]], [prop:2, group:["group2", "group3"]]],
                      2:[[prop:10, group:["group1"]], [prop:12, group:["group2"]]],
                      3:[[prop:3, group:["group3", "group2"]]]
        ]

        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, "nested", it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [score])

        then:
        results.outputData.size() == 1
        results.outputData[0].result.groups.size() == 3

        results.outputData[0].result.groups[0].results[0].result == 11
        results.outputData[0].result.groups[0].group == "group1"
        results.outputData[0].result.groups[0].count == 2

        results.outputData[0].result.groups[1].results[0].result == 18
        results.outputData[0].result.groups[1].group == "group2"
        results.outputData[0].result.groups[1].count == 4

        results.outputData[0].result.groups[2].results[0].result == 5
        results.outputData[0].result.groups[2].group == "group3"
        results.outputData[0].result.groups[2].count == 2

        results.metadata.activities == 5
        results.metadata.projects.size() == 1

    }

    def "testTopLevelGrouping"() {
        setup:
        def output = "output"
        def property = "prop"
        def list = 'nested'
        Map config = [childAggregations: [[type:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM.name(), property:'data.'+list+'.'+property]], label:property, groups:[type:'discrete', property:"data.nested.group"]]
        Score score = new Score([outputType:output, label:property, configuration:config])

        def themes = [1:"theme1", 2:"theme1", 3:"theme2", 4:"theme2", 5:"theme1"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        def activities = themes.collect{[activityId:it.key, mainTheme:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [score], [type:'discrete', property:'activity.mainTheme'])

        then:
        results.outputData.groups.size() == 2
        results.outputData.groups[0].group == 'theme1'

        def theme1Results = results.outputData.groups[0].results[0].groups
        theme1Results.size() == 4

        [group1:6, group2:4, group3:2, group4:5].each { k, v ->
            def nestedResult = theme1Results.find{it.group == k}
            nestedResult.results[0].result == v
        }

        results.outputData.groups[1].group == 'theme2'
        def theme2Results = results.outputData.groups[1].results[0].groups
        [group1:3, group2:6].each { k, v ->
            def nestedResult = theme2Results.find{it.group == k}
            nestedResult.results[0].result == v
        }
    }

    def "testTopLevelDateGrouping"() {
        setup:
        def output = "output"
        def property = "prop"
        def list = 'nested'
        Map config = [childAggregations: [[type:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM.name(), property:'data.'+list+'.'+property]], label:property, groups:[property:"data.nested.group", type:'discrete']]
        Score score = new Score([outputType:output, label:property, configuration:config])

        def activityDates = [1:"2013-01-02T00:00:00Z", 2:"2014-01-02T00:00:00Z", 3:"2014-07-02T00:00:00Z", 4:"2015-01-01T00:00:00Z", 5:"2015-01-02T00:00:00Z"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        def activities = activityDates.collect{[activityId:it.key, plannedEndDate:it.value]}
        def outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}

        setupInputs([[name:output, scores:[score]]], activities, outputs)

        when:
        def results = service.aggregate([], null, [score], [property:'activity.plannedEndDate', type:'date', buckets:['2014-01-01T00:00:00Z', '2015-01-01T00:00:00Z'], format:'MMM yyyy'])

        then:
        results.outputData.groups[0].group == 'Before Jan 2014'
        def group1Results = results.outputData.groups[0].results[0].groups

        [group1:1, group2:2].each { k, v ->
            def nestedResult = group1Results.find{it.group == k}
            nestedResult.results[0].result == v
        }

        results.outputData.groups[1].group == 'Jan 2014 - Dec 2014'
        def group2Results = results.outputData.groups[1].results[0].groups

        [group1:3, group2:4, group3:2].each { k, v ->
            def nestedResult = group2Results.find{it.group == k}
            nestedResult.results[0].result == v
        }

        results.outputData.groups[2].group == 'After Dec 2014'
        def group3Results = results.outputData.groups[2].results[0].groups
        [group1:5, group2:4, group4:5].each { k, v ->
            def nestedResult = group3Results.find{it.group == k}
            nestedResult.results[0].result == v
        }

    }

    def "The ReportService can produce a summary of project scores grouped by date"() {

        setup:
        String projectId = 'p1'
        def output = "output"
        def property = "prop"
        def list = 'nested'
        Map config = [childAggregations: [[type:au.org.ala.ecodata.reporting.Score.AGGREGATION_TYPE.SUM.name(), property:'data.'+list+'.'+property]], label:property]
        Score score = new Score([outputType:output, label:property, configuration:config])

        def activityDates = [1:"2013-01-02T00:00:00Z", 2:"2014-01-02T00:00:00Z", 3:"2014-07-02T00:00:00Z", 4:"2014-12-31T00:00:00Z", 5:"2015-01-02T00:00:00Z"]

        def values = [1:[[prop:1, group:'group1'], [prop:2, group:'group2']],
                      2:[[prop:2, group:'group3'], [prop:2, group:'group2']],
                      3:[[prop:3, group:'group1'], [prop:2, group:'group2']],
                      4:[[prop:4, group:'group2']],
                      5:[[prop:5, group:'group4'], [prop:2, group:'group1'], [prop:3, group:'group1']]]
        Map outputs = values.collectEntries{[(it.key):[createOutput(it.key, output, list, it.value)]]}
        List activities = activityDates.collect{[activityId:it.key, plannedEndDate:it.value, outputs:outputs[it.key]]}


        Map dateGroupingConfig = [property:'activity.plannedEndDate', type:'date', buckets:['2014-01-01T00:00:00Z', '2015-01-01T00:00:00Z'], format:'MMM yyyy']

        when:
        List summary = service.projectSummary(projectId, [score], false, dateGroupingConfig)

        then:
        1 * activityService.findAllForProjectId(projectId, 'FLAT') >> activities
        summary.size() == 3
        summary[0].group == 'Before Jan 2014'
        summary[0].count == 1
        summary[0].results[0].result.result == 3

        summary[1].group == "Jan 2014 - Dec 2014"
        summary[1].count == 3
        summary[1].results[0].result.result == 13

        summary[2].group == "After Dec 2014"
        summary[2].count == 1
        summary[2].results[0].result.result == 10

    }

    def "the user summary report finds all user roles for a specified hub"() {
        setup:
        String userId = "u1"
        createDataForUserReport(userId)
        StringWriter stringWriter = new StringWriter()
        PrintWriter writer = new PrintWriter(stringWriter)

        when:
        service.userSummary("h1", writer)

        then:
        1 * userService.lookupUserDetails(userId) >> [userId:userId, displayName:"User 1", userName:"email"]
        CSVReader reader = new CSVReader(new StringReader(stringWriter.toString()))
        List<String[]> lines = reader.readAll().collect{Arrays.asList(it).collect{it?.trim()}}
        lines[0] == "User Id, Name, Email, Last Login, Role, Type, ID, Grant ID, External ID, Name, Access Role".split(", ")
        lines[1] == [userId, "User 1", "email", "Fri Jan 01 11:00:00 AEDT 2021", "caseManager", "Project", "p1", "Grant 1", "External 1", "Project 1", "admin"]
        lines[2] == ["", "", "", "", "", "Project", "p3", "Grant 3", "External 3", "Project 3", "admin"]
        lines[3] == ["", "", "", "", "", "Management Unit", "m1", "", "", "MU 1", "admin"]
        lines[4] == ["", "", "", "", "", "Program", "prg2", "", "", "Program 2", "admin"]

    }

    def createOutput(activityId, name, property, value) {
        return [activityId:activityId, name:name, data:[(property):value]]
    }

    def createOutput(activityId, name, Map data) {
        return [activityId:activityId, name:name, data:data]
    }

    /**
     * Inserts data into the User, UserPermission, Project, ManagementUnit and Program collections
     * for use by the user summary report.
     */
    private void createDataForUserReport(String userId) {
        String hubId = "h1"
        User user = new User(userId:userId)
        user.loginToHub(hubId, DateUtil.parse("2021-01-01T00:00:00Z"))
        user.save()

        new UserPermission(userId:userId, entityType:Hub.name, entityId:hubId, accessLevel: 'caseManager').save()

        Map projects = ["p1":hubId, "p2":"h2", "p3":"h1"]
        configureProjects(projects, userId)
        Map managementUnits = ["m1":hubId, "m2":"h2"]
        configureManagementUnits(managementUnits, userId)
        Map programs = ["prg1":"h2", "prg2":"h1"]
        configurePrograms(programs, userId)
    }

    private void configureProjects(Map spec, String userId) {
        int count = 0
        spec.each { k, v ->
            count++
            new Project(projectId:k, name:"Project $count", grantId:"Grant $count", externalId:"External $count", hubId:v).save()
            new UserPermission(userId:userId, entityType:Project.name, entityId:k, accessLevel: AccessLevel.admin).save()
        }
    }

    private void configureManagementUnits(Map spec, String userId) {
        int count = 0
        spec.each { k, v ->
            new ManagementUnit(managementUnitId:k, name:"MU "+ (++count), hubId:v).save()
            new UserPermission(userId:userId, entityType:ManagementUnit.name, entityId:k, accessLevel: AccessLevel.admin).save()
        }
    }

    private void configurePrograms(Map spec, String userId) {
        int count = 0
        spec.each { k, v ->
            new Program(programId:k, name:"Program "+ (++count), hubId:v).save()
            new UserPermission(userId:userId, entityType:Program.name, entityId:k, accessLevel: AccessLevel.admin).save()
        }
    }

}
