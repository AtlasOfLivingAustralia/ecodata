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
            organisationId: "organisationId",
            portfolio: "portfolio"
    ]

    static Map namedParameterToDatabaseMap = [
            projectId:"projectId",
            meritProjectID:"grantId",
            programId: "programId",
            program: "associatedProgram",
            subProgram: "associatedSubProgram",
            managementUnitId: "managementUnitId",
            organisationId: "organisationId",
            portfolio: "portfolio",
            status:"status"
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
    List<String> portfolio
    String status

    DateRange startDate
    DateRange endDate
    DateTimeRange lastUpdated

    String query = "*:*"
    List<String> facetFilters

    Pagination pagination

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

        params.putAll(Pagination.asMap(pagination))
        params
    }

    Map buildDatabaseQueryParameters() {
        Map databaseQueryParams = [:]
        namedParameterToDatabaseMap.each { String property, String databaseProperty ->
            if (this[property]) {
                databaseQueryParams[databaseProperty] = this[property]
            }
        }
        databaseQueryParams
    }

    List buildFacetFilters() {
        List filters = new ArrayList(facetFilters?:[])

        // Status is a special case because the values are capitalized in
        // elasticsearch but not the database.
        if (status) {
            filters << "status:${status.capitalize()}"
        }
        namedParameterToFacetNameMap.each { String property, String facetName ->
            if (this[property]) {
                if (this[property] instanceof List) {
                    this[property].each {
                        filters << "${facetName}:${it}"
                    }
                }
                else {
                    filters << "${facetName}:${this[property]}"
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

    private String buildLastUpdatedQueryFilter(DateTimeRange lastUpdated) {
        List lastUpdatedFields = ["lastUpdated"]
        DataFetchingFieldSelectionSet selectionSet = environment.getSelectionSet()
        if (selectionSet.contains("results/reports")) {
            lastUpdatedFields << "activities.lastUpdated" // An activity will be updated when a report is edited/submitted/approved.
        }
        if ( selectionSet.contains("results/sites")) {
            lastUpdatedFields << "sites.lastUpdated"
        }
        if (selectionSet.contains("results/documents")) {
            lastUpdatedFields << "documents.lastUpdated"
        }

        String lastUpdatedQuery = lastUpdatedFields.collect {
            buildDateTimeRangeFilterQuery(it, lastUpdated)
        }.join(" OR ")

        "(" + lastUpdatedQuery + ")"
    }

    private static String buildDateRangeFilterQuery(String field, DateRange dateRange) {
        String from = dateRange.from ? DateUtil.formatAsDisplayDate(dateRange.from): "*"
        String to = dateRange.to ? DateUtil.formatAsDisplayDate(dateRange.to): "*"

        "${field}:[${from} TO ${to}]"
    }

    private static String buildDateTimeRangeFilterQuery(String field, DateTimeRange dateRange) {
        String from = dateRange.from ? DateUtil.format(dateRange.from): "*"
        String to = dateRange.to ? DateUtil.format(dateRange.to): "*"

        "${field}:[${from} TO ${to}]"
    }

}
