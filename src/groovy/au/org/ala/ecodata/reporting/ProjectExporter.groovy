package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.Project

abstract class ProjectExporter extends TabbedExporter {

    public ProjectExporter(XlsExporter exporter, List<String> tabsToExport = [], Map<String, Object> documentMap = [:], TimeZone timeZone) {
        super(exporter, tabsToExport, documentMap, timeZone)
    }
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
}