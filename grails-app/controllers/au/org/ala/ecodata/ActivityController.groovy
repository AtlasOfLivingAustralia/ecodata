package au.org.ala.ecodata

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

import java.text.SimpleDateFormat

class ActivityController {

    static dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'")

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json")
        model
    }

    def index() {
        log.debug "Total activities = ${Activity.count()}"
        render "${Activity.count()} activities"
    }

    def get(String id) {
        if (!id) {
            def list = []
            Activity.list().each { act ->
                list << toMap(act)
            }
            list.sort {it.name}
            //log.debug list
            asJson([list: list])
        } else {
            def a = Activity.findByActivityId(id)
            if (a) {
                asJson toMap(a)
            } else {
                render status:404, text: 'No such id'
            }
        }
    }

    def delete(String id) {
        def a = Activity.findByActivityId(id)
        if (a) {
            a.delete()
            asJson([message: 'deleted'])
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }

    def update(String id) {
        def props = request.JSON
        println props
        if (id) {
            def a = Activity.findByActivityId(id)
            if (a) {
                try {
                    updateProperties(a, props)
                    asJson([message: 'updated'])
                } catch (Exception e) {
                    log.error "Error updating activity ${id} - ${e.message}"
                    render status:400, text: e.message
                }
            } else {
                log.error "Error updating activity - no such id ${id}"
                render status:404, text: 'No such id'
            }
        }
        else {
            // no id - create the resource
            def site = Site.findBySiteId(props.siteId)
            if (site) {
                def a = new Activity(siteId: site.siteId, activityId: Identifiers.getNew(true,''))
                try {
                    updateProperties(a, props)
                    site.addToActivities(a)
                    site.save()
                    asJson([message: 'created', activityId: a.activityId])
                } catch (Exception e) {
                    log.error "Error creating activity ${id} - ${e.message}"
                    render status:400, text: e.message
                }
            } else {
                log.error "Error creating activity - no site with id = ${id}"
                render status:400, text: 'No such site'
            }
        }
    }

    def updateProperties(Activity a, props) {
        def domainDescriptor = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, 'au.org.ala.ecodata.Activity')
        props.remove('id')
        props.each { k,v ->
            if (domainDescriptor?.getPropertyByName(k)?.getType() == Date) {
                v = v ? dateFormat.parse(v as String) : null
            }
            a[k] = v
        }
        a.save()
    }

    def toMap = { act ->
        def dbo = act.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.findAll {k,v -> v != null}
    }

    def loadTestData() {
    }
}
