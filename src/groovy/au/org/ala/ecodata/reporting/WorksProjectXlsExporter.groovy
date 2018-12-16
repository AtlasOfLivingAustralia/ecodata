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

    List<String> commonProjectHeaders = ['Project ID', 'Project Name', 'Program Name', 'Project Status']
    List<String> commonProjectProperties = ['externalId', 'name', 'associatedProgram', 'projectStatus']

    List<String> projectHeaders = ['Project ID', 'Project Name', 'Program Name', 'Project Manager', 'Organisations', 'Description', 'Start Date', 'End Date', 'Status', 'Funding', 'P2R Reporting', 'Date of recent outcome update' ,'Progress on outcome', 'Type of outcome update', 'Overall risk rating']
    List<String> projectProperties = ['externalId', 'name', 'associatedProgram', 'managerEmail', 'allOrganisations', 'description', new DatePropertyGetter('plannedStartDate', DateTimeParser.Style.DATE,null, null,  timeZone), new DatePropertyGetter('plannedEndDate', DateTimeParser.Style.DATE,  null, null, timeZone), 'projectStatus', 'funding', 'keywords', new DatePropertyGetter('outcomeDate', DateTimeParser.Style.DATE,  null, null, timeZone), new TabbedExporter.LengthLimitedGetter('outcome'), 'outcomeType', 'custom.details.risks.overallRisk']

    List<String> outcomeHeaders = commonProjectHeaders + ['Date', 'Interim/Final', 'Outcome']
    List<String> outcomeProperties = commonProjectProperties + [new DatePropertyGetter('date', DateTimeParser.Style.DATE,  null, null, timeZone), 'type', new TabbedExporter.LengthLimitedGetter('progress')]


    List<String> budgetHeaders = commonProjectHeaders + ['Investment / Priority Area', 'Payment Number', 'Funding Source', 'Payment Status', 'Description', 'Date Due', '2011/2012', '2012/2013', '2013/2014', '2014/2015', '2015/2016', '2016/2017', '2017/2018', '2018/2019', '2019/2020', '2020/2021', '2021/2022', '2022/2023', '2023/2024', '2024/2025', '2025/2026']
    List<String> budgetProperties = commonProjectProperties + ['investmentArea', 'paymentNumber', 'fundingSource', 'paymentStatus', 'budgetDescription', new DatePropertyGetter('dueDate', DateTimeParser.Style.DATE,null, null,  timeZone), '2011/2012', '2012/2013', '2013/2014', '2014/2015', '2015/2016', '2016/2017', '2017/2018', '2018/2019', '2019/2020', '2020/2021', '2021/2022', '2022/2023', '2023/2024', '2024/2025', '2025/2026']


    AdditionalSheet projectSheet
    Map<String, Object> documentMap

    WorksProjectXlsExporter(XlsExporter exporter, Map<String, Object> documentMap, TimeZone timeZone) {
        super(exporter, [], documentMap, timeZone)
        this.documentMap = documentMap
    }

    @Override
    void export(Map project) {
        project.projectStatus = projectStatus(project)
        exportProject(project)
        exportOutcomes(project)
        exportBudget(project)
    }

    @Override
    void export(String projectId, Set<String> activityIds) {

        Map project = projectService.get(projectId)
        export(project)
    }

    private void exportProject(Map project) {
        projectSheet()
        int row = projectSheet.getSheet().lastRowNum
        project.allOrganisations = organisationNames(project).join(',')
        Map outcome = mostRecentOutcome(project)
        project.outcomeDate = outcome?.date ?: ''
        project.outcome = outcome?.progress ?: ''
        project.outcomeType = outcome?.type ?: ''
        projectSheet.add([project], projectProperties, row + 1)
    }

    private String projectStatus(Map project) {

        long now = System.currentTimeMillis()
        String status = 'Planned'
        if (project.plannedEndDate && project.plannedEndDate.getTime() < now) {
            status = 'Completed'
        }
        else if (project.plannedStartDate.getTime() < now) {
            status = 'Current'
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
        List outcomes =  project?.custom?.details?.outcomeProgress ?: []
        outcomes.sort{it.date}

        return outcomes ? outcomes.last() : [:]
    }

    private void exportOutcomes(Map project) {
        exportList("Outcomes", project, project?.custom?.details?.outcomeProgress, outcomeHeaders, outcomeProperties)
    }

    private void exportBudget(Map project) {

        AdditionalSheet sheet = getSheet("Budget", budgetHeaders)
        int row = sheet.getSheet().lastRowNum

        List financialYears = project?.custom?.details?.budget?.headers?.collect {it.data}
        List data = project?.custom?.details?.budget?.rows?.collect { Map lineItem ->

            Map budgetLineItem = [
                    investmentArea: lineItem.shortLabel,
                    budgetDescription: lineItem.description,
                    paymentStatus: lineItem.paymentStatus,
                    fundingSource: lineItem.fundingSource,
                    paymentNumber: lineItem.paymentNumber,
                    dueDate: lineItem.dueDate
            ]
            budgetLineItem.putAll(project)
            financialYears.eachWithIndex { String year, int i ->
                BigDecimal amount = null
                try {
                    amount = new BigDecimal(lineItem.costs[i].dollar)
                }
                catch (NumberFormatException e) {
                    log.warn("Project ${project.projectId} has invalid budget amount: ${lineItem.costs[i].dollar}")
                }
                budgetLineItem.put(year, amount)
            }

            budgetLineItem
        }

        sheet.add(data?:[], budgetProperties, row+1)


    }

    private AdditionalSheet projectSheet() {
        if (!projectSheet) {
            projectSheet = exporter.addSheet('Project', projectHeaders)
        }
        projectSheet
    }
}
