package au.org.ala.ecodata

import org.bson.types.ObjectId

/**
 * A Hub is a mechanism used to provide a filtered / skinned view of the data in ecodata.
 * It provides some options for configuring default searches, faceting, skin and homepage.
 */
class Hub implements ProcessEmbedded {
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
    /** Defines the facets that can be used to colour points on maps */
    List officerFacets
    /** Defines facet terms that will be applied to searches made by a user using this hub */
    List defaultFacetQuery
    /** Defines the path used as an entry point to this hub */
    String homePagePath
    /** If configurable template is chosen, then use this config to layout the page **/
    TemplateConfiguration templateConfiguration
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
    List userPermissions
    MapLayersConfiguration mapLayersConfig
    /** configure how activity is displayed on map for example point, heatmap or cluster. */
    List mapDisplays
    /** time series animation can be done on an index other than dateCreated. */
    String timeSeriesOnIndex

    String status = 'active'

    Date dateCreated
    Date lastUpdated

    /** The URL to the banner image for this hub */
    String bannerUrl

    /** The URL to the logo image for this hub */
    String logoUrl

    /** If an email is generated relating to this hub, use this sender address instead of the default it the config */
    String emailFromAddress

    /** If an email is generated relating to this hub, use this sender address instead of the default it the config */
    String emailReplyToAddress

    /** The URL prefix to use when creating a URL a user can use to download a report */
    String downloadUrlPrefix

    /** Fathom analytics site id for hub specific analytics. If not specified, BioCollect will use the default site id. */
    String fathomSiteId

    /** hub specific spatial layers to intersect. This overrides app.facets.geographic configuration. It has the same format i.e. contextual and grouped keys. */
    Map geographicConfig

    AccessManagementOptions accessManagementOptions

    /** Controller and action of home page action */
    Map homePageControllerAndAction

    static mapping = {
        hubId index: true
        version false
    }

    static constraints = {
        urlPath unique: true
        skin inList: ['ala2', 'nrm','mdba','ala', 'configurableHubTemplate1', 'bs4']
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
        fathomSiteId nullable: true
        geographicConfig nullable: true
        bannerUrl nullable: true
        logoUrl nullable: true
        userPermissions nullable: true
        homePageControllerAndAction nullable: true
        officerFacets nullable: true
        templateConfiguration nullable: true
        supportedPrograms nullable: true
        availableFacets nullable: true
        adminFacets nullable: true
        availableMapFacets nullable: true
        defaultFacetQuery nullable: true
        quickLinks nullable: true
    }

    static embedded = ['mapLayersConfig', 'accessManagementOptions', 'templateConfiguration']

    def beforeInsert () {
        processEmbeddedObjects()
    }

    def beforeUpdate () {
        processEmbeddedObjects()
    }
}
