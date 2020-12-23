package au.org.ala.ecodata

import grails.converters.JSON
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.grails.web.converters.marshaller.json.CollectionMarshaller
import org.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification
/**
 * Tests the CollectoryService
 */
class CollectoryServiceSpec extends Specification implements ServiceUnitTest<CollectoryService> {

    String collectoryBaseUrl = ''
    String meritDataProvider = 'drMerit'
    String biocollectDataProvider = 'drBiocollect'
    WebService webServiceMock = Mock(WebService)
    ProjectService projectService = Mock(ProjectService)


    String expectedConnectionJson = '{"protocol":"DwC","url":"sftp://upload.ala.org.au:biocollect/dr1234","automation":false,"csv_delimiter":",","csv_eol":"\\n","csv_escape_char":"\\\\","csv_text_enclosure":"\\"","termsForUniqueKey":["occurrenceID"],"strip":false,"incremental":false}'

    void setup() {

        service.webService = webServiceMock
        service.projectService =  projectService
        grailsApplication.config.collectory = [baseURL:collectoryBaseUrl, dataProviderUid:[merit:meritDataProvider, biocollect:biocollectDataProvider]]
        service.grailsApplication = grailsApplication

        JSON.registerObjectMarshaller(new MapMarshaller())
        JSON.registerObjectMarshaller(new CollectionMarshaller())

    }

    void "new organisations will be registered with the collectory"() {
        setup:
        Map organisation = [name:'org1', orgType:'other', description: 'org1 description', url:'http://www.org1.org', acronym: 'o1']
        Map expected =  ['institutionType':organisation.orgType, name:organisation.name, pubDescription: organisation.description, websiteUrl:organisation.url, acronym:organisation.acronym]
        Map actual = null

        when:
        service.createInstitution(organisation)

        then:

        1 * webServiceMock.doPost(collectoryBaseUrl+'ws/institution', _) >> {args -> actual = args[1]}
        actual == expected
        1 * webServiceMock.extractIdFromLocationHeader(_) >> ''
        0 * _

    }


    void "new projects will be registered with the collectory"() {
        setup:
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, alaHarvest: true]
        String dataResourceId = 'dr1234'


        when:
        service.createDataResource(projectData)

        then:
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource", [name:projectData.name, pubDescription:projectData.description, 'dataProvider':['uid':biocollectDataProvider], hiddenJSON:[isMERIT:false, alaHarvest: true]]) >> [:]
        1 * webServiceMock.extractIdFromLocationHeader(_) >> dataResourceId

        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, [connectionParameters:expectedConnectionJson]) >> [:]
        0 * webServiceMock.doPost(_, _)

    }

    void "new project with harvest disabled will create data resource"() {
        setup:
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, alaHarvest: false]
        String dataResourceId = 'dr1234'


        when:
        Map result = service.createDataResource(projectData)

        then:
        result.size() == 2
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource", [name:projectData.name, pubDescription:projectData.description, 'dataProvider':['uid':biocollectDataProvider], hiddenJSON:[isMERIT:false, alaHarvest: false]]) >> [:]
        1 * webServiceMock.extractIdFromLocationHeader(_) >> dataResourceId
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, [connectionParameters:expectedConnectionJson]) >> [:]
        0 * webServiceMock.doPost(_, _)

    }

    void "updates will be treated as creates if the project does not have a dataResourceId"() {
        setup:
        String dataResourceId = 'dr1234'
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, alaHarvest: true]

        when:
        service.updateDataResource(projectData)

        then:
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource", [name:projectData.name, pubDescription:projectData.description, 'dataProvider':['uid':biocollectDataProvider], hiddenJSON:[isMERIT:false, alaHarvest: true]]) >> [:]
        1 * webServiceMock.extractIdFromLocationHeader(_) >> dataResourceId

        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, [connectionParameters:expectedConnectionJson]) >> [:]
        0 * webServiceMock.doPost(_, _)
    }

    void "updates can be performed when a project is edited"() {
        setup:
        String dataResourceId = 'dr1234'
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, dataResourceId:dataResourceId, alaHarvest: true]

        when:
        service.updateDataResource(projectData)

        then:
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId,  [name:projectData.name, pubDescription:projectData.description, hiddenJSON:[isMERIT:false, alaHarvest: true, dataResourceId: dataResourceId]]) >> [:]
        0 * webServiceMock.doPost(_, _)

    }

    void "MERIT projects will use the MERIT data provider"() {
        setup:
        Map projectData = [name:'project 1', description:'test 123', isMERIT:true, alaHarvest: true]
        String dataResourceId = 'dr1234'


        when:
        service.createDataResource(projectData)

        then:
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource", [name:projectData.name, pubDescription:projectData.description, 'dataProvider':['uid':meritDataProvider], hiddenJSON:[isMERIT:true, alaHarvest: true]]) >> [:]
        1 * webServiceMock.extractIdFromLocationHeader(_) >> dataResourceId

        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, _) >> [:]
        0 * webServiceMock.doPost(_, _)
    }

    void "updates won't be sent if only hiddenJSON data has changed"() {
        setup:
        String dataResourceId = 'dr1234'
        String projectId = '1234'
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, dataResourceId:dataResourceId, alaHarvest: true]

        when:
        service.updateDataResource(projectData, [test:'value'])

        then:
        0 * webServiceMock.doPost(_, _)

    }

    void "if previously harvested project is disabled, its dataResourceId should be cleared"() {
        setup:
        String dataResourceId = 'dr1234'
        String projectId = '1234'
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, dataResourceId:dataResourceId, projectId: projectId, alaHarvest: false]

        when:
        service.updateDataResource(projectData, [alaHarvest: false])

        then:
        0 * webServiceMock.doPost(_, _)
        1 * projectService.update(['dataResourceId':null, 'dataProviderId':null], projectId, false)
    }


    void "the institution id will be mapped if an associated organisation has one"() {
        setup:
        String dataResourceId = 'dr1234'
        String institutionId = 'dr2345'
        Organisation.metaClass.static.findByOrganisationIdAndStatusNotEqual = {String orgId, String status -> [collectoryInstitutionId:institutionId]}

        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, dataResourceId:dataResourceId, organisationId:'1234', alaHarvest: true]

        when:
        service.updateDataResource(projectData)

        then:

        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId,  [name:projectData.name, pubDescription:projectData.description, institution:[uid:institutionId], hiddenJSON:[isMERIT:false, alaHarvest: true, dataResourceId: dataResourceId, organisationId: '1234']]) >> [:]
        0 * webServiceMock.doPost(_, _)
    }

}
