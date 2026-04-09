package au.org.ala.ecodata.reporting

import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.*
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

    /**
     * Produces a 31 character sheet name from the supplied string, sanitizing for Excel compatibility.
     */
    static String sheetName(String name) {
        // Remove invalid characters: \\ / ? * [ ] :
        String sanitized = name.replaceAll(/[\\\/\?\*\[\]:]/, "-")
        // Trim leading/trailing spaces
        sanitized = sanitized.trim()
        // Remove leading/trailing single quotes
        sanitized = sanitized.replaceAll(/^'+|'+$/, "")
        int prefixLength = 17
        int suffixLength = 11
        if (sanitized.size() > MAX_SHEET_NAME_LENGTH) {
            sanitized = sanitized[0..prefixLength-1] + '...' + sanitized[-suffixLength..sanitized.size()-1]
        }

        return sanitized
    }

    public AdditionalSheet addSheet(name, headers, groupHeaders = null, dataPathHeader = null) {
        AdditionalSheet sheet = sheet(sheetName(name))

        if (dataPathHeader != null) {
            sheet.fillHeader(dataPathHeader)
            sheet.fillRow(headers, 1)

            String lastHeader = ""
            def groupNumber = 0
            int fromCol = 0


            // If no group headers are provided, use the number of dots in the data path header to determine groupings.
            // That way each level of nesting in the form gets a distinct header style.
            if (!groupHeaders) {
                groupHeaders = dataPathHeader.collect { String header ->
                    Integer.toString(header.count('.'))
                }
            }

            // Apply a new style each time the group header changes.
            CellStyle cellStyle = headerStyle(getWorkbook())
            lastHeader = groupHeaders?[0] ?: ""
            dataPathHeader.eachWithIndex { item, index ->
                String groupHeader = groupHeaders ? groupHeaders[index] : item
                if (item != "" && groupHeader != "" && lastHeader != groupHeader) {
                    styleRowCells(sheet, 0, fromCol, index-1, cellStyle)
                    styleRowCells(sheet, 1, fromCol, index-1, cellStyle)
                    groupNumber ++
                    fromCol = index
                    cellStyle = customHeaderStyle(getWorkbook(), groupNumber)

                }
                lastHeader = groupHeader
            }

            styleRowCells(sheet, 0, fromCol, dataPathHeader.size()-1, cellStyle)
            styleRowCells(sheet, 1, fromCol, dataPathHeader.size()-1, cellStyle)

        }
        else {
            if (headers) {
                if (!(headers[0] instanceof List)) {
                    headers = [headers]
                }
                headers.eachWithIndex { List row, int i ->
                    if (i == 0) {
                        sheet.fillHeader(row)
                    }
                    else {
                        sheet.fillRow(row, i)
                    }

                    styleRow(sheet, i, headerStyle(getWorkbook()))
                }

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

        i = i % 10
        def backgroundColorIndex
        def foregroundColorIndex = HSSFColor.HSSFColorPredefined.BLACK.index
        switch (i) {
            case 0:
                backgroundColorIndex = IndexedColors.BLACK.index
                foregroundColorIndex = IndexedColors.WHITE.index
                break
            case 1:
                backgroundColorIndex = IndexedColors.LEMON_CHIFFON.index
                break
            case 2:
                backgroundColorIndex = IndexedColors.LIGHT_BLUE.index
                foregroundColorIndex = IndexedColors.WHITE.index
                break
            case 3:
                backgroundColorIndex = IndexedColors.LIGHT_ORANGE.index
                foregroundColorIndex = IndexedColors.WHITE.index
                break
            case 4:
                backgroundColorIndex = IndexedColors.LIGHT_GREEN.index
                break
            case 5:
                backgroundColorIndex = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
                break
            case 6:
                backgroundColorIndex = IndexedColors.LIGHT_YELLOW.index
                break
            case 7:
                backgroundColorIndex = IndexedColors.PALE_BLUE.index
                break
            case 8:
                backgroundColorIndex = IndexedColors.TAN.index
                foregroundColorIndex = IndexedColors.WHITE.index
                break;
            case 9:
                backgroundColorIndex = IndexedColors.ORCHID.index
                break
            default:
                backgroundColorIndex = IndexedColors.GREY_50_PERCENT.index
                foregroundColorIndex = IndexedColors.WHITE.index
                break
        }

        CellStyle headerStyle = workbook.createCellStyle();

        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFillForegroundColor(backgroundColorIndex);
        Font font = workbook.createFont();
        font.setBold(true)
        font.setColor(foregroundColorIndex)
        headerStyle.setFont(font);
        return headerStyle
    }


    CellStyle headerStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillBackgroundColor(IndexedColors.BLACK.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
        Font font = workbook.createFont()
        font.setBold(true)
        font.setColor(HSSFColor.HSSFColorPredefined.WHITE.index)
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
