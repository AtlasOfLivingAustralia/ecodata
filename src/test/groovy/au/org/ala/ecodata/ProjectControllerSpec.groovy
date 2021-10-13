package au.org.ala.ecodata

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import org.apache.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll

import static au.org.ala.ecodata.ElasticIndex.HOMEPAGE_INDEX

class ProjectControllerSpec extends Specification implements ControllerUnitTest<ProjectController>, DomainUnitTest<Project>{

    ProjectService projectService
    SiteService siteService
    ElasticSearchService elasticSearchService

    Class[] getDomainClassesToMock() {
        [Project, Site]
    }

    def setup() {
        projectService = Mock(ProjectService)
        siteService = Mock(SiteService)
        controller.projectService = projectService
        controller.siteService = siteService
        controller.elasticSearchService = elasticSearchService
    }


    def "clients can request different views of project data according to their needs"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1")
        p.save()

        when:
        controller.get(projectId)

        then:
        1 * projectService.toMap(p, [], false, null) >> [projectId:projectId, name:"Project 1"]

        when:
        params.view = 'all'
        controller.get(projectId)

        then:
        1 * projectService.toMap(p, ProjectService.ALL, false, null) >> [projectId:projectId, name:"Project 1", activities: [activityId: "activityId1"]]
    }

    void "index"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        controller.index()

        then:
        response.status == HttpStatus.SC_OK
        response.text == '1 sites'
    }

    void "list"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        controller.list()

        then:
        1 * projectService.list(null, null, null) >> [p]
        response.status == HttpStatus.SC_OK
        response.getJson().list.size() == 1
        response.getJson()[0].projectId == 'p1'
    }

    void "promoted list"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        controller.promoted()

        then:
        1 * projectService.promoted() >> [p]
        response.status == HttpStatus.SC_OK
        response.getJson().list.size() == 1
        response.getJson()[0].projectId == 'p1'
    }

    void "get by id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = projectId
        controller.get()

        then:
        1 * projectService.toMap(p, [], false, null) >> [projectId:projectId, name:"Project 1"]
        response.status == HttpStatus.SC_OK
        response.getJson().name == "Project 1"
        response.getJson().projectId == projectId
    }

    void "get by id - without id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        controller.get()

        then:
        1 * projectService.list([], false, false) >> [p]
        response.status == HttpStatus.SC_OK
        response.getJson().list.size() == 1
        response.getJson().list[0].projectId == projectId
    }

    void "get by id - invalid id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = 'p2'
        controller.get()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "get project services with targets"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = projectId
        controller.getProjectServicesWithTargets()

        then:
        1 * projectService.getProjectServicesWithTargets(projectId) >> [p]
        response.status == HttpStatus.SC_OK
        response.getJson()[0].name == "Project 1"
        response.getJson()[0].projectId == projectId
    }

    void "delete by id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = projectId
        controller.delete()

        then:
        1 * projectService.delete(projectId, false) >> [status: 'ok']
        response.status == HttpStatus.SC_OK
        response.text == "deleted"
    }

    void "delete by id - error"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = projectId
        controller.delete()

        then:
        1 * projectService.delete(projectId, false) >> [status: 'error', error: 'error']
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == "error"
    }

    void "delete by id - invalid id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = 'p2'
        controller.delete()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "resurrect by id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = projectId
        controller.resurrect()

        then:
        response.status == HttpStatus.SC_OK
        response.text == "raised from the dead"
    }

    void "resurrect by id - invalid id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = 'p2'
        controller.resurrect()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "delete sites by id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)
        Map props = [
                siteIds:['1']
        ]

        when:
        params.id = projectId
        request.json = props
        controller.deleteSites()

        then:
        1 * siteService.deleteSitesFromProject(projectId, ['1'], false) >> [status: 'ok']
        response.status == HttpStatus.SC_OK
        response.getJson().status == "ok"
    }

    void "delete sites by id - invalid id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)
        Map props = [
                siteIds:[]
        ]

        when:
        params.id = 'p2'
        request.json = props
        controller.deleteSites()

        then:
        1 * siteService.deleteSitesFromProject('p2', [], false) >> [status: 'error', error: 'error']
        response.status == HttpStatus.SC_INTERNAL_SERVER_ERROR
        response.text == 'No such id'
    }

    void "update sites by id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)
        Site site =new Site(name: 'site 1', siteId: 's1', projects: [projectId]).save(flush:true, failOnError: true)
        Map props = [ sites:[] ]

        when:
        params.id = projectId
        request.json = props
        controller.updateSites()

        then:
        response.status == HttpStatus.SC_OK
    }

    void "Create project"() {
        setup:
        String projectId = 'p1'
        Map props = [
                projectId:projectId, name:"Project 1"
        ]

        when:
        request.json = props
        controller.update()

        then:
        1 * projectService.create(request.JSON) >> [status: 'ok', projectId: projectId]

        response.status == HttpStatus.SC_OK
        response.getJson().message == 'created'
        response.getJson().projectId == 'p1'
    }

    void "Update project"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)
        Map props = [
                name:"Project 1 updated"
        ]

        when:
        request.json = props
        controller.update(projectId)

        then:
        1 * projectService.update(request.JSON, projectId) >> [status: 'ok', projectId: projectId]

        response.status == HttpStatus.SC_OK
        response.getJson().message == 'updated'
    }

    void "Update project - error"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)
        Map props = [
                name:"Project 1 updated"
        ]

        when:
        request.json = props
        controller.update(projectId)

        then:
        1 * projectService.update(request.JSON, projectId) >> [status:'error', error:"error"]

        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'error'
    }

    void "Download project data"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = projectId
        controller.downloadProjectData()

        then:
        1 * projectService.get(projectId, ProjectService.ALL) >> [p]

        response.status == HttpStatus.SC_OK
        response.getJson().size() == 1
        response.getJson()[0].projectId[0] == projectId
    }

    void "Download project data - invaid id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = 'p2'
        controller.downloadProjectData()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "Get Default Facets"() {

        when:
        controller.getDefaultFacets()

        then:
        response.status == HttpStatus.SC_OK
        response.text != null
    }

    void "Get data collection whitelist"() {

        when:
        controller.getDataCollectionWhiteList()

        then:
        response.status == HttpStatus.SC_OK
        response.text != null
    }

    void "Get countries"() {

        when:
        controller.getCountries()

        then:
        response.status == HttpStatus.SC_OK
        response.text != null
    }

    void "Get UN regions"() {

        when:
        controller.getUNRegions()

        then:
        response.status == HttpStatus.SC_OK
        response.text != null
    }

    void "Get eco science types"() {

        when:
        controller.getEcoScienceTypes()

        then:
        response.status == HttpStatus.SC_OK
        response.text != null
    }

    void "Get science types"() {

        when:
        controller.getScienceTypes()

        then:
        response.status == HttpStatus.SC_OK
        response.text != null
    }

    void "import Projects From SciStarter"() {

        when:
        controller.importProjectsFromSciStarter()

        then:
        1  * projectService.importProjectsFromSciStarter() >> 1
        response.status == HttpStatus.SC_OK
        response.getJson().count == 1
    }

    void "Download project Metrics - without params"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = projectId
        controller.projectMetrics()

        then:
        1 * projectService.projectMetrics(projectId, false, false, [], null, true) >> [projectId:projectId, name:"Project 1"]

        response.status == HttpStatus.SC_OK
        response.getJson().name == 'Project 1'
        response.getJson().projectId == projectId
    }

    void "Download project Metrics - with params"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)
        Map props = [
                approvedOnly:true, targetsOnly: true, includeTargets: true, scoreIds: ['1'], aggregationConfig: [:]
        ]

        when:
        request.json = props
        params.id = projectId
        controller.projectMetrics()

        then:
        1 * projectService.projectMetrics(projectId, false, true, ['1'], [:], true) >> [projectId:projectId, name:"Project 1"]

        response.status == HttpStatus.SC_OK
        response.getJson().name == 'Project 1'
        response.getJson().projectId == projectId
    }

    void "Download project Metrics - invalid id"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = 'p2'
        controller.projectMetrics()

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.text == 'No such id'
    }

    void "Search"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)
        Map props = [:]

        when:
        request.json = props
        controller.search()

        then:
        1 * projectService.search(props, ProjectService.BRIEF) >> [p]
        response.status == HttpStatus.SC_OK
        response.getJson().projects.size() == 1
        response.getJson().projects[0].projectId == projectId
    }

    void "Find by association"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.id = projectId
        params.entity = 'Project'
        controller.findByAssociation()

        then:
        1 * projectService.findAllByAssociation('ProjectId', projectId, ProjectService.BRIEF) >> [p]
        response.status == HttpStatus.SC_OK
        response.getJson().projects.size() == 1
        response.getJson().projects[0].projectId == projectId
        response.getJson().count == 1
    }

    void "Find by name"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        params.projectName = 'Project 1'
        controller.findByName()

        then:
        1 * projectService.findByName('Project 1') >> [p]
        response.status == HttpStatus.SC_OK
        response.getJson().projectId[0] == projectId
    }

    void "Find by name - without name"() {
        setup:
        String projectId = 'p1'
        Project p = new Project(projectId:projectId, name:"Project 1").save(flush:true, failOnError: true)

        when:
        controller.findByName()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == 'projectName is a required parameter'
    }

    void "eSearch - error"(def max, def offset, String sort, String order, String error) {

        when:
        params.max = max
        params.offset = offset
        params.sort = sort
        params.order = order
        controller.eSearch()

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.text == error

        where:
        max | offset | sort       | order  | error
        'a' | 'a'    | 'test'     | 'test' | 'Invalid max parameter.'
        '5' | 'a'    | 'test'     | 'test' | 'Invalid offset parameter.'
        '5' | '1'    | 'test'     | 'test' | 'Invalid sort parameter (Accepted values: nameSort, _score, organisationSort ).'
        '5' | '1'    |  null      | 'test' | 'Invalid order parameter (Accepted values: ASC, DESC ).'
    }
}