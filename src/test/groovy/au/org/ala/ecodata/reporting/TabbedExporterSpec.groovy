package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.ActivityFormService
import au.org.ala.ecodata.ExcelImportService
import au.org.ala.ecodata.ManagementUnitService
import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.ProjectService
import au.org.ala.ecodata.ReportingService
import au.org.ala.ecodata.UserService
import grails.core.GrailsApplication
import grails.testing.web.GrailsWebUnitTest
import grails.util.Holders
import spock.lang.Specification

class TabbedExporterSpec extends Specification implements GrailsWebUnitTest {

    TabbedExporter tabbedExporter
    XlsExporter xlsExporter = Mock(XlsExporter)
    //ProjectService projectService = Mock(ProjectService)
    MetadataService metadataService = Mock(MetadataService)
    UserService userService = Mock(UserService)
    ReportingService reportingService = Mock(ReportingService)
    //ManagementUnitService managementUnitService = Stub(ManagementUnitService)
    //ExcelImportService excelImportService
    ActivityFormService activityFormService = Mock(ActivityFormService)

    void setup() {
        Holders.grailsApplication = grailsApplication
        defineBeans {
            metadataService(MetadataService)
            userService(UserService)
            reportingService(ReportingService)
            activityFormService(ActivityFormService)
        }
        tabbedExporter = new TabbedExporter(xlsExporter)
    }

    void "If duplicate sheet names are encountered during an export, they will be made unique"() {
        when:
        tabbedExporter.createSheet("Test a name above 31 characters which will be shortened to the same name", [])

        then:
        tabbedExporter.activitySheetNames["Test a name above...e same name"] == "Test a name above 31 characters which will be shortened to the same name"

        when:
        tabbedExporter.createSheet("Test a name above 31 characters which will also be shortened to the same name", [])

        then:
        tabbedExporter.activitySheetNames == ["Test a name above...e same name":"Test a name above 31 characters which will be shortened to the same name",
                                              "Test a name above...e same n(1)":"Test a name above 31 characters which will also be shortened to the same name"]

        when: "we add another name that will become a duplicate when shortened to 31 characters"
        tabbedExporter.createSheet("Test a name above 31 characters which will be another one that has to be shortened to the same name", [])

        then:
        tabbedExporter.activitySheetNames == ["Test a name above...e same name":"Test a name above 31 characters which will be shortened to the same name",
                                              "Test a name above...e same n(1)":"Test a name above 31 characters which will also be shortened to the same name",
                                              "Test a name above...e same n(2)":"Test a name above 31 characters which will be another one that has to be shortened to the same name"]

        when: "we add another name that will become a duplicate when shortened to 31 characters"
        tabbedExporter.createSheet("Test a name above 31 characters which will be another one (2) that has to be shortened to the same name", [])

        then:
        tabbedExporter.activitySheetNames == ["Test a name above...e same name":"Test a name above 31 characters which will be shortened to the same name",
                                              "Test a name above...e same n(1)":"Test a name above 31 characters which will also be shortened to the same name",
                                              "Test a name above...e same n(2)":"Test a name above 31 characters which will be another one that has to be shortened to the same name",
                                              "Test a name above...e same n(3)":"Test a name above 31 characters which will be another one (2) that has to be shortened to the same name"]

    }
}
