package au.org.ala.ecodata

class ActivityService {


    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param act an Activity instance
     * @return map of properties
     */
    def toMap(act) {
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
        mapOfProperties.findAll {k,v -> v != null}
    }

}
