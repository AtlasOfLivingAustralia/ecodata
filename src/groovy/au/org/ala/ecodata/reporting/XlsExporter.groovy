package au.org.ala.ecodata.reporting
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import pl.touk.excel.export.WebXlsxExporter
import pl.touk.excel.export.multisheet.AdditionalSheet

import javax.servlet.http.HttpServletResponse
/**
 * Does basic header styling for an Xls spreadsheet.
 */
class XlsExporter extends WebXlsxExporter {

    static final int MAX_SHEET_NAME_LENGTH = 31
    def fileName

    public XlsExporter(fileName) {
        this.fileName = fileName
    }


    public static String sheetName(name) {
        def end = Math.min(name.length(), MAX_SHEET_NAME_LENGTH)-1
        return name[0..end]
    }
    public AdditionalSheet addSheet(name, headers) {

        AdditionalSheet sheet = sheet(sheetName(name))
        sheet.fillHeader(headers)
        styleRow(sheet, 0, headerStyle(getWorkbook()))

        sheet
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

    WebXlsxExporter setResponseHeaders(HttpServletResponse response) {
        super.setResponseHeaders(response, fileName+filenameSuffix)
        this
    }

    def sizeColumns() {
        for (Sheet sheet:workbook) {
            int columns = sheet.getRow(0).getLastCellNum()
            for (int col=0; col<columns; col++) {
                sheet.autoSizeColumn(col);
            }
        }


    }
}
