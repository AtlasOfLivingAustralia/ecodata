package au.org.ala.ecodata

import static au.org.ala.ecodata.Status.ACTIVE

import org.bson.types.ObjectId

class Record {
 //   def grailsApplication
    /** Represents a species guid that was unable to be matched against the ALA names list */
    static final String UNMATCHED_GUID = "A_GUID"

    static mapping = {
        occurrenceID index: true
        outputSpeciesId index: true
        status index: true
        activityId index: true
        projectActivityId index: true
        lastUpdated index: true
        dataSetId index: true
        outputId index: true
        version false
    }

    ObjectId id
    String projectId //ID of the project within ecodata
    String projectActivityId
    String dataSetId
    String activityId
    String occurrenceID
    String outputSpeciesId  // reference to output species outputSpeciesId.
    String userId
    String eventDate //should be a date in "yyyy-MM-dd" or "2014-11-24T04:55:48+11:00" format
    String scientificName
    String name
    String vernacularName
    Double decimalLatitude
    Double decimalLongitude
    Double generalizedDecimalLatitude
    Double generalizedDecimalLongitude
    Integer coordinateUncertaintyInMeters
    Integer individualCount = 1
    Integer numberOfOrganisms
    Date dateCreated
    Date lastUpdated
    String outputId
    String json
    Integer outputItemId
    List measurementsOrFacts = []
    String status = ACTIVE

    static transients = ['recordNumber']

    def beforeValidate() {
        if (occurrenceID == null) {
            //mint an UUID
            occurrenceID = UUID.randomUUID().toString()
        }
    }

    static constraints = {
        projectId nullable: true
        projectActivityId nullable: true
        activityId nullable: true
        eventDate nullable: true
        decimalLatitude nullable: true
        decimalLongitude nullable: true
        generalizedDecimalLatitude nullable: true
        generalizedDecimalLongitude nullable: true
        userId nullable: true
        coordinateUncertaintyInMeters nullable: true
        individualCount nullable: true
        numberOfOrganisms nullable: true
        outputId nullable: true
        json nullable: true
        outputItemId nullable: true
        status nullable: true
        outputSpeciesId nullable: true
        dataSetId nullable: true
        name nullable: true
        vernacularName nullable: true
        scientificName nullable: true
        measurementsOrFacts nullable: true
    }

    String getRecordNumber(sightingsUrl){
        "${sightingsUrl}/bioActivity/index/${activityId}"
    }
}
