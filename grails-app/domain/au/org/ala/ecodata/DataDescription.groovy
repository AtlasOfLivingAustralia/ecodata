package au.org.ala.ecodata

class DataDescription {

    String name
    String graphQlName
    String xlsxName
    String xlsxHeader
    String type
    String description
    Integer formVersion
    String entity
    String field
    boolean derived
    String userInterfaceReference
    String label
    String notes

    static constraints = {
        graphQlName nullable: true
        xlsxName nullable: true
        xlsxHeader nullable: true
        formVersion nullable: true
        entity nullable: true
        field nullable: true
        userInterfaceReference nullable: true
        label nullable: true
        notes nullable: true
    }
}
