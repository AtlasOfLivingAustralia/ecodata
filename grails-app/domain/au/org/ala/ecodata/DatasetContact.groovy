package au.org.ala.ecodata

class DatasetContact {

    String      title
    String      name
    String      role
    String      address
    String      email
    String      phone
    String      organizationName

    static constraints = {
        title               nullable: true
        name                nullable: true
        role                nullable: true
        address             nullable: true
        email               nullable: true
        phone               nullable: true
        organizationName    nullable: true
    }
}
