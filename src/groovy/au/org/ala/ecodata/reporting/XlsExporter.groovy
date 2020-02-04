package au.org.ala.ecodata.reporting

import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import pl.touk.excel.export.XlsxExporter
import pl.touk.excel.export.multisheet.AdditionalSheet

import javax.servlet.http.HttpServletResponse

/**
 * Does basic header styling for an Xls spreadsheet.
 */
class XlsExporter extends XlsxExporter {

    static final int MAX_SHEET_NAME_LENGTH = 31
    def fileName

    public XlsExporter(fileName) {
        super(fileName)
        this.fileName = fileName
    }

    public static String sheetName(String name) {
        int end = Math.min(name.length(), MAX_SHEET_NAME_LENGTH) - 1
        def shortName = name[0..end]
        shortName = shortName.replaceAll('[^a-zA-z0-9 ]', '')

        shortName
    }

    public AdditionalSheet addSheet(name, headers, groupHeaders = null) {
        AdditionalSheet sheet = sheet(sheetName(name))
        if (groupHeaders != null) {
            sheet.fillHeader(groupHeaders)
            sheet.fillRow(headers, 1)

            def lastHeader = ""
            def groupNumber = 0
            int fromCol = 0
            groupHeaders.eachWithIndex { item, index ->
                if (item != "" && lastHeader != item) {
                    styleRowCells(sheet, 0, fromCol, index-1, customHeaderStyle(getWorkbook(), groupNumber))
                    styleRowCells(sheet, 1, fromCol, index-1, customHeaderStyle(getWorkbook(), groupNumber))
                    groupNumber ++
                    fromCol = index
                }
            }

            styleRowCells(sheet, 0, fromCol, groupHeaders.size()-1, customHeaderStyle(getWorkbook(), groupNumber))
            styleRowCells(sheet, 1, fromCol, groupHeaders.size()-1, customHeaderStyle(getWorkbook(), groupNumber))

        } else {
            if(headers) {
                sheet.fillHeader(headers)
                styleRow(sheet, 0, headerStyle(getWorkbook()))
            }
        }



        sheet
    }

    def styleRowCells(AdditionalSheet sheet, int row, int fromCol, int toCol, CellStyle style) {
        sheet.sheet.getRow(row).cellIterator().toList().eachWithIndex {item, index ->
            if (index > toCol) return
            else if (index >= fromCol && index <= toCol) {
                item.setCellStyle(style)
            }
        }
    }

    def styleRow(AdditionalSheet sheet, int row, CellStyle style) {
        sheet.sheet.getRow(row).cellIterator().toList().each {
            it.setCellStyle(style)
        }
    }

    CellStyle customHeaderStyle(Workbook workbook, int i) {

        def backgroundColorIndex
        switch (i) {
            case 0: backgroundColorIndex = IndexedColors.LIGHT_TURQUOISE.index; break;
            case 1: backgroundColorIndex = IndexedColors.LEMON_CHIFFON.index; break;
            case 2: backgroundColorIndex = IndexedColors.LIGHT_BLUE.index; break;
            case 3: backgroundColorIndex = IndexedColors.LIGHT_ORANGE.index; break;
            case 4: backgroundColorIndex = IndexedColors.LIGHT_GREEN.index; break;
            case 5: backgroundColorIndex = IndexedColors.LIGHT_CORNFLOWER_BLUE.index; break;
            case 6: backgroundColorIndex = IndexedColors.LIGHT_YELLOW.index; break;
            case 7: backgroundColorIndex = IndexedColors.PALE_BLUE.index; break;
            case 8: backgroundColorIndex = IndexedColors.TAN.index; break;
            case 9: backgroundColorIndex = IndexedColors.ORCHID.index; break;
            default: backgroundColorIndex = IndexedColors.GREY_50_PERCENT.index; break;
        }

        CellStyle headerStyle = workbook.createCellStyle();

        headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        headerStyle.setFillForegroundColor(backgroundColorIndex);
        Font font = workbook.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setColor(HSSFColor.BLACK.index);
        headerStyle.setFont(font);
        return headerStyle
    }


    CellStyle headerStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillBackgroundColor(IndexedColors.BLACK.getIndex());
        headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setColor(HSSFColor.WHITE.index);
        headerStyle.setFont(font);
        return headerStyle
    }

    def getStyle() {
        headerStyle(getWorkbook())
    }

    XlsExporter setResponseHeaders(HttpServletResponse response) {
        setHeaders(response, fileName + filenameSuffix)
        this
    }

    def sizeColumns() {
        for (Sheet sheet : workbook) {
            // For table upload templates, the validation sheet may have no rows if nothing needs validation.
            def row = sheet.getRow(0)
            if (row) {
                int columns = row.getLastCellNum()
                for (int col = 0; col < columns; col++) {
                    sheet.autoSizeColumn(col);
                }
            }
        }


    }

    private XlsExporter setHeaders(HttpServletResponse response, def filename) {
        response.setHeader("Content-disposition", "attachment; filename=\"$filename\";")
        response.setHeader("Content-Type", "application/vnd.ms-excel")
        this
    }
}
