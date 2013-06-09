package au.org.ala.ecodata

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler

import java.text.SimpleDateFormat

class CommonService {

    //static transactional = false
    def grailsApplication

    static dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ")

    /**
     * Updates all properties other than 'id' and converts date strings to BSON dates.
     *
     * Note that dates are assumed to be ISO8601 in UTC with no millisecs
     *
     * Booleans must be handled explicitly because the JSON string "false" will by truthy if just
     *  assigned to a boolean property.
     *
     * @param o the domain instance
     * @param props the properties to use
     */
    def updateProperties(o, props) {
        assert grailsApplication
        def domainDescriptor = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE,
                o.getClass().name)
        props.remove('id')
        props.remove('api_key')  // don't ever let this be stored in public data
        props.remove('lastUpdated') // in case we are loading from dumped data
        props.each { k,v ->
            log.debug "updating ${k} to ${v}"
            /*
             * Checks the domain for properties of type Date and converts them.
             * Expects dates as strings in the form 'yyyy-MM-ddThh:mm:ssZ'. As indicated by the 'Z' these must be
             * UTC time. They are converted to java dates by forcing a zero time offset so that local timezone is
             * not used. All conversions to and from local time are the responsibility of the service consumer.
             */
            if (v instanceof String && domainDescriptor.hasProperty(k) && domainDescriptor?.getPropertyByName(k)?.getType() == Date) {
                v = v ? dateFormat.parse(v.replace("Z", "+0000")) : null
            }
            if (v == "false") {
                v = false
            }
            o[k] = v
        }
        // always flush the update so that that any exceptions are caught before the service returns
        o.save(flush:true,failOnError:true)
        if (o.hasErrors()) {
            log.error("has errors:")
            o.errors.each { log.error it }
            throw new Exception(o.errors[0] as String);
        }
    }

    /**
     * Converts the domain object into a map of properties with no additions.
     * @param o a domain instance
     * @return map of properties
     */
    def toBareMap(o) {
        def dbo = o.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.findAll {k,v -> v != null}
    }

}
