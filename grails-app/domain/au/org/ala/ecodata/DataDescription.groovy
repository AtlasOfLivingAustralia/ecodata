package au.org.ala.ecodata

/**
 * Allows for extended description of data that is able to be accessed by the MERIT API or via downloads.
 */
class DataDescription {

    String graphQlName
    String xlsxName
    String xlsxHeader
    String type
    Integer formVersion
    String entity
    String field
    boolean derived
    String userInterfaceReference
    String label
    String notes

    // updated metadata from Damien's, sent through (22/06/2022)
    String managementUnit
    String grantId
    String activityId
    String projectId
    String program
    String subProgram
    String name
    String description
    String organisation
    String reportFinancialYear
    String targetMeasure
    String service
    String siteId
    String externalId
    String reportStatus
    String projectStatus
    String status
    String measured
    String invoiced
    String actual
    String stage
    String activityType
    Date reportFromDate
    Date reportToDate
    Date startDate
    Date endDate
    Date contractedStartDate
    Date contractedEndDate
    Date  lastModified
    String category
    String context
    String species
    String grantOrProcurement
    String totalToBeDelivered
    String fyTarget
    String metaSourceSheetname
    String metaColMeasured
    String metaColActual
    String metaColInvoiced
    String metaColCategory
    String metaColContext
    String metaTextSubcategory
    String metaColSpecies
    String metaLineItemObjectClass
    String metaLineItemProperty
    String metaLineItemValue
    String muId
    String muState
    String extractDate
    String subcategory
    String meritReportsLink
    String metaColProjectStatus
    String metaColStatus
    String metaColReportLastModified

    static constraints = {
        graphQlName nullable: true
        xlsxName nullable: true
        xlsxHeader nullable: true
        formVersion nullable: true
        entity nullable: true
        field nullable: true
        userInterfaceReference nullable: true
        label nullable: true
        notes nullable: true
        type nullable: true

        managementUnit nullable: true
        grantId nullable: true
        activityId nullable: true
        projectId nullable: true
        program nullable: true
        subProgram nullable: true
        name nullable:true
        description nullable:true
        organisation nullable: true
        reportFinancialYear nullable: true
        targetMeasure nullable: true
        service nullable: true
        siteId nullable: true
        externalId nullable: true
        reportStatus nullable: true
        projectStatus nullable: true
        status nullable: true
        measured nullable: true
        invoiced nullable: true
        actual nullable: true
        stage nullable: true
        activityType nullable: true
        reportFromDate nullable: true
        reportToDate nullable: true
        startDate nullable: true
        endDate nullable: true
        contractedStartDate nullable: true
        contractedEndDate nullable: true
        lastModified nullable: true
        category nullable: true
        context nullable: true
        species nullable: true
        grantOrProcurement nullable: true
        totalToBeDelivered nullable: true
        fyTarget nullable: true
        metaSourceSheetname nullable: true
        metaColMeasured nullable: true
        metaColActual nullable: true
        metaColInvoiced nullable: true
        metaColCategory nullable: true
        metaColContext nullable: true
        metaTextSubcategory nullable: true
        metaColSpecies nullable: true
        metaLineItemObjectClass nullable: true
        metaLineItemProperty nullable: true
        metaLineItemValue nullable: true
        muId nullable: true
        muState nullable: true
        extractDate nullable: true
        subcategory nullable: true
        meritReportsLink nullable: true
        metaColProjectStatus nullable: true
        metaColStatus nullable: true
        metaColReportLastModified nullable: true
    }
}
