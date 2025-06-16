package au.org.ala.ecodata

class TemplateConfiguration implements ProcessEmbedded {
    Footer footer
    Header header
    Map banner
    Map styles
    Map homePage

    static constraints = {
        footer nullable: true
        header nullable: true
        banner nullable: true
        styles nullable: true
        homePage nullable: true
    }
    static embedded = ['footer', 'header']
}