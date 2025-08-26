package au.org.ala.ecodata.graphql.input

import au.org.ala.ecodata.graphql.enums.YesNo
import graphql.schema.DataFetchingEnvironment

class SearchProjects {

    List<String> associatedProgram
    List<String> associatedSubProgram
    List<String> biogeographicRegion
    List<String> cmz
    List<String> federalElectorate
    List<String> lga
    List<String> mainTheme
    List<String> majorVegetationGroup
    List<String> managementArea
    List<String> marineRegion
    Integer max = 10
    List<String> organisation
    List<String> otherRegion
    Integer page = 0

    String projectId
    List<String> state
    List<String> status
    List<String> tags
    String fromDate
    String toDate
    String order = "desc"
    String sort = "dateCreated"
//    String getQuery() {
//
//    }
//
//    String getFacetFilterList() {
//
//    }

//    String getLastUpdatedQueryString(DataFetchingEnvironment env) {
//        boolean lastUpdated = env.getSelectionSet().containsAnyOf(lastUpdated, "reports.lastUpdated", "sites.lastUpdated")
//        String dateQuery = "lastUpdated > ${fromDate?.format('yyyy-MM-dd') ?: '1970-01-01'}"
//        if (env.getSelectionSet().contains('sites')) {
//            dateQuery += " AND 'sites.lastUpdated' > ${fromDate?.format('yyyy-MM-dd') ?: '1970-01-01'}"
//        }
//        if (env.getSelectionSet().contains('reports')) {
//            dateQuery += " AND 'activities.lastUpdated' > ${fromDate?.format('yyyy-MM-dd') ?: '1970-01-01'}"
//        }
//        dateQuery
//    }
//
//    Integer getMax(DataFetchingEnvironment env) {
//        // If the user isn't querying associations we should allow a greater max.
//        Math.min(max, 50)
//    }
}
