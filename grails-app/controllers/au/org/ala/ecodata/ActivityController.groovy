package au.org.ala.ecodata

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

import java.text.SimpleDateFormat

class ActivityController {

    static dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ")

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
        log.debug props
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

    /**
     * Updates all properties other than 'id' and converts date strings to BSON dates.
     *
     * Note that dates are assumed to be ISO8601 in UTC
     * @param a the activity
     * @param props the properties to use
     */
    def updateProperties(Activity a, props) {
        def domainDescriptor = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, 'au.org.ala.ecodata.Activity')
        props.remove('id')
        props.each { k,v ->
            /*
             * Checks the domain for properties of type Date and converts them.
             * Expects dates as strings in the form 'yyyy-MM-ddThh:mm:ssZ'. As indicated by the 'Z' these must be
             * UTC time. They are converted to java dates by forcing a zero time offset so that local timezone is
             * not used. All conversions to and from local time is the responsibility of the service consumer.
             */
            if (domainDescriptor.hasProperty(k) && domainDescriptor.getPropertyByName(k).getType() == Date) {
                v = v ? dateFormat.parse(v.replace("Z", "+0000")) : null
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
        mapOfProperties.remove("outputs")
        mapOfProperties.outputs = act.outputs.collect {
            [outputId: it.outputId,
             assessmentDate: it.assessmentDate,
             collector: it.collector]
        }
        /*mapOfProperties.outputs = outputList.collect {
            def o = Output.findByOutputId(it)
            if (o) {
                [outputId: o.outputId,
                 assessmentDate: o.assessmentDate,
                 collector: o.collector]
            } else {
                [:]
            }
        }*/
        mapOfProperties.findAll {k,v -> v != null}
    }

    def loadTestData() {
    }
}
