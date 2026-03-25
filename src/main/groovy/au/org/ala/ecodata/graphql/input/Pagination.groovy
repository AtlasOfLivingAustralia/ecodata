package au.org.ala.ecodata.graphql.input

import groovy.transform.CompileStatic

@CompileStatic
class Pagination {
    private static final int DEFAULT_MAX = 10
    private static final int MAXIMUM_RESULTS = 100
    int max = DEFAULT_MAX
    int page = 0
    String sort = "dateCreated"
    String order = "asc"

    /** Don't allow requests for more than 100 results */
    void setMax(int max) {
        this.max = Math.min(MAXIMUM_RESULTS, max)
    }

    static Map asMap(Pagination pagination) {
        pagination = pagination ?: new Pagination() // Use defaults if it's null
        pagination.properties
    }
}
