package au.org.ala.ecodata;

class MenuItem implements ProcessEmbedded {

    String role
    String displayName
    String href
    String contentType
    String introductoryText

    static constraints = {
        role nullable: true
        displayName nullable: true
        href nullable: true
        contentType nullable: true
        introductoryText nullable: true
    }

    def beforeUpdate () {
        sanitize()
    }

    def beforeInsert () {
        sanitize()
    }

    def sanitize() {
        if (introductoryText) {
            introductoryText = SanitizerService.sanitize(introductoryText)
        }
    }
}