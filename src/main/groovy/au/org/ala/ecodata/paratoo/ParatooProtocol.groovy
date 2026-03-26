package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.ExternalId

class ParatooProtocol {
    Map clientMeta
    Integer id
    String identifier
    String name
    Integer version
    String module

    ParatooProtocol(ActivityForm form, Map clientMeta) {
        id = form.externalIds?.find {it.idType == ExternalId.IdType.MONITOR_PROTOCOL_INTERNAL_ID}?.externalId as Integer
        identifier = form.externalIds?.find {it.idType == ExternalId.IdType.MONITOR_PROTOCOL_GUID}?.externalId
        name = form.name
        version = form.formVersion as Integer
        module = form.category
        this.clientMeta = clientMeta
    }
}
