package au.org.ala.ecodata

import org.bson.types.ObjectId

class Activity {

    static mapping = {
        activityId index: true
        version false
    }

    ObjectId id
    String activityId
    String siteId
    String description
    List types = []  // type id? and name
    Date startDate
    Date endDate
    String collector
    String censusMethod
    String methodAccuracy
    List actualOutputs = []  // type and value (and units?)
    String fieldNotes
    String notes
    Date dateCreated
    Date lastUpdated

    static activityTypes = [
            [name:'Site condition survey', list: [
                [key:'', name:'DECCW vegetation assessment']
            ]],
            [name:'Biological survey', list: [
                [key:'birdSurvey', name:'Bird survey'],
                [key:'reptileSurvey', name:'Reptile survey'],
                [key:'insectSurvey', name:'Insect survey'],
                [key:'smallMammalSurvey', name:'Small mammal survey'],
                [key:'batSurvey', name:'Bat survey'],
                [key:'koalaSurvey', name:'Koala survey'],
                [key:'floraSurvey', name:'Flora survey'],
                [key:'rapidAssessment', name:'Rapid assessment'],
            ]],
            [key:'speciesObservation', name:'Species observation'],
            [key:'weedControl', name:'Weed control'],
            [key:'pestControl', name:'Pest control'],
            [key:'planting', name:'Planting']
    ]

    static constraints = {
        description nullable: true
        startDate nullable: true
        endDate nullable: true
        collector nullable: true
        censusMethod nullable: true
        methodAccuracy nullable: true
        fieldNotes nullable: true, maxSize: 4000
        notes nullable: true, maxSize: 4000
    }
}
