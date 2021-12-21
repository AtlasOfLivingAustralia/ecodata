package au.org.ala.ecodata.reporting

import spock.lang.Specification

class XlsExporterSpec extends Specification {

    def "The sheet name preserves the start and end of an activity name if that name exceeds 31 characters, which is an Excel limitation"() {
        expect:
        XlsExporter.sheetName("Community Participation and Engagement") == "Community Partici... Engagement"
        XlsExporter.sheetName("Community Participation and Engagement_V1") == "Community Partici...gagement_V1"
        XlsExporter.sheetName("Wildlife Recovery Progress Report - CVA") == "Wildlife Recovery...eport - CVA"
        XlsExporter.sheetName("Wildlife Recovery Progress Report - GA") == "Wildlife Recovery...Report - GA"
        XlsExporter.sheetName("Revegetation") == "Revegetation"
        XlsExporter.sheetName("1234567890123456789012345678901") == "1234567890123456789012345678901"
        XlsExporter.sheetName("12345678901234567890123456789012") == "12345678901234567...23456789012"
        XlsExporter.sheetName("Developing/updating Guidelines/Protocols/Plans") == "Developing-updati...ocols-Plans"

    }
}
