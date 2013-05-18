package au.org.ala.ecodata

class ProjectService {

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param prj a Project instance
     * @return map of properties
     */
    def toMap(prj) {
        def dbo = prj.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.remove("sites")
        mapOfProperties.sites = prj.sites.collect {
            def s = [siteId: it.siteId, name: it.name, location: it.location]
            s
        }
        // remove nulls
        mapOfProperties.findAll {k,v -> v != null}
    }

}
