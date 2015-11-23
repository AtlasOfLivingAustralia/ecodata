package au.org.ala.ecodata

/**
 * Created by sat01a on 18/11/15.
 */
interface ElasticIndex {
    // Used by MERIT for generic search
    String DEFAULT_INDEX = "search"

    // Generic index used by both MERIT & Biocollect for homepage search
    String HOMEPAGE_INDEX = "homepage"

    //Used by Biocollect for survey/projectActivity based projects.
    String PROJECT_ACTIVITY_INDEX = "pasearch"
}
