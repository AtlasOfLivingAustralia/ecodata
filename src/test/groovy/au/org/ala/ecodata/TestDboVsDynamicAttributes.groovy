package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import org.grails.datastore.mapping.core.Session
import spock.lang.Ignore

/**
 * This test is not designed to be run as a part of the regular suite - instead it is a one off
 * that is attempting to find and document dynamic attributes in the database and allow for a safe
 * refactoring to remove use of Entity.getDbo() from the codebase.
 */
@Ignore
class TestDboVsDynamicAttributesSpec extends MongoSpec {

    void "test projects"() {
        expect:
        true
        Project.withSession { Session session ->

            Closure specialConditions = { String key, Map groovy, Map dbo ->
                false
            }
            checkProperties(Project.findAll(), session, specialConditions)
        }
    }

//    void "test comments"() {
//        expect:
//        Comment.withSession { Session session ->
//
//            Closure specialConditions = { String key, Map groovy, Map dbo ->
//               key == 'parentId' || key == 'parent' || key == 'children'
//            }
//            checkProperties(Comment.findAll(), session, specialConditions)
//        }
//    }

//    void "test management units"() {
//        expect:
//        ManagementUnit.withSession { Session session ->
//
//            Closure specialConditions = { String key, Map groovy, Map dbo ->
//               key == 'reportConfig' || key == 'associatedOrganisations' || key == 'geographicInfo'
//            }
//            checkProperties(ManagementUnit.findAll(), session, specialConditions)
//        }
//    }
//    void "test programs"() {
//        expect:
//        Program.withSession { Session session ->
//
//            Closure specialConditions = { String key, Map groovy, Map dbo ->
//               key == 'inhertitedConfig' || key == 'parentId' || key == 'subPrograms'
//            }
//            checkProperties(Program.findAll(), session, specialConditions)
//        }
//    }
//    void "test sites"() {
//        expect:
//        Site.withSession { Session session ->
//
//            Closure specialConditions = { String key, Map groovy, Map dbo ->
//                key == 'isSciStarter' || key == 'geoPoint' || key == 'compoundSite' || key == 'geometryType' ||
//                key == 'associations' // getAssociations looks like it doesn't do anything
//            }
//            checkProperties(Site.findAll(), session, specialConditions)
//        }
//    }

//    void "test outputs"() {
//        expect:
//        Output.withSession { Session session ->
//
//            Closure specialConditions = { String key, Map groovy, Map dbo ->
//                key == "tempArgs"
//            }
//            checkProperties(Output.findAll(), session, specialConditions)
//        }
//    }

//    void testActivities() {
//
//        expect:
//        // Can't use a stateless session as the dbo doesn't work
//        Activity.withSession { Session session ->
//            Set dynamicAttributes = new HashSet()
//            Closure specialConditions = { String key, Map groovy, Map dbo ->
//                key == "complete" || key == "assessment" ||
//                        (key == 'progress' && groovy.type == 'OzAtlas Sightings')
//            }
//            checkProperties(Activity.findAll(), session, specialConditions)
//        }
//    }




    private Set checkProperties(List entities, Session session, Closure specialConditions) {
        int count = 0
        Set dynamicAttributes = new HashSet()
        entities.each {
            Map dbo = GormMongoUtil.extractPropertiesViaDbo(it)

            Map groovyPlusDynamic = [:]
            Map activityProps = it.properties
            groovyPlusDynamic.putAll(activityProps)
            groovyPlusDynamic.putAll(it.attributes())
            dynamicAttributes.addAll(it.attributes().keySet())

            dbo.remove('_id')
            dbo.remove('version')
            groovyPlusDynamic.remove("dbo")

            if (dbo.keySet() != groovyPlusDynamic.keySet()) {
                dbo.keySet().minus(groovyPlusDynamic.keySet()).each {
                    assert dbo[it] == null
                }
                groovyPlusDynamic.keySet().minus(dbo.keySet()).each {
                    assert specialConditions(it, groovyPlusDynamic, dbo) ||
                            !groovyPlusDynamic[it] // Accounts for empty collections & null values
                }

            }
            if (dbo != groovyPlusDynamic) {
                dbo.keySet().each {
                    assert it == "_id" || specialConditions(it, groovyPlusDynamic, dbo) || dbo[it] == groovyPlusDynamic[it]
                }

            }
            count++
            if (count % 1000 == 0) {
                println "Processed $count activities"
                session.clear() // SO we don't run out of memory

            }
        }
        println dynamicAttributes
        dynamicAttributes
    }

}

