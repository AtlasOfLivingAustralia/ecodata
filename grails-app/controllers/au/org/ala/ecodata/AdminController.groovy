package au.org.ala.ecodata

import com.mongodb.util.JSON
import grails.util.Environment
import groovy.json.JsonBuilder
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

class AdminController {

    def projectService, outputService, activityService, siteService

    def index() {}

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
        ['Project','Site','Activity','Output'].each { collection ->
            def f = new File("/data/ecodata/${collection}s.json")
            f.createNewFile()
            def domainClass = grailsApplication.getDomainClass('au.org.ala.ecodata.'+collection).newInstance()
            def serviceClass = grailsApplication.getServiceClass("au.org.ala.ecodata.${collection}Service").newInstance()
            def instances = []
            domainClass.list().each {
                instances << serviceClass.toMap(it)
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
        [/*'Project','Site','Activity',*/'Output'].each { collection ->
            def f = new File("/data/ecodata/${collection}s.json")
            def serviceClass = grailsApplication.getServiceClass("au.org.ala.ecodata.${collection}Service").newInstance()
            serviceClass.loadAll(JSON.parse(f.text))
        }
    }
}
