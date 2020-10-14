package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.gorm.DomainUnitTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Stepwise
import spock.lang.Unroll

import java.text.DateFormat
import java.text.SimpleDateFormat


class ProjectActivitySpec extends MongoSpec implements ServiceUnitTest<ProjectActivityService>, DomainUnitTest<ProjectActivity> {

    /** Insert some project activities into the database to work with */
    def setup() {

        DateFormat format = new SimpleDateFormat('yyyy/MM/dd')
        def methodTypes = ['opportunistic', 'opportunistic', 'systematic', 'systematic', 'systematic', 'systematic']
        def methodNames = ['Opportunistic/ad-hoc observation recording','Opportunistic/ad-hoc observation recording','Bird survey - Fixed-area','Vegetation survey - Intensive inventory','Vegetation survey - Intensive inventory','Vegetation survey - Intensive inventory']
        def spatialAccuracy = ['low', 'moderate', 'high', 'low', 'moderate', 'moderate']
        def speciesIdentification = ['low', 'moderate', 'high', 'na', 'low', 'moderate']
        def temporalAccuracy = ['low', 'moderate', 'high', 'low', 'moderate', 'high']
        def nonTaxonomicAccuracy = ['low', 'moderate', 'high', 'low', 'moderate', 'high']
        def dataQualityAssuranceMethods = [["dataownercurated"], ["subjectexpertverification"], ["crowdsourcedverification"], ["recordannotation"], ["systemsupported"], ["nodqmethodsused"]]
        def dataAccessMethods = [["oasrdfs"], ["oaordfs"], ["lsrds"], ["ordfsvr"], ["oasrdes"], ["casrdes"]]
        def dataSharingLicense = ['CC BY', 'CC BY ZERO', 'CC BY AU', 'CC BY ZERO', 'CC BY', 'CC BY']
        def status = ['active', 'active', 'active', 'active', 'active', 'deleted']
        def startDates = ['2014/02/01', '2014/06/01', '2014/03/01', '2014/08/01', '2015/02/01', '2015/09/13']
        def endDates = ['2014/06/30', '2015/01/01', '2014/04/01', '2015/01/01', '2015/06/01', '2015/09/27']

        for (int i = 0; i < methodTypes.size(); i++) {
            def props = [
                    name                       : 'name ' + i,
                    projectActivityId          : 'projectActivity' + i,
                    projectId                  : 'project' + i,
                    status                     : status[i],
                    methodType                 : methodTypes[i],
                    methodName                 : methodNames[i],
                    description                : 'description ' + i,
                    spatialAccuracy            : spatialAccuracy[i],
                    speciesIdentification      : speciesIdentification[i],
                    temporalAccuracy           : temporalAccuracy[i],
                    nonTaxonomicAccuracy       : nonTaxonomicAccuracy[i],
                    dataQualityAssuranceMethods: dataQualityAssuranceMethods[i],
                    dataAccessMethods          : dataAccessMethods[i],
                    dataSharingLicense         : dataSharingLicense[i],
                    startDate                  : format.parse(startDates[i]),
                    endDate                    : format.parse(endDates[i])
            ]

            createProjectActivity(props)
        }
        ProjectActivity.metaClass.getDbo = {
            delegate.properties
        }
    }

    void cleanup() {
        ProjectActivity.withNewTransaction {
            ProjectActivity.findAll().each {
                it.delete()
            }
        }
        ProjectActivity.metaClass.getDbo = null
    }

    private def createProjectActivity(props) {
        ProjectActivity.withNewTransaction {
            ProjectActivity projectActivity = new ProjectActivity(props)
            projectActivity.save(failOnError: true, flush: true)
        }
    }

    @Unroll
    def "project activities can be searched on different criterias"(criteria, expectedActivityIds) {

        when:
        def results

        ProjectActivity.withNewSession {
            results = service.search(criteria, LevelOfDetail.flat.name())
        }
        results.sort { a1, a2 -> a1.projectActivityId <=> a2.projectActivityId }

        then:
        results.collect { it.projectActivityId } == expectedActivityIds

        where:
        criteria                                                    | expectedActivityIds
        [methodType: 'opportunistic']                               | ['projectActivity0',  'projectActivity1']
        [description: 'description 3']                              | ['projectActivity3']
        [methodType: 'opportunistic', description: 'description 1'] | ['projectActivity1']
        [methodType: 'systematic', description: 'description 0']    | []
    }


    def "when a project activity is deleted, it should not be returned"(criteria, expectedProjectActivityIds) {
        when:
        def results
        ProjectActivity.withNewTransaction {
            results = service.search(criteria, LevelOfDetail.flat.name())
        }
        results.sort { a1, a2 -> a1.projectActivityId <=> a2.projectActivityId }

        then:
        results.collect { it.projectActivityId } == expectedProjectActivityIds

        where:
        criteria                   | expectedProjectActivityIds
        [methodType: 'systematic'] | ['projectActivity2', 'projectActivity3', 'projectActivity4']
        [:]                        | ['projectActivity0', 'projectActivity1', 'projectActivity2', 'projectActivity3', 'projectActivity4']
    }
}
