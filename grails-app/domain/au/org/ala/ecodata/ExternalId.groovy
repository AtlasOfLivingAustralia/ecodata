package au.org.ala.ecodata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.EqualsAndHashCode

/**
 * Associates an id held in an external system with a Project
 */
@EqualsAndHashCode
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ExternalId implements Comparable {

    enum IdType {
        INTERNAL_ORDER_NUMBER, TECH_ONE_CODE, WORK_ORDER, GRANT_AWARD, GRANT_OPPORTUNITY, RELATED_PROJECT,
        MONITOR_PROTOCOL_INTERNAL_ID, MONITOR_PROTOCOL_GUID, TECH_ONE_CONTRACT_NUMBER }

    static constraints = {
    }

    IdType idType
    String externalId

    @Override
    int compareTo(Object otherId) {
        ExternalId other = (ExternalId)otherId
        return (idType.ordinal()+externalId).compareTo(other?.idType?.ordinal()+other?.externalId)
    }
}
