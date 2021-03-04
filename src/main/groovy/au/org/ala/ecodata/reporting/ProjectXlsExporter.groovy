package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.ManagementUnit
import au.org.ala.ecodata.ManagementUnitService
import au.org.ala.ecodata.ProjectService
import au.org.ala.ecodata.Report
import au.org.ala.ecodata.metadata.OutputDataGetter
import au.org.ala.ecodata.metadata.OutputModelProcessor
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.getters.PropertyGetter
import pl.touk.excel.export.multisheet.AdditionalSheet

/**
 * Exports project, site, activity and output data to a Excel spreadsheet.
 */
class ProjectXlsExporter extends ProjectExporter {

    static Log log = LogFactory.getLog(ProjectXlsExporter.class)

    // Avoids name clashes for fields that appear in activities and projects (such as name / description)
    private static final String ACTIVITY_DATA_PREFIX = 'activity_'
    private static final String PROJECT_DATA_PREFIX = 'project_'

    List<String> stateHeaders = (1..3).collect{'State '+it}
    List<String> stateProperties = (0..2).collect{'state'+it}

    List<String> electorateHeaders = (1..15).collect{'Electorate '+it}
    List<String> electorateProperties = (0..14).collect{'elect'+it}

    List<String> projectStateHeaders = (1..5).collect{'State '+it}
    List<String> projectStateProperties = (0..4).collect{'state'+it}

    List<String> commonProjectHeadersWithoutSites = ['Project ID', 'Grant ID', 'External ID', 'Internal order number', 'Organisation', 'Service Provider', 'Management Unit', 'Name', 'Description', 'Program', 'Sub-program', 'Start Date', 'End Date', 'Contracted Start Date', 'Contracted End Date', 'Funding', 'Status', 'Last Modified']
    List<String> commonProjectPropertiesRaw =  ['grantId', 'externalId', 'workOrderId', 'organisationName', 'serviceProviderName', 'managementUnitName', 'name', 'description', 'associatedProgram', 'associatedSubProgram', 'plannedStartDate', 'plannedEndDate', 'contractStartDate', 'contractEndDate', 'funding', 'status', 'lastUpdated']

    List<String> commonProjectPropertiesWithoutSites = ['projectId'] + commonProjectPropertiesRaw.collect{PROJECT_DATA_PREFIX+it}

    List<String> projectApprovalHeaders = ['MERI plan status','Last approval Date', 'Last approved by']
    List<String> projectApprovalProperties = ['planStatus','approvalDate', 'approvedBy']

    List<String> commonProjectHeaders = commonProjectHeadersWithoutSites + stateHeaders + electorateHeaders + projectApprovalHeaders
    List<String> commonProjectProperties = commonProjectPropertiesWithoutSites + stateProperties + electorateProperties + projectApprovalProperties

    List<String> projectHeaders = commonProjectHeadersWithoutSites + projectStateHeaders
    List<String> projectProperties = commonProjectPropertiesWithoutSites + projectStateProperties

    List<String> siteStateHeaders = (1..5).collect{'State '+it}
    List<String> siteStateProperties = (0..4).collect{'state'+it+'-site'}

    List<String> siteElectorateHeaders = (1..40).collect{'Electorate '+it}
    List<String> siteElectorateProperties = (0..39).collect{'elect'+it+'-site'}

    List<String> siteHeaders = commonProjectHeaders + ['Site ID', 'Name', 'Description', 'lat', 'lon', 'Area (m2)', 'Last Modified', 'NRM'] + siteStateHeaders + siteElectorateHeaders
    List<String> siteProperties = commonProjectProperties + ['siteId', 'siteName', 'siteDescription', 'lat', 'lon', 'aream2', 'lastUpdated', 'nrm0-site'] + siteStateProperties + siteElectorateProperties

    List<String> commonActivityHeaders = commonProjectHeaders + ['Activity ID', 'Site ID', 'Planned Start date', 'Planned End date', 'Stage', 'Description', 'Activity Type', 'Form Version', 'Theme', 'Status', 'Report Status', 'Last Modified']
    List<String> activityProperties = commonProjectProperties+ ['activityId', 'siteId', 'plannedStartDate', 'plannedEndDate', 'stage', 'description', 'type', 'formVersion', 'mainTheme', 'progress', 'publicationStatus', 'lastUpdated'].collect{ACTIVITY_DATA_PREFIX+it}
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

    List<String> approvalsHeaders = commonProjectHeaders + ['Date / Time Approved', 'Change Order Numbers','Comment','Approved by']
    List<String> approvalsProperties = commonProjectProperties + ['approvalDate', 'changeOrderNumber', 'comment','approvedBy']

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

    List<String> eventHeaders = commonProjectHeaders + ['Funding', 'Name', 'Description', 'Scheduled Date', 'Media', 'Grant Announcement Date', 'Type']
    List<String> eventProperties = commonProjectProperties + ['funding', 'name', 'description', 'scheduledDate', 'media', 'grantAnnouncementDate', 'Type']
    List<String> baselineHeaders = commonProjectHeaders + ['Baseline Method', 'Baseline']
    List<String> baselineProperties = commonProjectProperties + ['method', 'baseline']

    //Different data model   RLP outcomes show data on rows not cols
    List<String> rlpOutcomeHeaders = commonProjectHeaders + ['Type of outcomes', 'Outcome','Investment Priority']
    List<String> rlpOutcomeProperties = commonProjectProperties +['outcomeType', 'outcome','priority']

    List<String> rlpProjectDetailsHeaders=commonProjectHeaders + ["Project description","Project rationale","Project methodology",	"Project review, evaluation and improvement methodology", "Related Project"]
    List<String> rlpProjectDetailsProperties =commonProjectProperties + ["projectDescription", "projectRationale", "projecMethodology", "projectREI", "relatedProjects"]

    List<String> datasetHeader = commonProjectHeaders + ["Dataset Title", "What program outcome does this dataset relate to?", "What primary or secondary investment priorities or assets does this dataset relate to?","Other Investment Priority","Is this data being collected for reporting against short or medium term outcome statements?", "Is this (a) a baseline dataset associated with a project outcome i.e. against which, change will be measured, (b) a project progress dataset that is tracking change against an established project baseline dataset or (c) a standalone, foundational dataset to inform future management interventions?", "What types of measurements or observations does the dataset include?", "Identify the method(s) used to collect the data", "Describe the method used to collect the data in detail", "Identify any apps used during data collection", "Provide a coordinate centroid for the area surveyed", "First collection date", "Last collection date", "Is this data an addition to existing time-series data collected as part of a previous project, or is being collected as part of a broader/national dataset?", "Who developed/collated the dataset?", "Has a quality assurance check been undertaken on the data?", "Has the data contributed to a publication?", "Where is the data held?", "For all public datasets, please provide the published location. If stored internally by your organisation, write ‘stored internally'", "What format is the dataset?", "Are there any sensitivities in the dataset?", "Primary source of data (organisation or individual that owns or maintains the dataset)", "Dataset custodian (name of contact to obtain access to dataset)", "Progress", "Is Data Collection Ongoing"]
    List<String> datasetProperties = commonProjectProperties + ["name", "programOutcome", "investmentPriorities","otherInvestmentPriority", "term", "type", "measurementTypes", "methods", "methodDescription", "collectionApp", "location", "startDate", "endDate", "addition", "collectorType", "qa", "published", "storageType", "publicationUrl", "format", "sensitivities", "owner", "custodian", "progress", "dataCollectionOngoing"]

    AdditionalSheet projectSheet
    AdditionalSheet sitesSheet
    AdditionalSheet outputTargetsSheet
    AdditionalSheet risksAndThreatsSheet

    OutputModelProcessor processor = new OutputModelProcessor()
    ProjectService projectService

    /** Enables us to pre-create headers for each electorate that will appear in the result set */
    List<String> distinctElectorates

    /** Map of key: management unit id, value: management unit name */
    Map<String, String> managementUnitNames

    ProjectXlsExporter(ProjectService projectService, XlsExporter exporter, ManagementUnitService managementUnitService) {
        super(exporter)
        this.projectService = projectService
        distinctElectorates = new ArrayList()
        setupManagementUnits(managementUnitService)
    }

    ProjectXlsExporter(ProjectService projectService, XlsExporter exporter, List<String> tabsToExport, List<String> electorates, ManagementUnitService managementUnitService, Map<String, Object> documentMap = [:]) {
        super(exporter, tabsToExport, documentMap, TimeZone.default)
        this.projectService = projectService
        distinctElectorates = new ArrayList(electorates?:[])
        distinctElectorates.sort()
        projectHeaders += distinctElectorates
        projectProperties += distinctElectorates
        setupManagementUnits(managementUnitService)
    }

    /** This sets up a lazy Map that will query and cache management uints names on demand. */
    private Map setupManagementUnits(ManagementUnitService managementUnitService) {
        managementUnitNames = [:].withDefault { String managementUnitId ->
            ManagementUnit mu = managementUnitService.get(managementUnitId)
            mu?.name
        }
    }

    void export(Map project) {

        addCommonProjectData(project)

        addProjectGeo(project)
        exportProject(project)
        exportOutputTargets(project)
        exportSites(project)
        exportDocuments(project)
        exportActivities(project )
        exportParticipantInfo(project)
        exportRisks(project)
        exportMeriPlan(project)
        exportReports(project)
        exportReportSummary(project)
        exportBlog(project)
        exportDataSet(project)


        if(exporter.workbook.numberOfSheets == 0){
            createEmptySheet()
        }
    }

    /**
     * Sets up / populates common data that is displayed on every tab.
     * @param project the project being exported.
     */
    private addCommonProjectData(Map project) {
        commonProjectPropertiesRaw.each {
            project[PROJECT_DATA_PREFIX+it] = project.remove(it)
        }
        if (project.managementUnitId) {
            project[PROJECT_DATA_PREFIX+'managementUnitName'] = managementUnitNames[project.managementUnitId]
        }
    }

    private addProjectGeo(Map project) {
        List geo = ['state',  'elect']
        Map geoData = [:].withDefault{[]}
        project.sites?.each { site ->
            if (site?.extent?.geometry instanceof Map) {
                Map props = site?.extent?.geometry ?: [:]
                geo.each { facet ->
                    Object value = props[facet]
                    if (value instanceof List) {
                        geoData[facet].addAll(value)
                    } else {
                        geoData[facet] << value
                    }
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

     void exportActivities(Map project) {
         tabsToExport.each{ tab ->
             if(tab =="Activity Summary"){
                 exportActivitySummary(project)
             } else {
                 List activities = project?.activities?.findAll{it.type == tab}
                 if(activities) {
                     activities.each {
                         exportActivity(project, it)
                     }
                 }
             }
         }

     }

    private void exportActivity(Map project, Map activity) {
        Map commonData = commonActivityData(project, activity)
        String activityType = activity.type
        List exportConfig = getActivityExportConfig(activityType)

        // Split into all the bits.
        Map<String, List> configPerSection = exportConfig.groupBy{it.section}
        // We are relying on the grouping preserving order here....
        configPerSection.each { String section, List sectionConfig ->
            String sheetName = activityType
            if (configPerSection.size() > 1){
                sheetName = section +' '+activityType
            }
            List sheetData = prepareActivityDataForExport(activity, section)
            exportActivityOrOutput(sheetName, sectionConfig, commonData, sheetData)
        }

    }

    private void exportActivityOrOutput(String sheetName, List exportConfig, Map commonData, List activityOrOutputData) {
        List blank = commonActivityHeaders.collect{""}
        List versionHeaders = blank + exportConfig.collect{ it.formVersion }
        List propertyHeaders = blank + exportConfig.collect{ it.property }

        List outputGetters = activityProperties + exportConfig.collect{ it.getter }
        List headers = commonActivityHeaders + exportConfig.collect{ it.header }

        AdditionalSheet outputSheet = createSheet(sheetName, [propertyHeaders, versionHeaders, headers])
        int outputRow = outputSheet.sheet.lastRowNum
        List outputData = activityOrOutputData.collect { commonData + it }
        outputSheet.add(outputData, outputGetters, outputRow + 1)
    }

    private void exportActivitySummary(Map project) {
        AdditionalSheet sheet = getSheet("Activity Summary", commonActivityHeaders)
        project.activities.each { activity ->
            Map activityData = commonActivityData(project, activity)
            sheet.add(activityData, activityProperties, sheet.getSheet().lastRowNum + 1)
        }
    }

    /**
     * Collect participant info cross multi activities to one tab sheet
     * @param project
     */
    private void exportParticipantInfo(Map project) {
        String tab = 'Participant Information'
        if (shouldExport(tab)){
            List outputGetters =[]
            List headers = []
            List outputData = []

            project?.activities.each{activity->
                    Map commonData = commonActivityData(project, activity)
                    Map sheetData = buildOutputSheetData(activity, tab)
                    //Combine data from output with  header,getter and  common data
                    outputGetters += sheetData.getters
                    headers += sheetData.headers
                    List currentData = sheetData.data.collect { commonData + it }
                    outputData += currentData
                }
            //Remove duplicated headers and getters
            //getter is an object. Need to Stringfy to unique
            outputGetters = activityProperties + outputGetters.unique{it.toString()}
            headers = commonActivityHeaders + headers.unique()
            AdditionalSheet outputSheet = createSheet(tab, headers)
            int outputRow = outputSheet.sheet.lastRowNum

            outputSheet.add(outputData, outputGetters, outputRow + 1)
        }
    }

    private void exportSites(Map project) {
        if (shouldExport('Sites')) {
            sitesSheet()
            if (project.sites) {
                def sites = project.sites.collect {
                    def centre = it.extent?.geometry?.centre
                    def aream2 = it.extent?.geometry?.aream2
                    Map data = [siteId: it.siteId, siteName: it.name, siteDescription: it.description, lat: centre ? centre[1] : "", lon: centre ? centre[0] : "", aream2: aream2, lastUpdated: it.lastUpdated] + project
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

            List<String> projectElectorates = project.sites?.collect { it?.extent?.geometry?.elect }?.flatten()?.findAll()

            distinctElectorates.each{ electorate ->
                project[electorate] = projectElectorates.contains(electorate)? 'Y' : 'N'
            }

            projectSheet.add([project], properties, row + 1)
        }
    }

    private exportDataSet(Map project) {
        if (shouldExport("Dataset")) {
            AdditionalSheet sheet = getSheet("Data_set_Summary", datasetHeader)
            int row = sheet.getSheet().lastRowNum

            List data = project?.custom?.dataSets?.collect { Map dataValue ->
                Map dataSets = [:]
                dataValue.each{k, v -> dataSets.put(k,v)}

                // joining all investmentPriority, methods, measurementTypes and sensitivities  from list to String

                if (dataSets?.investmentPriorities){
                    dataSets["investmentPriorities"] = dataValue?.investmentPriorities?.join(", ")
                }

                if (dataSets?.methods){
                    dataSets["methods"] = dataValue?.methods?.join(", ")
                }

                if (dataSets?.measurementTypes){
                    dataSets["measurementTypes"] = dataValue?.measurementTypes?.join(", ")
                }

                if (dataSets?.sensitivities){
                    dataSets["sensitivities"] = dataValue?.sensitivities?.join(", ")
                }

                dataSets.putAll(project)
                log.debug("Exporting data set for this projectId: " + dataSets.projectId)
                dataSets
            }

            sheet.add(data ?: [], datasetProperties, row + 1)
        }
    }

    private void exportMeriPlan(Map project) {
        String[] meriPlanTabs = [
                "MERI_Budget","MERI_Outcomes","MERI_Monitoring","MERI_Project Partnerships","MERI_Project Implementation",
                "MERI_Key Evaluation Question","MERI_Priorities","MERI_WHS and Case Study",'MERI_Risks and Threats',
                "MERI_Attachments", "MERI_Baseline", "MERI_Event", "MERI_Approvals", "RLP_Outcomes", "RLP_Project_Details", "RLP_Key_Threats", "RLP_Services_and_Targets"
        ]
        //Add extra info about approval status if any MERI plan information is to be exported.
        if (shouldExport(meriPlanTabs)){
            Map approval  = projectService.getMostRecentMeriPlanApproval(project.projectId)
            if (approval) {
                project['approvalDate'] = approval.approvalDate
                project['approvedBy'] =  approval.approvedBy
            }
        }

        exportBudget(project)
        exportOutcomes(project)
        exportMonitoring(project)
        exportProjectPartnerships(project)
        exportProjectImplementation(project)
        exportKeyEvaluationQuestion(project)
        exportPriorities(project)
        exportWHSAndCaseStudy(project)
        exportAttachments(project)
        exportBaseline(project)
        exportEvents(project)
        exportApprovals(project)
        exportRLPOutcomes(project)
        exportRLPProjectDetails(project)
        exportRLPKeyThreats(project)
        exportRLPServicesTargets(project)
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
        if (shouldExport('MERI_Risks and Threats')) {
            risksAndThreatsSheet()
            int row = risksAndThreatsSheet.getSheet().lastRowNum
            if (project.risks && project.risks.rows) {
                List data = project.risks.rows.collect { it + project }
                risksAndThreatsSheet.add(data, risksAndThreatsProperties, row + 1)
            }

        }
    }

    private void exportApprovals(Map project) {
        if (shouldExport('MERI_Approvals')) {
            AdditionalSheet sheet = getSheet("MERI_Approvals", approvalsHeaders)
            int row = sheet.getSheet().lastRowNum

            List approvals  = projectService.getMeriPlanApprovalHistory(project.projectId)
             if (approvals && approvals.size()>0) {
                List data = approvals.collect { it + project }
                sheet.add(data, approvalsProperties, row + 1)
            }

        }
    }

    private void exportAttachments(Map project) {
        List meriPlanAttachments = project.documents?.findAll {it.role == "programmeLogic"}
        exportList("MERI_Attachments", project, meriPlanAttachments, attachmentHeaders, attachmentProperties)
    }

    private void exportBaseline(Map project){
        if (shouldExport("MERI_Baseline")) {
            AdditionalSheet sheet = getSheet("MERI_Baseline", baselineHeaders)
            int row = sheet.getSheet().lastRowNum
            List data = project?.custom?.details?.baseline?.rows?.collect { Map baseline ->
                Map baseLineItem = [:]
                baseline.each{k, v -> baseLineItem.put(k,v)}
                baseLineItem.putAll(project)
                baseLineItem
            }
            sheet.add(data?:[], baselineProperties, row+1)
        }
    }

    private void exportEvents(Map project) {
        if (shouldExport("MERI_Event")) {
            AdditionalSheet sheet = getSheet("MERI_Event", eventHeaders)
            int row = sheet.getSheet().lastRowNum
            List data = project?.custom?.details?.events?.collect { Map event ->
                Map eventItem = [:]
                event.each{k, v -> eventItem.put(k,v)}
                eventItem.putAll(project)
                eventItem
            }
            sheet.add(data?:[], eventProperties, row+1)
        }
    }

    private void exportRLPProjectDetails(Map project){
        if (shouldExport("RLP_Project_Details")) {
            /**
             * RLP outcome does not use HEADERs from DB
             */
            AdditionalSheet sheet = getSheet("RLP Project Details", rlpProjectDetailsHeaders)
            int row = sheet.getSheet().lastRowNum

            List data = []
            //["projectDescription","projectRationale","projecMethodology", "projectREI"]
            Map item = [:]
            if (project?.custom?.details?.description){
                item["projectDescription"]=project?.custom?.details?.description
            }

            if (project?.custom?.details?.implementation){
                item["projecMethodology"]=project?.custom?.details?.implementation?.description
            }

            if (project?.custom?.details?.rationale){
                item["projectRationale"]=project?.custom?.details?.rationale
            }

            if (project?.custom?.details?.projectEvaluationApproach){
                item["projectREI"]=project?.custom?.details?.projectEvaluationApproach
            }
            if (project?.custom?.details?.relatedProjects){
                item["relatedProjects"] = project?.custom?.details?.relatedProjects
            }

            item.putAll(project)

            data.add(item)

            sheet.add(data?:[], rlpProjectDetailsProperties, row+1)
        }
    }

    private void exportRLPOutcomes(Map project) {

        if (shouldExport("RLP_Outcomes")) {
            /**
             * RLP outcome does not use HEADERs from DB
             */
            AdditionalSheet sheet = getSheet("RLP Outcomes", rlpOutcomeHeaders)
            int row = sheet.getSheet().lastRowNum
            Map fields = [:]
            fields["secondaryOutcomes"] = "Secondary Outcome/s"
            fields["shortTermOutcomes"] = "Short-term"
            fields["midTermOutcomes"] = "Medium-term"
            List data = []
            //['outcomeType', 'outcome','priority']

            if (project?.custom?.details?.outcomes?.primaryOutcome){
                def po = project?.custom?.details?.outcomes?.primaryOutcome
                Map outcome = [:]
                outcome.put('outcomeType',"Primary outcome")
                outcome.put('outcome',po.description)
                String assets = po.assets?.join(", ")
                outcome.put('priority',assets)
                data.add(project+outcome)
            }
            if (project?.custom?.details?.outcomes?.otherOutcomes){
                def assets = project?.custom?.details?.outcomes?.otherOutcomes
                Map outcome = [:]
                outcome.put("outcomeType", "Other Outcomes")
                String otherOutcome = assets.join(",")
                outcome.put("outcome", otherOutcome)
                data.add(project + outcome)
            }

            fields.each{ocitem, desc->
                List oocs = project?.custom?.details?.outcomes?.get(ocitem)
                oocs?.collect{ Map oc ->
                    Map outcome = [:]
                    outcome.put('outcomeType',desc)
                    outcome.put('outcome',oc.description)
                    String assets = oc.assets?.join(",")
                    outcome.put('priority',assets)
                    data.add(project+outcome)
                }
            }

            sheet.add(data?:[], rlpOutcomeProperties, row+1)
        }
    }

    private  void exportRLPKeyThreats(Map project){
        List<String> rlpKeyThreatHeaders =commonProjectHeaders + ['Key threats and/or threatening processes', 'Interventions to address threats']
        List<String> rlpKeyThreatProperties =commonProjectProperties + ['keyThreat', 'keyTreatIntervention']

        if (shouldExport("RLP_Key_Threats")) {
            /**
             * RLP outcome does not use HEADERs from DB
             */
            AdditionalSheet sheet = getSheet("RLP Key Threats", rlpKeyThreatHeaders)
            int row = sheet.getSheet().lastRowNum

            List data = []

            if (project?.custom?.details?.threats?.rows){
                def items = project?.custom?.details?.threats?.rows
                items.each{ Map item ->
                    //['keyThreat', 'keyTreatIntervention']
                    Map threat = [:]
                    threat["keyThreat"] = item.threat
                    threat["keyTreatIntervention"] = item.intervention
                    threat.putAll(project)
                    data.add(project + threat)
                }
            }

            sheet.add(data?:[], rlpKeyThreatProperties, row+1)
        }
    }

    private void exportRLPServicesTargets(project){
        if (!shouldExport("RLP_Services_and_Targets"))
            return

        List<String> rlpSTProperties=commonProjectProperties +["service", "targetMeasure", "total", "2018/2019","2019/2020", "2020/2021", "2021/2022", "2022/2023", "targetDate" ]
        List<String> rlpSTHeaders=commonProjectHeaders +["Service", "Target measure", "Total to be delivered", "2018/2019","2019/2020", "2020/2021", "2021/2022", "2022/2023", "Target Date"]
        def results = metadataService.getProjectServicesWithTargets(project)

        AdditionalSheet sheet = getSheet("Project services and targets", rlpSTHeaders)
        int row = sheet.getSheet().lastRowNum

        List data = []
        results.each { item ->
            def serviceName = item.name
            item.scores.each {
                    Map st = [:]
                    st['service'] = serviceName
                    st['targetMeasure'] = it.label
                    st['total'] = it.target
                    st['targetDate'] = it.targetDate
                    it.periodTargets.each { pt ->
                        st[pt.period] = pt.target
                    }
                    data.add(project+st)
            }
        }

        sheet.add(data?:[], rlpSTProperties, row+1)
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
