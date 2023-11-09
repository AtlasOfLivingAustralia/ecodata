package au.org.ala.ecodata.paratoo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooPlotSelection implements Validateable {
    ParatooPlotSelectionData data
}

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooPlotSelectionData {
    String plot_label
    ParatooPlotSelectionLocation recommended_location
    String uuid
    String comment
}

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooPlotSelectionLocation {
    double lat
    double lng
}