package au.org.ala.ecodata.graphql.input

import au.org.ala.ecodata.graphql.enums.YesNo
import graphql.schema.DataFetchingEnvironment

class SearchMeritProjects  extends SearchProjects {

    List<String> meritProjectID
    List<String> assetsAddressed

    List<YesNo> grantManagerNominatedProject
    List<String> managementUnit
    List<String> marineRegion
    List<String> primaryOutcome
    ReportQuery reports
    List<String> secondaryOutcomes
    List<String> tags
    List<String> userNominatedProject


    String getQuery() {

    }

    String getFacetFilterList() {

    }

    String getLastUpdatedQueryString(DataFetchingEnvironment env) {
        boolean lastUpdated = env.getSelectionSet().containsAnyOf(lastUpdated, "reports.lastUpdated", "sites.lastUpdated")
        String dateQuery = "lastUpdated > ${fromDate?.format('yyyy-MM-dd') ?: '1970-01-01'}"
        if (env.getSelectionSet().contains('sites')) {
            dateQuery += " AND 'sites.lastUpdated' > ${fromDate?.format('yyyy-MM-dd') ?: '1970-01-01'}"
        }
        if (env.getSelectionSet().contains('reports')) {
            dateQuery += " AND 'activities.lastUpdated' > ${fromDate?.format('yyyy-MM-dd') ?: '1970-01-01'}"
        }
        dateQuery
    }

    Integer getMax(DataFetchingEnvironment env) {
        // If the user isn't querying associations we should allow a greater max.
        Math.min(max, 50)
    }
}
