package au.org.ala.ecodata

import au.org.ala.ecodata.graphql.mappers.SiteGraphQLMapper
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.operation.valid.IsValidOp
import org.locationtech.jts.operation.valid.TopologyValidationError
import grails.converters.JSON
import org.bson.types.ObjectId
import org.geotools.geojson.geom.GeometryJSON

class Site {

    static String TYPE_COMPOUND = 'compound'
    static String TYPE_PROJECT_AREA = 'projectArea'
    static String TYPE_WORKS_AREA = 'worksArea'
    static String TYPE_SURVEY_AREA = 'surveyArea'

    static graphql = SiteGraphQLMapper.graphqlMapping()

    def siteService

    /*
    Associations:
        sites may belong to 0..n Projects - a list of projectIds are stored in each site
        sites may have 0..n Activities/Assessments - mapped from the Activity side
    */

    static hasMany = [externalIds:ExternalId]
    static embedded = ['externalIds']

    static mapping = {
        name index: true
        siteId index: true
        projects index: true
        status index: true
        version false
    }

    static transients = ['externalId', 'defaultExternalId']
    ObjectId id
    String siteId
    String status = 'active'
    String visibility

    List projects = []
    String name
    String type
    String description
    String habitat
  //  Double area - the area field is a mess, it's about 1/2 doubles, 1/2 strings, most of which are numbers....
    String recordingMethod
    String landTenure
    String protectionMechanism
    String notes
    String catchment
    Date dateCreated
    Date lastUpdated
    Boolean isSciStarter = false
    Map extent
    Map geoIndex
    /**
     * A list of geojson formatted Features.  These are used to represent internal details for this Site, such
     * as plots and transects for a site assessment.
     */
    List features

    /** Tracks whether a Site has been used in a report and follows the Report publication lifecycle */
    String publicationStatus

    static constraints = {
        visibility nullable: true
        name nullable: true
        externalId nullable:true
        type nullable:true
        description nullable:true, maxSize: 40000
        habitat nullable:true
   //     area nullable:true
        recordingMethod nullable:true
        landTenure nullable:true
        protectionMechanism nullable:true
        notes nullable:true, maxSize: 40000
        isSciStarter nullable: true
        extent nullable: true
        features nullable: true
        catchment nullable: true
        publicationStatus nullable: true
        geoIndex nullable: true, validator: { value, site ->
            // Checks validity of GeoJSON object
            if(value){
                Geometry geom = new GeometryJSON().read((value as JSON).toString())
                IsValidOp isValidOp = new IsValidOp(geom);
                TopologyValidationError error = isValidOp.getValidationError()
                if(error){
                    String errorMsg = error?.toString()
                    log.error ("Site shape is not valid. ${errorMsg}")
                    ['inValidShape', errorMsg]
                }
            }
        }
        externalIds nullable: true
    }

    def getAssociations(){
      def map = [:]
      projects.each { map.put("Project", it)}
    }

    /**
     * Remove duplicate co-ordinates that appear consecutively. Such co-ordinates causes an exception during indexing.
     */
    def beforeValidate(){
        if((extent?.geometry?.type != 'pid') && extent?.geometry){
            geoIndex = siteService?.geometryAsGeoJson(this)
        }
    }

    /**
     * A compound site consists of an extent that is the convex hull of a list of features.  It is used for
     * sites with internal details such as sites for a site assessment methodology (e.g. a site that contains
     * nested plots and transects)
     */
    boolean isCompoundSite() {
        return type == TYPE_COMPOUND
    }

    String getGeometryType() {
        geoIndex?.type
    }

    List getGeoPoint() {
        if ( extent?.geometry?.centre ) {
            if (extent.geometry.centre.getClass().isArray() || (extent.geometry.centre instanceof List)) {
                List coords = extent.geometry.centre
                if (coords) {
                    [coords[0] as Double, coords[1] as Double]
                }
            }
        }
    }

    /**
     * The MERIT and BioCollect UIs have a site page that allows the user to enter an external Id
     * for the Site without specifying the type.
     * Hence we treat this as the "Default" external Id which is either the first unspecified external Id
     * or the first external Id if there are no unspecified external Ids.
     * @return
     */
    private ExternalId defaultExternalId() {
        if (!externalIds) {
            return null
        }
        ExternalId defaultExternalId = externalIds?.find {it.idType == ExternalId.IdType.UNSPECIFIED}
        if (!defaultExternalId) {
            defaultExternalId = externalIds[0]
        }
        defaultExternalId
    }

    String getExternalId() {
        ExternalId defaultExternalId = defaultExternalId()
        defaultExternalId?.externalId
    }

    void setExternalId(String externalId) {
        ExternalId defaultExternalId = defaultExternalId()
        if (defaultExternalId) {
            defaultExternalId.externalId = externalId

        } else {
            externalIds = [new ExternalId(idType: ExternalId.IdType.UNSPECIFIED, externalId: externalId)]
        }
        markDirty('externalIds')
    }


    static Site findByExternalId(ExternalId.IdType idType, String externalId) {
        where {
            externalIds {
                idType == idType
                externalId == externalId
            }
            status != Status.DELETED
        }.find()
    }
}
