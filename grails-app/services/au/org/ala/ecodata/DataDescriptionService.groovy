package au.org.ala.ecodata

import grails.gorm.transactions.Transactional
import org.apache.poi.ss.usermodel.Sheet
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
            'Description':'description',
            'Example':'excelExportedExample',
            'Entity':'entity',
            'Field':'field'
    ]

    /**
     * Import rows from excel and save in the DB collection
     * @param inputStream
     * @return
     */
    def importData(InputStream inputStream) {
        Workbook workbook = WorkbookFactory.create(inputStream)

        def columnMap = [:]
        Sheet sheet = workbook.getSheetAt(0)
        Map headers = excelImportService.getDataHeaders(sheet)

        headers.each { String cellRef, String header ->
            String field = EXCEL_COLUMN_MAP[header]
            columnMap << [(cellRef):field]
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
            DataDescription dataDesc = DataDescription.findByEntityAndField(dataDescription.entity, dataDescription.field)
            if (dataDesc) {
                dataDesc.excelExportedStatus = dataDescription.excelExportedStatus
                dataDesc.excelExportedRequired = dataDescription.excelExportedRequired
                dataDesc.excelExportedSource = dataDescription.excelExportedSource
                dataDesc.excelExportedDescription = dataDescription.description
                dataDesc.excelExportedExample = dataDescription.excelExportedExample
                dataDesc.description = dataDescription.description

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
