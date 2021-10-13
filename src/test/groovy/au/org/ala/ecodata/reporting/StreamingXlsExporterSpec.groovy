package au.org.ala.ecodata.reporting

import org.apache.poi.xssf.streaming.SXSSFWorkbook
import spock.lang.Specification

class StreamingXlsExporterSpec extends Specification {

    def "The StreamingXlsExporter creates an instance of the POI SXSSFWorkbook instead of the default"() {
        when:
        StreamingXlsExporter exporter = new StreamingXlsExporter("test")

        then:
        exporter.workbook instanceof SXSSFWorkbook

    }
}
