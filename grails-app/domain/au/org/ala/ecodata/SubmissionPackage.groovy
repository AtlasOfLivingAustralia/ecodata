package au.org.ala.ecodata

import org.bson.types.ObjectId

class SubmissionPackage {

    ObjectId id

    String                  acknowledgement
    String                  associatedMaterialNane
    String                  associatedMaterialIdentifier
    String                  collectionStartDate
    String                  collectionEndDate
    String                  curationStatus
    String                  curationActivitiesOther
    DatasetContact          datasetContact
    List<DatasetAuthor>     datasetAuthors
    String                  geographicalExtentDescription
    String                  methodDriftDescription
    String                  otherMaterials
    List<String>            selectedAnimalGroups
    List<String>            selectedAnthropogenic
    List<String>            selectedConservativeMgmt
    List<String>            selectedEconomicResearch
    List<String>            selectedEnvironmentFeatures
    List<String>            selectedFieldsOfResearch
    List<String>            selectedIdentificationMethod
    String                  selectedMaterialIdentifier
    String                  selectedMaterialType
    List<String>            selectedObservedAttributes
    List<String>            selectedObservationMeasurements
    List<String>            selectedPlantGroups
    List<String>            selectedSamplingDesign
    List<String>            selectedSocioEconomic

    String                  submissionRecordId

    static belongsTo = [SubmissionRecord]

    static embedded = ['datasetContact', 'datasetAuthors']

    static constraints = {
        acknowledgement                 nullable:true
        associatedMaterialNane          nullable:true
        associatedMaterialIdentifier    nullable:true
        collectionStartDate             nullable:true
        collectionEndDate               nullable:true
        curationStatus                  nullable:true
        curationActivitiesOther         nullable:true
        datasetContact                  nullable:true
        datasetAuthors                  nullable:true
        geographicalExtentDescription   nullable:true
        methodDriftDescription          nullable:true
        otherMaterials                  nullable:true
        selectedAnimalGroups            nullable:true
        selectedAnthropogenic           nullable:true
        selectedConservativeMgmt        nullable:true
        selectedEconomicResearch        nullable:true
        selectedEnvironmentFeatures     nullable:true
        selectedFieldsOfResearch        nullable:true
        selectedIdentificationMethod    nullable:true
        selectedMaterialIdentifier      nullable:true
        selectedMaterialType            nullable:true
        selectedObservedAttributes      nullable:true
        selectedObservationMeasurements nullable:true
        selectedPlantGroups             nullable:true
        selectedSamplingDesign          nullable:true
        selectedSocioEconomic           nullable:true
        submissionRecordId              nullable:true
    }

/*
    Association:
        SubmissionPackages must belong to 1 Submission Record
    */
    static mapping = {
        submissionRecordId index: true
        version false
    }

}
