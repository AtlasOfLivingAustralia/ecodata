package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.*
import au.org.ala.ecodata.metadata.*
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.multisheet.AdditionalSheet


/**
 * Export NRM works style projects to a MS Excel file.
 */
class WorksProjectXlsExporter extends ProjectExporter {
    static Log log = LogFactory.getLog(WorksProjectXlsExporter.class)

    ProjectService projectService = Holders.grailsApplication.mainContext.getBean("projectService")

    List<String> projectHeaders = ['Project ID', 'Project Name', 'Program Name', 'Organisations', 'Description', 'Start Date', 'End Date', 'Status', 'Funding', 'P2R Reporting']
    List<String> projectProperties = ['externalId', 'name', 'associatedProgram', 'allOrganisations', 'description', new DatePropertyGetter('plannedStartDate', DateTimeParser.Style.DATE,null, null,  timeZone), new DatePropertyGetter('plannedEndDate', DateTimeParser.Style.DATE,  null, null, timeZone), 'status', 'funding', 'keywords']

    List<String> outcomeHeaders = ['Project ID', 'Project Name', 'Program Name', 'Date', 'Interim/Final', 'Outcome']
    List<String> outcomeProperties = ['externalId', 'name', 'assocatedProgram', 'date', 'type', 'outcome']

    AdditionalSheet projectSheet
    Map<String, Object> documentMap

    WorksProjectXlsExporter(XlsExporter exporter, Map<String, Object> documentMap, TimeZone timeZone) {
        super(exporter, [], documentMap, timeZone)
        this.documentMap = documentMap
    }

    @Override
    void export(Map project) {
        exportProject(project)
        exportOutcomes(project)
    }

    @Override
    void export(String projectId, Set<String> activityIds) {

        Map project = projectService.get(projectId)
        export(project)
    }

    private void exportProject(Map project) {
        projectSheet()
        int row = projectSheet.getSheet().lastRowNum
        project.status = projectStatus(project)
        project.allOrganisations = organisationNames(project).join(',')
        Map outcome = mostRecentOutcome(project)
        project.outcomeDate = outcome?.date ?: ''
        project.outcome = outcome?.outcome ?: ''
        projectSheet.add([project], projectProperties, row + 1)
    }

    private String projectStatus(Map project) {

        String now = CommonService.dateFormat.format(new Date())
        String status = 'Current'
        if (project.startDate < now) {
            status = 'Planned'
        }
        else if (project.endDate < now) {
            status = 'Completed'
        }
        status
    }

    private List organisationNames(Map project) {
        List organisationNames = []
        organisationNames << project.organisationName
        project.associatedOrgs?.each{
            organisationNames << it.name
        }
        organisationNames
    }

    private Map mostRecentOutcome(Map project) {
        List outcomes =  project?.custom.details?.outcomes ?: []
        outcomes.sort{it.date}

        return outcomes.last()
    }

    private void exportOutcomes(Map project) {
        exportList("Outcomes", project, project?.custom.details?.outcomes, outcomeHeaders, outcomeProperties)
    }

    private AdditionalSheet projectSheet() {
        if (!projectSheet) {
            projectSheet = exporter.addSheet('Project', projectHeaders)
        }
        projectSheet
    }
}
