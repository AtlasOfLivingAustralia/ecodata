package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.ss.util.CellReference
import pl.touk.excel.export.WebXlsxExporter
import pl.touk.excel.export.multisheet.AdditionalSheet

import javax.servlet.http.HttpServletResponse

class OutputUploadTemplateBuilder extends WebXlsxExporter {

    static final int MAX_SHEET_NAME_LENGTH = 31
    def model
    def outputName

    public OutputUploadTemplateBuilder(outputName, model) {
        this.outputName = outputName
        this.model = model
    }


    public static String sheetName(name) {
        def end = Math.min(name.length(), MAX_SHEET_NAME_LENGTH)-1
        return name[0..end]
    }
    public void build() {

        AdditionalSheet outputSheet = sheet(sheetName(outputName))

        def headers = model.collect {
            def label = it.label ?: it.name
            if (it.dataType == 'species') {
                label += ' (Scientific Name Only)'
            }
            label
        }
        outputSheet.fillHeader(headers)
        styleRow(outputSheet, 0, headerStyle(getWorkbook()))

        new ValidationProcessor(getWorkbook(), outputSheet.sheet, model).process()
        finalise()
    }

    def styleRow(AdditionalSheet sheet, int row, CellStyle style) {
        sheet.sheet.getRow(row).cellIterator().toList().each {
            it.setCellStyle(style)
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillBackgroundColor(IndexedColors.BLACK.getIndex());
        headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setColor(HSSFColor.WHITE.index);
        headerStyle.setFont(font);
        return headerStyle
    }

    def finalise() {
        for (Sheet sheet:workbook) {
            int columns = sheet.getRow(0).getLastCellNum()
            for (int col=0; col<columns; col++) {
                sheet.autoSizeColumn(col);
            }
        }


    }

    WebXlsxExporter setResponseHeaders(HttpServletResponse response) {
        super.setResponseHeaders(response, outputName+filenameSuffix)
        this
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
                operator, rules.min().toString(), rules.max()?:"")

        addValidation(node, context, dvConstraint)
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
    def species(Object node, ExcelValidationContext context) {

    }

    def columnRange(int col) {
        final int MAX_ROWS = 1000
        CellRangeAddressList range = new CellRangeAddressList(1, MAX_ROWS, col, col)
        return range
    }

}