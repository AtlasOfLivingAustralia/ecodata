package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.*
import au.org.ala.ecodata.util.ExportTestUtils
import com.mongodb.BasicDBObject
import grails.testing.gorm.DataTest
import grails.util.Holders

/*
 * Copyright (C) 2021 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Created by Temi on 9/11/21.
 */

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.grails.testing.GrailsUnitTest
import grails.test.mongodb.MongoSpec

import java.time.ZoneId

/**
 * Spec for the csProjectXlsExporter
 */
class CSProjectXlsExporterSpec extends MongoSpec implements GrailsUnitTest, DataTest {

    def projectService = Mock(ProjectService)
    def projectActivityService = Mock(ProjectActivityService)
    def siteService = Mock(SiteService)
    def activityService = Mock(ActivityService)
    def recordService = Mock(RecordService)
    def userService = Mock(UserService)
    def metadataService = Mock(MetadataService)
    def reportingService = Mock(ReportingService)
    def activityFormService = Mock(ActivityFormService)
    def permissionService = Mock(PermissionService)
    def documentService = Mock(DocumentService)
    def xlsExporter
    CSProjectXlsExporter csProjectXlsExporter

    File outputFile

    void setup() {
        grailsApplication.config.export.record.max = 1
        Holders.grailsApplication = grailsApplication
        defineBeans {
            projectActivityService(ProjectActivityService)
            projectService(ProjectService)
            siteService(SiteService)
            activityService(ActivityService)
            recordService(RecordService)
            permissionService(PermissionService)
            metadataService(MetadataService)
            userService(UserService)
            reportingService(ReportingService)
            activityFormService(ActivityFormService)
            documentService(DocumentService)
        }
        outputFile = File.createTempFile('test', '.xlsx')
        String name = outputFile.absolutePath
        outputFile.delete() // The exporter will attempt to load the file if it exists, but we want a random file name.
        xlsExporter = new StreamingXlsExporter(name)
        csProjectXlsExporter = new CSProjectXlsExporter( xlsExporter,null, TimeZone.default)
        csProjectXlsExporter.projectActivityService = projectActivityService
        csProjectXlsExporter.projectService = projectService
        csProjectXlsExporter.siteService = siteService
        csProjectXlsExporter.activityService = activityService
        csProjectXlsExporter.recordService = recordService
        csProjectXlsExporter.recordService.grailsApplication = grailsApplication
        csProjectXlsExporter.userService = userService
        csProjectXlsExporter.permissionService = permissionService
        csProjectXlsExporter.activityFormService = activityFormService
        csProjectXlsExporter.documentService = documentService
    }

    void teardown() {
        outputFile.delete()
    }

    def cleanup() {
        Activity.collection.remove(new BasicDBObject())
        Record.collection.remove(new BasicDBObject())
    }

    def "Project activity sheet must not contain a list of sites associated with project activity and records sheet must contain all records"() {
        setup:
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of("UTC")))
        calendar.set(Calendar.YEAR, 2015) //2005-12-31
        calendar.set(Calendar.MONTH, 12)
        calendar.set(Calendar.DAY_OF_MONTH, 31)

        Date startDate = calendar.getTime()
        Date endDate = null

        Map project = project()
        String projectId = project.projectId
        project.sites = []
        Map pa = projectActivity()
        String paId = pa.projectActivityId
        String paName = pa.name
        Set activities = new HashSet<String>()
        activities.add('abc123')
        ActivityForm activityForm = form()
        Activity activity = new Activity(activityId:'abc123', type:'Type 1', description:'Test', progress:Activity.STARTED,
                plannedStartDate:new Date(), plannedEndDate: new Date(), projectActivityId: paId).save()
        Record record = new Record(occurrenceID: 'occurs123', projectId: projectId,  activityId:'abc123', scientificName:'name 1', decimalLatitude:10.0, decimalLongitude: 10.0,
                projectActivityId: paId).save(flush: true)
        projectService.get(projectId) >> project
        projectActivityService.getAllByProject(projectId, ProjectActivityService.ALL) >> [pa]
        projectActivityService.listRestrictedProjectActivityIds(_, _) >> []
        activityService.toMap(_, []) >> [activityId:'abc123', type:'Type 1', description:'Test', progress:Activity.STARTED,
                                                 plannedStartDate:new Date(), plannedEndDate: new Date(), projectActivityId: paId, outputs:[[name: "test", data: ['item1': 1], name: "test", outputId: "abc"]]]
        activityFormService.findActivityForm(pa.pActivityFormName) >> activityForm
        recordService.list(_) >> [total: 1, list: [[occurrenceID: 'occurs123', projectId: projectId,  activityId:'abc123', scientificName:'name 1', decimalLatitude:10.0, decimalLongitude: 10.0,
                                                    projectActivityId: paId]]]

        when:
        csProjectXlsExporter.export(projectId, activities)
        xlsExporter.save()

        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)

        then:
        workbook.numberOfSheets == 4
        Sheet paSheet = workbook.getSheet(paName)
        paSheet.physicalNumberOfRows == 2 //returns 2 rows, header and one data row
        List summaryRow =  ExportTestUtils.readRow(0, paSheet)
        List activityHeaders = summaryRow.subList(0, 7)
        activityHeaders == csProjectXlsExporter.surveyHeaders.subList(0, 7)
        activityHeaders.contains('Site IDs') == false
        Sheet recordsSheet = workbook.getSheet('DwC Records')
        recordsSheet.physicalNumberOfRows == 2
        List recordRow1 =  ExportTestUtils.readRow(1, recordsSheet)
        recordRow1[0] == 'occurs123'
    }

    def "CsProjectXlsExporter.exportDocumentsByProject is creating work sheet"() {
        setup:
        Map documentsByProjects = [
                "abc": new LinkedHashSet<String>(['doc1', 'doc2']),
        ]

        documentService.get('doc1') >> [name: 'doc 1', type: 'image', role: 'primary', projectId: 'abc', documentId: 'doc1', filepath: '/path/to/doc1.jpg']
        documentService.get('doc2') >> [name: 'doc 2', type: 'image', role: 'primary', projectId: 'abc', documentId: 'doc2', filepath: '/path/to/doc1.jpg']
        projectService.get('abc', projectService.BASIC) >> [projectId: 'abc', name: 'Project ABC', organisationName: 'Test Org']
        when:
        csProjectXlsExporter.exportDocumentsByProjects(documentsByProjects)
        xlsExporter.save()
        Workbook workbook =  ExportTestUtils.readWorkbook(outputFile)
        Sheet products = workbook.getSheet("Product")
        List doc1Row =  ExportTestUtils.readRow(1, products)
        List doc2Row =  ExportTestUtils.readRow(2, products)

        then:
        workbook.numberOfSheets == 1
        products.physicalNumberOfRows == 3 //returns 2 rows, header and one data row
        doc1Row[0] == 'abc'
        doc1Row[1] == 'Project ABC'
        doc1Row[2] == 'doc1'
        doc1Row[3] == 'doc 1'
        doc2Row[0] == 'abc'
        doc1Row[1] == 'Project ABC'
        doc2Row[2] == 'doc2'
        doc2Row[3] == 'doc 2'
    }



    private Map project() {
        new groovy.json.JsonSlurper().parseText(projectJson)
    }

    private Map projectActivity() {
        new groovy.json.JsonSlurper().parseText(projectActivityJson)
    }

    private ActivityForm form() {
        new ActivityForm(new groovy.json.JsonSlurper().parseText(formJson))
    }

    private String projectJson = """
                {
                    "bushfireCategories" : [],
                    "origin" : "atlasoflivingaustralia",
                    "dateCreated" : "2021-11-09T03:53:18.671Z",
                    "promoteOnHomepage" : "no",
                    "ecoScienceType" : [],
                    "countries" : [ 
                        "Australia"
                    ],
                    "name" : "ALA sightings Test",
                    "funding" : 0.0,
                    "isCitizenScience" : true,
                    "uNRegions" : [ 
                        "Oceania"
                    ],
                    "industries" : [],
                    "tags" : [ 
                        "noCost"
                    ],
                    "lastUpdated" : "2021-11-09T03:56:44.084Z",
                    "isBushfire" : false,
                    "alaHarvest" : true,
                    "scienceType" : [ 
                        "Biology", 
                        "Animals", 
                        "Birds", 
                        "Ecology"
                    ],
                    "status" : "active",
                    "isMERIT" : false,
                    "isSciStarter" : false,
                    "isExternal" : false,
                    "projectId" : "1d4dc21b-f036-492d-ae73-d8335c763ecd",
                    "organisationId" : "3a04141a-2290-4c54-aee3-a433d60b4476",
                    "aim" : "ALA sightings Test",
                    "projectType" : "survey",
                    "description" : "ALA sightings Test",
                    "associatedOrgs" : [],
                    "fundings" : [],
                    "mapLayersConfig" : {
                        "baseLayers" : [],
                        "overlays" : []
                    },
                    "baseLayer" : "",
                    "organisationName" : "Atlas of Living Australia",
                    "task" : "Nothing",
                    "plannedStartDate" : "2021-11-08T13:00:00.000Z",
                    "projectSiteId" : "137e0d22-5a47-4474-b713-ac62c670029c",
                    "legalCustodianOrganisationType" : "",
                    "regenerateProjectTimeline" : false,
                    "associatedProgram" : "Citizen Science Projects",
                    "facets" : [],
                    "termsOfUseAccepted" : true,
                    "orgGrantee" : "",
                    "legalCustodianOrganisation" : "Happywhale",
                    "orgSponsor" : "",
                    "dataProviderId" : "dp244",
                    "dataResourceId" : "dr17933"
                }

            """
        private String projectActivityJson = """
                {
                    "allowAdditionalSurveySites" : false,
                    "allowPoints" : true,
                    "allowPolygons" : false,
                    "commentsAllowed" : false,
                    "description" : "efdsgad",
                    "name" : "project activity",
                    "pActivityFormName" : "test",
                    "projectActivityId" : "8b5500ff-1bfb-4e88-a4ce-d14c2dde7abd",
                    "projectId" : "1d4dc21b-f036-492d-ae73-d8335c763ecd",
                    "published" : true,
                    "restrictRecordToSites" : true,
                    "sites" : [ {
                        "siteId":"2e2728c0-be67-4435-8ee9-4963cfa975c5",
                        }
                    ],
                    "startDate" : "2005-12-31T13:00:00.000Z",
                    "status" : "active",
                    "version" : 2,
                    "visibility" : {
                        "setDate" : 60,
                        "constraint" : "PUBLIC",
                        "embargoDate" : ""
                    },
                    "surveySiteOption" : "sitepickcreate",
                    "allowLine" : false,
                    "addCreatedSiteToListOfSelectedSites" : false
                }
            """

    private String formJson = """
        {
          "category" : "Assessment & monitoring",
          "dateCreated" : "2019-06-26T08:56:25.616Z",
          "formVersion" : 1,
          "lastUpdated" : "2020-03-15T23:34:37.719Z",
          "name" : "test",
          "publicationStatus" : "published",
          "sections" : [
            {
              "collapsedByDefault" : false,
              "name" : "test",
              "optional" : false,
              "template" : {
                "modelName" : "decimalexample",
                "dataModel": [
                  {
                    "decimalPlaces": 0,
                    "defaultValue": 1,
                    "dataType": "number",
                    "name": "item1"
                  }
                ],
                "viewModel": [
                  {
                    "type": "row",
                    "items": [
                      {
                        "type": "col",
                        "items": [
                          {
                            "preLabel": "Item 1",
                            "source": "item1",
                            "type": "number"
                          }
                        ]
                      }
                    ]
                  }
                ]
              },
              "templateName" : "decimalexamplesurvey"
            }
          ],
          "status" : "active",
          "supportsPhotoPoints" : false,
          "supportsSites" : false,
          "type" : "Assessment"
        }
    """
}
