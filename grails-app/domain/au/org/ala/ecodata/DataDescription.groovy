package au.org.ala.ecodata

/**
 * Allows for extended description of data that is able to be accessed by the MERIT API or via downloads.
 */
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
    String excelExportedColumn
    String excelExportedStatus
    String excelExportedRequired
    String excelExportedSource
    String excelExportedDescription
    String excelExportedExample
    Date dateCreated
    Date lastUpdated

    static constraints = {
        name nullable: true
        description nullable:true
        graphQlName nullable: true
        xlsxName nullable: true
        xlsxHeader nullable: true
        formVersion nullable: true
        entity nullable: true
        field nullable: true
        userInterfaceReference nullable: true
        label nullable: true
        notes nullable: true
        type nullable: true
        excelExportedColumn nullable: true
        excelExportedStatus nullable: true
        excelExportedRequired nullable: true
        excelExportedSource nullable: true
        excelExportedDescription nullable: true
        excelExportedExample nullable: true

    }
}
