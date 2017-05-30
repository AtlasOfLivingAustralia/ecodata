package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.ProjectService
import au.org.ala.ecodata.Report
import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.multisheet.AdditionalSheet

/**
 * Exports project, site, activity and output data to a Excel spreadsheet.
 */
class ProjectXlsExporter extends ProjectExporter {

    static Log log = LogFactory.getLog(ProjectXlsExporter.class)

    // Avoids name clashes for fields that appear in activitites and projects (such as name / description)
    private static final String ACTIVITY_DATA_PREFIX = 'activity_'
    private static final String PROJECT_DATA_PREFIX = 'project_'

    List<String> stateHeaders = (1..3).collect{'State '+it}
    List<String> stateProperties = (0..2).collect{'state'+it}

    List<String> electorateHeaders = (1..15).collect{'Electorate '+it}
    List<String> electorateProperties = (0..14).collect{'elect'+it}

    List<String> projectStateHeaders = (1..5).collect{'State '+it}
    List<String> projectStateProperties = (0..4).collect{'state'+it}

    List<String> projectElectorateHeaders = (1..40).collect{'Electorate '+it}
    List<String> projectElectorateProperties = (0..39).collect{'elect'+it}

    List<String> commonProjectHeadersWithoutSites = ['Project ID', 'Grant ID', 'External ID', 'Organisation', 'Service Provider', 'Name', 'Description', 'Program', 'Sub-program', 'Start Date', 'End Date', 'Funding', 'Status', 'Last Modified']
    List<String> commonProjectPropertiesRaw =  ['grantId', 'externalId', 'organisationName', 'serviceProviderName', 'name', 'description', 'associatedProgram', 'associatedSubProgram', 'plannedStartDate', 'plannedEndDate', 'funding', 'status', 'lastUpdated']

    List<String> commonProjectPropertiesWithoutSites = ['projectId'] + commonProjectPropertiesRaw.collect{PROJECT_DATA_PREFIX+it}

    List<String> commonProjectHeaders = commonProjectHeadersWithoutSites + stateHeaders + electorateHeaders
    List<String> commonProjectProperties = commonProjectPropertiesWithoutSites + stateProperties + electorateProperties

    List<String> projectHeaders = commonProjectHeadersWithoutSites + projectStateHeaders + projectElectorateHeaders
    List<String> projectProperties = commonProjectPropertiesWithoutSites + projectStateProperties + projectElectorateProperties

    List<String> siteStateHeaders = (1..5).collect{'State '+it}
    List<String> siteStateProperties = (0..4).collect{'state'+it+'-site'}

    List<String> siteElectorateHeaders = (1..40).collect{'Electorate '+it}
    List<String> siteElectorateProperties = (0..39).collect{'elect'+it+'-site'}

    List<String> siteHeaders = commonProjectHeaders + ['Site ID', 'Name', 'Description', 'lat', 'lon', 'Last Modified', 'NRM'] + siteStateHeaders + siteElectorateHeaders
    List<String> siteProperties = commonProjectProperties + ['siteId', 'siteName', 'siteDescription', 'lat', 'lon', 'lastUpdated', 'nrm0-site'] + siteStateProperties + siteElectorateProperties

    List<String> commonActivityHeaders = commonProjectHeaders + ['Activity ID', 'Site ID', 'Planned Start date', 'Planned End date', 'Stage', 'Description', 'Activity Type', 'Theme', 'Status', 'Report Status', 'Last Modified']
    List<String> activityProperties = commonProjectProperties+ ['activityId', 'siteId', 'plannedStartDate', 'plannedEndDate', 'stage', 'description', 'type', 'mainTheme', 'progress', 'publicationStatus', 'lastUpdated'].collect{ACTIVITY_DATA_PREFIX+it}
    List<String> outputTargetHeaders = commonProjectHeaders + ['Output Target Measure', 'Target', 'Delivered - approved', 'Delivered - total', 'Units']
    List<String> outputTargetProperties = commonProjectProperties + ['scoreLabel', new TabbedExporter.StringToDoublePropertyGetter('target'), 'deliveredApproved', 'deliveredTotal', 'units']
    List<String> risksAndThreatsHeaders = commonProjectHeaders + ['Type of threat / risk', 'Description', 'Likelihood', 'Consequence', 'Risk rating', 'Current control', 'Residual risk']
    List<String> risksAndThreatsProperties = commonProjectProperties + ['threat', 'description', 'likelihood', 'consequence', 'riskRating', 'currentControl', 'residualRisk']
    List<String> budgetHeaders = commonProjectHeaders + ['Investment / Priority Area', 'Description', '2011/2012', '2012/2013', '2013/2014', '2014/2015', '2015/2016', '2016/2017', '2017/2018', '2018/2019', '2019/2020']
    List<String> budgetProperties = commonProjectProperties + ['investmentArea', 'budgetDescription', '2011/2012', '2012/2013', '2013/2014', '2014/2015', '2015/2016', '2016/2017', '2017/2018', '2018/2019', '2019/2020']
    List<String> assetsAddressed = ['Natural/Cultural assets managed','Threatened Species', 'Threatened Ecological Communities',
        'Migratory Species', 'Ramsar Wetland', 'World Heritage area', 'Community awareness/participation in NRM', 'Indigenous Cultural Values',
        'Indigenous Ecological Knowledge', 'Remnant Vegetation', 'Aquatic and Coastal systems including wetlands', 'Not Applicable']
    List<String> outcomesHeaders = commonProjectHeaders + ['Outcomes'] + assetsAddressed
    List<String> outcomesProperties = commonProjectProperties + ['outcomes'] + assetsAddressed
    List<String> monitoringHeaders = commonProjectHeaders + ['Monitoring Indicators', 'Monitoring Approach']
    List<String> monitoringProperties = commonProjectProperties + ['indicator', 'approach']
    List<String> projectPartnershipHeaders = commonProjectHeaders + ['Partner name', 'Nature of partnership', 'Type of organisation']
    List<String> projectPartnershipProperties = commonProjectProperties + ['data1', 'data2', 'data3']
    List<String> projectImplementationHeaders = commonProjectHeaders + ['Project implementation / delivery mechanism']
    List<String> projectImplementationProperties = commonProjectProperties + ['implementation']
    List<String> keyEvaluationQuestionHeaders = commonProjectHeaders + ['Project Key evaluation question (KEQ)', 'How will KEQ be monitored?']
    List<String> keyEvaluationQuestionProperties = commonProjectProperties + ['data1', 'data2']
    List<String> prioritiesHeaders = commonProjectHeaders + ['Document name', 'Relevant section', 'Explanation of strategic alignment']
    List<String> prioritiesProperties = commonProjectProperties + ['data1', 'data2', 'data3']
    List<String> whsAndCaseStudyHeaders = commonProjectHeaders + ['Are you aware of, and compliant with, your workplace health and safety legislation and obligations', 'Do you have appropriate policies and procedures in place that are commensurate with your project activities?', 'Are you willing for your project to be used as a case study by the Department?']
    List<String> whsAndCaseStudyProperties = commonProjectProperties + ['obligations', 'policies', 'caseStudy']
    List<String> attachmentHeaders = commonProjectHeaders + ['Title', 'Attribution', 'File name']
    List<String> attachmentProperties = commonProjectProperties + ['name', 'attribution', 'filename']
    List<String> reportHeaders = commonProjectHeaders + ['Stage', 'From Date', 'To Date', 'Action', 'Action Date', 'Actioned By', 'Weekdays since last action', 'Comment']
    List<String> reportProperties = commonProjectProperties + ['stageName', 'fromDate', 'toDate', 'reportStatus', 'dateChanged', 'changedBy', 'delta', 'comment']
    List<String> reportSummaryHeaders = commonProjectHeaders + ['Stage', 'Stage from', 'Stage to', 'Activity Count', 'Current Report Status', 'Date of action', 'No. weekdays since previous action', 'Actioned By: user number', 'Actioned by: user name']
    List<String> reportSummaryProperties = commonProjectProperties + ['reportName', 'fromDate', 'toDate', 'activityCount', 'reportStatus', 'dateChanged', 'delta', 'changedBy', 'changedByName']
    List<String> documentHeaders = commonProjectHeaders + ['Title', 'Attribution', 'File name', 'Purpose']
    List<String> documentProperties = commonProjectProperties + ['name', 'attribution', 'filename', 'role']
    List<String> blogHeaders = commonProjectHeaders + ['Type', 'Date', 'Title', 'Content', "See more URL"]
    List<String> blogProperties = commonProjectProperties + ['type', 'date', 'title', 'content', 'viewMoreUrl']


    AdditionalSheet projectSheet
    AdditionalSheet sitesSheet
    AdditionalSheet outputTargetsSheet
    AdditionalSheet risksAndThreatsSheet

    // These fields map full activity names to shortened names that are compatible with Excel tabs.
    Map<String, String> activitySheetNames = [:]
    Map<String, List<AdditionalSheet>> typedActivitySheets = [:]

    Map<String, String> outputSheetNames = [:]
    Map<String, List<AdditionalSheet>> typedOutputSheets = [:]


    OutputModelProcessor processor = new OutputModelProcessor()
    ProjectService projectService

    public ProjectXlsExporter(ProjectService projectService, XlsExporter exporter, List<String> tabsToExport, Map<String, Object> documentMap = [:]) {
        super(exporter, tabsToExport, documentMap, TimeZone.default)
        this.projectService = projectService
    }

    public void export(Map project) {

        commonProjectPropertiesRaw.each {
            project[PROJECT_DATA_PREFIX+it] = project.remove(it)
        }

        Map activitiesModel = metadataService.activitiesModel()

        addProjectGeo(project)

        exportProject(project)
        exportOutputTargets(project)
        exportSites(project)
        exportDocuments(project)
        exportActivities(project, activitiesModel)
        exportOutputs(project, activitiesModel)
        exportRisks(project)
        exportMeriPlan(project)
        exportReports(project)
        exportReportSummary(project)
        exportBlog(project)
    }

    private addProjectGeo(Map project) {
        List geo = ['state',  'elect']
        Map geoData = [:].withDefault{[]}
        project.sites?.each { site ->
            Map props = site?.extent?.geometry ?: [:]

            geo.each { facet ->
                Object value = props[facet]
                if (value instanceof List) {
                    geoData[facet].addAll(value)
                }
                else {
                    geoData[facet] << value
                }
            }
        }
        geoData.each { facet, values ->
            values.findAll().unique().eachWithIndex {value, i ->
                project[facet+i] = value
            }
        }
    }

    private Map commonActivityData(Map project, Map activity) {
        String activityDataPrefix = ACTIVITY_DATA_PREFIX
        Map activityBaseData = activity.collectEntries{k,v -> [activityDataPrefix+k, v]}
        Map activityData = project + activityBaseData  + [(activityDataPrefix+'stage'): getStage(activity, project)]
        activityData[(activityDataPrefix+'publicationStatus')] = translatePublicationStatus(activity.publicationStatus)
        activityData
    }

    /** Matches the status string supplied for a Report (which is determined via the status change history) */
    private String translatePublicationStatus(String status) {

        String translated
        switch (status) {
            case Report.REPORT_APPROVED:
                translated = 'Approved'
                break
            case Report.REPORT_NOT_APPROVED:
                translated = 'Returned'
                break
            case Report.REPORT_SUBMITTED:
                translated = 'Submitted'
                break
            default:
                translated = 'Unpublished (no action – never been submitted)'
                break

        }
        translated
    }

    private void exportActivities(Map project, Map activitiesModel) {
        if (project.activities) {
            project.activities.each { activity ->
                if (shouldExport('Activity Summary')) {
                    AdditionalSheet sheet = getSheet("Activity Summary", commonActivityHeaders)
                    Map activityData = commonActivityData(project, activity)
                    sheet.add(activityData, activityProperties, sheet.getSheet().lastRowNum + 1)
                }
                if (shouldExport(activity.type)) {
                    exportActivity(project, activitiesModel, activity)
                }
            }
        }
    }

    private void exportOutputs(Map project, Map activitiesModel) {
        List exportableOutputs = ['Participant Information']
        if (project.activities) {
            activitiesModel.outputs.each { outputConfig ->
                if ((!tabsToExport && outputConfig.name in exportableOutputs) || tabsToExport.contains(outputConfig.name)) {
                    project.activities.each { activity ->
                        exportOutput(outputConfig.name, project, activity)
                    }
                }
            }
        }
    }

    private void exportOutput(String outputName, Map project, Map activity) {
        Map output = activity.outputs?.find{it.name == outputName}
        if (output) {
            List outputGetters = activityProperties + outputProperties(outputName).propertyGetters
            Map commonData = commonActivityData(project, activity)
            List outputData = getOutputData(outputName, activity, commonData)

            AdditionalSheet outputSheet = getOutputSheet(outputName)
            int outputRow = outputSheet.sheet.lastRowNum
            outputSheet.add(outputData, outputGetters, outputRow + 1)
        }

    }

    private List getOutputData(String outputName, Map activity, Map commonData) {
        List flatData = []

        OutputMetadata outputModel = new OutputMetadata(metadataService.getOutputDataModelByName(outputName))
        Map outputData = activity.outputs?.find { it.name == outputName }
        if (outputData) {
            flatData = processor.flatten(outputData, outputModel, false)
            flatData = flatData.collect { commonData + it }
        }
        flatData
    }

    private void exportActivity(Map project, Map activitiesModel, Map activity) {

        Map commonData = commonActivityData(project, activity)
        List activityData = []
        List activityGetters = []

        activityGetters += activityProperties

        Map activityModel = activitiesModel.activities.find { it.name == activity.type }
        if (activityModel) {
            activityModel.outputs?.each { output ->
                if (activityModel.outputs.contains(output)) {
                    activityGetters += outputProperties(output).propertyGetters
                    activityData += getOutputData(output, activity, commonData)
                }
            }
            AdditionalSheet activitySheet = getActivitySheet(activityModel)
            int activityRow = activitySheet.sheet.lastRowNum
            activitySheet.add(activityData, activityGetters, activityRow + 1)

        } else {
            log.error("Found activity not in model: " + activity.type)
        }
    }

    private void exportSites(Map project) {
        if (shouldExport('Sites')) {
            sitesSheet()
            if (project.sites) {
                def sites = project.sites.collect {
                    def centre = it.extent?.geometry?.centre
                    Map data = [siteId: it.siteId, siteName: it.name, siteDescription: it.description, lat: centre ? centre[1] : "", lon: centre ? centre[0] : "", lastUpdated: it.lastUpdated] + project
                    Map props = it.extent?.geometry ?: [:]
                    props?.each { key, value ->
                        if (value instanceof List) {
                            value.eachWithIndex { val, i ->
                                data.put(key+i+'-site', val)
                            }
                        }
                        else {
                            data.put(key+'0-site', value)
                        }
                    }
                    data
                }
                int row = sitesSheet.getSheet().lastRowNum
                sitesSheet.add(sites, siteProperties, row + 1)
            }
        }
    }

    private void exportOutputTargets(Map project) {
        if (shouldExport('Output Targets')) {
            outputTargetsSheet()
            if (project.outputTargets) {
                List approvedMetrics = projectService.projectMetrics(project.projectId, true, true)
                List totalMetrics = projectService.projectMetrics(project.projectId, true, false)
                List targets = approvedMetrics.findAll{it.target && it.target != "0"}.collect{project + [scoreLabel:it.label, target:it.target, deliveredApproved:it.result?.result, units:it.units?:'']}
                targets.each { target ->
                    target.deliveredTotal = totalMetrics.find{it.label == target.scoreLabel}?.result?.result
                }
                int row = outputTargetsSheet.getSheet().lastRowNum
                outputTargetsSheet.add(targets, outputTargetProperties, row + 1)
            }
        }
    }

    private void exportProject(Map project) {
        if (shouldExport('Projects')) {
            projectSheet()
            int row = projectSheet.getSheet().lastRowNum

            List properties = new ArrayList(projectProperties)

            projectSheet.add([project], properties, row + 1)
        }
    }

    private void exportMeriPlan(Map project) {

        exportBudget(project)
        exportOutcomes(project)
        exportMonitoring(project)
        exportProjectPartnerships(project)
        exportProjectImplementation(project)
        exportKeyEvaluationQuestion(project)
        exportPriorities(project)
        exportWHSAndCaseStudy(project)
        exportAttachments(project)

    }

    private void exportBudget(Map project) {
        if (shouldExport("MERI_Budget")) {
            AdditionalSheet sheet = getSheet("MERI_Budget", budgetHeaders)
            int row = sheet.getSheet().lastRowNum

            List financialYears = project?.custom?.details?.budget?.headers?.collect {it.data}
            List data = project?.custom?.details?.budget?.rows?.collect { Map lineItem ->

                Map budgetLineItem = [
                        investmentArea: lineItem.shortLabel,
                        budgetDescription: lineItem.description
                ]
                budgetLineItem.putAll(project)
                financialYears.eachWithIndex { String year, int i ->
                    budgetLineItem.put(year, lineItem.costs[i].dollar)
                }

                budgetLineItem
            }

            sheet.add(data?:[], budgetProperties, row+1)
        }

    }



    private void exportOutcomes(Map project) {
        if (shouldExport("MERI_Outcomes")) {
            AdditionalSheet sheet = getSheet("MERI_Outcomes", outcomesHeaders)
            int row = sheet.getSheet().lastRowNum


            List data = project?.custom?.details?.objectives?.rows1?.collect { Map objective ->

                Map objectivesItem = [
                        outcomes: objective.description
                ]
                objectivesItem.putAll(project)
                outcomesHeaders.each { String asset ->
                    if (objective.assets?.contains(asset)) {
                        objectivesItem[asset] = 'Y'
                    }
                    else {
                        objectivesItem[asset] = 'N'
                    }
                }

                objectivesItem
            }

            sheet.add(data?:[], outcomesProperties, row+1)
        }

    }

    private void exportMonitoring(Map project) {
        if (shouldExport("MERI_Monitoring")) {
            AdditionalSheet sheet = getSheet("MERI_Monitoring", monitoringHeaders)
            int row = sheet.getSheet().lastRowNum


            List data = project?.custom?.details?.objectives?.rows?.collect { Map monitoringLine ->

                Map monitoringItem = [
                        indicator: monitoringLine.data1,
                        approach: monitoringLine.data2
                ]
                monitoringItem.putAll(project)
                monitoringItem
            }

            sheet.add(data?:[], monitoringProperties, row+1)
        }

    }

    private void exportList(String tab, Map project, List data, List headers, List properties) {
        if (shouldExport(tab) && data) {
            AdditionalSheet sheet = getSheet(tab, headers)
            int row = sheet.getSheet().lastRowNum
            List augmentedList = data?.collect {
                it.putAll(project)
                it
            }
            sheet.add(augmentedList, properties, row+1)
        }
    }

    private void exportProjectPartnerships(Map project) {

        exportList("MERI_Project Partnerships", project, project?.custom?.details?.partnership?.rows,
                projectPartnershipHeaders, projectPartnershipProperties)
    }

    private void exportProjectImplementation(Map project) {
        if (shouldExport("MERI_Project Implementation")) {
            AdditionalSheet sheet = getSheet("MERI_Project Implementation", projectImplementationHeaders)
            int row = sheet.getSheet().lastRowNum

            if (project?.custom?.details?.implementation) {
                Map data = [implementation:project?.custom?.details?.implementation?.description]
                data.putAll(project)

                sheet.add(data, projectImplementationProperties, row+1)
            }
        }

    }

    private void exportKeyEvaluationQuestion(Map project) {
        exportList("MERI_Key Evaluation Question", project, project?.custom?.details?.keq?.rows,
                keyEvaluationQuestionHeaders, keyEvaluationQuestionProperties)
    }

    private void exportPriorities(Map project) {
        exportList("MERI_Priorities", project, project?.custom?.details?.priorities?.rows,
            prioritiesHeaders, prioritiesProperties)
    }

    private void exportWHSAndCaseStudy(Map project) {
        if (shouldExport("MERI_WHS and Case Study")) {
            AdditionalSheet sheet = getSheet("MERI_WHS and Case Study", whsAndCaseStudyHeaders)
            int row = sheet.getSheet().lastRowNum

            if (project?.custom?.details) {
                Map data = project?.custom?.details
                data.putAll(project)

                sheet.add(data, whsAndCaseStudyProperties, row + 1)
            }
        }
    }

    private void exportRisks(Map project) {
        if (tabsToExport && tabsToExport.contains('MERI_Risks and Threats')) {
            risksAndThreatsSheet()
            int row = risksAndThreatsSheet.getSheet().lastRowNum
            if (project.risks && project.risks.rows) {
                List data = project.risks.rows.collect { it + project }
                risksAndThreatsSheet.add(data, risksAndThreatsProperties, row + 1)
            }

        }
    }

    private void exportAttachments(Map project) {
        List meriPlanAttachments = project.documents?.findAll {it.role == "programmeLogic"}
        exportList("MERI_Attachments", project, meriPlanAttachments, attachmentHeaders, attachmentProperties)
    }

    private void exportDocuments(Map project) {
        exportList("Documents", project, project.documents, documentHeaders, documentProperties)
    }

    private void exportReports(Map project) {
        if (shouldExport("Reports")) {
            AdditionalSheet sheet = getSheet("Reports", reportHeaders)
            exportReports(sheet, project, reportProperties)
        }
    }

    private void exportReportSummary(Map project) {
        if (shouldExport("Report Summary")) {
            AdditionalSheet sheet = getSheet("Report Summary", reportSummaryHeaders)
            exportReportSummary(sheet, project, reportSummaryProperties)
        }
    }

    private void exportBlog(Map project) {
        exportList("Blog", project, project.blog, blogHeaders, blogProperties)
    }

    String getStage(Map activity, project) {
        Date activityEndDate = activity.plannedEndDate

        if (!activityEndDate) {
            log.error("No end date for activity: ${activity.activityId}, project: ${project.projectId}")
            return ''
        }

        Report report = project.reports?.find { it.fromDate.getTime() < activityEndDate.getTime() && it.toDate.getTime() >= activityEndDate.getTime() }

        report ? report.name : ''
    }

    AdditionalSheet getActivitySheet(Map activityModel) {
        String activityType = activityModel.name

        if (!typedActivitySheets[activityType]) {
            String name = XlsExporter.sheetName(activityType)

            // If the sheets are named similarly, they may end up the same after being changed to excel
            // tab compatible strings
            int i = 1
            while (activitySheetNames[name]) {
                name = name.substring(0, name.length()-1)
                name = name + Integer.toString(i)
            }

            activitySheetNames[name] = activityType
            List<String> headers = buildActivityHeaders(activityModel)
            typedActivitySheets[activityType] = exporter.addSheet(name, headers)
        }
        typedActivitySheets[activityType]
    }

    AdditionalSheet getOutputSheet(String outputName) {

        if (!typedOutputSheets[outputName]) {
            String name = XlsExporter.sheetName(outputName)

            // If the sheets are named similarly, they may end up the same after being changed to excel
            // tab compatible strings
            int i = 1
            while (outputSheetNames[name]) {
                name = name.substring(0, name.length()-1)
                name = name + Integer.toString(i)
            }

            outputSheetNames[name] = outputName
            List<String> headers = buildOutputHeaders(outputName)
            typedOutputSheets[outputName] = exporter.addSheet(name, headers)
        }
        typedOutputSheets[outputName]
    }

    List<String> buildActivityHeaders(Map activityModel) {
        List<String> activityHeaders = [] + commonActivityHeaders

        activityModel.outputs?.each { output ->
            Map config = outputProperties(output)
            activityHeaders += config.headers
        }

        activityHeaders
    }

    List<String> buildOutputHeaders(String outputName) {
        List<String> outputHeaders = [] + commonActivityHeaders
        outputHeaders += outputProperties(outputName).headers
        outputHeaders
    }

    AdditionalSheet projectSheet() {
        if (!projectSheet) {
            projectSheet = exporter.addSheet('Projects', projectHeaders)
        }
        projectSheet
    }

    AdditionalSheet sitesSheet() {
        if (!sitesSheet) {
            sitesSheet = exporter.addSheet('Sites', siteHeaders)
        }
        sitesSheet
    }

    AdditionalSheet outputTargetsSheet() {
        if (!outputTargetsSheet) {
            outputTargetsSheet = exporter.addSheet('Output Targets', outputTargetHeaders)
        }
        outputTargetsSheet
    }

    AdditionalSheet risksAndThreatsSheet() {
        if (!risksAndThreatsSheet) {
            risksAndThreatsSheet = exporter.addSheet('Risks and Threats', risksAndThreatsHeaders)
        }
        risksAndThreatsSheet
    }
}
