package au.org.ala.ecodata.metadata


import au.org.ala.ecodata.Record

class SpeciesAttributeGetter extends OutputDataGetter {
    final static String SPECIES_ID = 'guid'
    final static String SPECIES_COMMON_NAME = 'commonName'
    final static String SPECIES_SCIENTIFIC_NAME = 'scientificName'
    String biePrefix
    String attributeName

    SpeciesAttributeGetter(String propertyName, Map dataNode, Map<String, Object> documentMap, TimeZone timeZone, String attribute, String biePrefix) {
        super(propertyName, dataNode, documentMap, timeZone)
        this.biePrefix = biePrefix
        this.attributeName = attribute
    }

    @Override
    def species(Object node, Value outputValue) {
        switch (attributeName) {
            case SPECIES_ID:
                return getGUID(node, outputValue)
            case SPECIES_COMMON_NAME:
                return getCommonName(node, outputValue)
            case SPECIES_SCIENTIFIC_NAME:
                return getScientificName(node, outputValue)
            default:
                throw new IllegalArgumentException("Unknown species attribute: ${attributeName}")
        }
    }

    String getGUID(Object node, Value outputValue) {
        def val = outputValue.value
        if (!val?.name) {
            return ""
        }
        if (!val?.guid || val.guid == Record.UNMATCHED_GUID) {
            return "Unmatched name"
        }
        return "${biePrefix}${val.guid}"
    }

    String getCommonName(Object node, Value outputValue) {
        def val = outputValue.value
        if (val instanceof Map)
            return val[SPECIES_COMMON_NAME] ?: ""

        return ""
    }

    String getScientificName(Object node, Value outputValue) {
        def val = outputValue.value
        if (val instanceof Map)
            return val[SPECIES_SCIENTIFIC_NAME] ?: ""

        return ""
    }

}
