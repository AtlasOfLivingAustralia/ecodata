package au.org.ala.ecodata
import au.org.ala.ecodata.reporting.Aggregator
import au.org.ala.ecodata.reporting.AggregatorBuilder
import au.org.ala.ecodata.reporting.Score
import com.vividsolutions.jts.geom.Geometry
import grails.converters.JSON
import org.geotools.data.DataUtilities
import org.geotools.data.FeatureStore
import org.geotools.data.FeatureWriter
import org.geotools.data.FileDataStoreFactorySpi
import org.geotools.data.shapefile.ShapefileDataStore
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.geojson.geom.GeometryJSON
import org.grails.plugins.csv.CSVReaderUtils
import org.opengis.feature.simple.SimpleFeature
import org.opengis.feature.simple.SimpleFeatureType
import org.opengis.feature.type.FeatureType

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The ReportService aggregates and returns output scores.
 */
class ReportService {

    def activityService, elasticSearchService, projectService, siteService, outputService, metadataService, userService

    static final String PUBLISHED_ACTIVITIES_FILTER = 'publicationStatus:published'

    /**
     * Creates an aggregation specification from the Scores defined in the activities model.
     */
    def buildReportSpec() {
        def toAggregate = []

        metadataService.activitiesModel().outputs?.each{
            Score.outputScores(it).each { score ->
                def scoreDetails = [score:score]
                toAggregate << scoreDetails
            }
        }
        toAggregate
    }

    def queryPaginated(List filters, Closure action) {

        // Only dealing with approved activities.
        def additionalFilters = [PUBLISHED_ACTIVITIES_FILTER]
        additionalFilters.addAll(filters)

        Map params = [offset:0, max:100]

        def results = elasticSearchService.searchActivities(additionalFilters, params)

        def total = results.hits.totalHits
        while (params.offset < total) {

            def hits = results.hits.hits
            for (def hit : hits) {
                action(hit.source)
            }
            params.offset += params.max

            results  = elasticSearchService.searchActivities(additionalFilters, params)
        }
    }

    def aggregate(List filters) {

        def toAggregate = buildReportSpec()

        List<Aggregator> aggregators = buildAggregators(toAggregate)
        def metadata = [activities: 0, distinctSites:new HashSet(), distinctProjects:new HashSet()]

        def aggregateActivity = { activity ->
            metadata.activities++
            metadata.distinctProjects << activity.projectId
            if (activity.sites) {
                metadata.distinctSites << activity.sites.siteId
            }

            Output.withNewSession {
                def outputs = outputService.findAllForActivityId(activity.activityId, ActivityService.FLAT)
                activity.outputs = outputs
                aggregators.each { it.aggregate(activity) }
            }
        }

        queryPaginated(filters, aggregateActivity)

        def allResults = aggregators.collect {it.results()}
        def outputData = allResults.findAll{it.results}
        [outputData:outputData, metadata:[activities: metadata.activities, sites:metadata.distinctSites.size(), projects:metadata.distinctProjects]]
    }

    /**
     * Returns aggregated scores for a specified project.
     * @param projectId the project of interest.
     * @param aggregationSpec defines the scores to be aggregated and if any grouping needs to occur.
     * [{score:{name: , units:, aggregationType}, groupBy: {entity: <one of 'activity', 'output', 'project', 'site>, property: String <the entity property to group by>}, ...]
     *
     * @return the results of the aggregration.  The results will be an array of maps, the structure of each Map is
     * described in @see au.org.ala.ecodata.reporting.Aggregation.results()
     *
     */
    def projectSummary(String projectId, List aggregationSpec, boolean approvedActivitiesOnly = false) {


       // We definitely could be smarter about this query - only getting activities with outputs of particular
        // types or containing particular scores for example.
        List activities = activityService.findAllForProjectId(projectId, 'FLAT')
        if (approvedActivitiesOnly) {
            activities = activities.findAll{it.publicationStatus == 'published'}
        }
        List outputs = Output.findAllByActivityIdInListAndStatus(activities.collect{it.activityId}, OutputService.ACTIVE).collect {outputService.toMap(it)}
        Map outputsByActivityId = outputs.groupBy{it.activityId}

        return aggregate(aggregationSpec, activities, outputsByActivityId)
    }


    def aggregate(aggregationSpec, List<Activity> activities, Map outputsByActivityId) {

        // Determine if we need to group by site or project properties, if not we can avoid a lot of queries.
        boolean projectGrouping = aggregationSpec.find {it.groupBy?.entity == 'project'}
        boolean siteGrouping = aggregationSpec.find {it.groupBy?.entity == 'site'}

        List<Aggregator> aggregators = buildAggregators(aggregationSpec)

        activities.each { activity ->
            // This is really a bad way to do this as we are going to be running a lot of queries do do the aggregation.
            // I think the best way is going to be to index Activities with project and site data and do the
            // query via the search index.
            if (projectGrouping && activity.projectId) {
                activity['project'] = projectService.toMap(Project.findByProjectId(activity.projectId), ProjectService.BRIEF)
            }
            if (siteGrouping && activity.siteId) {
                activity['site'] = siteService.toMap(Site.findBySiteId(activity.siteId), SiteService.BRIEF)
            }
            activity['outputs'] = outputsByActivityId[activity.activityId]
            aggregators.each { it.aggregate(activity) }
        }

        aggregators.collect {it.results()}

    }

    def buildAggregators(aggregationSpec) {
        List<Aggregator> aggregators = []

        def groupedScores = aggregationSpec.groupBy{it.score.label}

        groupedScores.each { k, v ->
            aggregators << new AggregatorBuilder().scores(v.collect{it.score}).build()
        }

        aggregators
    }

    /** Temporary method to assist running the user report.  Needs work */
    def userSummary() {

        def levels = [100:'admin',60:'caseManager', 40:'editor', 20:'favourite']

        def userSummary = [:]
        def users = UserPermission.findAll().groupBy{it.userId}
        users.each { userId, projects ->
            def userDetails = userService.lookupUserDetails(userId)


            userSummary[userId] = [id:userDetails.userId, name:userDetails.displayName, email:userDetails.userName, role:'FC_USER']
            userSummary[userId].projects = projects.collect {
                def project = projectService.get(it.projectId, ProjectService.FLAT)

                [projectId: project.projectId, grantId:project.grantId, externalId:project.externalId, name:project.name, access:levels[it.accessLevel.code]]
            }
        }

        // TODO need a web service from auth to support this properly.
        def fcOfficerList = new File('/Users/god08d/Documents/MERIT/users/fc_officer.csv')
        CSVReaderUtils.eachLine(fcOfficerList, { String[] tokens ->
            def userIdStr = tokens[0]
            if (userIdStr.isInteger()) {
                def userId = userIdStr as Integer
                def user = userSummary[userId]
                if (!user) {
                    user = [:]
                    userSummary[userId] = user
                    def userDetails = userService.lookupUserDetails(userIdStr)
                    user.userId = userDetails.userId
                    user.name = userDetails.displayName
                    user.email = userDetails.userName
                    user.projects = []
                }
                user.role = tokens[2]
            }
        })

        userSummary
    }

    def exportShapeFile(projectIds) {

        FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();

        File file = File.createTempFile("siteShapeFile", ".shp")
        String filename = file.getName().substring(0, file.getName().indexOf(".shp"))

        Map map = ["url": file.toURI().toURL() ]

        FeatureWriter<SimpleFeatureType,SimpleFeature>  writer = null
        try {
            ShapefileDataStore store = factory.createNewDataStore( map );
            FeatureType featureType = DataUtilities.createType( filename, "geom:MultiPolygon:srid=4326,name:String,description:String,projectName:String,grantId:String,externalId:String" );
            store.createSchema( featureType );

            writer = store.getFeatureWriterAppend(((FeatureStore)store.getFeatureSource(filename)).getTransaction())

            GeometryJSON gjson = new GeometryJSON()


            projectIds.each { projectId ->
                def project = projectService.get(projectId)

                if (!project) {
                    return
                }

                project.sites?.each { site ->

                    try {
                        def siteGeom = siteService.geometryAsGeoJson(site)
                        if (siteGeom) {


                            Geometry geom = gjson.read((siteGeom as JSON).toString())

                            SimpleFeature siteFeature = writer.next()

                            siteFeature.setAttributes([geom, site.name, site.description, project.name, project.grantId, project.externalId].toArray())

                            writer.write()
                        } else {
                            log.warn("Unable to get geometry for site: ${site.siteId}")
                        }
                    }
                    catch (Exception e) {
                        log.error("Error getting geomerty for site: ${site.siteId}", e)
                    }
                }
            }
        }
        finally {
            if (writer != null) {
                writer.close()
            }
        }

        buildZip(file)

    }

    def buildZip(File shapeFile) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ZipOutputStream zipFile = new ZipOutputStream(baos)

        String filename = shapeFile.getName().substring(0, shapeFile.getName().indexOf(".shp"))
        String path = shapeFile.getParent()

        def fileExtensions = ['.shp', '.dbf', '.fix', '.prj', '.shx']

        fileExtensions.each { extension ->
            File file = new File(path, filename+extension)
            zipFile.putNextEntry(new ZipEntry("meritSites"+extension))
            file.withInputStream {
                zipFile << it
            }
            zipFile.closeEntry()
        }
        zipFile.finish()
        baos
    }


}
