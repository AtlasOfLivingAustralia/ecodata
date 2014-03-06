package au.org.ala.ecodata

import au.org.ala.ecodata.metadata.OutputMetadata
import au.org.ala.ecodata.metadata.OutputModelProcessor
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddressList
import pl.touk.excel.export.WebXlsxExporter
import pl.touk.excel.export.multisheet.AdditionalSheet

import javax.servlet.http.HttpServletResponse

class OutputUploadTemplateBuilder extends WebXlsxExporter {

    def model
    def outputName

    public OutputUploadTemplateBuilder(outputName, model) {
        this.outputName = outputName
        this.model = model
    }

    public void build() {

        AdditionalSheet outputSheet = sheet(outputName)

        def headers = model.collect { it.label ?: it.name }
        outputSheet.fillHeader(headers)
        styleRow(outputSheet, 0, headerStyle(getWorkbook()))

        new ValidationProcessor(outputSheet.sheet, model).process()
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

    def sheet, model

    public ValidationProcessor(sheet, model) {
        this.sheet = sheet
        this.model = model
    }

    public void process(){
        ExcelContext context = new ExcelContext([currentSheet:sheet])
        ValidationHandler validationHandler = new ValidationHandler()
        model.eachWithIndex{node, i ->
            context.currentColumn = i
            processNode(validationHandler, node, context)
        }
    }


}

class ExcelContext implements OutputModelProcessor.ProcessingContext {
    Sheet currentSheet
    int currentColumn

}

class ValidationHandler implements OutputModelProcessor.Processor<ExcelContext> {


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
    def number(Object node, ExcelContext context) {
        DataValidationHelper dvHelper = context.currentSheet.getDataValidationHelper();
        OutputMetadata.ValidationRules rules = new OutputMetadata.ValidationRules(node)

        def operator = rules.max() ? DataValidationConstraint.OperatorType.BETWEEN : DataValidationConstraint.OperatorType.GREATER_OR_EQUAL

        DataValidationConstraint dvConstraint =
                dvHelper.createNumericConstraint(DataValidationConstraint.ValidationType.DECIMAL,
                operator, rules.min().toString(), rules.max()?:"")

        addValidation(node, context, dvConstraint)
    }

    @Override
    def text(Object node, ExcelContext context) {
        if (node.constraints) {
            DataValidationHelper dvHelper = context.currentSheet.getDataValidationHelper();
            DataValidationConstraint dvConstraint =
                    dvHelper.createExplicitListConstraint(node.constraints.toArray(new String[0]))


            addValidation(node, context, dvConstraint)
        }
    }

    @Override
    def date(Object node, ExcelContext context) {

    }

    @Override
    def image(Object node, ExcelContext context) {

    }

    @Override
    def species(Object node, ExcelContext context) {

    }

    def columnRange(int col) {
        final int MAX_ROWS = 1000
        CellRangeAddressList range = new CellRangeAddressList(1, MAX_ROWS, col, col)
        return range
    }
}
