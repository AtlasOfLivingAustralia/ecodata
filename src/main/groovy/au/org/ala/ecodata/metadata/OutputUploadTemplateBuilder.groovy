package au.org.ala.ecodata.metadata

import au.org.ala.ecodata.reporting.XlsExporter
import groovy.util.logging.Slf4j
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.ss.util.CellReference
import pl.touk.excel.export.multisheet.AdditionalSheet

import java.text.ParseException

@Slf4j
class OutputUploadTemplateBuilder extends XlsExporter {
    static final String SERIAL_NUMBER_NAME = 'Serial Number'
    static final String SERIAL_NUMBER_DATA = 'serial'

    def model
    def outputName
    def data
    boolean editMode = false
    boolean extraRowsEditable = true
    boolean autoSizeColumns = true
    boolean includeSerialNumber = false

    Map additionalFieldsForDataTypes
    Map hints = [:]
    static final int DEFAULT_NUMBER_OF_COLUMNS_FOR_MULTISELECT = 3

    OutputUploadTemplateBuilder(filename, outputName, model, data, boolean editMode = false, boolean extraRowsEditable = true, boolean autoSizeColumns = true, boolean includeSerialNumber, Map hints = [:], Map additionalFieldsForDataTypes = [:]) {
        super(filename)
        this.outputName = outputName
        this.model = model.findAll{!it.computed}
        this.data = data
        this.editMode = editMode
        this.extraRowsEditable = extraRowsEditable
        this.autoSizeColumns = autoSizeColumns
        this.includeSerialNumber = includeSerialNumber
        this.hints = hints
        this.additionalFieldsForDataTypes = additionalFieldsForDataTypes
    }

    /**
     * When building an output template, if any of the fields are multiselect
     * we allow each value to be added in a new column.
     * When data has been supplied to be included in the template, we need
     * to ensure that there are at least enough columns for each multiselect field
     * to be able to include all of the supplied data.
     * e.g. if the supplied data for a multi-select field includes 4 selections for one row,
     * we need to have at least 4 columns for that field in the template to be able to include all of the supplied data.
     */
    private void modifyMultiselectHintsBasedOnData() {
        model.each {
            if (OutputModelProcessor.isMultiSelect(it)) {
                // If data has been supplied for the multi-select field, we need to create enough columns to cover the maximum number of selections
                // across all rows.
                int maxSelections = DEFAULT_NUMBER_OF_COLUMNS_FOR_MULTISELECT
                if (data) {
                    data.each { row ->
                        def value = row[it.name]
                        if (!value instanceof List) {
                            return
                        }
                        maxSelections = Math.max(maxSelections, value.size())
                    }
                }
                if (!hints[it.name]) {
                    hints[it.name] = [:]
                }
                hints[it.name].numColumns = maxSelections
            }
        }
    }

    private static String processNodeHeaders(String path, Map node, String lastHeader, List headers, List dataPathHeader, List groupHeaders, List augmentedModel, int startIndex) {
        String fullPath = path ? path + '.' + node.name : node.name
        // Use the parent path as the group identifier so columns under the
        // same parent share a style, while a change in parent (or nesting
        // level) triggers a new style.  For multi-column fields (e.g.
        // multiselect), use a distinct group so all columns for that field
        // are visually grouped together and stand out from neighbours.
        String parentPath = path ?: "root"
        String groupHeader = parentPath
        if (OutputModelProcessor.isMultiSelect(node)) {
            groupHeader = "multi:"+parentPath
        }
        groupHeaders.add(groupHeader)
        lastHeader = parentPath

        dataPathHeader.add(fullPath)
        headers.add(node.label ?: node.name)
        augmentedModel.add(startIndex, node)

        lastHeader
    }

    void build() {

        def groupHeaders = []
        def dataPathHeader = []
        def headers = []
        def lastHeader = ""

        List augmentedModel = []
        if (includeSerialNumber){
            headers.add(SERIAL_NUMBER_NAME)
            dataPathHeader.add(SERIAL_NUMBER_DATA)
            groupHeaders.add(SERIAL_NUMBER_DATA)
            augmentedModel << [
                   name: SERIAL_NUMBER_DATA,
                   label: SERIAL_NUMBER_NAME,
                   dataType: 'number',
                   required: true
           ]
        }
        else {
            augmentedModel = []
        }
        int startIndex = augmentedModel.size()

        modifyMultiselectHintsBasedOnData()
        model.eachWithIndex { it, index ->
            switch (it.dataType) {
                case 'species':
                    List fields = additionalFieldsForDataTypes[it.dataType]?.fields
                    if (!fields) {
                        Map speciesNode = it + [label: it.label + " (Scientific Name Only)"]
                        processNodeHeaders(it.path, speciesNode, lastHeader, headers, dataPathHeader, groupHeaders, augmentedModel, startIndex)
                        startIndex++
                        break
                    }
                    // If there are additional fields, fall through to the next case as they are handled the same.
                case 'image':
                case 'geoMap':
                    List fields = additionalFieldsForDataTypes[it.dataType]?.fields
                    if (fields) {
                        String path = it.path ? it.path + '.' + it.name : it.name
                        fields.each {
                            lastHeader = processNodeHeaders(path, it, lastHeader, headers, dataPathHeader, groupHeaders, augmentedModel, startIndex)
                            startIndex++
                        }
                    }
                    break
                case 'stringList':
                case 'text':
                    int numColumns = hints[it.name]?.numColumns ?: 1
                    for (int i=0; i<numColumns; i++) {
                        lastHeader = processNodeHeaders(it.path, it, lastHeader, headers, dataPathHeader, groupHeaders, augmentedModel, startIndex)
                        startIndex ++
                    }
                    break;
                default:
                    lastHeader = processNodeHeaders(it.path, it, lastHeader, headers, dataPathHeader, groupHeaders, augmentedModel, startIndex)
                    startIndex ++
                    break
            }
        }


        AdditionalSheet outputSheet = addSheet(outputName, headers, groupHeaders, dataPathHeader)

        if (!includeSerialNumber) {
            Row firstRow = outputSheet.sheet.getRow(0)
            if (firstRow) {
                firstRow.setZeroHeight(true)
            }
        }
        final int firstDataRow = 2
        new ValidationProcessor(getWorkbook(), outputSheet.sheet, augmentedModel, hints).process(firstDataRow)
        new OutputDataProcessor(getWorkbook(), outputSheet.sheet, model, data, getStyle(), editMode, extraRowsEditable, hints).process(firstDataRow)

        finalise(outputSheet)
    }

    private static int widthFromString(String widthString, int defaultWidth) {
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

            if (sum <= 256*10) { // Just a catch for invalid widths causing problems.  They normally would add up to 100%
                sizeColumns()
            }
            else {
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

}

@Slf4j
class OutputDataProcessor {

    private Workbook workbook
    private Sheet sheet
    def model
    def data
    def rowHeaderStyle
    boolean editMode
    boolean extraRowsEditable
    CellStyle unlockedCellStyle
    CellStyle dateCellStyle
    Map hints

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
    public OutputDataProcessor(Workbook workbook, Sheet sheet, List<Map> model, List<Map> data, rowHeaderStyle, boolean editMode = false, boolean extraRowsEditable = true, Map hints = [:]) {
        this.workbook = workbook
        this.sheet = sheet
        this.model = model
        this.data = data
        this.hints = hints
        this.rowHeaderStyle = rowHeaderStyle
        this.editMode = editMode
        this.extraRowsEditable = extraRowsEditable

        // Apply date format after other style overrides to ensure it is not lost
        dateCellStyle = workbook.createCellStyle()
        short formatId = (short) BuiltinFormats.getBuiltinFormat("m/d/yy") // Localised date format
        dateCellStyle.setDataFormat(formatId)
        if (editMode) {
            unlockedCellStyle =  workbook.createCellStyle()
            unlockedCellStyle.setLocked(false)
            dateCellStyle.setLocked(false)
        }
    }

    private static Date parseDate(value) {
        Date dateValue = null
        if (value instanceof Date) {
            dateValue = value
        }
        else if (value instanceof String) {
            try {
                dateValue = au.org.ala.ecodata.DateUtil.parse(value)
            }
            catch (ParseException e) {
                log.info("Unable to parse date value ${value} in output template upload, leaving cell blank")
            }

        }
        return dateValue
    }

    public void process(int firstRow = 1) {

        data?.eachWithIndex { rowValue, rowCount ->
            Row row = sheet.createRow((rowCount+firstRow))

            def dataType, value, rowHeader
            int columnIndex = 0
            model.each { Map modelVal ->
                value = rowValue[modelVal.name] ?: ''

                dataType = modelVal.dataType
                if (OutputModelProcessor.isMultiSelect(modelVal)) {
                    dataType = 'stringList' // work around some forms declared with a data type of 'text' and a view type of 'select2Many'.
                }
                rowHeader = modelVal.rowHeader

                int numColumns = hints[modelVal.name]?.numColumns ?: 1
                for (int i=0; i<numColumns; i++) {
                    Cell cell = row.createCell(columnIndex)

                    switch (dataType) {
                        case 'number':
                            try {
                                cell.setCellValue(new BigDecimal(value))
                            }
                            catch (NumberFormatException e) {
                                cell.setCellValue(0)
                                log.warn("Invalid numeric value: " + value)
                            }

                            break
                        case 'species':
                            cell.setCellValue(value ? value.name : '')
                            break
                        case 'stringList':
                            if (value instanceof List) {
                                if (i<value.size()) {
                                    cell.setCellValue(value[i]?.toString() ?: '')
                                } else {
                                    cell.setCellValue('')
                                }
                            }
                            else {
                                cell.setCellValue(i==0 ? value.toString() : '')
                            }
                            break
                        case 'feature':
                            cell.setCellValue("")
                            break
                        case 'date':
                            if (value instanceof Number) {
                                cell.setCellValue(value.doubleValue())
                            }
                            else {
                                Date dateValue = parseDate(value)
                                if (dateValue) {
                                    cell.setCellValue(dateValue)
                                }
                            }
                            cell.setCellStyle(dateCellStyle)
                            break
                        case 'text':
                        default:
                            cell.setCellValue(value.toString())
                            break
                    }
                    if (rowHeader) {
                        cell.setCellStyle(rowHeaderStyle)
                    }
                    if (editMode && dataType != 'date') {
                        if (!modelVal.readOnly) {
                            cell.setCellStyle(unlockedCellStyle)
                        }
                    }
                    columnIndex++
                }
            }
        }
    }
}

class ValidationProcessor extends OutputModelProcessor {

    private Workbook workbook
    private Sheet sheet
    List<Map> model
    Map hints = [:]

    public ValidationProcessor(Workbook workbook, Sheet sheet, List<Map> model, Map hints = [:]){
        this.workbook = workbook
        this.sheet = sheet
        this.model = model
        this.hints = hints
    }

    public void process(int firstRow = 1) {

        // Create a worksheet to store validation lists in.
        def validationSheetName = OutputUploadTemplateBuilder.sheetName("Validation - "+sheet.getSheetName())
        Sheet validationSheet = workbook.createSheet(validationSheetName)
        workbook.setSheetHidden(workbook.getSheetIndex(validationSheet), true)

        ExcelValidationContext context = new ExcelValidationContext([currentSheet:sheet, validationSheet:validationSheet])
        ValidationHandler validationHandler = new ValidationHandler(hints)
        validationHandler.firstRow = firstRow
        int currentColumn = 0
        model.each { Map node ->
            context.currentColumn = currentColumn++
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
    int firstRow = 1
    final int MAX_ROWS = 1000
    Map hints = [:]

    ValidationHandler(Map hints = [:]) {
        this.hints = hints
    }

    def addValidation(Map node, ExcelValidationContext context, DataValidationConstraint constraint = null, String message = null) {

        DataValidationHelper dvHelper = context.currentSheet.getDataValidationHelper();
        OutputMetadata.ValidationRules rules = new OutputMetadata.ValidationRules(node)

        DataValidation dataValidation = dvHelper.createValidation(constraint, columnRange(context.currentColumn))
        if (rules.isMandatory()) {
            dataValidation.setEmptyCellAllowed(false)
        }
        dataValidation.setErrorStyle(DataValidation.ErrorStyle.STOP)
        if (message) {
            dataValidation.createErrorBox("Invalid Value", message)
        }
        dataValidation.setShowErrorBox(true)

        context.currentSheet.addValidationData(dataValidation);
    }

    static void addHeaderComment(ExcelValidationContext excelValidationContext, String message) {
        Sheet sheet = excelValidationContext.currentSheet
        int column = excelValidationContext.currentColumn
        // Add a comment to the header cell to explain column validation rules
        Drawing<?> drawing = sheet.createDrawingPatriarch()
        CreationHelper factory = sheet.getWorkbook().getCreationHelper()
        ClientAnchor anchor = factory.createClientAnchor()
        anchor.setCol1(column)
        anchor.setCol2(column + 3)
        anchor.setRow1(1)
        anchor.setRow2(6)
        Comment comment = drawing.createCellComment(anchor)
        RichTextString str = factory.createRichTextString(message)
        comment.setString(str)
        Cell headerCell = sheet.getRow(1).getCell(column)
        headerCell.setCellComment(comment)

    }

    @Override
    def number(Object node, ExcelValidationContext context) {
        DataValidationHelper dvHelper = context.currentSheet.getDataValidationHelper();
        OutputMetadata.ValidationRules rules = new OutputMetadata.ValidationRules(node)

        def max = rules.max()
        def min = rules.min()
        if (!min) {
            min = '0'
        }
        def operator = max ? DataValidationConstraint.OperatorType.BETWEEN : DataValidationConstraint.OperatorType.GREATER_OR_EQUAL

        DataValidationConstraint dvConstraint =
                dvHelper.createNumericConstraint(DataValidationConstraint.ValidationType.DECIMAL,
                operator, min, max?max.toString():"")

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

    /**
     * Extracts the constraints from the node configuration and writes them to the validation sheet.
     * @param node The annotated model node.
     * @param context The processing context.
     * @return The list of constraints for use in the data validation formula and message.
     */
    private static List buildConstraintList(Map node, ExcelValidationContext context, Map hints) {
        List constraints = []
        if (node.constraints instanceof Map) {
            if (node.constraints.type == 'computed') {
                constraints = node.constraints.options?.collect { it.value }?.flatten()?.unique()
            // Pre-populated constraints are complex and can depend on form rendering context (e.g. the project) so we use hints supplied by the caller to populate constraints
            } else if (node.constraints.type == 'pre-populated') {
                if (hints[node.name]?.constraints) {
                    constraints = hints[node.name].constraints
                }
            }
        } else {
            constraints = node.constraints
        }

        constraints.eachWithIndex { value, i ->
            Row row = context.validationSheet.getRow(i)
            if (!row) {
                row = context.validationSheet.createRow(i)
            }
            Cell cell = row.createCell(context.currentColumn)
            cell.setCellValue(value)
        }
        return constraints
    }

    @Override
    def text(Object node, ExcelValidationContext context) {
        if (node.constraints) {
            List constraintList = buildConstraintList(node, context, hints)
            createDropDownValidation(node, context, constraintList)
        }
    }

    private void createDropDownValidation(node, ExcelValidationContext context, List constraintList) {
        if (!constraintList) {
            return // We can't always construct constraints, e.g. pre-populated constraints without hints (which happens if the table has no rows at time of download)
        }

        String colString = CellReference.convertNumToColString(context.currentColumn)
        String formula = "'${context.validationSheet.getSheetName()}'!\$${colString}\$1:\$${colString}\$${constraintList.size()}"
        DataValidationHelper dvHelper = context.currentSheet.getDataValidationHelper();
        DataValidationConstraint dvConstraint =
                dvHelper.createFormulaListConstraint(formula)
        addValidation(node, context, dvConstraint)
    }

    @Override
    def date(Object node, ExcelValidationContext context) {
        addHeaderComment(context, "Please use the format dd/mm/yyyy for dates")
        DataValidationHelper dvHelper = context.currentSheet.getDataValidationHelper()
        OutputMetadata.ValidationRules rules = new OutputMetadata.ValidationRules(node)

        def max = rules.max()
        def min = rules.min()
        if (!min) {
            min = '2013-01-01'
        }
        def operator = max ? DataValidationConstraint.OperatorType.BETWEEN : DataValidationConstraint.OperatorType.GREATER_OR_EQUAL

        DataValidationConstraint dvConstraint =
                dvHelper.createDateConstraint(operator, min, max?max.toString():"", "yyyy-MM-dd")

        addValidation(node, context, dvConstraint)

        // Apply date format after other style overrides to ensure it is not lost
        CellStyle dateCellStyle = context.currentSheet.workbook.createCellStyle()
        short formatId = (short) BuiltinFormats.getBuiltinFormat("m/d/yy") // Localised date format
        dateCellStyle.setDataFormat(formatId)

        context.currentSheet.setDefaultColumnStyle(context.currentColumn, dateCellStyle)
    }

    @Override
    def image(Object node, ExcelValidationContext context) {

    }

    @Override
    def embeddedImages(Object node, ExcelValidationContext context) {

    }

    @Override
    def species(Object node, ExcelValidationContext context) {
        addHeaderComment(context, "Please enter only the scientific name of the species.  A lookup of the name will be performed during data import to match the name to an ALA taxon.")
    }

    @Override
    def stringList(Object node, ExcelValidationContext context) {
        List constraintList = buildConstraintList(node, context, hints)
        addHeaderComment(context, "For multiple selections, add each value in a separate column.  You may add new columns to the sheet as required but must use the same column heading for each.  Acceptable values are: " + constraintList.join(", "))
        createDropDownValidation(node, context, constraintList)
    }

    def columnRange(int col) {
        CellRangeAddressList range = new CellRangeAddressList(firstRow, MAX_ROWS, col, col)
        return range
    }

    @Override
    def booleanType(node, ExcelValidationContext context) {

    }

    @Override
    def document(node, ExcelValidationContext context) {
        addHeaderComment("Document uploads are not supported in this template.  Documents should be uploaded via the user interface after uploading the spreadsheet.")
    }

    @Override
    def feature(node, ExcelValidationContext context) {
        addHeaderComment("Uploading spatial data is not supported in this template.  Please enter spatial data via the user interface after uploading the spreadsheet.")
    }

}