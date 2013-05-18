package au.org.ala.ecodata

class OutputService {

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param output an Output instance
     * @return map of properties
     */
    def toMap(output) {
        def dbo = output.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        mapOfProperties.findAll {k,v -> v != null}
    }

    def loadAll(list) {
        list.each {
            load(it)
        }
    }

    def load(props) {
        def o = new Output(props)

    }
}
