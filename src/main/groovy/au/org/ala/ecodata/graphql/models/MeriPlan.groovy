package au.org.ala.ecodata.graphql.models

import au.org.ala.ecodata.OutputTarget
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class MeriPlan {

    Object details
    List<OutputTarget> outputTargets
}