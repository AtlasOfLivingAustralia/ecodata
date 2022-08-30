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
            'Column':'excelExportedColumn',
            'Status':'excelExportedStatus',
            'Required':'excelExportedRequired',
            'Source':'excelExportedSource',
            'Description':'excelExportedDescription',
            'Example':'excelExportedExample',
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
            DataDescription dataDesc = DataDescription.findByExcelExportedColumn(dataDescription.excelExportedColumn)
            if (dataDesc) {
                dataDesc.excelExportedStatus = dataDescription.excelExportedStatus
                dataDesc.excelExportedRequired = dataDescription.excelExportedRequired
                dataDesc.excelExportedSource = dataDescription.excelExportedSource
                dataDesc.excelExportedDescription = dataDescription.excelExportedDescription
                dataDesc.excelExportedExample = dataDescription.excelExportedExample
                dataDesc.save(flush:true,failOnError:true)
            } else {
                DataDescription newDataDesc = new DataDescription(dataDescription)
                newDataDesc.save(flush:true,failOnError:true)
            }

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
