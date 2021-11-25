package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * A Hub is a mechanism used to provide a filtered / skinned view of the data in ecodata.
 * It provides some options for configuring default searches, faceting, skin and homepage.
 */
class Hub {
    ObjectId id

    String hubId
    /** The title for the hub - used in the banner in the nrm hub */
    String title
    /** The path used to access this hub (must appear as the first path after the context path) */
    String urlPath
    /** One of ala2 or nrm - defines the look of pages accessed via this hub */
    String skin = 'ala2'
    /** The names of the programs projects created under this hub can be assigned to */
    List supportedPrograms
    /** New projects created from this hub will inherit the default program, if specified */
    String defaultProgram
    /** Defines the full set of facets available for use on search pages for this hub */
    List availableFacets
    /** Defines the subset of availableFacets that should only be visible to FC_ADMINS */
    List adminFacets
    /** Defines the facets that can be used to colour points on maps */
    List availableMapFacets
    /** Defines facet terms that will be applied to searches made by a user using this hub */
    List defaultFacetQuery
    /** Defines the path used as an entry point to this hub */
    String homePagePath
    /** If configurable template is chosen, then use this config to layout the page **/
    Map templateConfiguration
    /**
     * contains configuration for hiding bread crumbs, cancel button and survey and project names
     */
    Map content
    /** quick links to Biocollect pages that appeared on certain pages like create record, view record etc. */
    List quickLinks
    /** provide breadcrumb overrides for a controller action */
    List customBreadCrumbs = []
    /** configure facets for different data pages like all records, my records etc. */
    Map pages
    /** on record listing pages like all records, my records etc., configure table columns using this property */
    List dataColumns
    MapLayersConfiguration mapLayersConfig
    /** configure how activity is displayed on map for example point, heatmap or cluster. */
    List mapDisplays
    /** time series animation can be done on an index other than dateCreated. */
    String timeSeriesOnIndex

    String status = 'active'

    Date dateCreated
    Date lastUpdated

    /** If an email is generated relating to this hub, use this sender address instead of the default it the config */
    String emailFromAddress

    /** If an email is generated relating to this hub, use this sender address instead of the default it the config */
    String emailReplyToAddress

    /** The URL prefix to use when creating a URL a user can use to download a report */
    String downloadUrlPrefix

    AccessManagementOptions accessManagementOptions

    static mapping = {
        hubId index: true
        version false
    }

    static constraints = {
        urlPath unique: true
        skin inList: ['ala2', 'nrm','mdba','ala', 'configurableHubTemplate1']
        title nullable:true
        homePagePath nullable:true
        defaultProgram nullable: true
        templateConfiguration nullable: true
        content nullable: true
        customBreadCrumbs nullable: true
        pages nullable: true
        dataColumns nullable: true
        mapLayersConfig nullable: true
        mapDisplays nullable: true
        timeSeriesOnIndex nullable: true
        emailFromAddress nullable: true
        emailReplyToAddress nullable: true
        downloadUrlPrefix nullable: true
        accessManagementOptions nullable: true
    }

    static embedded = ['mapLayersConfig', 'accessManagementOptions']
}
