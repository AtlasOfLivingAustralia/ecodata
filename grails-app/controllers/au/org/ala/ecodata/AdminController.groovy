package au.org.ala.ecodata
import com.mongodb.util.JSON
import grails.util.Environment
import groovy.json.JsonBuilder
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class AdminController {

    def outputService, activityService, siteService, projectService, authService,
        commonService, cacheService, metadataService, elasticSearchService
    def beforeInterceptor = [action:this.&auth, only:['index','tools','settings','audit']]

    /**
     * Triggered by beforeInterceptor, this restricts access to specified (only) actions to ROLE_ADMIN
     * users.
     *
     * @return
     */
    private auth() {
        if (!authService.userInRole(grailsApplication.config.security.cas.adminRole)) {
            flash.message = "You are not authorised to access the page: Administration."
            redirect(uri: "/")
            false
        } else {
            true
        }
    }

    def index() {}
    def tools() {}
    def users() {
        def userList = authService.getAllUserNameList()
        [ userNamesList: userList ]
    }

    def settings() {
        def settings = [
                [key:'app.external.model.dir', value: grailsApplication.config.app.external.model.dir,
                        comment: 'location of the application meta-models such as the list of activities and ' +
                                'the output data models'],
                [key:'app.dump.location', value: grailsApplication.config.app.dump.location,
                        comment: 'directory where DB dump files will be created']
        ]
        def config = grailsApplication.config.flatten()
        ['ala.baseURL','grails.serverURL','grails.config.locations','collectory.baseURL',
                'headerAndFooter.baseURL','ecodata.use.uuids'
        ].each {
            settings << [key: it, value: config[it], comment: '']
        }
        [settings: settings]
    }

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json; charset=\"UTF-8\"")
        model
    }

    def reloadConfig = {
        // clear any cached external config
        cacheService.clear()

        // reload system config
        def resolver = new PathMatchingResourcePatternResolver()
        def resource = resolver.getResource(grailsApplication.config.reloadable.cfgs[0])
        def stream = null

        try {
            stream = resource.getInputStream()
            ConfigSlurper configSlurper = new ConfigSlurper(Environment.current.name)
            if(resource.filename.endsWith('.groovy')) {
                def newConfig = configSlurper.parse(stream.text)
                grailsApplication.getConfig().merge(newConfig)
            }
            else if(resource.filename.endsWith('.properties')) {
                def props = new Properties()
                props.load(stream)
                def newConfig = configSlurper.parse(props)
                grailsApplication.getConfig().merge(newConfig)
            }
            String res = "<ul>"
            grailsApplication.config.each { key, value ->
                if (value instanceof Map) {
                    res += "<p>" + key + "</p>"
                    res += "<ul>"
                    value.each { k1, v1 ->
                        res += "<li>" + k1 + " = " + v1 + "</li>"
                    }
                    res += "</ul>"
                }
                else {
                    res += "<li>${key} = ${value}</li>"
                }
            }
            render res + "</ul>"
        }
        catch (FileNotFoundException fnf) {
            println "No external config to reload configuration. Looking for ${grailsApplication.config.reloadable.cfgs[0]}"
            render "No external config to reload configuration. Looking for ${grailsApplication.config.reloadable.cfgs[0]}"
        }
        catch (Exception gre) {
            println "Unable to reload configuration. Please correct problem and try again: " + gre.getMessage()
            render "Unable to reload configuration - " + gre.getMessage()
        }
        finally {
            stream?.close()
        }
    }

    def getBare(String entity, String id) {
        def map = [:]
        switch (entity) {
            case 'activity': map = commonService.toBareMap(Activity.findByActivityId(id)); break
            case 'assessment': map = commonService.toBareMap(Activity.findByActivityId(id)); break
            case 'project': map = commonService.toBareMap(Project.findByProjectId(id)); break
            case 'site': map = commonService.toBareMap(Site.findBySiteId(id)); break
            case 'output': map = commonService.toBareMap(Output.findByOutputId(id)); break
        }
        asJson map
    }

    def showCache() {
        render cacheService.cache
    }

    def test() {
        render metadataService.getOutputModel('Feral Animal Abundance Score')
    }

    /**
     * Writes all data to files as JSON.
     */
    def dump() {
        ['project','site','activity','output'].each { collection ->
            def f = new File(grailsApplication.config.app.dump.location + "${collection}s.json")
            f.createNewFile()
            def instances = []
            switch (collection) {
                case 'output':
                    Output.list().each { instances << commonService.toBareMap(it) }
                    break
                case 'activity':
                    Activity.list().each { instances << commonService.toBareMap(it) }
                    break
                case 'site':
                    Site.list().each { instances << commonService.toBareMap(it) }
                    break
                case 'project':
                    Project.list().each { instances << commonService.toBareMap(it) }
                    break
            }
            def pj = new JsonBuilder( instances ).toPrettyString()
            f.withWriter( 'UTF-8' ) { it << pj }
        }
        flash.message = "Database dumped to ${grailsApplication.config.app.dump.location}."
        render 'done'
    }

    /**
     * Re-index all docs with ElasticSearch
     */
    def reIndexAll() {
        def resp = elasticSearchService.indexAll()
        flash.message = "Search index re-indexed - ${resp?.size()} docs"
        render "Indexing done"
    }

    def clearMetadataCache() {
        // clear any cached external config
        cacheService.clear()
        flash.message = "Metadata cache cleared."
        render 'done'
    }

    /**
     * Imports all data from files in the format written by dump().
     */
    def load() {
        if (params.drop) {
            dropDB()
        }
        def errorMsg
        ['project','site','activity','output'].each { collection ->
            try {
                elasticSearchService.indexingTempInactive = true // turn off search indexing
                def f = new File(grailsApplication.config.app.dump.location + "${collection}s.json")
                switch (collection) {
                    case 'output': outputService.loadAll(JSON.parse(f.text)); break
                    case 'activity': activityService.loadAll(JSON.parse(f.text)); break
                    case 'site': siteService.loadAll(JSON.parse(f.text)); break
                    case 'project': projectService.loadAll(JSON.parse(f.text)); break
                }
            } catch (Exception e) {
                errorMsg = "Load error: ${e}"
                log.error errorMsg, e
            } finally {
                log.debug "Turning elasticSearch indexing ON"
                elasticSearchService.indexingTempInactive = false // turn on search indexing
            }
        }
        elasticSearchService.indexAll()
        flash.message = errorMsg?:"DB reloaded "
        forward action: 'count'
    }

    def count() {
        def res = [
            projects: Project.collection.count(),
            sites: Site.collection.count(),
            activities: Activity.collection.count(),
            outputs: Output.collection.count()
        ]
        flash.message += res
        render res
    }

    def drop() {
        dropDB()
        forward action: 'count'
    }

    def dropDB() {
        Output.collection.drop()
        Activity.collection.drop()
        Site.collection.drop()
        Project.collection.drop()
    }

    def audit() { }
    def auditMessagesByEntity() { }
    def auditMessagesByProject() { }

}
