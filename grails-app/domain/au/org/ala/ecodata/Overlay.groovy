package au.org.ala.ecodata

class Overlay {
    String alaId
    String alaName
    String layerName
    String title
    Boolean defaultSelected
    String boundaryColour
    Boolean showPropertyName
    String fillColour
    String textColour
    String userAccessRestriction
    Boolean inLayerShapeList
    Float opacity
    Boolean changeLayerColour

    static constraints = {
        userAccessRestriction inList: ['anyUser', 'loggedInUser']
        layerName nullable: true
        title nullable: true
        defaultSelected nullable: true
        boundaryColour nullable: true
        showPropertyName nullable: true
        fillColour nullable: true
        textColour nullable: true
        userAccessRestriction nullable: true
        inLayerShapeList nullable: true
        opacity nullable: true
        changeLayerColour nullable: true
    }
}
