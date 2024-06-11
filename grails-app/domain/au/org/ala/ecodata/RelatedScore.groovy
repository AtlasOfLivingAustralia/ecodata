package au.org.ala.ecodata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString


@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class RelatedScore {

    String scoreId

    /** A description of the association - e.g. Service Provider, Grantee, Sponsor */
    String description

}
