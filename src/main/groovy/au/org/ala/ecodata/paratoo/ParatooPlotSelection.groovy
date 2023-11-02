package au.org.ala.ecodata.paratoo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooPlotSelection {
    ParatooPlotSelectionData data
}

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooPlotSelectionData {
    String plot_label
    ParatooPlotSelectionLocation recommended_location
}

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooPlotSelectionLocation {
    double lat
    double lng
}