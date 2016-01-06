package au.org.ala.ecodata.metadata

/**
 * Created by Temi Varghese on 6/01/16.
 */
class DataModel {
    Map model
    public DataModel(model){
        this.model = model
    }

    public List getNamesforDataType(String type){
        List names = []
        model.dataModel?.each { data ->
            if(data.dataType == type){
                names.push(data.name);
            }
        }
        return names;
    }
}
