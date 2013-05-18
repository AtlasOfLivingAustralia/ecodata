package au.org.ala.ecodata

class SiteService {

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param site a Site instance
     * @return map of properties
     */
    def toMap(site) {
        def dbo = site.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.remove("activites")
        mapOfProperties.activities = site.activities.collect {
            def a = [activityId: it.activityId, siteId: it.siteId,
                    type: it.type,
                    startDate: it.startDate, endDate: it.endDate]
            a
        }

        mapOfProperties.findAll {k,v -> v != null}
    }

}
