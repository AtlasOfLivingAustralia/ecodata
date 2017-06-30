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
    boolean editMode = false
    boolean extraRowsEditable = true
    boolean autoSizeColumns = true

    public OutputUploadTemplateBuilder(filename, outputName, model) {
        super(filename)
        this.outputName = outputName
        this.model = model.findAll{!it.computed}
        this.editMode = false
        this.autoSizeColumns = true
    }

    public OutputUploadTemplateBuilder(filename, outputName, model, data, boolean editMode = false, boolean extraRowsEditable = true, boolean autoSizeColumns = true) {
        super(filename)
        this.outputName = outputName
        this.model = model.findAll{!it.computed}
        this.data = data
        this.editMode = editMode
        this.extraRowsEditable = extraRowsEditable
        this.autoSizeColumns = autoSizeColumns
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

        new OutputDataProcessor(getWorkbook(), outputSheet.sheet, model, data, getStyle(), editMode, extraRowsEditable).process()

        finalise(outputSheet)
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

        new OutputDataProcessor(getWorkbook(), outputSheet.sheet, model, data, getStyle(), editMode, extraRowsEditable).process()

        finalise()
    }

    private int widthFromString(String widthString, int defaultWidth) {
        int width = defaultWidth

        // Strip non-numerics to allow for trailing '%' / 'px' / 'em', if the model
        // mixes units we will get some strange results..
        widthString = widthString?.replaceAll(/[^\d]/, '')
        if (widthString) {
            width = Integer.parseInt(widthString)
        }

        width
    }

    def finalise(AdditionalSheet outputSheet) {
        if (autoSizeColumns) {
            sizeColumns()
        }
        else {
            // Attempt to give some sensible sizes...
            // lets say we have 250 chars to work with.
            // Excel sizes are in 1/256 of a character.
            int TOTAL_WIDTH_IN_EXCEL_UNITS = 250*256

            List widths = model.collect { widthFromString(it.width, 0) }
            int sum = widths.sum()
            List columnSizes = widths.collect { (int)(it / sum * TOTAL_WIDTH_IN_EXCEL_UNITS) }

            columnSizes.eachWithIndex { width, i ->
                if (width) {
                    outputSheet.sheet.setColumnWidth(i, width)
                }
                else {
                    outputSheet.sheet.autoSizeColumn(i)
                }
            }

        }
    }

}

class OutputDataProcessor {
    static Logger log = Logger.getLogger(getClass())

    private Workbook workbook
    private Sheet sheet
    def model
    def data
    def rowHeaderStyle
    boolean editMode
    boolean extraRowsEditable
    CellStyle unlockedCellStyle

    /**
     *
     * @param workbook The Excel workbook to populate
     * @param sheet The sheet/tab to populate
     * @param model The model describing the data
     * @param data The data to add to the sheet
     * @param rowHeaderStyle Cell style for the column headers
     * @param editMode true if this workbook is being populated for the purposes of uploading data for an activity / table.  If
     * set to true, the "readOnly" status of model items will be taken into account when writing to cells.
     * @param extraRowsEditable only used if editMode is true.  If so, extra rows will be able to be added to the data populated in the sheet.
     * Otherwise the cells in the extra rows will be locked.
     */
    public OutputDataProcessor(workbook, sheet, model, data, rowHeaderStyle, boolean editMode = false, boolean extraRowsEditable = true){
        this.workbook = workbook
        this.sheet = sheet
        this.model = model
        this.data = data
        this.rowHeaderStyle = rowHeaderStyle
        this.editMode = editMode
        this.extraRowsEditable = extraRowsEditable

        if (editMode) {
            unlockedCellStyle =  workbook.createCellStyle();
            unlockedCellStyle.setLocked(false);
        }
    }

    private void protectSheet() {

        sheet.protectSheet("")
        sheet.getCTWorksheet().getSheetProtection().setFormatColumns(false)

        // If we allow extra rows to be added, by default make editable columns editable for the whole sheet.
        if (extraRowsEditable) {
            model.eachWithIndex { modelVal, i ->
                if (!modelVal.readOnly)  {
                    sheet.setDefaultColumnStyle(i, unlockedCellStyle)
                }
            }
        }
    }

    public void process() {

        if (editMode) {
            protectSheet()
        }

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
                        if (value) {
                            if (value instanceof List) {
                                value = new ArrayList(value) // Copy the list to avoid JSONArray "join" behaviour
                            }
                            else {
                                value = [value]
                            }
                        }
                        cell.setCellValue(value?value.join(','):'')
                        break
                    case 'date':
                    case 'text':
                    default:
                        cell.setCellValue(value.toString())
                        break
                }
                if(rowHeader){
                    cell.setCellStyle(rowHeaderStyle)
                }
                if (editMode) {
                    if (!modelVal.readOnly) {
                        cell.setCellStyle(unlockedCellStyle)
                    }
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