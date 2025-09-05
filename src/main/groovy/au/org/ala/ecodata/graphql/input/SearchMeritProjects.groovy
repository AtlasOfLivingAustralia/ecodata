package au.org.ala.ecodata.graphql.input

import au.org.ala.ecodata.DateUtil
import grails.validation.Validateable
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingFieldSelectionSet

class SearchMeritProjects implements Validateable {

    static Map meritParams = [hubFq:"isMERIT:true", query:"docType:project", max:20]
    static Map namedParameterToFacetNameMap = [
            projectId: "projectId",
            meritProjectID: "grantId",
            programId: "programId",
            program: "associatedProgramFacet",
            subProgram: "associatedSubProgramFacet",
            managementUnit: "muFacet",
            managementUnitId: "managementUnitId",
            organisation: "organisationFacet",
            organisationId: "organisationId"
    ]

    ReportQuery reports

    List<String> projectId
    List<String> meritProjectID
    List<String> programId
    List<String> program
    List<String> subProgram
    List<String> managementUnit
    List<String> managementUnitId
    List<String> organisation
    List<String> organisationId

    DateRange startDate
    DateRange endDate
    DateRange lastUpdated

    String query = "*:*"
    List<String> facetFilters

    // Pagination options
    int max = 10
    int page = 1
    String sort = "dateCreated"
    String order = "desc"

    private DataFetchingEnvironment environment

    SearchMeritProjects(DataFetchingEnvironment environment) {
        this.environment = environment
    }

    String getQuery() {
        query
    }

    Map buildESQueryParameters() {
        Map params = new HashMap(meritParams)

        params.fq = buildFacetFilters()

        params.putAll(getSortAndPagingParams())
        params
    }

    final int MAX_PAGE_SIZE = 50

    private Map getSortAndPagingParams() {
        Map params = [:]
        int max = Math.min(max, MAX_PAGE_SIZE)
        params["max"] = max
        int page = Math.max(1, page)
        params["offset"] = max*(page-1)

        params["sort"] = sort
        params["order"] = order
        params
    }


    List buildFacetFilters() {
        List filters = new ArrayList(facetFilters?:[])

        Map namedParams = this.properties
        namedParams.each { String property, def value ->
            String facetName = namedParameterToFacetNameMap[property]

            if (facetName && value) {
                value.each {
                    filters << "${facetName}:${it}"
                }

            }
        }

        String dateFilter = buildDateQueryFilter()
        if (dateFilter) {
            filters << dateFilter
        }
        filters
    }

    String buildDateQueryFilter() {
        List dateQueries = []
        String filterQuery = null
        if (startDate) {
            dateQueries << buildDateRangeFilterQuery("plannedStartDate", startDate)
        }
        if (endDate) {
            dateQueries << buildDateRangeFilterQuery("plannedEndDate", endDate)
        }
        if (lastUpdated) {
            dateQueries << buildLastUpdatedQueryFilter(lastUpdated)
        }
        if (dateQueries) {
            filterQuery = "_query:(" + dateQueries.join(" AND ") + ")"
        }
        filterQuery

    }

    private String buildLastUpdatedQueryFilter(DateRange lastUpdated) {
        List lastUpdatedFields = ["lastUpdated"]
        DataFetchingFieldSelectionSet selectionSet = environment.getSelectionSet()
        if (selectionSet.contains("results/reports")) {
            lastUpdatedFields << "activities.lastUpdated" // An activity will be updated when a report is edited/submitted/approved.
        }
        if ( selectionSet.contains("results/sites")) {
            lastUpdatedFields << "sites.lastUpdated"
        }

        String lastUpdatedQuery = lastUpdatedFields.collect {
            buildDateRangeFilterQuery(it, lastUpdated)
        }.join(" OR ")

        "(" + lastUpdatedQuery + ")"
    }

    private static String buildDateRangeFilterQuery(String field, DateRange dateRange) {
        String from = dateRange.from ? DateUtil.formatAsDisplayDate(dateRange.from): "*"
        String to = dateRange.to ? DateUtil.formatAsDisplayDate(dateRange.to): "*"

        "${field}:[${from} TO ${to}]"
    }

}
