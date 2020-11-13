package au.org.ala.ecodata

import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.bson.types.ObjectId
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class Output {

    static graphql = GraphQLMapping.lazy {
        // Disable default operations, including get as we only want to expose UUIDs in the API not internal ones
        operations.get.enabled false
        operations.list.enabled true
        operations.count.enabled false
        operations.create.enabled false
        operations.update.enabled false
        operations.delete.enabled false


//        add('data', Map) {
//            dataFetcher { Output output ->
//                Activity activity = Activity.findByActivityId(output.activityId)
//                ActivityForm form = Holders.grailsApplication.mainContext.activityFormService.findActivityForm(activity.type, activity.formVersion)
//                form.sections.each { FormSection section ->
//                    section.template.dataModel.each {
//
//                    }
//                }
//            }
//            input false
//        }
    }

    /*
    Associations:
        outputs must belong to 1 Activity - this is mapped by the activityId in this domain
    */

    static mapWith="mongo"

    static mapping = {
        activityId index: true
        outputId index: true
        version false
    }

    ObjectId id
    String outputId
    String status = 'active'
    String activityId
    Date assessmentDate
    String name
    Date dateCreated
    Date lastUpdated

    static constraints = {
        assessmentDate nullable: true
        name nullable: true
    }
}
