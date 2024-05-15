package au.org.ala.ecodata.metadata

import pl.touk.excel.export.getters.Getter

class SpeciesUrlGetter extends OutputDataGetter implements Getter<String> {
    String biePrefix
    SpeciesUrlGetter(String propertyName, Map dataNode, Map<String, Object> documentMap, TimeZone timeZone, String biePrefix) {
        super(propertyName, dataNode, documentMap, timeZone)
        this.biePrefix = biePrefix
    }

    @Override
    def species(Object node, Value outputValue) {
        def val = outputValue.value
        if (!val?.name) {
            return ""
        }

        return val?.guid ? biePrefix+val.guid : "Unmatched name"
    }


}
