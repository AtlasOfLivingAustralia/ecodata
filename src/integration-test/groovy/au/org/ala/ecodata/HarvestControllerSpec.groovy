package au.org.ala.ecodata

import au.com.bytecode.opencsv.CSVReader
import com.mongodb.BasicDBObject
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.util.GrailsWebMockUtil
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Integration
@Rollback
class HarvestControllerSpec extends Specification {
    @Autowired
    HarvestController harvestController

    @Autowired
    WebApplicationContext ctx

    @Autowired
    RecordService recordService

    def setup() {
        cleanup()
        GrailsMockHttpServletRequest grailsMockHttpServletRequest = new GrailsMockHttpServletRequest()
        GrailsMockHttpServletResponse grailsMockHttpServletResponse = new GrailsMockHttpServletResponse()
        GrailsWebMockUtil.bindMockWebRequest(ctx, grailsMockHttpServletRequest, grailsMockHttpServletResponse)
    }

    def cleanup() {
        Project.collection.remove(new BasicDBObject())
        ProjectActivity.collection.remove(new BasicDBObject())
        Activity.collection.remove(new BasicDBObject())
        Output.collection.remove(new BasicDBObject())
        ActivityForm.collection.remove(new BasicDBObject())
        Organisation.collection.remove(new BasicDBObject())
        Document.collection.remove(new BasicDBObject())
    }

    // write integration test for harvestController#getDarwinCoreArchiveForProject
    void "getDarwinCoreArchiveForProject should be able to generate event core zip file"() {
        setup:
        // create organization
        Date now = new Date()
        Date yesterday = now.minus(1)
        String nowStr = recordService.toStringIsoDateTime(now)
        String yesterdayStr = recordService.toStringIsoDateTime(yesterday)
        def organisation = new Organisation(name: "Test Organisation", organisationId: "org1")
        organisation.save(flush: true, failOnError: true)
        def project = new Project (
                name: "Test Project",
                alaHarvest: true,
                projectId: "project1",
                organisationId: organisation.organisationId,
                organisationName: "Test Organisation",
                description: "Test project description",
                dataResourceId: "dr1",
                dateCreated: yesterday
        )
        project.save(flush: true, failOnError: true)
        def activityForm = new ActivityForm(
                name: 'testForm',
                publicationStatus: "published",
                status: "active",
                type: "Activity",
                formVersion: 1,
                sections: [
                        new FormSection(
                                name: "section1",
                                template: [
                                        dataModel: [
                                                [
                                                        name        : "testField",
                                                        dataType    : "text",
                                                        description : "event remarks",
                                                        dwcAttribute: "eventRemarks",
                                                ],
                                                [
                                                        name       : "speciesName",
                                                        dataType   : "species",
                                                        description: "species field"
                                                ],
                                                [
                                                        dataType           : "number",
                                                        name               : "distance",
                                                        dwcAttribute       : "measurementValue",
                                                        measurementType    : "number",
                                                        measurementTypeID  : "http://qudt.org/vocab/quantitykind/Number",
                                                        measurementAccuracy: "0.001",
                                                        measurementUnit    : "m",
                                                        measurementUnitID  : "http://qudt.org/vocab/unit/M",
                                                        description        : "distance from source"
                                                ],
                                                [
                                                        dataType   : "image",
                                                        name       : "images",
                                                        description: "photo of species"
                                                ]
                                        ]
                                ]
                        )
                ]
        )
        activityForm.save(failOnError: true, flush: true)
        def projectActivity = new ProjectActivity(
                name: "Test Project Activity",
                projectId: project.projectId,
                projectActivityId: "pa1",
                published: true,
                endDate: now, startDate: yesterday,
                status: "active",
                dataSharingLicense: "CC BY 4.0",
                description: "Test event remarks",
                methodName: "opportunistic",
                pActivityFormName: activityForm.name,
                methodType: "opportunistic",
                spatialAccuracy: "low",
                speciesIdentification: "high",
                temporalAccuracy: "moderate",
                nonTaxonomicAccuracy: "high",
                dataQualityAssuranceMethods: ["dataownercurated"],
                dataAccessMethods: ["na"],
        )
        projectActivity.save(flush: true, failOnError: true)
        // create an activity
        def activity = new Activity(name: "Test Activity",
                projectId: project.projectId,
                projectActivityId: projectActivity.projectActivityId,
                activityId: 'activity1',
                siteId: 'site1'
        )
        activity.save(flush: true, failOnError: true)
        def document = new Document(
                documentId: "document1",
                filename: 'z.png',
                status: 'active',
                imageId: 'image1',
                contentType: 'image/png',
                attribution: "John Doe",
                licence    : "CC BY 4.0"
        )
        document.save(flush: true, failOnError: true)
        // create an output
        def output = new Output(
                name: "section1",
                activityId: activity.activityId,
                outputId: 'output1'
        )
        output.data = [
                        testField  : "Test event remarks",
                        speciesName: [name: "Anura (Frog)", scientificName: 'Anura', commonName: "Frog", outputSpeciesId: "outputSpecies1"],
                        distance   : 1.0,
                        images     : [[
                                              documentId : "document1",
                                              filename   : "z.png",
                                              identifier : "http://example.com/image",
                                              creator    : "John Doe",
                                              title      : "Image of a frog",
                                              rightHolder: "John Doe",
                                              license    : "CC BY 4.0",
                                      ]]
        ]
        output.save(flush: true, failOnError: true)
        //
        when:
        harvestController.getDarwinCoreArchiveForProject(project.projectId)

        then:
        harvestController.response.status == 200
        ByteArrayInputStream zipStream = new ByteArrayInputStream(harvestController.response.contentAsByteArray)
        ZipInputStream zipInputStream = new ZipInputStream(zipStream)
        ZipEntry entry
        int numberOfEntries = 0
        while ((entry = zipInputStream.getNextEntry()) != null) {
            numberOfEntries ++
            StringBuffer buffer = new StringBuffer()
            BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream))
            String line
            String content = reader.lines().collect().join("\n")

            switch (entry.name) {
                case "eml.xml":
                    // read content of eml.xml and check if it contains the correct project
                    // and project activity information
                    GPathResult xml = new XmlSlurper().parseText(content)
                    assert xml?.dataset?.title.text().trim() == project.name
                    assert xml?.dataset?.creator?.organizationName.text().trim() == organisation.name
                    assert xml?.dataset?.abstract?.para.text().trim() == project.description
                    break
                case "meta.xml":
                    List spokes = ["Occurrence.csv", "MeasurementOrFact.csv", "Media.csv"]
                    GPathResult xml = new XmlSlurper().parseText(content)
                    assert xml?.core?.files.location.text().trim() == "Event.csv"
                    assert xml?.core?.field?.size() > 1
                    assert xml?.core?.id?.size() == 1
                    assert spokes.contains(xml?.extension.files.location[0].text().trim())
                    assert spokes.contains(xml?.extension.files.location[1].text().trim())
                    assert spokes.contains(xml?.extension.files.location[2].text().trim())
                    assert xml?.extension[0].coreid.size() == 1
                    assert xml?.extension[1].coreid.size() == 1
                    assert xml?.extension[2].coreid.size() == 1
                    break
                case "Event.csv":
                    CSVReader readerCSV = new CSVReader(new StringReader(content))
                    List<String[]> lines = readerCSV.readAll()
                    List headers = lines[0]
                    List expectedHeaders = ["eventID","parentEventID","eventType","eventDate","eventRemarks","samplingProtocol","geodeticDatum", "locationID", "endDate", "dataSharingLicense", "license", "name", "pActivityFormName", "startDate"]
                    List expectedValues = ["pa1", "", "Survey", "${yesterdayStr}/${nowStr}", "Test event remarks", "opportunistic", "","" , nowStr, "CC BY 4.0", "", "Test Project Activity", "testForm", yesterdayStr]
                    expectedHeaders.eachWithIndex { header, index ->
                        int csvIndex = headers.findIndexOf { it == header }
                        assert lines[1][csvIndex] == expectedValues[index]
                    }
                    assert lines[2][0] == "activity1"
                    assert lines[2][1] == "pa1"
                    assert lines[2][2] == "SiteVisit"
                    assert lines[2][4] == "Test event remarks"
                    assert lines.size() == 3
                    break
                case "Media.csv":
                    CSVReader readerCSV = new CSVReader(new StringReader(content))
                    List<String[]> lines = readerCSV.readAll()
                    assert lines.size() == 2
                    assert lines[0] == ["eventID","occurrenceID","type","identifier","format","creator","license","rightsHolder"]
                    assert lines[1] == ["activity1","outputSpecies1","StillImage","https://images-test.ala.org.au/proxyImage?id=image1","image/png","John Doe","CC BY 4.0","John Doe"]
                    // check Media.csv
                    break
                case "Occurrence.csv":
                    // check Occurrence.csv
                    CSVReader readerCSV = new CSVReader(new StringReader(content))
                    List<String[]> lines = readerCSV.readAll()
                    List headers = lines[0]
                    List expectedHeaders = ["eventID","occurrenceID","basisOfRecord","scientificName","occurrenceStatus","individualCount"]
                    List expectedValues = ["activity1","outputSpecies1","HumanObservation","Anura","present","1"]
                    expectedHeaders.eachWithIndex { header, index ->
                        int csvIndex = headers.findIndexOf { it == header }
                        assert lines[1][csvIndex] == expectedValues[index]
                    }
                    assert lines.size() == 2
                    break
                case "MeasurementOrFact.csv":
                    // check MeasurementOrFact.csv
                    CSVReader readerCSV = new CSVReader(new StringReader(content))
                    List<String[]> lines = readerCSV.readAll()
                    assert lines[0] == ["eventID","occurrenceID","measurementValue","measurementAccuracy","measurementUnit","measurementUnitID","measurementType","measurementTypeID", "measurementID"]
                    assert lines[1] == ["activity1","outputSpecies1","1.0","0.001","m","http://qudt.org/vocab/unit/M","distance from source","http://qudt.org/vocab/quantitykind/Number",""]
                    assert lines.size() ==2
                    break
            }
        }

        assert numberOfEntries == 6
    }
}
