package au.org.ala.ecodata

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

import java.text.SimpleDateFormat

class OutputController {

    static dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ")

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json")
        model
    }

    def index() {
        log.debug "Total outputs = ${Output.count()}"
        render "${Output.count()} outputs"
    }

    def get(String id) {
        if (!id) {
            def list = []
            Output.list().each { act ->
                list << toMap(act)
            }
            list.sort {it.name}
            //log.debug list
            asJson([list: list])
        } else {
            def a = Output.findByOutputId(id)
            if (a) {
                asJson toMap(a)
            } else {
                render status:404, text: 'No such id'
            }
        }
    }

    def delete(String id) {
        def a = Output.findByOutputId(id)
        if (a) {
            a.delete()
            asJson([message: 'deleted'])
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }

    /*def update(String id) {
        def props = request.JSON
        def p = Output.findByOutputId(id)
        if (p) {
            props.each { k,v ->
                if (k != 'id') {
                    p[k] = v
                }
            }
            p.save()
            render (status: 200, text: 'updated')
        } else {
            def t = new Output(outputId: props.outputId)
            try {
                updateProperties(t, props)
                asJson([message: 'created', outputId: t.outputId])
            } catch (Exception e) {
                log.error "Error creating output - ${e.message}"
                render status:400, text: e.message
            }
        }
    }*/

    def update(String id) {
        def props = request.JSON
        log.debug props
        if (id) {
            def a = Output.findByOutputId(id)
            if (a) {
                try {
                    updateProperties(a, props)
                    asJson([message: 'updated'])
                } catch (Exception e) {
                    log.error "Error updating output ${id} - ${e.message}"
                    render status:400, text: e.message
                }
            } else {
                log.error "Error updating output - no such id ${id}"
                render status:404, text: 'No such id'
            }
        }
        else {
            // no id - create the resource
            def activity = Activity.findByActivityId(props.activityId)
            if (activity) {
                def o = new Output(activityId: activity.activityId, outputId: Identifiers.getNew(true,''))
                try {
                    updateProperties(o, props)
                    activity.addToOutputs(o)
                    //activity.outputs << o.outputId
                    activity.save()
                    asJson([message: 'created', outputId: o.outputId])
                } catch (Exception e) {
                    log.error "Error creating output for ${props.activityId} - ${e.message}"
                    render status:400, text: e.message
                }
            } else {
                log.error "Error creating output - no activity with id = ${props.activityId}"
                render status:400, text: 'No such activity'
            }
        }
    }

    /**
     * Updates all properties other than 'id' and converts date strings to BSON dates.
     *
     * Note that dates are assumed to be ISO8601 in UTC
     * @param o the output
     * @param props the properties to use
     */
    def updateProperties(o, props) {
        def domainDescriptor = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, 'au.org.ala.ecodata.Output')
        props.remove('id')
        props.each { k,v ->
            log.debug "updating ${k} to ${v}"
            /*
             * Checks the domain for properties of type Date and converts them.
             * Expects dates as strings in the form 'yyyy-MM-ddThh:mm:ssZ'. As indicated by the 'Z' these must be
             * UTC time. They are converted to java dates by forcing a zero time offset so that local timezone is
             * not used. All conversions to and from local time are the responsibility of the service consumer.
             */
            if (domainDescriptor.hasProperty(k) && domainDescriptor?.getPropertyByName(k)?.getType() == Date) {
                v = v ? dateFormat.parse(v.replace("Z", "+0000")) : null
            }
            o[k] = v
        }
        o.save(failOnError:true)
        if (o.hasErrors()) {
            o.errors.each { log.debug it }
            throw new Exception(o.errors[0] as String);
        }
    }

    def toMap = { act ->
        def dbo = act.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.findAll {k,v -> v != null}
    }

}
