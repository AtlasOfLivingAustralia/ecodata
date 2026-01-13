package au.org.ala.ecodata.graphql.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class SectionTemplate {

    Object sectionTemplate
}