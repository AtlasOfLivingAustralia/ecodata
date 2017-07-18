package au.org.ala.ecodata

class DatasetAuthor {

    String authorInitials
    String authorSurnameOrOrgName
    String authorAffiliation

    static constraints = {
        authorInitials  nullable: true
        authorSurnameOrOrgName  nullable: true
        authorAffiliation  nullable: true
    }
}
