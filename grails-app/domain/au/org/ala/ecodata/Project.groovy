package au.org.ala.ecodata

import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Interval
import org.joda.time.Period
import org.joda.time.Weeks
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat

class Project {

    static final COMPLETED = "completed"

    /*
    Associations:
        projects may have 0..n Sites - these are mapped from the Site side
        Activities - not implemented yet
    */
    static mapping = {
        name index: true
        projectId index: true
		promoteOnHomepage index: true
        version false
    }

    ObjectId id
    String projectId      // same as collectory dataProvider id
    String dataResourceId // one collectory dataResource stores all sightings
    String status = 'active'
    String externalId
    String name  // required
    String description
    String manager
    String grantId
    String workOrderId
    String groupId
    String groupName
    String organisationName
    String serviceProviderName
    String organisationId
    Date plannedStartDate
    Date plannedEndDate
    Date serviceProviderAgreementDate
    Date actualStartDate
    Date actualEndDate
    String fundingSource
    String fundingSourceProjectPercent
    String plannedCost
    String reportingMeasuresAddressed
    String projectPlannedOutputType
    String projectPlannedOutputValue
	Map custom
	Map risks
	Date dateCreated
    Date lastUpdated
	String promoteOnHomepage = 'no'
    List activities
	
    boolean isCitizenScience
    String projectType    // survey, works
    String aim, keywords, urlAndroid, urlITunes, urlWeb
    String getInvolved, scienceType, projectSiteId
    double funding
    String orgIdGrantee, orgIdSponsor, orgIdSvcProvider
    String userCreated, userLastModified

    static transients = ['activities', 'plannedDurationInWeeks', 'actualDurationInWeeks']

    Date getActualStartDate() {
        if (actualStartDate) {
            return actualStartDate
        }
        if (activities) {
            return activities.min{it.startDate}?.startDate ?: null
        }
        return null;
    }

    Date getActualEndDate() {
        if (actualEndDate) {
            return actualEndDate
        }
        if (status == COMPLETED && activities) {
            return activities.max{it.endDate}?.endDate ?: null
        }
        return null;
    }

    Integer getActualDurationInWeeks() {
        return intervalInWeeks(getActualStartDate(), getActualEndDate())
    }

    Integer getPlannedDurationInWeeks() {
        return intervalInWeeks(plannedStartDate, plannedEndDate)
    }

    private Integer intervalInWeeks(Date startDate, Date endDate) {
        if (!startDate || !endDate) {
            return null
        }
        DateTime start = new DateTime(startDate)
        DateTime end = new DateTime(endDate);

        Interval interval = new Interval(start, end)
        return Weeks.weeksIn(interval).weeks
    }

    static constraints = {
        externalId nullable:true
        description nullable:true, maxSize: 40000
        workOrderId nullable:true
        manager nullable:true
        groupId nullable:true
        groupName nullable:true
        organisationName nullable:true
        serviceProviderName nullable:true
        plannedStartDate nullable:true
        plannedEndDate nullable:true
        serviceProviderAgreementDate nullable:true
        actualStartDate nullable:true
        actualEndDate nullable:true
        fundingSource nullable:true
        fundingSourceProjectPercent nullable:true
        plannedCost nullable:true
        reportingMeasuresAddressed nullable:true
        projectPlannedOutputType nullable:true
        projectPlannedOutputValue nullable:true
        grantId nullable:true
		custom nullable:true
		risks nullable:true
        promoteOnHomepage nullable:true
        organisationId nullable:true
        projectType nullable:true    // nullable for backward compatibility; survey, works
        dataResourceId nullable:true // nullable for backward compatibility
        aim nullable:true
        keywords nullable:true
        urlAndroid nullable:true
        urlITunes nullable:true
        urlWeb nullable:true
        getInvolved nullable:true
        scienceType nullable:true
        orgIdGrantee nullable:true
        orgIdSponsor nullable:true
        orgIdSvcProvider nullable:true
        projectSiteId nullable:true // nullable for backward compatibility
        userCreated nullable:true
        userLastModified nullable:true
    }
}
