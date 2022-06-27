package au.org.ala.ecodata

import grails.gorm.transactions.Transactional
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellReference
import org.apache.poi.ss.usermodel.DateUtil

@Transactional
class DataDescriptionService {

    ExcelImportService excelImportService

    static def EXCEL_COLUMN_MAP = [
            'Name':'name',
            'Description':'description',
            'Start Date':'startDate',
            'End Date':'endDate',
            'Grant ID':'grantId',
            'Program':'program',
            'Sub Program':'subProgram',
            'Organisation':'organisation',
            'Management Unit':'managementUnit',
            'Activity Id':'activityId',
            'Project Id':'projectId',
            'Report Financial Year':'reportFinancialYear',
            'Target Measure':'targetMeasure',
            'Service':'service',
            'Site Id':'siteId',
            'External Id':'externalId',
            'Report Status':'reportStatus',
            'Rroject Status':'projectStatus',
            'Status':'status',
            'Measured':'measured',
            'Invoiced':'invoiced',
            'Actual':'actual',
            'Stage':'stage',
            'Activity Type':'activityType',
            'Report From Date':'reportFromDate',
            'Report To Date':'reportToDate',
            'Contracted Start Date':'contractedStartDate',
            'Contracted End Date':'contractedEndDate',
            'Last Modified':'lastModified',
            'Category':'category',
            'Context':'context',
            'Species':'species',
            'Grant Or Procurement':'grantOrProcurement',
            'Total To Be Delivered':'totalToBeDelivered',
            'fy Target':'fyTarget',
            'Meta Source Sheetname':'metaSourceSheetname',
            'MetaColMeasured':'metaColMeasured',
            'MetaColActual':'metaColActual',
            'MetaColInvoiced':'metaColInvoiced',
            'MetaColCategory':'metaColCategory',
            'MetaColContext':'metaColContext',
            'MetaTextSubcategory':'metaTextSubcategory',
            'MetaColSpecies':'metaColSpecies',
            'MetaLineItemObjectClass':'metaLineItemObjectClass',
            'MetaLineItemProperty':'metaLineItemProperty',
            'MetaLineItemValue':'metaLineItemValue',
            'MU Id':'muId',
            'MU State':'muState',
            'Extract Date':'extractDate',
            'Subcategory':'subcategory',
            'Merit Reports Link':'meritReportsLink',
            'MetaColProjectStatus':'metaColProjectStatus',
            'MetaColStatus':'metaColStatus',
            'MetaColReportLastModified':'metaColReportLastModified'

    ]

    /**
     * Import rows from excel and save in the DB collection
     * @param inputStream
     * @return
     */
    def importData(InputStream inputStream) {
        Workbook workbook = WorkbookFactory.create(inputStream)

        def columnMap = [:]
        EXCEL_COLUMN_MAP.eachWithIndex { headerMap, i ->
            columnMap << [(CellReference.convertNumToColString(i)):headerMap.value]
        }
        def config = [
                sheet: workbook.getSheetAt(0).getSheetName(),
                startRow:1,
                columnMap:columnMap
        ]

        List dataDescriptions =  excelImportService.mapSheet(workbook, config)
        if (dataDescriptions.size() == 0) {
            return false
        }

        dataDescriptions.each { dataDescription->
            dataDescription.reportFromDate = (dataDescription.reportFromDate) ? DateUtil.getJavaDate(dataDescription.reportFromDate).toInstant() : dataDescription.reportFromDate
            dataDescription.reportToDate = (dataDescription.reportToDate) ? DateUtil.getJavaDate(dataDescription.reportToDate).toInstant() : dataDescription.reportToDate
            dataDescription.startDate = (dataDescription.startDate) ? DateUtil.getJavaDate(dataDescription.startDate).toInstant() : dataDescription.startDate
            dataDescription.endDate = (dataDescription.endDate) ? DateUtil.getJavaDate(dataDescription.endDate).toInstant() : dataDescription.endDate
            dataDescription.contractedStartDate = (dataDescription.contractedStartDate) ? DateUtil.getJavaDate(dataDescription.contractedStartDate).toInstant() : dataDescription.contractedStartDate
            dataDescription.contractedEndDate = (dataDescription.contractedEndDate) ? DateUtil.getJavaDate(dataDescription.contractedEndDate).toInstant() : dataDescription.contractedEndDate
            dataDescription.lastModified = (dataDescription.lastModified) ? DateUtil.getJavaDate(dataDescription.lastModified).toInstant() : dataDescription.lastModified
            DataDescription dataDescription1 = new DataDescription(dataDescription)
            dataDescription1.save(flush:true,failOnError:true)
        }

        return true
    }

    /**
     * Check if the file is excel type
     * @param fileType
     * @return
     */
    protected boolean isValidFileType(String fileType) {
        List allowedFileTypes = [
                'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        ]
        if (fileType in allowedFileTypes) {
            return true
        }
        return false
    }

}
