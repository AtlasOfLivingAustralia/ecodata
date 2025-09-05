package au.org.ala.ecodata.graphql.input

import grails.validation.Validateable

class ReportQuery implements Validateable {
    DateRange dateSubmitted
    DateRange dateApproved
    DateRange lastUpdated
}
