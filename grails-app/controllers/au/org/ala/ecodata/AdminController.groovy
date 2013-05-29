package au.org.ala.ecodata

import com.mongodb.util.JSON
import grails.util.Environment
import groovy.json.JsonBuilder
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class AdminController {

    def index() {}

    def outputService, activityService, siteService, projectService

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json")
        model
    }

    def reloadConfig = {
        // clear any cached external config
        //clearCache()

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

    /**
     * Writes all data to files as JSON.
     */
    def dump() {
        ['project','site','activity','output'].each { collection ->
            def f = new File("/data/ecodata/${collection}s.json")
            f.createNewFile()
            def instances = []
            /* this pattern does not always inject dependencies correctly
            def domainClass = grailsApplication.getDomainClass('au.org.ala.ecodata.'+collection).newInstance()
            def serviceClass = grailsApplication.getServiceClass("au.org.ala.ecodata.${collection}Service").newInstance()
            domainClass.list().each {
                instances << serviceClass.toMap(it)
            }*/
            switch (collection) {
                case 'output':
                    Output.list().each { instances << outputService.toMap(it) }
                    break
                case 'activity':
                    Activity.list().each { instances << activityService.toMap(it) }
                    break
                case 'site':
                    Site.list().each { instances << siteService.toMap(it) }
                    break
                case 'project':
                    Project.list().each { instances << projectService.toMap(it) }
                    break
            }
            def pj = new JsonBuilder( instances ).toPrettyString()
            f.withWriter( 'UTF-8' ) { it << pj }
        }
        render 'done'
    }

    /**
     * Imports all data from files in the format written by dump().
     */
    def load() {
        if (params.drop) {
            dropDB()
        }
        ['project','site','activity','output'].each { collection ->
            def f = new File("/data/ecodata/${collection}s.json")
            switch (collection) {
                case 'output': outputService.loadAll(JSON.parse(f.text)); break
                case 'activity': activityService.loadAll(JSON.parse(f.text)); break
                case 'site': siteService.loadAll(JSON.parse(f.text)); break
                case 'project': projectService.loadAll(JSON.parse(f.text)); break
            }
        }
        forward action: 'count'
    }

    def count() {
        def res = [
            projects: Project.collection.count(),
            sites: Site.collection.count(),
            activities: Activity.collection.count(),
            outputs: Output.collection.count()
        ]
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
}
