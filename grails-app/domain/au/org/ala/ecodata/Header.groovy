package au.org.ala.ecodata

class Header implements ProcessEmbedded {
    List<MenuItem> links
    String type

    static constraints = {
        links nullable: true
        type nullable: true
    }

    static embedded = ['links']
}