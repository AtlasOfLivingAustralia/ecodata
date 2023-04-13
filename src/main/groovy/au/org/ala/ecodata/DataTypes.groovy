package au.org.ala.ecodata

class DataTypes {
    final static String TEXT = "text"
    final static String STRINGLIST = "stringList"
    final static String NUMBER = "number"
    final static String DATE = "date"
    final static String TIME = "time"
    final static String SET = "set"
    final static String LIST = "list"
    final static String SPECIES = "species"
    final static String IMAGE ="image"
    final static String BOOLEAN = "boolean"
    final static String DOCUMENT = "document"
    final static String FEATURE = "feature"
    final static String GEOMAP = "geoMap"
    final static String PHOTOPOINTS = "photoPoints"

    static List getDataTypesWithDataAsStringList() {
        [STRINGLIST, SET]
    }

    static List getDataTypesWithDataAsList(){
        [LIST, IMAGE, PHOTOPOINTS]
    }

    static List getDataTypesWithDataAsMap(){
        [GEOMAP, FEATURE, DOCUMENT, SPECIES]
    }

    static List getDataTypesWithDataAsPrimitiveType() {
        [BOOLEAN, TIME, DATE, NUMBER, TEXT]
    }

    static List getModelsWithPrimitiveData(List models) {
        models.findAll { model ->
            getDataTypesWithDataAsPrimitiveType().contains(model.dataType)
        }
    }

    static List getModelsWithMapData(List models) {
        models.findAll { model ->
            getDataTypesWithDataAsMap().contains(model.dataType)
        }
    }

    static List getModelsWithListData(List models) {
        models.findAll { model ->
            getDataTypesWithDataAsList().contains(model.dataType)
        }
    }

    static List getModelsWithStringListData(List models) {
        models.findAll { model ->
            getDataTypesWithDataAsStringList().contains(model.dataType)
        }
    }

    static List getSpeciesModels(List models) {
        models.findAll { model ->
            [SPECIES].contains(model.dataType)
        }
    }

    static List getListModels(List models) {
        models.findAll { model ->
            [LIST].contains(model.dataType)
        }
    }

}
