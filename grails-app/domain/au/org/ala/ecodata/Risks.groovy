package au.org.ala.ecodata


import grails.databinding.BindingFormat

/**
 * Represents risks identified for a project.
 */
class Risks {

    static constraints = {
        dateUpdated nullable: true
    }

    @BindingFormat("iso8601")
    Date dateUpdated // lastUpdated is not used as it is ignored by the data binding and not auto-populated for embedded objects
    String overallRisk
    List<Risk> rows
    static embedded = ['rows']

}
