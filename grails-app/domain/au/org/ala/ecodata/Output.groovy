package au.org.ala.ecodata

import au.org.ala.ecodata.graphql.models.KeyValue
import au.org.ala.ecodata.graphql.models.OutputData
import org.bson.types.ObjectId

class Output {

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
    List tempArgs = []
    Map data

    static constraints = {
        assessmentDate nullable: true
        name nullable: true
    }

    static transients = ['tempArgs']

    Map getData() {
       this.data
    }

    OutputData getData(List fields) {
        OutputData outputData = new OutputData(dataList: new ArrayList<KeyValue>())
        if(this.data) {
            this.data.each() {
                //if no fields, all the fields will be returned
                //otherwise, only requested fields will be returned
                if(!fields || (fields && fields.contains(it.key))) {
                    outputData.dataList.add(new KeyValue(key: it.key, value: it.value))
                }
            }
        }
         return outputData
    }
}
