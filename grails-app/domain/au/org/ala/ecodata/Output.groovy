package au.org.ala.ecodata

import au.org.ala.ecodata.graphql.mappers.OutputGraphQLMapper
import au.org.ala.ecodata.graphql.models.OutputData
import au.org.ala.ecodata.graphql.models.KeyValue
import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.bson.types.ObjectId
import org.grails.gorm.graphql.entity.dsl.GraphQLMapping

class Output {

    static graphql = OutputGraphQLMapper.graphqlMapping()

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

    OutputData getData(def data) {
        OutputData outputData = new OutputData(dataList: new ArrayList<KeyValue>())
        if(data) {
            data.each() {
                outputData.dataList.add(new KeyValue(key: it.key, value: it.value))
            }
        }
         return outputData
    }
}
