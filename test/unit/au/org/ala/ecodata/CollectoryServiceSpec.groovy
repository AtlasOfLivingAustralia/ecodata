package au.org.ala.ecodata

import grails.converters.JSON
import grails.test.mixin.TestFor
import org.codehaus.groovy.grails.commons.InstanceFactoryBean
import org.codehaus.groovy.grails.web.converters.marshaller.json.CollectionMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.json.MapMarshaller
import spock.lang.Specification

/**
 * Tests the CollectoryService
 */
@TestFor(CollectoryService)
class CollectoryServiceSpec extends Specification {

    String collectoryBaseUrl = ''
    String meritDataProvider = 'drMerit'
    String biocollectDataProvider = 'drBiocollect'
    WebService webServiceMock

    String expectedConnectionJson = '{"protocol":"DwC","url":"sftp://upload.ala.org.au:biocollect/dr1234","automation":false,"csv_delimiter":",","csv_eol":"\\n","csv_escape_char":"\\\\","csv_text_enclosure":"\\"","termsForUniqueKey":["occurrenceID"],"strip":false,"incremental":false}'

    void setup() {
        webServiceMock = Mock(WebService)
        service.webService = webServiceMock
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
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false]
        String dataResourceId = 'dr1234'


        when:
        service.createDataResource(projectData)

        then:
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource", [name:projectData.name, pubDescription:projectData.description, 'dataProvider':['uid':biocollectDataProvider], hiddenJSON:[isMERIT:false]]) >> [:]
        1 * webServiceMock.extractIdFromLocationHeader(_) >> dataResourceId

        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, [connectionParameters:expectedConnectionJson]) >> [:]
        0 * webServiceMock.doPost(_, _)

    }

    void "updates will be treated as creates if the project does not have a dataResourceId"() {
        setup:
        String dataResourceId = 'dr1234'
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false]

        when:
        service.updateDataResource(projectData)

        then:
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource", [name:projectData.name, pubDescription:projectData.description, 'dataProvider':['uid':biocollectDataProvider], hiddenJSON:[isMERIT:false]]) >> [:]
        1 * webServiceMock.extractIdFromLocationHeader(_) >> dataResourceId

        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, [connectionParameters:expectedConnectionJson]) >> [:]
        0 * webServiceMock.doPost(_, _)
    }

    void "updates can be performed when a project is edited"() {
        setup:
        String dataResourceId = 'dr1234'
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, dataResourceId:dataResourceId]

        when:
        service.updateDataResource(projectData)

        then:
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId,  [name:projectData.name, pubDescription:projectData.description, hiddenJSON:[isMERIT:false, dataResourceId: dataResourceId]]) >> [:]
        0 * webServiceMock.doPost(_, _)

    }

    void "MERIT projects will use the MERIT data provider"() {
        setup:
        Map projectData = [name:'project 1', description:'test 123', isMERIT:true]
        String dataResourceId = 'dr1234'


        when:
        service.createDataResource(projectData)

        then:
        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource", [name:projectData.name, pubDescription:projectData.description, 'dataProvider':['uid':meritDataProvider], hiddenJSON:[isMERIT:true]]) >> [:]
        1 * webServiceMock.extractIdFromLocationHeader(_) >> dataResourceId

        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId, _) >> [:]
        0 * webServiceMock.doPost(_, _)
    }

    void "updates won't be sent if only hiddenJSON data has changed"() {
        setup:
        String dataResourceId = 'dr1234'
        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, dataResourceId:dataResourceId]

        when:
        service.updateDataResource(projectData, [test:'value'])

        then:
        0 * webServiceMock.doPost(_, _)

    }

    void "the institution id will be mapped if an associated organisation has one"() {
        setup:
        String dataResourceId = 'dr1234'
        String institutionId = 'dr2345'
        Organisation.metaClass.static.findByOrganisationIdAndStatusNotEqual = {String orgId, String status -> [collectoryInstitutionId:institutionId]}

        Map projectData = [name:'project 1', description:'test 123', isMERIT:false, dataResourceId:dataResourceId, organisationId:'1234']

        when:
        service.updateDataResource(projectData)

        then:

        1 * webServiceMock.doPost(collectoryBaseUrl+"ws/dataResource/"+dataResourceId,  [name:projectData.name, pubDescription:projectData.description, institution:[uid:institutionId], hiddenJSON:[isMERIT:false, dataResourceId: dataResourceId, organisationId: '1234']]) >> [:]
        0 * webServiceMock.doPost(_, _)
    }

}
