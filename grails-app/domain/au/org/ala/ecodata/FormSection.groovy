package au.org.ala.ecodata

import au.org.ala.ecodata.graphql.mappers.FormSectionGraphQLMapper
import au.org.ala.ecodata.graphql.models.SectionTemplate
import au.org.ala.ecodata.metadata.OutputMetadata

class FormSection {

    static graphql = FormSectionGraphQLMapper.graphqlMapping()

    static constraints = {
        title nullable: true
        modelName nullable: true
        optionalQuestionText nullable: true
        description nullable: true
        collapsibleHeading nullable: true
    }

    String name
    String title
    String modelName
    String description
    String collapsibleHeading

    /**
     * Deprecated but required for compatibility with the old API which retrieves the form template
     * separately from the metadata definition.
     */
    String templateName

    /** Defines the fields collected in this FormSection and how they are displayed.  Currently created and edited as JSON */
    Map template

    // Form sections can be individually marked as optional and expanded/collapsed
    String optionalQuestionText
    boolean optional = false
    boolean collapsedByDefault = false

    SectionTemplate getSectionTemplate() {
        SectionTemplate outputData = new SectionTemplate()
        if(template) {
            outputData.sectionTemplate = template.findAll{ it.key != "viewModel"}
        }
        return outputData
    }

    List annotatedTemplate() {
        OutputMetadata metadata = new OutputMetadata(template)
        return metadata.annotateDataModel()
    }

}
