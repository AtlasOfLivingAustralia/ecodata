package au.org.ala.ecodata

class FormSection {

    static constraints = {
        title nullable: true
        modelName nullable: true
        optionalQuestionText nullable: true
    }

    String name
    String title
    String modelName

    /**
     * Deprecated but required for compatibility with the old API which retrieves the form template
     * separately from the metadata definition.
     */
    String templateName

    /**  */
    Map template

    // Form sections can be individually marked as optional and expanded/collapsed
    String optionalQuestionText
    boolean optional = false
    boolean collapsedByDefault = false

}
