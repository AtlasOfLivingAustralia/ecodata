package au.org.ala.ecodata
class Footer implements ProcessEmbedded {
    List<MenuItem> links
    List socials
    String type
    List logos

    static constraints = {
        links nullable: true
        socials nullable: true
        type nullable: true
        logos nullable: true
    }

    static embedded = ['links']
}