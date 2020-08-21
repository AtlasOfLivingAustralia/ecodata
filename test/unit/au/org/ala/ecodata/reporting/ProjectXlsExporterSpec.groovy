package au.org.ala.ecodata.reporting

import au.org.ala.ecodata.MetadataService
import au.org.ala.ecodata.ProjectService
import au.org.ala.ecodata.ReportingService
import au.org.ala.ecodata.UserService
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellReference
import org.grails.plugins.excelimport.ExcelImportService
import spock.lang.Specification

/**
 * Spec for the ProjectXlsExporter
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([MetadataService, UserService, ReportingService])
class ProjectXlsExporterSpec extends Specification {

    def projectService = Mock(ProjectService)
    def xlsExporter
    ProjectXlsExporter projectXlsExporter
    ExcelImportService excelImportService
    File outputFile

    void setup() {
        outputFile = new File('test.xlsx')
        outputFile.deleteOnExit()
        xlsExporter = new XlsExporter(outputFile.name)

        excelImportService = new ExcelImportService()
    }

    void teardown() {
        outputFile.delete()
    }

    void "project details can be exported"() {
        setup:
        String sheet = 'Projects'
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter, [sheet], [], [:])
        projectXlsExporter.metadataService = Mock(MetadataService)

        when:
        projectXlsExporter.export([projectId:'1234', workOrderId:'work order 1', contractStartDate:'2019-06-30T14:00:00Z', contractEndDate:'2022-06-30T14:00:00Z', funding:1000])
        xlsExporter.save()

        then:
        List<Map> results = readSheet(sheet, projectXlsExporter.projectHeaders)
        results.size() == 1
        results[0]['Project ID'] == '1234'
        results[0]['Internal order number'] == 'work order 1'
        results[0]['Contracted Start Date'] == '2019-06-30T14:00:00Z'
        results[0]['Contracted End Date'] == '2022-06-30T14:00:00Z'
        results[0]['Funding'] == 1000

    }


    void "RLP Merit Baseline exported to XSLS"() {
        setup:
        String sheet = 'MERI_Baseline'
        List<String> properties = ['Baseline Method','Baseline']
        Map project = new groovy.json.JsonSlurper().parseText(projectJson)
        projectXlsExporter = new ProjectXlsExporter(projectService, xlsExporter)
        projectXlsExporter.metadataService = Mock(MetadataService)

        when:
        projectXlsExporter.export(project)
        xlsExporter.save()

        then:
        outputFile.withInputStream {fileIn ->
            Workbook workbook = WorkbookFactory.create(fileIn)
            Sheet testSheet = workbook.getSheet(sheet)
            testSheet.physicalNumberOfRows == 3

            Cell baselineCell = testSheet.getRow(0).find{it.stringCellValue == 'Baseline'}
            baselineCell != null
            testSheet.getRow(1).getCell(baselineCell.getColumnIndex()).stringCellValue == 'Test'

        }

    }


    private List readSheet(String sheet, List properties) {
        def columnMap = [:]
        properties.eachWithIndex { prop, index ->
            def colString = CellReference.convertNumToColString(index)
            columnMap[colString] = prop
        }
        def config = [
                sheet:sheet,
                startRow:1,
                columnMap:columnMap
        ]
        outputFile.withInputStream {fileIn ->
            Workbook workbook = WorkbookFactory.create(fileIn)
            excelImportService.convertColumnMapConfigManyRows(workbook, config)
        }

    }

    private String projectJson = "{\n" +
            "    \"alaHarvest\" : false,\n" +
            "    \"associatedProgram\" : \"\",\n" +
            "    \"associatedSubProgram\" : \"\",\n" +
            "    \"countries\" : [],\n" +
            "    \"custom\" : {\n" +
            "        \"details\" : {\n" +
            "            \"partnership\" : {\n" +
            "                \"description\" : \"\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"data3\" : \"\",\n" +
            "                        \"data2\" : \"\",\n" +
            "                        \"data1\" : \"\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"projectEvaluationApproach\" : \"Test\",\n" +
            "            \"implementation\" : {\n" +
            "                \"description\" : \"Test methodology\"\n" +
            "            },\n" +
            "            \"obligations\" : \"\",\n" +
            "            \"policies\" : \"\",\n" +
            "            \"description\" : \"TBA - this is a temporary description\",\n" +
            "            \"baseline\" : {\n" +
            "                \"description\" : \"This is a baseline\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"method\" : \"Test\",\n" +
            "                        \"baseline\" : \"Test\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"method\" : \"Test1\",\n" +
            "                        \"baseline\" : \"Test2\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"rationale\" : \"Test rational\",\n" +
            "            \"caseStudy\" : true,\n" +
            "            \"lastUpdated\" : \"2019-06-06T06:07:27Z\",\n" +
            "            \"priorities\" : {\n" +
            "                \"description\" : \"\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"data3\" : \"Test\",\n" +
            "                        \"data2\" : \"Test\",\n" +
            "                        \"data1\" : \"Test\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"serviceIds\" : [ \n" +
            "                1, \n" +
            "                2, \n" +
            "                3, \n" +
            "                4, \n" +
            "                5, \n" +
            "                34, \n" +
            "                6, \n" +
            "                7, \n" +
            "                8, \n" +
            "                10, \n" +
            "                9, \n" +
            "                11, \n" +
            "                12, \n" +
            "                13, \n" +
            "                14, \n" +
            "                15, \n" +
            "                16, \n" +
            "                17, \n" +
            "                18, \n" +
            "                19, \n" +
            "                20, \n" +
            "                21, \n" +
            "                22, \n" +
            "                23, \n" +
            "                24, \n" +
            "                25, \n" +
            "                26, \n" +
            "                27, \n" +
            "                28, \n" +
            "                35, \n" +
            "                29, \n" +
            "                30, \n" +
            "                31, \n" +
            "                32, \n" +
            "                33\n" +
            "            ],\n" +
            "            \"outcomes\" : {\n" +
            "                \"secondaryOutcomes\" : [ \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Soil carbon\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation.\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Natural Temperate Grassland of the South Eastern Highlands\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"By 2023, the implementation of priority actions is leading to an improvement in the condition of EPBC Act listed Threatened Ecological Communities.\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"shortTermOutcomes\" : [ \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Asset 1\", \n" +
            "                            \"Assert2\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"Test\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Asset3\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"Test 2\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"assets\" : [],\n" +
            "                        \"description\" : \"sfasdf\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"midTermOutcomes\" : [ \n" +
            "                    {\n" +
            "                        \"assets\" : [ \n" +
            "                            \"Asset 1\", \n" +
            "                            \"Assert2\"\n" +
            "                        ],\n" +
            "                        \"description\" : \"Test\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"assets\" : [],\n" +
            "                        \"description\" : \"\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"primaryOutcome\" : {\n" +
            "                \"primaryOutcome\" : {\n" +
            "                    \"assets\" : [ \n" +
            "                        \"Climate change adaptation\", \n" +
            "                        \"Market traceability\"\n" +
            "                    ],\n" +
            "                    \"description\" : \"By 2023, there is an increase in the capacity of agriculture systems to adapt to significant changes in climate and market demands for information on provenance and sustainable production.\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"keq\" : {\n" +
            "                \"description\" : \"\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"data3\" : \"\",\n" +
            "                        \"data2\" : \"Test\",\n" +
            "                        \"data1\" : \"*** This is a monitoring indictor ** \"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"threats\" : {\n" +
            "                \"description\" : \"\",\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"threat\" : \"Test this is another EDIT\",\n" +
            "                        \"intervention\" : \"Test\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"objectives\" : {\n" +
            "                \"rows1\" : [ \n" +
            "                    {\n" +
            "                        \"assets\" : [],\n" +
            "                        \"description\" : \"\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"data3\" : \"\",\n" +
            "                        \"data2\" : \"\",\n" +
            "                        \"data1\" : \"\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"events\" : [ \n" +
            "                {\n" +
            "                    \"funding\" : \"0\",\n" +
            "                    \"name\" : \"\",\n" +
            "                    \"description\" : \"\",\n" +
            "                    \"scheduledDate\" : \"\",\n" +
            "                    \"media\" : \"\",\n" +
            "                    \"grantAnnouncementDate\" : \"\",\n" +
            "                    \"type\" : \"\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"status\" : \"active\",\n" +
            "            \"budget\" : {\n" +
            "                \"overallTotal\" : 0,\n" +
            "                \"headers\" : [ \n" +
            "                    {\n" +
            "                        \"data\" : \"2017/2018\"\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"data\" : \"2018/2019\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"rows\" : [ \n" +
            "                    {\n" +
            "                        \"costs\" : [ \n" +
            "                            {\n" +
            "                                \"dollar\" : \"0\"\n" +
            "                            }, \n" +
            "                            {\n" +
            "                                \"dollar\" : \"0\"\n" +
            "                            }\n" +
            "                        ],\n" +
            "                        \"rowTotal\" : 0,\n" +
            "                        \"description\" : \"\",\n" +
            "                        \"shortLabel\" : \"\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"columnTotal\" : [ \n" +
            "                    {\n" +
            "                        \"data\" : 0\n" +
            "                    }, \n" +
            "                    {\n" +
            "                        \"data\" : 0\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        }\n" +
            "    },\n" +
            "    \"dateCreated\" : \"2018-06-14T04:22:13.057Z\",\n" +
            "    \"description\" : \"TBA - this is a temporary description\",\n" +
            "    \"ecoScienceType\" : [],\n" +
            "    \"externalId\" : \"\",\n" +
            "    \"fundingSource\" : \"RLP\",\n" +
            "    \"funding\" : 10000,\n" +
            "    \"grantId\" : \"RLP-Test-Program-Project-1\",\n" +
            "    \"industries\" : [],\n" +
            "    \"bushfireCategories\" : [],\n" +
            "    \"isCitizenScience\" : false,\n" +
            "    \"isExternal\" : false,\n" +
            "    \"isMERIT\" : true,\n" +
            "    \"isSciStarter\" : false,\n" +
            "    \"lastUpdated\" : \"2019-08-13T05:17:48.686Z\",\n" +
            "    \"manager\" : \"\",\n" +
            "    \"name\" : \"Test Program - Project 1\",\n" +
            "    \"orgIdSvcProvider\" : \"\",\n" +
            "    \"organisationId\" : \"\",\n" +
            "    \"organisationName\" : \"Test Org\",\n" +
            "    \"origin\" : \"merit\",\n" +
            "    \"outputTargets\" : [ \n" +
            "        {\n" +
            "            \"scoreId\" : \"0df7c177-2864-4a25-b420-2cf3c45ce749\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : \"2\"\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : \"2\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"2\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"69deaaf9-cdc2-439a-b684-4cffdc7f224e\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : \"1\"\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : \"4\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"4\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"26a8213e-1770-4dc4-8f99-7e6302197504\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : \"1\"\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : \"1\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"2\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"c464b652-be5e-4658-b62f-02bf1a80bcf8\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : \"1\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"50\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"3cbf653f-f74c-4066-81d2-e3f78268185c\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"300\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"0f9ef068-b2f9-4e6f-9ab5-521857b036f4\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"300\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"e48faf01-72eb-479c-be9b-d2d71d254fa4\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"482bdf4e-6f7a-4bdf-80d5-d619ac7cdf50\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"8025b157-44d7-4283-bc1c-f40fb9b99501\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"600\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"a3afea6e-711c-4ef2-bb20-6d2630b7ee93\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"12\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"757d6c9e-ec24-486f-a128-acc9bfb87830\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"600\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"b7c067e3-6ae7-4e76-809a-312165b75f94\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"60\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"d1c10295-05e5-4265-a5f1-8a5683af2efe\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"2\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"011a161f-7275-4b5e-986e-3fe4640d0265\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"500\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"c2dc6f91-ccb1-412e-99d0-a842a4ac4b03\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"def4e2af-dcad-4a15-8336-3765e6671f08\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"c46842b6-d7b6-4917-b56f-f1b0594663fa\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"199\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"2d877a91-6312-4c44-9ae1-2494ea3e43db\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"4\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"d4ba13a1-00c8-4e7f-8463-36b6ea37eee6\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"2\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"4bcab901-879a-402d-83f3-01528c6c86a5\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"1\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"45994b98-21f1-4927-a03e-3d940ac75116\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"100\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"6eaa061c-b77b-4440-8e8f-7ebaa2ff6207\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"1000\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"0e887410-a3c5-49ca-a6f5-0f2f6fae30db\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"200\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"725d9365-0889-4355-8a7f-a21ef260c468\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"450\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"0f11a699-6063-4e91-96ca-53e45cf26b80\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"900\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"3c2c4aaa-fd5f-43d8-a72f-3567e6dea6f4\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"50\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"ed30b80b-7bb9-4c04-9949-093df64d124c\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"5000\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"4cbcb2b5-45cd-42dc-96bf-a9a181a4865b\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"3090\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"f38fbd9e-d208-4750-96ce-3c032ad37684\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"500\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"dea1ff8b-f4eb-4987-8073-500bbbf97fcd\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"500\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"4f747371-fa5f-4200-ae37-6cd59d268fe8\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"4\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"685d61e9-2ebd-4198-a83a-ac7a2fc1477a\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"4\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"91387f2b-258d-4325-aa60-828d1acf6ac6\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"3\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"ba3d0a20-1e4d-404a-9907-b95239499c2f\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }, \n" +
            "        {\n" +
            "            \"scoreId\" : \"28dd9736-b66a-4ab4-9111-504d5cffba88\",\n" +
            "            \"periodTargets\" : [ \n" +
            "                {\n" +
            "                    \"period\" : \"2017/2018\",\n" +
            "                    \"target\" : 0\n" +
            "                }, \n" +
            "                {\n" +
            "                    \"period\" : \"2018/2019\",\n" +
            "                    \"target\" : 0\n" +
            "                }\n" +
            "            ],\n" +
            "            \"target\" : \"400\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"planStatus\" : \"not approved\",\n" +
            "    \"plannedEndDate\" : \"2019-06-29T14:00:00.000Z\",\n" +
            "    \"plannedStartDate\" : \"2017-08-01T14:00:00.000Z\",\n" +
            "    \"programId\" : \"test_program\",\n" +
            "    \"projectId\" : \"8693cbc5-6947-4614-9bd1-b22ef44bc8fd\",\n" +
            "    \"projectType\" : \"works\",\n" +
            "    \"promoteOnHomepage\" : \"no\",\n" +
            "    \"risks\" : {\n" +
            "        \"overallRisk\" : \"Low\",\n" +
            "        \"rows\" : [ \n" +
            "            {\n" +
            "                \"consequence\" : \"Minor\",\n" +
            "                \"likelihood\" : \"Unlikely\",\n" +
            "                \"residualRisk\" : \"Low\",\n" +
            "                \"currentControl\" : \"Test\",\n" +
            "                \"description\" : \"Low\",\n" +
            "                \"threat\" : \"Work Health and Safety\",\n" +
            "                \"riskRating\" : \"Low\"\n" +
            "            }, \n" +
            "            {\n" +
            "                \"consequence\" : \"Moderate\",\n" +
            "                \"likelihood\" : \"Unlikely\",\n" +
            "                \"residualRisk\" : \"High\",\n" +
            "                \"currentControl\" : \"Test\",\n" +
            "                \"description\" : \"Test 2\",\n" +
            "                \"threat\" : \"Performance\",\n" +
            "                \"riskRating\" : \"Low\"\n" +
            "            }, \n" +
            "            {\n" +
            "                \"consequence\" : \"Minor\",\n" +
            "                \"likelihood\" : \"Possible\",\n" +
            "                \"residualRisk\" : \"Medium\",\n" +
            "                \"currentControl\" : \"yep\",\n" +
            "                \"description\" : \"lalala\",\n" +
            "                \"threat\" : \"People resources\",\n" +
            "                \"riskRating\" : \"Low\"\n" +
            "            }, \n" +
            "            {\n" +
            "                \"consequence\" : \"High\",\n" +
            "                \"likelihood\" : \"Possible\",\n" +
            "                \"residualRisk\" : \"Medium\",\n" +
            "                \"currentControl\" : \"yrd\",\n" +
            "                \"description\" : \"\$\",\n" +
            "                \"threat\" : \"Financial\",\n" +
            "                \"riskRating\" : \"Medium\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"status\" : \"\"\n" +
            "    },\n" +
            "    \"scienceType\" : [],\n" +
            "    \"serviceProviderName\" : \"\",\n" +
            "    \"status\" : \"active\",\n" +
            "    \"tags\" : [],\n" +
            "    \"uNRegions\" : [],\n" +
            "    \"workOrderId\" : \"1234565\",\n" +
            "    \"blog\" : []\n" +
            "}"
}
