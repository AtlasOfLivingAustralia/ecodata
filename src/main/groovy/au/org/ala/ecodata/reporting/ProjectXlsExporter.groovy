package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.*
import au.org.ala.ecodata.metadata.OutputModelProcessor
import grails.util.Holders
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import pl.touk.excel.export.multisheet.AdditionalSheet
/**
 * Exports project, site, activity and output data to a Excel spreadsheet.
 */
class ProjectXlsExporter extends ProjectExporter {

    static Log log = LogFactory.getLog(ProjectXlsExporter.class)

    // Avoids name clashes for fields that appear in activities and projects (such as name / description)
    private static final String PROJECT_DATA_PREFIX = 'project_'

    List<String> stateHeaders = (1..3).collect{'State '+it}
    List<String> stateProperties = (0..2).collect{'state'+it}

    List<String> electorateHeaders = (1..15).collect{'Electorate '+it}
    List<String> electorateProperties = (0..14).collect{'elect'+it}

    List<String> projectStateHeaders = (1..5).collect{'State '+it}
    List<String> projectStateProperties = (0..4).collect{'state'+it}

    List<String> configurableIntersectionHeaders = getIntersectionHeaders()
    List<String> configurableIntersectionProperties = getIntersectionProperties()

    List<String> commonProjectHeadersWithoutSites = ['Project ID', 'Grant ID', 'External ID', 'Internal order number', 'Work order id', 'Contracted recipient name', 'Recipient (ID)', 'Management Unit', 'Name', 'Description', 'Program', 'Sub-program', 'Start Date', 'End Date', 'Contracted Start Date', 'Contracted End Date', 'Funding', 'Funding Type', 'Status', "Last Modified"] + configurableIntersectionHeaders
    List<String> commonProjectPropertiesRaw =  ['grantId', 'externalId', 'internalOrderId', 'workOrderId', 'organisationName', 'organisationId', 'managementUnitName', 'name', 'description', 'associatedProgram', 'associatedSubProgram', 'plannedStartDate', 'plannedEndDate', 'contractStartDate', 'contractEndDate', 'funding', 'fundingType', 'status', 'lastUpdated'] + configurableIntersectionProperties

    List<String> projectHeadersWithTerminationReason = ['Project ID', 'Grant ID', 'External ID', 'Internal order number', 'Work order id', 'Contracted recipient name', 'Recipient (ID)', 'Management Unit', 'Name', 'Description', 'Program', 'Sub-program', 'Start Date', 'End Date', 'Contracted Start Date', 'Contracted End Date', 'Funding', 'Funding Type', 'Status'] + configurableIntersectionHeaders + ['Termination Reason', 'Last Modified']
    List<String> projectPropertiesTerminationReason =  ['grantId', 'externalId', 'internalOrderId', 'workOrderId', 'organisationName', 'organisationId', 'managementUnitName', 'name', 'description', 'associatedProgram', 'associatedSubProgram', 'plannedStartDate', 'plannedEndDate', 'contractStartDate', 'contractEndDate', 'funding', 'fundingType', 'status'] + configurableIntersectionProperties

    List<String> projectPropertiesWithTerminationReason = ['projectId'] + projectPropertiesTerminationReason.collect{PROJECT_DATA_PREFIX+it} + ["terminationReason", PROJECT_DATA_PREFIX+"lastUpdated"]

    List<String> commonProjectPropertiesWithoutSites = ['projectId'] + commonProjectPropertiesRaw.collect{PROJECT_DATA_PREFIX+it}


    List<String> projectApprovalHeaders = ['MERI plan status','Last approval Date', 'Last approved by']
    List<String> projectApprovalProperties = ['planStatus','approvalDate', 'approvedBy']

    List<String> commonProjectHeaders = commonProjectHeadersWithoutSites + stateHeaders + electorateHeaders + projectApprovalHeaders
    List<String> commonProjectProperties = commonProjectPropertiesWithoutSites + stateProperties + electorateProperties + projectApprovalProperties

    List<String> associatedOrgProjectHeaders = (1..3).collect{['Contracted recipient name '+it, 'Organisation ID '+it, 'Organisation relationship from date '+it, 'Organisation relationship to date '+it, 'Organisation relationship '+it]}.flatten()
    List<String> associatedOrgProperties = ['name', 'organisationId', 'fromDate', 'toDate', 'description']

    List<String> associatedOrgProjectProperties = (1..3).collect{['associatedOrg_name'+it, 'associatedOrg_organisationId'+it, 'associatedOrg_fromDate'+it, 'associatedOrg_toDate'+it, 'associatedOrg_description'+it]}.flatten()

    List organisationDetailsHeaders = ['Project ID', 'Grant ID', 'External ID', 'Program', 'Sub-program', 'Management Unit', 'Project Name', 'Project start date', 'Project end date', 'Contracted recipient name', 'Organisation ID', 'Organisation relationship from date', 'Organisation relationship to date', 'Organisation relationship', 'ABN', 'MERIT organisation name']
    List organisationDetailsProperties = ['projectId', 'project_grantId', 'project_externalId', 'project_associatedProgram', 'project_associatedSubProgram', 'project_managementUnitName', 'project_name', 'project_plannedStartDate', 'project_plannedEndDate', 'name', 'organisationId', 'fromDate', 'toDate', 'description', 'abn', 'organisationName']

    List<String> projectHeaders = projectHeadersWithTerminationReason + associatedOrgProjectHeaders + projectStateHeaders
    List<String> projectProperties = projectPropertiesWithTerminationReason + associatedOrgProjectProperties + projectStateProperties


    List<String> siteStateHeaders = (1..5).collect{'State '+it}
    List<String> siteStateProperties = (0..4).collect{'state'+it+'-site'}

    List<String> siteElectorateHeaders = (1..40).collect{'Electorate '+it}
    List<String> siteElectorateProperties = (0..39).collect{'elect'+it+'-site'}

    List<String> siteHeaders = commonProjectHeaders + ['Site ID', 'Name', 'Description', 'lat', 'lon', 'Area (m2)', 'Last Modified', 'NRM'] + siteStateHeaders + siteElectorateHeaders
    List<String> siteProperties = commonProjectProperties + ['siteId', 'siteName', 'siteDescription', 'lat', 'lon', 'aream2', 'lastUpdated', 'nrm0-site'] + siteStateProperties + siteElectorateProperties

    List<String> commonActivityHeaders = commonProjectHeaders + ['Activity ID', 'Site ID', 'Planned Start date', 'Planned End date', 'Stage', 'Report Type', 'Description', 'Activity Type', 'Form Version', 'Theme', 'Status', 'Report From Date', 'Report To Date', 'Report Financial Year', 'Report Status', 'Last Modified']
    List<String> activityProperties = commonProjectProperties+ ['activityId', 'siteId', 'plannedStartDate', 'plannedEndDate', 'stage', 'reportType', 'description', 'type', 'formVersion', 'mainTheme', 'progress', 'fromDate', 'toDate', 'financialYear', 'publicationStatus', 'lastUpdated'].collect{ACTIVITY_DATA_PREFIX+it}
    List<String> activitySummaryHeaders = commonProjectHeaders + ['Activity ID', 'Site ID', 'Planned Start date', 'Planned End date', 'Report From Date', 'Report To Date', 'Report Financial Year', 'Stage', 'Report Type', 'Description', 'Activity Type', 'Theme', 'Status', 'Report Status', 'Last Modified']
    List<String> activitySummaryProperties = commonProjectProperties+ ['activityId', 'siteId', 'plannedStartDate', 'plannedEndDate', 'fromDate', 'toDate', 'financialYear', 'stage', 'reportType', 'description', 'type', 'mainTheme', 'progress', 'publicationStatus', 'lastUpdated'].collect{ACTIVITY_DATA_PREFIX+it}

    List<String> outputTargetHeaders = commonProjectHeaders + ['Output Target Measure', 'Target', 'Delivered - approved', 'Delivered - total', 'Units']
    List<String> outputTargetProperties = commonProjectProperties + ['scoreLabel', new TabbedExporter.StringToDoublePropertyGetter('target'), 'deliveredApproved', 'deliveredTotal', 'units']
    List<String> risksAndThreatsHeaders = commonProjectHeaders + ['Type of threat / risk', 'Description', 'Likelihood', 'Consequence', 'Risk rating', 'Current control', 'Residual risk']
    List<String> risksAndThreatsProperties = commonProjectProperties + ['threat', 'description', 'likelihood', 'consequence', 'riskRating', 'currentControl', 'residualRisk']
    List<String> fundingPeriodHeaders = ['2011/2012', '2012/2013', '2013/2014', '2014/2015', '2015/2016', '2016/2017', '2017/2018', '2018/2019', '2019/2020', '2020/2021', '2021/2022', '2022/2023', '2023/2024', '2024/2025', '2025/2026', '2026/2027', '2027/2028', '2028/2029', '2029/2030']
    List<String> fundingPeriodProperties = ['2011/2012', '2012/2013', '2013/2014', '2014/2015', '2015/2016', '2016/2017', '2017/2018', '2018/2019', '2019/2020', '2020/2021', '2021/2022', '2022/2023', '2023/2024', '2024/2025', '2025/2026', '2026/2027', '2027/2028', '2028/2029', '2029/2030']
    List<String> budgetHeaders = commonProjectHeaders + ['Investment / Priority Area', 'Description'] + fundingPeriodHeaders
    List<String> budgetProperties = commonProjectProperties + ['investmentArea', 'budgetDescription'] + fundingPeriodProperties
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
    List<String> projectDeliveryAssumptionsHeaders = commonProjectHeaders + ['Project delivery assumptions']

    List<String> keyEvaluationQuestionHeaders = commonProjectHeaders + ['Project Key evaluation question (KEQ)', 'How will KEQ be monitored?']
    List<String> keyEvaluationQuestionProperties = commonProjectProperties + ['data1', 'data2']
    List<String> prioritiesHeaders = commonProjectHeaders + ['Document name', 'Relevant section', 'Explanation of strategic alignment', 'Link to document']
    List<String> prioritiesProperties = commonProjectProperties + ['data1', 'data2', 'data3', 'documentUrl']
    List<String> whsAndCaseStudyHeaders = commonProjectHeaders + ['Are you aware of, and compliant with, your workplace health and safety legislation and obligations', 'Do you have appropriate policies and procedures in place that are commensurate with your project activities?', 'Are you willing for your project to be used as a case study by the Department?']
    List<String> whsAndCaseStudyProperties = commonProjectProperties + ['obligations', 'policies', 'caseStudy']
    List<String> projectAssetHeaders = commonProjectHeaders + ["Asset", "Category"]
    List<String> projectAssetProperties = commonProjectProperties + ["description", "category"]

    List<String> approvalsHeaders = commonProjectHeaders + ['Date / Time Approved', 'Change Order Numbers','Comment','Approved by']
    List<String> approvalsProperties = commonProjectProperties + ['approvalDate', 'changeOrderNumber', 'comment','approvedBy']

    List<String> attachmentHeaders = commonProjectHeaders + ['Title', 'Attribution', 'File name']
    List<String> attachmentProperties = commonProjectProperties + ['name', 'attribution', 'filename']
    List<String> reportHeaders = commonProjectHeaders + ['Report From Date', 'Report To Date', 'Report Financial Year', 'Report Type', 'Description', 'Action', 'Action Date', 'Actioned By', 'Weekdays since last action', 'Comment', 'Categories']
    List<String> reportProperties = commonProjectProperties + ['reportName', 'fromDate', 'toDate',  'financialYear',  'reportType', 'reportStatus', 'dateChanged', 'changedBy', 'delta', 'comment', 'categories']
    List<String> reportSummaryHeaders = commonProjectHeaders + ['Stage', 'Stage from', 'Stage to', 'Activity Count', 'Current Report Status', 'Date of action', 'No. weekdays since previous action', 'Actioned By: user number', 'Actioned by: user name']
    List<String> reportSummaryProperties = commonProjectProperties + ['reportName', 'fromDate', 'toDate', 'activityCount', 'reportStatus', 'dateChanged', 'delta', 'changedBy', 'changedByName']
    List<String> documentHeaders = commonProjectHeaders + ['Title', 'Attribution', 'File name', 'Purpose']
    List<String> documentProperties = commonProjectProperties + ['name', 'attribution', 'filename', 'role']
    List<String> blogHeaders = commonProjectHeaders + ['Type', 'Date', 'Title', 'Content', "See more URL"]
    List<String> blogProperties = commonProjectProperties + ['type', 'date', 'title', 'content', 'viewMoreUrl']

    List<String> eventHeaders = commonProjectHeaders + ['Funding', 'Name', 'Description', 'Scheduled Date', 'Media', 'Grant Announcement Date', 'Type']
    List<String> eventProperties = commonProjectProperties + ['funding', 'name', 'description', 'scheduledDate', 'media', 'grantAnnouncementDate', 'Type']
    List<String> baselineHeaders = commonProjectHeaders + ['Outcome statement/s','Baseline Method', 'Baseline', 'Code', 'Evidence', 'Monitoring Data Status', 'Baseline Protocol', 'Project Service/Target measure']
    List<String> baselineProperties = commonProjectProperties + ['relatedOutcomes', 'method', 'baseline', 'code', 'evidence', 'monitoringDataStatus', 'protocols', 'relatedTargetMeasures']

    //Different data model   RLP outcomes show data on rows not cols
    List<String> rlpOutcomeHeaders = commonProjectHeaders + ['Type of outcomes', 'Outcome','Investment Priority', 'Related Program Outcome']
    List<String> rlpOutcomeProperties = commonProjectProperties +['outcomeType', 'outcome','priority','relatedOutcome']

    List<String> rlpProjectDetailsHeaders=commonProjectHeaders + ["Project description","Project rationale","Project methodology",	"Project review, evaluation and improvement methodology", "Related Project"]
    List<String> rlpProjectDetailsProperties =commonProjectProperties + ["projectDescription", "projectRationale", "projectMethodology", "projectREI", "relatedProjects"]

    List<String> rdpProjectDetailsHeaders=commonProjectHeaders + ["Does this project directly support a priority place?","Supported priority places", "Are First Nations people (Indigenous) involved in the project?", "What is the nature of the involvement?","Project delivery assumptions","Project review, evaluation and improvement methodology"]
    List<String> rdpProjectDetailsProperties =commonProjectProperties + ["supportsPriorityPlace", "supportedPriorityPlaces", "indigenousInvolved", "indigenousInvolvementType", "projectMethodology", "projectREI"]

    List<String> datasetHeader = commonProjectHeaders + ["Dataset Title", "What program outcome does this dataset relate to?", "What primary or secondary investment priorities or assets does this dataset relate to?","Other Investment Priority","Which project service and outcome/s does this data set support?","Is this data being collected for reporting against short or medium term outcome statements?", "Is this (a) a baseline dataset associated with a project outcome i.e. against which, change will be measured, (b) a project progress dataset that is tracking change against an established project baseline dataset or (c) a standalone, foundational dataset to inform future management interventions?","Other Dataset Type","Which project baseline does this data set relate to or describe?","What EMSA protocol was used when collecting the data?", "What types of measurements or observations does the dataset include?","Other Measurement Type","Identify the method(s) used to collect the data", "Describe the method used to collect the data in detail", "Identify any apps used during data collection", "Provide a coordinate centroid for the area surveyed", "First collection date", "Last collection date", "Is this data an addition to existing time-series data collected as part of a previous project, or is being collected as part of a broader/national dataset?", "Has your data been included in the Threatened Species Index?","Date of upload", "Who developed/collated the dataset?", "Has a quality assurance check been undertaken on the data?", "Has the data contributed to a publication?", "Where is the data held?", "For all public datasets, please provide the published location. If stored internally by your organisation, write ‘stored internally'", "What format is the dataset?","What is the size of the dataset (KB)?","Unknown size", "Are there any sensitivities in the dataset?", "Primary source of data (organisation or individual that owns or maintains the dataset)", "Dataset custodian (name of contact to obtain access to dataset)", "Progress", "Is Data Collection Ongoing", "Technical data from Monitor"]
    List<String> datasetProperties = commonProjectProperties + ["name", "programOutcome", "investmentPriorities","otherInvestmentPriority","projectOutcomes", "term", "type", "otherDataSetType","baselines", "protocol", "measurementTypes","otherMeasurementType", "methods", "methodDescription", "collectionApp", "location", "startDate", "endDate", "addition", "threatenedSpeciesIndex","threatenedSpeciesIndexUploadDate", "collectorType", "qa", "published", "storageType", "publicationUrl", "format","sizeInKB","sizeUnknown", "sensitivities", "owner", "custodian", "progress", "dataCollectionOngoing", "orgMintedIdentifier"]

    List<String> electorateInternalOrderNoHeader = (2..3).collect{'Internal order number '+it}
    List<String> electorateInternalOrderNoProperties = (1..2).collect{PROJECT_DATA_PREFIX+'internalOrderId'+it}
    List<String> techOneCodeHeaders = ['Tech One Project Code'] + (2..3).collect{'Tech One Project Code '+it}
    List<String> techOneCodeProperties = [PROJECT_DATA_PREFIX+'techOneProjectCode'] + (1..2).collect{PROJECT_DATA_PREFIX+'techOneProjectCode'+it}

    List<String> electorateCoordHeaders = commonProjectHeadersWithoutSites + stateHeaders + electorateInternalOrderNoHeader + techOneCodeHeaders + ['GO ID', 'Work order id', 'Funding Recipient Entity ABN'] + fundingPeriodHeaders + ['Total  Funding (GST excl)', 'Nationwide/Statewide', 'Primary Electorate', 'Primary State', 'Other Electorates', 'Other States', 'Electorate Reporting Comment', 'Grant/Procurement/Other', 'Election Commitment Calendar Year', 'Portfolio', 'Agency Managing Grant Delivery']
    List<String> electorateCoordProperties = commonProjectPropertiesWithoutSites + stateProperties + electorateInternalOrderNoProperties + techOneCodeProperties + [PROJECT_DATA_PREFIX+'grantAwardId', PROJECT_DATA_PREFIX+'workOrderId', PROJECT_DATA_PREFIX+'abn'] + fundingPeriodProperties + [PROJECT_DATA_PREFIX+'gstFunding', 'nationwide', 'geographicInfo.primaryElectorate', 'geographicInfo.primaryState', new ListGetter('geographicInfo.otherElectorates'), new ListGetter('geographicInfo.otherStates'), 'comment', PROJECT_DATA_PREFIX+'fundingType', 'electionCommitmentYear', 'portfolio', 'manager']

    List<String> nativeThreatsHeaders =commonProjectHeaders + ['Could this control approach pose a threat to Native Animals/Plants or Biodiversity?', 'Details']
    List<String> nativeThreatsProperties =commonProjectProperties + ['couldBethreatToSpecies', 'details']

    List<String> pestControlMethodsHeaders =commonProjectHeaders + ['Are there any current control methods for this pest?', 'Has it been successful?', 'Type of method', 'Details']
    List<String> pestControlMethodsProperties =commonProjectProperties + ['currentControlMethod', 'hasBeenSuccessful', 'methodType', 'details']

    List<String> rdpKeyThreatHeaders =commonProjectHeaders + ['Outcome Statement/s', 'Threats / Threatening processes', 'Description', 'Project service / Target measure/s to address threats', 'Methodology', 'Evidence to be retained']
    List<String> rdpKeyThreatProperties =commonProjectProperties + ['relatedOutcomes', 'threatCode', 'keyThreat','relatedTargetMeasures', 'keyTreatIntervention', 'evidence']

    List<String> rdpSTHeaders=commonProjectHeaders +["Service", "Target measure", 'Delivered - approved', 'Delivered - total', "Project Outcome/s", "Total to be delivered","2023/2024","2024/2025","2025/2026","2026/2027","2027/2028","2028/2029","2029/2030"]
    List<String> rdpSTProperties=commonProjectProperties +["service", "targetMeasure", 'deliveredApproved', 'deliveredTotal', "relatedOutcomes", "total", "2023/2024","2024/2025","2025/2026","2026/2027","2027/2028","2028/2029","2029/2030"]

    List<String> rlpSTProperties=commonProjectProperties +["service", "targetMeasure", 'deliveredApproved', 'deliveredTotal', "relatedOutcomes", "total", "2018/2019","2019/2020", "2020/2021", "2021/2022", "2022/2023", "targetDate" ]
    List<String> rlpSTHeaders=commonProjectHeaders +["Service", "Target measure", 'Delivered - approved', 'Delivered - total', "Project Outcome/s", "Total to be delivered", "2018/2019","2019/2020", "2020/2021", "2021/2022", "2022/2023", "2023/2024", "2024/2025", "Target Date"]

    List<String> rlpKeyThreatHeaders =commonProjectHeaders + ['Key threats and/or threatening processes', 'Interventions to address threats']
    List<String> rlpKeyThreatProperties =commonProjectProperties + ['keyThreat', 'keyTreatIntervention']

    List<String> rdpMonitoringIndicatorsHeaders =commonProjectHeaders + ['Code', 'Monitoring methodology', 'Project service / Target measure/s', 'Monitoring method', 'Evidence to be retained']
    List<String> rdpMonitoringIndicatorsProperties =commonProjectProperties + ['relatedBaseline', 'data1', 'relatedTargetMeasures','protocols', 'evidence']


    OutputModelProcessor processor = new OutputModelProcessor()
    ProjectService projectService
    OrganisationService organisationService

    /** Enables us to pre-create headers for each electorate that will appear in the result set */
    List<String> distinctElectorates

    /** Map of key: management unit id, value: management unit name */
    Map<String, String> managementUnitNames

    Map<String, String> fundingAbn

    Map<String, String> programFundingType

    Map<String, String> programGrantAwardId

    /** If set to true, activities containing more than one form section will be split over one tab per form section */
    boolean formSectionPerTab = false

    ProjectXlsExporter(ProjectService projectService, XlsExporter exporter, ManagementUnitService managementUnitService, OrganisationService organisationService, ProgramService programService) {
        super(exporter)
        this.projectService = projectService
        this.organisationService = organisationService
        distinctElectorates = new ArrayList()
        addAdditionalSpeciesColumns = true
        setupManagementUnits(managementUnitService)
        setupFundingAbn(organisationService)
        setupProgramData(programService)
    }

    ProjectXlsExporter(ProjectService projectService, XlsExporter exporter, List<String> tabsToExport, List<String> electorates, ManagementUnitService managementUnitService, OrganisationService organisationService, ProgramService programService, Map<String, DataDescription> downloadMetadata, boolean formSectionPerTab = false) {
        super(exporter, tabsToExport, [:], TimeZone.default)
        this.projectService = projectService
        this.organisationService = organisationService
        this.formSectionPerTab = formSectionPerTab
        addAdditionalSpeciesColumns = true
        addDataDescriptionToDownload(downloadMetadata)
        distinctElectorates = new ArrayList(electorates?:[])
        distinctElectorates.sort()
        projectHeaders += distinctElectorates
        projectProperties += distinctElectorates
        setupManagementUnits(managementUnitService)
        setupFundingAbn(organisationService)
        setupProgramData(programService)
    }

    /** This sets up a lazy Map that will query and cache management uints names on demand. */
    private Map setupManagementUnits(ManagementUnitService managementUnitService) {
        managementUnitNames = [:].withDefault { String managementUnitId ->
            ManagementUnit mu = managementUnitService.get(managementUnitId)
            mu?.name
        }
    }

    private Map setupFundingAbn(OrganisationService organisationService) {
        fundingAbn = [:].withDefault { String organisationId ->
            Organisation org = organisationService.get(organisationId)
            org?.abn
        }
    }

    private Map setupProgramData(ProgramService programService) {
        programFundingType = [:].withDefault { String programId ->
            Program program = programService.get(programId)
            program?.fundingType
        }

        programGrantAwardId = [:].withDefault { String programId ->
            Program program = programService.get(programId)
            if(program){
                program.externalIds.find{it.idType == ExternalId.IdType.GRANT_AWARD}?.externalId
            }

        }
    }

    private static List getIntersectionProperties() {
        List props = [ProjectService.GEOGRAPHIC_RANGE_OVERRIDDEN]
        def metadataService = Holders.grailsApplication.mainContext.getBean("metadataService")
        Map config = metadataService.getGeographicConfig()
        List intersectionLayers = config.checkForBoundaryIntersectionInLayers
        intersectionLayers.each { layer ->
            Map facetName = metadataService.getGeographicFacetConfig(layer)
            if (facetName.name) {
                props.add(getPropertyNameForFacet(facetName.name))
                props.add(getPropertyNameForFacet(facetName.name, "other"))
            }
            else
                log.error ("No facet config found for layer $layer.")
        }

        props
    }

    private static List getIntersectionHeaders() {
        List headers = ["Geographic range overridden"]
        def metadataService = Holders.grailsApplication.mainContext.getBean("metadataService")
        Map config = metadataService.getGeographicConfig()
        List intersectionLayers = config.checkForBoundaryIntersectionInLayers
        intersectionLayers.each { layer ->
            Map facetName = metadataService.getGeographicFacetConfig(layer)
            if (facetName.name) {
                headers.add(getHeaderNameForFacet(facetName.name))
                headers.add(getHeaderNameForFacet(facetName.name, "Other"))
            }
            else
                log.error ("No facet config found for layer $layer.")
        }

        headers
    }

    private static String getHeaderNameForFacet (String facetName, String prefix = "Primary") {
        Map names = Holders.config.getProperty("app.facets.displayNames", Map)
        String name = names[facetName]['headerName']
        return "$prefix $name (Interpreted)"
    }

    private static String getPropertyNameForFacet (String facetName, String prefix = "primary") {
        return "$prefix$facetName"
    }

    void export(Map project) {

        addCommonProjectData(project)

        addProjectGeo(project)
        exportProject(project)
        exportProjectOrganisationData(project)
        exportOutputTargets(project)
        exportSites(project)
        exportDocuments(project)
        exportActivitySummary(project)
        exportActivities(project)
        exportParticipantInfo(project)
        exportRisks(project)
        exportMeriPlan(project)
        exportReports(project)
        exportReportSummary(project)
        exportBlog(project)
        exportDataSet(project)
        exportElectorate(project)

        if(exporter.workbook.numberOfSheets == 0){
            createEmptySheet()
        }
    }

    /**
     * Sets up / populates common data that is displayed on every tab.
     * @param project the project being exported.
     */
    private addCommonProjectData(Map project) {
        addPrimaryAndOtherIntersections(project)
        commonProjectPropertiesRaw.each {
            project[PROJECT_DATA_PREFIX+it] = project.remove(it)
        }
        if (project.managementUnitId) {
            project[PROJECT_DATA_PREFIX+'managementUnitName'] = managementUnitNames[project.managementUnitId]
        }

        Date now = new Date()
        List orgs = project.associatedOrgs?.findAll{(!it.fromDate || it.fromDate <= now) && (!it.toDate || it.toDate >= now)}
        if (orgs) {
            project[PROJECT_DATA_PREFIX+'organisationName'] = orgs[0].name
            project[PROJECT_DATA_PREFIX+'organisationId'] = orgs[0].organisationId
        }

        filterExternalIds(project, PROJECT_DATA_PREFIX)

    }

    private void addPrimaryAndOtherIntersections (Map project) {
        Map result = projectService.findAndFormatStatesAndElectoratesForProject(project) ?: [:]
        project << result
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



     void exportActivities(Map project) {
         tabsToExport.each { tab ->
             List activities = project?.activities?.findAll { it.type == tab }
             if (activities) {
                 activities.each {
                     exportActivity(commonActivityHeaders, activityProperties, project, it, formSectionPerTab)
                 }
             }
         }
     }

    private void exportActivitySummary(Map project) {
        String tab = "Activity Summary"
        if (shouldExport(tab)) {
            AdditionalSheet sheet = getSheet(tab, activitySummaryProperties, activitySummaryHeaders)
            project.activities.each { activity ->
                Map activityData = commonActivityData(project, activity)
                sheet.add(activityData, activitySummaryProperties, sheet.getSheet().lastRowNum + 1)
            }
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

    void exportProjectOrganisationData(Map project) {
        String sheetName = 'Organisation Details'
        if (shouldExport(sheetName)) {
            AdditionalSheet sitesSheet = getSheet(sheetName, organisationDetailsProperties, organisationDetailsHeaders)
            List associatedOrgs = []

            project.associatedOrgs?.each { org ->
                Map orgProps = org+project
                if (org.organisationId) {
                    Map organisation = organisationService.get(org.organisationId)
                    orgProps['abn'] = organisation?.abn
                    orgProps['organisationName'] = organisation?.name
                }
                else {
                    orgProps['organisationName'] = ''
                }

                associatedOrgs << orgProps

            }
            int row = sitesSheet.getSheet().lastRowNum
            sitesSheet.add(associatedOrgs, organisationDetailsProperties, row + 1)
        }
    }

    private void exportSites(Map project) {
        String sheetName = 'Sites'
        if (shouldExport(sheetName)) {
            AdditionalSheet sitesSheet = getSheet(sheetName, siteProperties, siteHeaders)
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
        String sheetName = 'Output Targets'
        if (shouldExport(sheetName)) {
            AdditionalSheet outputTargetsSheet = getSheet(sheetName, outputTargetProperties, outputTargetHeaders)
            if (project.outputTargets) {
                List approvedMetrics = projectService.projectMetrics(project.projectId, true, true)
                List totalMetrics = projectService.projectMetrics(project.projectId, true, false)
                List targets = approvedMetrics.findAll{hasTarget(it.target)}.collect{project + [scoreId: it.scoreId, scoreLabel:it.label, target:it.target, deliveredApproved:it.result?.result, units:it.units?:'']}
                targets.each { target ->
                    target.deliveredTotal = totalMetrics.find{it.scoreId == target.scoreId}?.result?.result
                }
                int row = outputTargetsSheet.getSheet().lastRowNum
                outputTargetsSheet.add(targets, outputTargetProperties, row + 1)
            }
        }
    }

    /** Returns true if the target is a number (or string representation of a number) and is not zero
     * For legacy reasons, some targets are strings */
    private static boolean hasTarget(target) {
        if (target instanceof String) {
            return target && target != "0"
        }
        target && target != 0
    }

    private void exportProject(Map project) {
        String sheetName = 'Projects'
        if (shouldExport(sheetName)) {
            AdditionalSheet projectSheet = getSheet(sheetName, projectProperties, projectHeaders)
            List properties = new ArrayList(projectProperties)
            int row = projectSheet.getSheet().lastRowNum

            List<String> projectElectorates = project.sites?.collect { it?.extent?.geometry?.elect }?.flatten()?.findAll()

            distinctElectorates.each { electorate ->
                project[electorate] = projectElectorates.contains(electorate) ? 'Y' : 'N'
            }

            project.associatedOrgs?.eachWithIndex { org, i ->
                Map orgProps = associatedOrgProperties.collectEntries{
                    [('associatedOrg_'+it+(i+1)):org[it]]
                }
                project.putAll(orgProps)
            }

            projectSheet.add([project], properties, row + 1)
        }

    }

    private exportDataSet(Map project) {
        if (shouldExport("Dataset")) {
            AdditionalSheet sheet = getSheet("Data_set_Summary", datasetProperties, datasetHeader)
            int row = sheet.getSheet().lastRowNum

            List data = project?.custom?.dataSets?.collect { Map dataValue ->
                Map dataSets = [:]
                dataValue.each{k, v -> dataSets.put(k,v)}

                if (dataSets?.protocol){
                    if (dataSets.protocol == "other") {
                        dataSets["protocol"] = "other"
                    } else {
                        ActivityForm protocolForm = ActivityForm.findByExternalId(dataSets.protocol as String)
                        dataSets["protocol"] = protocolForm?.name
                    }

                }

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
                "MERI_Attachments", "MERI_Baseline", "MERI_Event", "MERI_Approvals", "MERI_Project Assets",
                'MERI_Pest Control Methods', 'MERI_Native Species Threat',
                "RLP_Outcomes", "RLP_Project_Details", "RLP_Key_Threats", "RLP_Services_and_Targets",
                "RDP_Outcomes", "RDP_Project_Details", "RDP_Key_Threats", "RDP_Services_and_Targets", "RDP_Monitoring"
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
        exportProjectDeliveryAssumptions(project)
        exportKeyEvaluationQuestion(project)
        exportPriorities(project)
        exportWHSAndCaseStudy(project)
        exportAttachments(project)
        exportBaseline(project)
        exportEvents(project)
        exportApprovals(project)
        exportProjectAssets(project)
        exportRLPOutcomes(project)
        exportRLPProjectDetails(project)
        exportRLPKeyThreats(project)
        exportRLPServicesTargets(project)
        exportControlMethods(project)
        exportNativeThreats(project)
        exportRDPKeyThreats(project)
        exportRDPProjectDetails(project)
        exportRDPOutcomes(project)
        exportRDPServicesTargets(project)
        exportRdpMonitoring(project)

    }

    private void exportBudget(Map project) {
        String sheetName = "MERI_Budget"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, budgetProperties, budgetHeaders)
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
        String sheetName = "MERI_Outcomes"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, outcomesProperties, outcomesHeaders)
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
        String sheetName = "MERI_Monitoring"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, monitoringProperties, monitoringHeaders)
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
        String sheetName = "MERI_Project Implementation"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, projectImplementationProperties, projectImplementationHeaders)
            int row = sheet.getSheet().lastRowNum

            if (project?.custom?.details?.implementation) {
                Map data = [implementation:project?.custom?.details?.implementation?.description]
                data.putAll(project)

                sheet.add(data, projectImplementationProperties, row+1)
            }
        }

    }

    private void exportProjectDeliveryAssumptions(Map project) {
        String sheetName = "RDP_Project_Delivery_Assumptions"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet("RDP Project Delivery Assumptions", projectImplementationProperties, projectDeliveryAssumptionsHeaders)
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
        String sheetName = "MERI_WHS and Case Study"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, whsAndCaseStudyProperties, whsAndCaseStudyHeaders)
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
            AdditionalSheet sheet = getSheet('Risks and Threats', risksAndThreatsProperties, risksAndThreatsHeaders)
            int row = sheet.getSheet().lastRowNum
            if (project.risks && project.risks.rows) {
                List data = project.risks.rows.collect { it + project }
                sheet.add(data, risksAndThreatsProperties, row + 1)
            }

        }
    }

    private void exportApprovals(Map project) {
        String sheetName = 'MERI_Approvals'
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, approvalsProperties, approvalsHeaders)
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

    private void exportBaseline(Map project) {
        String sheetName = "MERI_Baseline"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, baselineProperties, baselineHeaders)
            int row = sheet.getSheet().lastRowNum
            List data = []

            if (project?.custom?.details?.baseline?.rows){
                def items = project?.custom?.details?.baseline?.rows
                items.each{ Map item ->
                    Map baseline = [:]
                    baseline["relatedOutcomes"] = item.relatedOutcomes
                    baseline["method"] = item.method
                    baseline["baseline"] = item.baseline
                    baseline["code"] = item.code
                    baseline["evidence"] = item.evidence
                    baseline["monitoringDataStatus"] = item.monitoringDataStatus
                    baseline["protocols"] = item.protocols
                    baseline["relatedTargetMeasures"] = findScoreLabels(item.relatedTargetMeasures as List)
                    baseline.putAll(project)
                    data.add(project + baseline)
                }
            }

            sheet.add(data?:[], baselineProperties, row+1)
        }

    }

    private void exportRdpMonitoring(Map project) {
        String sheetName = "RDP_Monitoring"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, rdpMonitoringIndicatorsProperties, rdpMonitoringIndicatorsHeaders)
            int row = sheet.getSheet().lastRowNum
            List data = []

            if (project?.custom?.details?.monitoring?.rows){
                def items = project?.custom?.details?.monitoring?.rows
                items.each{ Map item ->
                    Map monitoringIndicator = [:]
                    monitoringIndicator["relatedBaseline"] = item.relatedBaseline
                    monitoringIndicator["data1"] = item.data1
                    monitoringIndicator["relatedTargetMeasures"] = findScoreLabels(item.relatedTargetMeasures)
                    monitoringIndicator["protocols"] = item.protocols
                    monitoringIndicator["evidence"] = item.evidence

                    monitoringIndicator.putAll(project)
                    data.add(project + monitoringIndicator)
                }
            }

            sheet.add(data?:[], rdpMonitoringIndicatorsProperties, row+1)
        }

    }

    private void exportRLPKeyThreats(Map project) {
        String sheetName = "RLP_Key_Threats"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, rlpKeyThreatProperties, rlpKeyThreatHeaders)
            int row = sheet.getSheet().lastRowNum
            List data = []

            if (project?.custom?.details?.threats?.rows){
                def items = project?.custom?.details?.threats?.rows
                items.each{ Map item ->
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

    private void exportRDPKeyThreats(Map project) {
        String sheetName = "RDP_Key_Threats"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, rdpKeyThreatProperties, rdpKeyThreatHeaders)
            int row = sheet.getSheet().lastRowNum
            List data = []

            if (project?.custom?.details?.threats?.rows){
                def items = project?.custom?.details?.threats?.rows
                items.each{ Map item ->
                    Map threat = [:]
                    threat["relatedOutcomes"] = item.relatedOutcomes
                    threat["threatCode"] = item.threatCode
                    threat["keyThreat"] = item.threat
                    threat["relatedTargetMeasures"] = findScoreLabels(item.relatedTargetMeasures as List)
                    threat["keyTreatIntervention"] = item.intervention
                    threat["evidence"] = item.evidence
                    threat.putAll(project)
                    data.add(project + threat)
                }
            }

            sheet.add(data?:[], rdpKeyThreatProperties, row+1)
        }
    }

    private void exportEvents(Map project) {
        String sheetName = "MERI_Event"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, eventProperties, eventHeaders)
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
            getProjectDetailsSheet(project, "RLP Project Details", rlpProjectDetailsHeaders, rlpProjectDetailsProperties)
        }
    }

    private void exportRDPProjectDetails(Map project){
        if (shouldExport("RDP_Project_Details")) {
            getProjectDetailsSheet(project, "RDP Project Details", rdpProjectDetailsHeaders, rdpProjectDetailsProperties)
        }
    }

    private void exportRLPOutcomes(Map project) {

        if (shouldExport("RLP_Outcomes")) {
            getOutcomeSheet(project,"RLP Outcomes")
        }
    }

    private void exportRDPOutcomes(Map project) {

        if (shouldExport("RDP_Outcomes")) {
            getOutcomeSheet(project,"RDP Outcomes")
        }
    }

    private void exportRLPServicesTargets(project){
        if (shouldExport("RLP_Services_and_Targets")) {
            getServicesTargetsSheet(project, "Project services and targets", rlpSTHeaders, rlpSTProperties)
        }
    }

    private void exportRDPServicesTargets(project){
        if (shouldExport("RDP_Services_and_Targets")) {
            getServicesTargetsSheet(project, "RDP Project services and targets", rdpSTHeaders, rdpSTProperties)
        }
    }

    private void exportControlMethods(Map project) {
        if (shouldExport("MERI_Pest Control Methods")) {
            AdditionalSheet sheet = getSheet("Pest Control Methods", pestControlMethodsProperties, pestControlMethodsHeaders)
            int row = sheet.getSheet().lastRowNum
            List data = []

            if (project?.custom?.details?.threats?.rows){
                def items = project?.custom?.details?.threatControlMethod?.rows
                items.each{ Map item ->
                    Map controlMethod = [:]
                    controlMethod["currentControlMethod"] = item.currentControlMethod
                    controlMethod["hasBeenSuccessful"] = item.hasBeenSuccessful
                    controlMethod["methodType"] = item.methodType
                    controlMethod["details"] = item.details
                    controlMethod.putAll(project)
                    data.add(project + controlMethod)
                }
            }

            sheet.add(data?:[], pestControlMethodsProperties, row+1)
        }
    }

    private  void exportNativeThreats(Map project) {
        if (shouldExport("MERI_Native Species Threat")) {
            AdditionalSheet sheet = getSheet("Native Species Threat", nativeThreatsProperties, nativeThreatsHeaders)
            int row = sheet.getSheet().lastRowNum
            List data = []

            if (project?.custom?.details?.threats?.rows){
                def items = project?.custom?.details?.threatToNativeSpecies?.rows
                items.each{ Map item ->
                    Map nativeThreat = [:]
                    nativeThreat["couldBethreatToSpecies"] = item.couldBethreatToSpecies
                    nativeThreat["details"] = item.details
                    nativeThreat.putAll(project)
                    data.add(project + nativeThreat)
                }
            }

            sheet.add(data?:[], nativeThreatsProperties, row+1)
        }
    }

    private void exportDocuments(Map project) {
        exportList("Documents", project, project.documents, documentHeaders, documentProperties)
    }

    private void exportReports(Map project) {
        String sheetName = "Reports"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, reportProperties, reportHeaders)
            exportReports(sheet, project, reportProperties)
        }
    }

    private void exportReportSummary(Map project) {
        String sheetName = "Report Summary"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, reportSummaryProperties, reportSummaryHeaders)
            exportReportSummary(sheet, project, reportSummaryProperties)
        }
    }

    private void exportElectorate(Map project) {
        String sheetName = "Electorate Coord"
        if (shouldExport(sheetName)) {
            AdditionalSheet sheet = getSheet(sheetName, electorateCoordProperties, electorateCoordHeaders)
            int row = sheet.getSheet().lastRowNum

            if (project.organisationId) {
                project[PROJECT_DATA_PREFIX+'abn'] = fundingAbn[project.organisationId]
            }
            if (!project[PROJECT_DATA_PREFIX+'fundingType'] && project.programId) {
                project[PROJECT_DATA_PREFIX+'fundingType'] = programFundingType[project.programId]
            }

            List financialYears = project?.custom?.details?.budget?.headers?.collect {it.data}
            List data = []
            Map budgetLineItem = [:]

            BigDecimal totalBudget = new BigDecimal(0)
            financialYears.eachWithIndex { String year, int i ->
                BigDecimal totalBudgetPerYear = new BigDecimal(0)
                project?.custom?.details?.budget?.rows?.each { Map lineItem ->
                    totalBudgetPerYear += BigDecimal.valueOf(Double.valueOf(lineItem.costs[i].dollar));
                }
                totalBudget +=  totalBudgetPerYear
                budgetLineItem.put(year,totalBudgetPerYear)
            }
            project[PROJECT_DATA_PREFIX+'gstFunding'] = totalBudget ?: project[PROJECT_DATA_PREFIX+'funding']
            budgetLineItem.putAll(project)
            data << budgetLineItem

            sheet.add(data?:[], electorateCoordProperties, row+1)
        }

    }

    private void exportProjectAssets(Map project) {
        exportList("MERI_Project Assets", project, project?.custom?.details?.assets, projectAssetHeaders, projectAssetProperties)
    }

    private void exportBlog(Map project) {
        exportList("Blog", project, project.blog, blogHeaders, blogProperties)
    }

    /**
     * Takes the embedded externalIds array in the project, and adds them as individual
     * key value pairs to the supplied project Map, with the key identifying the externalId type
     * and the index. The index is omitted from the first id of each type for
     * backwards compatibility.
     * (eg. [internalOrderId:'io1', internalOrderId1:'oi2', workOrderId:'w1']
     * @param project the project containing the externalIds
     * @param prefix the prefix to use when creating a key to use for the flattened values.
     */
    private void filterExternalIds(Map project, String prefix = '') {
        Map externalIdTypeToExportProperty =
                [(ExternalId.IdType.INTERNAL_ORDER_NUMBER.toString()):'internalOrderId',
                 (ExternalId.IdType.GRANT_AWARD.toString()):'grantAwardId',
                 (ExternalId.IdType.TECH_ONE_CODE.toString()):'techOneProjectCode',
                 (ExternalId.IdType.WORK_ORDER.toString()):'workOrderId']

        project.externalIds?.groupBy { it.idType }.each { idType, externalIds ->
            externalIds.eachWithIndex { value, i ->
                String property = externalIdTypeToExportProperty[value.idType] ?: value.idType
                if (i > 0) {
                    property+=i
                }
                project[prefix+property] = value.externalId
            }
        }

    }

    private AdditionalSheet getOutcomeSheet(Map project, String sheetName) {
        AdditionalSheet sheet = getSheet(sheetName, rlpOutcomeProperties, rlpOutcomeHeaders)
        int row = sheet.getSheet().lastRowNum
        Map fields = [:]
        fields["secondaryOutcomes"] = "Additional outcome/s"
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
                outcome.put('relatedOutcome',oc.relatedOutcome)
                data.add(project+outcome)
            }
        }

        sheet.add(data?:[], rlpOutcomeProperties, row+1)

    }

    private AdditionalSheet getProjectDetailsSheet(Map project, String sheetName, List projectDetailsHeaders, List projectDetailsProperties) {
        AdditionalSheet sheet = getSheet(sheetName, projectDetailsProperties, projectDetailsHeaders)
        int row = sheet.getSheet().lastRowNum

        List data = []
        Map item = [:]

        if (project?.custom?.details?.description){
            item["projectDescription"]=project?.custom?.details?.description
        }

        if (project?.custom?.details?.supportsPriorityPlace){
            item["supportsPriorityPlace"]=project?.custom?.details?.supportsPriorityPlace
        }

        if (project?.custom?.details?.supportedPriorityPlaces){
            item["supportedPriorityPlaces"]=project?.custom?.details?.supportedPriorityPlaces
        }

        if (project?.custom?.details?.indigenousInvolved){
            item["indigenousInvolved"]=project?.custom?.details?.indigenousInvolved
        }

        if (project?.custom?.details?.indigenousInvolvementType){
            item["indigenousInvolvementType"]=project?.custom?.details?.indigenousInvolvementType
        }

        if (project?.custom?.details?.implementation){
            item["projectMethodology"]=project?.custom?.details?.implementation?.description
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

        sheet.add(data?:[], projectDetailsProperties, row+1)
    }

    private AdditionalSheet getServicesTargetsSheet(Map project, String sheetName, List stHeaders, List stProperties) {
        List<Map> results = metadataService.getProjectServicesWithTargets(project)
        AdditionalSheet sheet = getSheet(sheetName, stProperties, stHeaders)
        int row = sheet.getSheet().lastRowNum
        List scoreIds = results?.scores?.scoreId?.flatten()?.unique()
        List approvedMetrics = projectService.projectMetrics(project.projectId, true, true, scoreIds)
        List totalMetrics = projectService.projectMetrics(project.projectId, false, false, scoreIds )
        List data = []
        results.each { item ->
            def serviceName = item.name
            item.scores.each {
                Map totalMetric = totalMetrics?.find { metric -> metric.scoreId == it.scoreId}
                Map approvedMetric = approvedMetrics?.find {metric -> metric.scoreId == it.scoreId}
                Map st = [:]
                st['service'] = serviceName
                st['targetMeasure'] = it.label
                st['relatedOutcomes'] = it.relatedOutcomes
                st['total'] = it.target
                st['targetDate'] = it.targetDate
                st['deliveredTotal'] = totalMetric?.result?.result
                st['deliveredApproved'] = approvedMetric?.result?.result
                it.periodTargets.each { pt ->
                    st[pt.period] = pt.target
                }
                data.add(project+st)
            }
        }

        sheet.add(data?:[], stProperties, row+1)
    }

    private static String findScoreLabels(List scoreIds) {
        List labels = []
        for (String scoreId : scoreIds) {
            labels.add(au.org.ala.ecodata.Score.findByScoreId(scoreId)?.label)
        }
        return labels
    }
}
