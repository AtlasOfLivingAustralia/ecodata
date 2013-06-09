package au.org.ala.ecodata

import grails.converters.JSON

class ProjectController {

    def projectService, commonService

    static final BRIEF = 'brief'
    static final RICH = 'rich'

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json")
        model
    }

    static ignore = ['action','controller','id']

    def index() {
        render "${Project.count()} sites"
    }

    def list() {
        println 'brief = ' + params.brief
        def list = projectService.list(params.brief, params.includeDeleted)
        list.sort {it.name}
        render list as JSON
    }

    def get(String id) {
        def levelOfDetail = []
        if (params.brief || params.view == BRIEF) { levelOfDetail << BRIEF }
        if (params.view == RICH) { levelOfDetail << RICH }
        if (!id) {
            def list = projectService.list(levelOfDetail, params.includeDeleted)
            list.sort {it.name}
            asJson([list: list])
        } else {
            def p = Project.findByProjectId(id)
            if (p) {
                asJson projectService.toMap(p, levelOfDetail)
            } else {
                render (status: 404, text: 'No such id')
            }
        }
    }

    def delete(String id) {
        def p = Project.findByProjectId(id)
        if (p) {
            if (params.destroy) {
                p.delete()
            } else {
                p.status = 'deleted'
                p.save(flush: true)
            }
            render (status: 200, text: 'deleted')
        } else {
            render (status: 404, text: 'No such id')
        }
    }

    def resurrect(String id) {
        def p = Project.findByProjectId(id)
        if (p) {
            p.status = 'active'
            p.save(flush: true)
            render (status: 200, text: 'raised from the dead')
        } else {
            render (status: 404, text: 'No such id')
        }
    }

    /**
     * Update a project.
     *
     * @param id - identifies the resource
     * @return
     */
    def update(String id) {
        def props = request.JSON
        log.debug props
        def result
        def message
        if (id) {
            result = projectService.update(props,id)
            message = [message: 'updated']
        }
        else {
            result = projectService.create(props)
            message = [message: 'created', projectId: result.projectId]
        }
        if (result.status == 'ok') {
            asJson(message)
        } else {
            log.error result.error
            render status:400, text: result.error
        }
    }

    def loadTestData() {
        def testFile = new File('/data/fieldcapture/site-test-data.csv')
        testFile.eachCsvLine { tokens ->
            def projectName = tokens[2]
            def siteName = tokens[4]
            if (projectName && siteName.toLowerCase() != 'site_name') {
                Project p = Project.findByName(projectName)
                if (!p) {
                    p = new Project(name: tokens[2],
                            organisationName: tokens[1],
                            projectId: Identifiers.getNew(
                                    grailsApplication.config.ecodata.use.uuids, tokens[2]))
                    if (p.name == 'Bushbids') { p.description = bushbidsDescription }
                }
                Site s = Site.findByName(siteName)
                if (s) {
                    s.projectId = p.projectId
                    s.projectName = p.name
                    s.save()
                    p.addToSites(s)
                }
                p.save()
            }
        }
        render "${Project.count()} projects"
    }

    def bushbidsDescription = "Within the South Australian Murray-Darling Basin the northern Murray Plains and the southern parts of the Rangelands contain a concentration of remnant native woodlands on private land that are not well represented in conservation parks and reserves. The Woodland BushBids project will be implemented across this area. The eastern section of the Woodland BushBids project area contains large areas of woodland and mallee woodland where habitat quality could be improved through management. The western section contains smaller areas of priority woodland types in a largely cleared landscape. Protection and enhancement of native vegetation is necessary for the conservation of vegetation corridors through the region as well as management of woodland types such as Black Oak Woodlands. Management of native vegetation will also assist the protection of threatened species such as the Carpet Python, Regent Parrot, Bush Stone Curlew and the endangered Hopbush, Dodonea subglandulifera and will provide habitat for significant species such as the Southern Hairy Nosed Wombat. Woodland BushBids will assist landholders to provide management services to protect and enhance native vegetation quality."
}
