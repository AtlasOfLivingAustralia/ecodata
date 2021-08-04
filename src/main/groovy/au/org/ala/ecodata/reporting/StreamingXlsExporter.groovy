package au.org.ala.ecodata.reporting


import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook

/**
 * Does basic header styling for an Xls spreadsheet.
 */
class StreamingXlsExporter extends XlsExporter {

    StreamingXlsExporter(fileName) {
        super(fileName)
    }

    /**
     * Override the parent method to ignore the workbook and create one of the type we want (to use the streaming
     * API).  The default workbook XSSFWorkbook uses too much memory when large downloads are requested.
     * @param workbook ignored.
     */
    protected setUp(Workbook workbook) {
        // Ignore the workbook param to create a streaming version to manage memory use better.
        this.workbook = new SXSSFWorkbook(100)
        super.setUp(this.workbook)
    }
}
