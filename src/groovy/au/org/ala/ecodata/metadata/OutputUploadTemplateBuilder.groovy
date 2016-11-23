package au.org.ala.ecodata.metadata

import au.org.ala.ecodata.reporting.XlsExporter
import org.apache.log4j.Logger
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.ss.util.CellReference
import pl.touk.excel.export.multisheet.AdditionalSheet

class OutputUploadTemplateBuilder extends XlsExporter {

    def model
    def outputName
    def data

    public OutputUploadTemplateBuilder(filename, outputName, model) {
        super(filename)
        this.outputName = outputName
        this.model = model.findAll{!it.computed}
    }

    public OutputUploadTemplateBuilder(filename, outputName, model, data) {
        super(filename)
        this.outputName = outputName
        this.model = model.findAll{!it.computed}
        this.data = data
    }


    public void build() {

        def headers = model.collect {
            def label = it.label ?: it.name
            if (it.dataType == 'species') {
                label += ' (Scientific Name Only)'
            }
            label
        }
        AdditionalSheet outputSheet = addSheet(outputName, headers)

        new ValidationProcessor(getWorkbook(), outputSheet.sheet, model).process()

        new OutputDataProcessor(getWorkbook(), outputSheet.sheet, model, data, getStyle()).process()

        finalise()
    }

    public void buildGroupHeaderList() {

        def groupHeaders = []
        def lastHeader = ""
        boolean fillHeader = false

        def headers = model.collect {
                if (it.header && it.header != lastHeader) {
                    groupHeaders.add(it.header)
                    lastHeader = it.header
                    fillHeader = true
                } else {
                    groupHeaders.add("")
                }

                def label = it.label ?: it.name
                if (it.dataType == 'species') {
                    label += ' (Scientific Name Only)'
                }
                label
        }

        if (!fillHeader) groupHeaders = null

        AdditionalSheet outputSheet = addSheet(outputName, headers, groupHeaders)

        new ValidationProcessor(getWorkbook(), outputSheet.sheet, model).process()

        new OutputDataProcessor(getWorkbook(), outputSheet.sheet, model, data, getStyle()).process()

        finalise()
    }

    def finalise() {
        sizeColumns()
    }

}

class OutputDataProcessor {
    static Logger log = Logger.getLogger(getClass())

    private Workbook workbook
    private Sheet sheet
    def model
    def data
    def rowHeaderStyle

    public OutputDataProcessor(workbook, sheet, model, data, rowHeaderStyle){
        this.workbook = workbook
        this.sheet = sheet
        this.model = model
        this.data = data
        this.rowHeaderStyle = rowHeaderStyle

    }

    public void process() {

        data?.eachWithIndex { rowValue, rowCount ->
            Row row = sheet.createRow((rowCount+1))

            def dataType, value, rowHeader
            model.eachWithIndex { modelVal, i ->
                value = rowValue[modelVal.name] ?: ''

                dataType = modelVal.dataType
                rowHeader = modelVal.rowHeader

                Cell cell = row.createCell(i)
                switch(dataType){
                    case 'number':
                        try {
                            cell.setCellValue(new BigDecimal(value))
                        }
                        catch (NumberFormatException e) {
                            cell.setCellValue(0)
                            log.warn("Invalid numeric value: "+value)
                        }

                        break
                    case 'species':
                        cell.setCellValue(value?value.name:'')
                        break
                    case 'stringList':
                        cell.setCellValue(value?value.join(','):'')
                    case 'date':
                    case 'text':
                    default:
                        cell.setCellValue(value.toString())
                        break
                }
                if(rowHeader){
                    cell.setCellStyle(rowHeaderStyle);
                }

            }
        }
    }
}

class ValidationProcessor extends OutputModelProcessor {

    private Workbook workbook
    private Sheet sheet
    def model

    public ValidationProcessor(workbook, sheet, model) {
        this.workbook = workbook
        this.sheet = sheet
        this.model = model
    }

    public void process() {

        // Create a worksheet to store validation lists in.
        def validationSheetName = OutputUploadTemplateBuilder.sheetName("Validation - "+sheet.getSheetName())
        Sheet validationSheet = workbook.createSheet(validationSheetName)
        workbook.setSheetHidden(workbook.getSheetIndex(validationSheet), true)

        ExcelValidationContext context = new ExcelValidationContext([currentSheet:sheet, validationSheet:validationSheet])
        ValidationHandler validationHandler = new ValidationHandler()
        model.eachWithIndex{node, i ->
            context.currentColumn = i
            processNode(validationHandler, node, context)
        }
    }


}

class ExcelValidationContext implements OutputModelProcessor.ProcessingContext {
    Sheet currentSheet
    Sheet validationSheet
    int currentColumn

}


class ValidationHandler implements OutputModelProcessor.Processor<ExcelValidationContext> {


    def addValidation(node, context, constraint = null) {

        DataValidationHelper dvHelper = context.currentSheet.getDataValidationHelper();
        OutputMetadata.ValidationRules rules = new OutputMetadata.ValidationRules(node)

        DataValidation dataValidation = dvHelper.createValidation(constraint, columnRange(context.currentColumn))
        if (rules.isMandatory()) {
            dataValidation.setEmptyCellAllowed(false)
        }
        dataValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        dataValidation.setShowErrorBox(true);

        context.currentSheet.addValidationData(dataValidation);
    }

    @Override
    def number(Object node, ExcelValidationContext context) {
        DataValidationHelper dvHelper = context.currentSheet.getDataValidationHelper();
        OutputMetadata.ValidationRules rules = new OutputMetadata.ValidationRules(node)

        def operator = rules.max() ? DataValidationConstraint.OperatorType.BETWEEN : DataValidationConstraint.OperatorType.GREATER_OR_EQUAL

        DataValidationConstraint dvConstraint =
                dvHelper.createNumericConstraint(DataValidationConstraint.ValidationType.DECIMAL,
                operator, rules.min().toString(), rules.max()?rules.max().toString():"")

        addValidation(node, context, dvConstraint)
    }

    @Override
    def integer(Object node, ExcelValidationContext context) {
        number(node, context)
    }

    @Override
    def time(Object node, ExcelValidationContext context) {
        text(node, context)
    }

    @Override
    def text(Object node, ExcelValidationContext context) {
        if (node.constraints) {

            node.constraints.eachWithIndex { value, i ->
                Row row = context.validationSheet.getRow(i)
                if (!row) {
                    row = context.validationSheet.createRow(i)
                }
                Cell cell = row.createCell(context.currentColumn)
                cell.setCellValue(value)
            }
            def colString = CellReference.convertNumToColString(context.currentColumn)
            def rangeFormula = "'${context.validationSheet.getSheetName()}'!\$${colString}\$1:\$${colString}\$${node.constraints.length()}"

            DataValidationHelper dvHelper = context.currentSheet.getDataValidationHelper();
            DataValidationConstraint dvConstraint =
                    dvHelper.createFormulaListConstraint(rangeFormula)


            addValidation(node, context, dvConstraint)

        }
    }

    @Override
    def date(Object node, ExcelValidationContext context) {

    }

    @Override
    def image(Object node, ExcelValidationContext context) {

    }

    @Override
    def embeddedImages(Object node, ExcelValidationContext context) {

    }

    @Override
    def species(Object node, ExcelValidationContext context) {

    }

    @Override
    def stringList(Object node, ExcelValidationContext context) {

    }

    def columnRange(int col) {
        final int MAX_ROWS = 1000
        CellRangeAddressList range = new CellRangeAddressList(1, MAX_ROWS, col, col)
        return range
    }

    @Override
    def booleanType(node, ExcelValidationContext context) {

    }

    @Override
    def document(node, ExcelValidationContext context) {}

}