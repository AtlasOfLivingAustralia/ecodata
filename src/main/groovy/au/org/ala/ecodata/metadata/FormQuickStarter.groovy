package au.org.ala.ecodata.metadata

import au.com.bytecode.opencsv.CSVReader
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.grails.plugins.excelimport.ExcelImportService
import org.grails.plugins.excelimport.ImportCellCollector

/**
 * This class extracts data from an excel spreadsheet and produces JSON for an output model.
 */
class FormQuickStarter {

    ExcelImportService excelImportService

    FormQuickStarter(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService
    }

    Map outputModelFromSpreadsheet(InputStream spreadsheetIn) {
        List errors = []

        Workbook workbook = WorkbookFactory.create(spreadsheetIn)

        Map dataConfig = [
                sheet:"Data",
                startRow:1,
                columnMap:[
                        A:'name',
                        B:'dataType',
                        C:'viewType',
                        D:'label',
                        E:'helpText',
                        F:'constraints',
                        G:'validation',
                        H:'dwc',
                        I:'defaultValue',
                        J:'parent',
                        K:'width'
                ]
        ]

        List data = excelImportService.convertColumnMapConfigManyRows(workbook, dataConfig)

        Map outputDescription = [:]

        outputDescription.dataModel = data.findAll{it.name && it.dataType}.findAll{!it.parent}.collect{
            dataModelItem(it, errors)
        }

        data.findAll{it.parent}.each { child ->
            Map parent = outputDescription.dataModel.find{it.name == child.parent}
            if (!parent) {
                errors << "Cannot find a matching parent for value: ${child.name}, parent table: ${child.parent}"
            }
            else if (parent.dataType != 'list') {
                errors << "The parent column must match a data item with a data type of list"
            }
            else if (child.dataType == 'list') {
                errors << 'Nested lists are not supported'
            }
            else {
                if (parent.columns == null) {
                    parent.columns = []
                }
                Map childItem = dataModelItem(child, errors)

                parent.columns << childItem

            }

        }

        int maxCols = 6
        Map layoutConfig = [
                sheet:"Layout",
                startRow:1,
                columnMap:[
                        A:0,
                        B:1,
                        C:2,
                        D:3,
                        E:4,
                        F:5
                ]
        ]
        List layout =  convertColumnMapConfigManyRows(workbook, layoutConfig)
        List rows = []
        List columns = []
        layout.each {
            boolean found = false
            for (int i=0; i<maxCols; i++) {
                if (it[i]) {
                    if (columns.size() <= i) {
                        for (int j=columns.size(); j<=i; j++) {
                            columns << []
                        }
                    }
                    Map viewResult = viewModelItem(it[i], outputDescription.dataModel, data)
                    if (viewResult.result) {
                        columns[i] << viewResult.result
                    }

                    if (viewResult.errors) {
                        errors.addAll(viewResult.errors)
                    }

                    found = true
                }
            }
            if (!found) {
                // Blank row encountered, start a new row.

                if (columns) {
                    rows << columns
                }

                columns = []
            }
        }
        if (columns.size() > 0) {
            rows << columns
        }

        outputDescription.viewModel = []
        rows.each { row ->
            Map viewRow = [
                    type:'row',
                    items:[]
            ]
            row.each { col ->
                Map viewCol = [
                        type:'col',
                        items:[]
                ]
                col.each { item ->
                    viewCol.items << item
                }
                viewRow.items << viewCol
            }
            outputDescription.viewModel << viewRow
        }

        if (errors) {
            outputDescription.errors = errors
        }

        outputDescription
    }

    private Map dataModelItem(Map config, List errors) {
        Map dataModelItem = [
                name:config.name,
                dataType:config.dataType,
                description:config.helpText ?:'',
                validate:config.validation
        ]
        if (config.dwc) {
            dataModelItem.dwcAttribute = config.dwc
        }
        if (config.constraints) {

            if (!(config.dataType == 'text' || config.dataType == 'stringList')) {
                errors << "Constraints will be ignored for ${config.name}.  Only supported for text and stringList data types"
            }
            else {
                CSVReader csvReader = new CSVReader(new StringReader(config.constraints))
                dataModelItem.constraints = csvReader.readNext()
            }
        }
        if (config.defaultValue) {
            dataModelItem.defaultValue = defaultValue(config.defaultValue as String, dataModelItem)
        }

        dataModelItem

    }

    private Map viewModelItem(String value, List dataModel, List data) {
        List errors = []
        Map result
        if (value.startsWith('"') && value.endsWith('"') && value.length() > 2) {
            result = [
                    type:'literal',
                    source:value.substring(1, value.length()-2)
            ]
        }
        else {
            Map dataModelItem = dataModel.find{it.name == value}
            Map spreadsheetRow = data.find{it.name == value}

            if (!dataModelItem) {
                if (value.startsWith('"')) {
                    errors << "Invalid literal: ${value}"
                }
                else {
                    errors << "Unable to find data model item ${value}"
                }
            }
            else {
                if (dataModelItem.dataType == 'list') {
                    result = [
                            source:value,
                            type:'table',
                            userAddedRows:true,
                            columns:[]
                    ]

                    dataModelItem.columns.each { column ->
                        Map child = data.find{it.name == column.name}
                        Map col = [
                                source:column.name,
                                title:child.label,
                                type:child.viewType

                        ]
                        if (child.width) {
                            col.width = child.width
                        }
                        result.columns << col
                    }
                }
                else {


                    result = [
                            source  : value,
                            type    : spreadsheetRow.viewType,
                            preLabel: spreadsheetRow.label
                    ]


                }
            }
        }
        [errors:errors, result:result]
    }

    private Object defaultValue(String value, Map dataModelItem) {
        switch (dataModelItem.dataType) {
            case "number":
                return value as Number
            case "boolean":
                return Boolean.parseBoolean(value)
            default:
                return value
        }
    }


    /**
     * The implementation in the plugin ignores blank rows, this is an adaption that does not.
     */
    private def convertColumnMapConfigManyRows(Workbook workbook, Map config, ImportCellCollector pcc = null, FormulaEvaluator evaluator = null, propertyConfigurationMap = [:], int lastRow = -1) {
        if (!evaluator) evaluator = workbook.creationHelper.createFormulaEvaluator()
        def sheet = workbook.getSheet(config.sheet)

        return convertColumnMapManyRows(sheet, config, config.startRow, pcc, evaluator, [:], -1)
    }

    /**
     * The implementation in the plugin ignores blank rows, this is an adaption that does not.
     */
    private def convertColumnMapManyRows(Sheet currentSheet, Map config, int firstRow, ImportCellCollector pcc = null, FormulaEvaluator evaluator = null, propertyConfigurationMap = null, int lastRow = -1) {
        if (currentSheet == null) return []
        boolean blankRowBreak = false
        int blankRowCount = 0
        def returnList = []
        for (int rowIndex = firstRow; (rowIndex < lastRow || ((lastRow == -1)) && !blankRowBreak); rowIndex++) {
            //println "ColumnMap $columnMap"
            Map returnParams = excelImportService.convertColumnMapOneRow(currentSheet, config, rowIndex, pcc, evaluator, propertyConfigurationMap)
            //println "Row Columns - returning $returnParams"
            log.debug "Index $rowIndex Result map values $returnParams"
            //println "Index $rowIndex Result map values $returnParams"
            if (!returnParams) {
                blankRowCount += 1
                returnList << [:]
            } else {
                blankRowCount = 0
                returnList << returnParams
            }
            blankRowBreak = (blankRowCount > 10)
        }
        returnList
    }
}
