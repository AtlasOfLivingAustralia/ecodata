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
    /** hide bread crumbs **/
    Boolean hideBreadCrumbs = false
    /** quick links to Biocollect pages that appeared on certain pages like create record, view record etc. */
    List quickLinks

    String status = 'active'

    Date dateCreated
    Date lastUpdated


    static mapping = {
        hubId index: true
        version false
    }

    static constraints = {
        urlPath unique: true
        skin inList: ['ala2', 'nrm','mdba','ala', 'configurableHubTemplate1', 'configurableHubTemplate-ALA']
        title nullable:true
        homePagePath nullable:true
        defaultProgram nullable: true
        templateConfiguration nullable: true
        hideBreadCrumbs nullable: true
    }
}
