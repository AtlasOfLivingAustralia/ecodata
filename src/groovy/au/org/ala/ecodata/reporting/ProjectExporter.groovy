package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.Project
import au.org.ala.ecodata.metadata.OutputDataPropertiesBuilder
import grails.util.Holders

abstract class ProjectExporter {
    MetadataService metadataService = Holders.grailsApplication.mainContext.getBean("metadataService")

    void export(Map project) {
        // to be overridden if needed
    }

    void export(String projectId, Set<String> activityIds) {
        // to be overridden if needed
    }

    public void exportAllProjects(List<Map> projects) {
        projects.each { export(it) }
    }

    public void exportActivities(Map<String, Set<String>> activityIdsByProjectId) {
        Project.withSession { session ->
            activityIdsByProjectId.each { projectId, activityIds ->
                export(projectId, activityIds)
            }
        }
    }

    protected outputProperties(name) {
        def model = metadataService.annotatedOutputDataModel(name)

        def headers = []
        def properties = []
        model.each {
            if (it.dataType == 'list') {
                it.columns.each { col ->
                    properties << it.name + '.' + col.name
                    headers << col.label
                }
            } else if (it.dataType == "singleSighting") {
                properties.addAll([
                        "singleSighting.locationLatitude",
                        "singleSighting.locationLongitude",
                        "singleSighting.dateStr",
                        "singleSighting.userId",
                        "singleSighting.scientificName",
                        "singleSighting.guid",
                        ])
                headers.addAll([
                        "Latitude",
                        "Longitude",
                        "Date",
                        "UserId",
                        "Scientific Name",
                        "GUID",
                ])
            } else if (it.dataType in ['photoPoints', 'matrix', 'masterDetail', 'geoMap']) {
                // not supported, do nothing.
            } else {
                properties << it.name
                headers << it.description
            }
        }
        List propertyGetters = properties.collect { new OutputDataPropertiesBuilder(it, model) }
        [headers: headers, propertyGetters: propertyGetters]
    }
}