package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.*
import grails.testing.gorm.DomainUnitTest
import grails.testing.web.GrailsWebUnitTest
import grails.util.Holders
import spock.lang.Specification

class TabbedExporterSpec extends Specification implements GrailsWebUnitTest, DomainUnitTest<ActivityForm> {

    TabbedExporter tabbedExporter
    XlsExporter xlsExporter = Mock(XlsExporter)
    MetadataService metadataService = Mock(MetadataService)
    UserService userService = Mock(UserService)
    ReportingService reportingService = Mock(ReportingService)
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
        tabbedExporter.activityFormService = activityFormService
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

    def "The getSheet method invokes createSheet if the sheet is not found"() {
        when:
        tabbedExporter.getSheet("Test a name above 31 characters which will be shortened to the same name", [])

        then:
        tabbedExporter.activitySheetNames["Test a name above...e same name"] == "Test a name above 31 characters which will be shortened to the same name"

        when:
        tabbedExporter.getSheet("Test a name above 31 characters which will also be shortened to the same name", [])

        then:
        tabbedExporter.activitySheetNames == ["Test a name above...e same name":"Test a name above 31 characters which will be shortened to the same name",
                                              "Test a name above...e same n(1)":"Test a name above 31 characters which will also be shortened to the same name"]
    }

    def "Array valued data models will be spread across multiple columns in the output if the set of possible values are avaialble"() {
        setup:
        String type = 'form'
        ActivityForm form = buildMockForm(type, buildFormTemplateWithPrepopConstraints())

        when:
        List config = tabbedExporter.getActivityExportConfig(type, true)

        then:
        1 * activityFormService.findVersionedActivityForm(type) >> [form]
        config[1].header == 'Normal constraints - c1'
        config[1].property == 'form.withConstraints[c1]'

        config[2].header == 'Normal constraints - c2'
        config[2].property == 'form.withConstraints[c2]'

        config[3].header == 'Pre-populated constraints'
        config[3].property == 'form.withPrePopConstraints'


    }

    private ActivityForm buildMockForm(String name, Map template) {
        ActivityForm form = new ActivityForm(name:name, formVersion:1)
        FormSection section = new FormSection(name:name, template:template)
        form.sections = [section]
        form
    }

    private Map buildFormTemplateWithPrepopConstraints() {
        [
                dataModel:[
                        [
                                name:"withConstraints",
                                dataType:"stringList",
                                constraints:[
                                        "c1", "c2"
                                ]
                        ],
                        [
                                name:"withPrePopConstraints",
                                dataType:"stringList",
                                constraints:[
                                        type:"pre-populated",
                                        "source":[
                                            "url":"test.com"
                                        ]
                                ]
                        ]
                ],
                viewModel:[
                        [type:'row', items:[
                                [
                                        type:'selectMany',
                                        source:'withConstraints',
                                        preLabel:'Normal constraints'
                                ],
                                [
                                        type:'selectMany',
                                        source:'withPrePopConstraints',
                                        preLabel:'Pre-populated constraints'
                                ]
                        ]]

                ]
        ]
    }
}
