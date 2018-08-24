scores = [
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Baseline data",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberBaselineDataSets",
            "type": "SUM"
         }],
         "label": "Number of baseline data sets collected and/or synthesised"
      },
      "outputType": "RLP - Baseline data",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of baseline data sets collected and/or synthesised",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Communication materials",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.communicationMaterials.numberOfCommunicationMaterialsPublished",
            "type": "SUM"
         }],
         "label": "Number of communication materials published"
      },
      "outputType": "RLP - Communication materials",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of communication materials published",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Community engagement",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "Field day",
               "property": "data.events.eventType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.events.eventType",
               "type": "COUNT"
            }]
         }],
         "label": "Number of field days"
      },
      "outputType": "RLP - Community engagement",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of field days",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Community engagement",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "Training / workshop event",
               "property": "data.events.eventType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.events.eventType",
               "type": "COUNT"
            }]
         }],
         "label": "Number of training / workshop events"
      },
      "outputType": "RLP - Community engagement",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of training / workshop events",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Community engagement",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "Conference / seminar",
               "property": "data.events.eventType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.events.eventType",
               "type": "COUNT"
            }]
         }],
         "label": "Number of conferences / seminars"
      },
      "outputType": "RLP - Community engagement",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of conferences / seminars",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Community engagement",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "One-on-one technical advice interaction",
               "property": "data.events.eventType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.events.eventType",
               "type": "COUNT"
            }]
         }],
         "label": "Number of one-on-one technical advice interactions"
      },
      "outputType": "RLP - Community engagement",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of one-on-one technical advice interactions",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Community engagement",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "On-ground trial / demonstration",
               "property": "data.events.eventType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.events.eventType",
               "type": "COUNT"
            }]
         }],
         "label": "Number of on-ground trials / demonstrations"
      },
      "outputType": "RLP - Community engagement",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of on-ground trials / demonstrations",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Community engagement",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "On-ground works",
               "property": "data.events.eventType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.events.eventType",
               "type": "COUNT"
            }]
         }],
         "label": "Number of on-ground works"
      },
      "outputType": "RLP - Community engagement",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of on-ground works",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Controlling access",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.accessControlDetails.accessControlType",
            "type": "SUM"
         }],
         "label": "Number of structures installed"
      },
      "outputType": "RLP - Controlling access",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of structures installed",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Controlling access",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.accessControlDetails.structureLengthKm",
            "type": "SUM"
         }],
         "label": "Length (km) installed"
      },
      "outputType": "RLP - Controlling access",
      "entityTypes": ["RLP Output Report"],
      "label": "Length (km) installed",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Controlling access",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.tba",
            "type": "SUM"
         }],
         "label": "Area (ha) where access has been controlled"
      },
      "outputType": "RLP - Controlling access",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) where access has been controlled",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Pest animal management",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "Initial",
               "property": "data.pestAnimalManagementDetails.managementType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.pestAnimalManagementDetails.areaHa",
               "type": "SUM"
            }]
         }],
         "label": "Area (ha) treated for pest animals - initial"
      },
      "outputType": "RLP - Pest animal management",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) treated for pest animals - initial",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Pest animal management",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "Follow-up",
               "property": "data.pestAnimalManagementDetails.managementType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.pestAnimalManagementDetails.areaHa",
               "type": "SUM"
            }]
         }],
         "label": "Area (ha) treated for pest animals - follow-up"
      },
      "outputType": "RLP - Pest animal management",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) treated for pest animals - follow-up",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Debris removal",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaHa",
            "type": "SUM"
         }],
         "label": "Area (ha) of debris removal"
      },
      "outputType": "RLP - Debris removal",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) of debris removal",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Management plan development",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.managementPlans.managementPlanType",
            "type": "SUM"
         }],
         "label": "Number of farm/project/site plans developed"
      },
      "outputType": "RLP - Management plan development",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of farm/project/site plans developed",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Management plan development",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.managementPlans.areaCoveredByPlanHa",
            "type": "SUM"
         }],
         "label": "Area (ha) covered by plan"
      },
      "outputType": "RLP - Management plan development",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) covered by plan",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Erosion Management",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaOfErosionControlHa",
            "type": "SUM"
         }],
         "label": "Area (ha) of erosion control"
      },
      "outputType": "RLP - Erosion Management",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) of erosion control",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Erosion Management",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.lengthTreatedForErosionKm",
            "type": "SUM"
         }],
         "label": "Length (km) of stream/coastline treated for erosion"
      },
      "outputType": "RLP - Erosion Management",
      "entityTypes": ["RLP Output Report"],
      "label": "Length (km) of stream/coastline treated for erosion",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Establishing Agreements",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfAgreements",
            "type": "SUM"
         }],
         "label": "Number of agreements"
      },
      "outputType": "RLP - Establishing Agreements",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of agreements",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Establishing Agreements",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaHaCoveredByAgreements",
            "type": "SUM"
         }],
         "label": "Area (ha) covered by agreements"
      },
      "outputType": "RLP - Establishing Agreements",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) covered by agreements",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Establishing Agreements",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.agreements.numberOfDaysMaintainingAgreements",
            "type": "SUM"
         }],
         "label": "Number of days maintaining agreements"
      },
      "outputType": "RLP - Establishing Agreements",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of days maintaining agreements",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Maintaining feral free enclosures",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.enclosureDetails.newOrMaintained",
            "type": "COUNT"
         }],
         "label": "Number of feral free enclosures"
      },
      "outputType": "RLP - Maintaining feral free enclosures",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of feral free enclosures",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Maintaining feral free enclosures",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.enclosureDetails.areaProtectedHa",
            "type": "SUM"
         }],
         "label": "Area (ha) of feral-free enclosure"
      },
      "outputType": "RLP - Maintaining feral free enclosures",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) of feral-free enclosure",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Maintaining feral free enclosures",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.enclosureDetails.numberOfDaysMaintainingEnclosures",
            "type": "SUM"
         }],
         "label": "Number of days maintaining feral-free enclosures"
      },
      "outputType": "RLP - Maintaining feral free enclosures",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of days maintaining feral-free enclosures",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Establishing ex-situ breeding programs",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.breedingProgramDetails.numberOfIndividuals",
            "type": "SUM"
         }],
         "label": "Number of breeding sites and/or populations"
      },
      "outputType": "RLP - Establishing ex-situ breeding programs",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of breeding sites and/or populations",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Establishing ex-situ breeding programs",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.breedingProgramDetails.numberOfDaysMaintainingBreedingPrograms",
            "type": "SUM"
         }],
         "label": "Number of days maintaining breeding programs"
      },
      "outputType": "RLP - Establishing ex-situ breeding programs",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of days maintaining breeding programs",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Establishing monitoring regimes",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberMonitoringRegimesEstablished",
            "type": "SUM"
         }],
         "label": "Number of monitoring regimes established"
      },
      "outputType": "RLP - Establishing monitoring regimes",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of monitoring regimes established",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Establishing monitoring regimes",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfDaysMaintainingMonitoringRegemes",
            "type": "SUM"
         }],
         "label": "Number of days maintaining monitoring regimes"
      },
      "outputType": "RLP - Establishing monitoring regimes",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of days maintaining monitoring regimes",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Farm Management Survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Number of farm management surveys conducted"
      },
      "outputType": "RLP - Farm Management Survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of farm management surveys conducted",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Fauna survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaCoveredByFaunaSurveyHa",
            "type": "SUM"
         }],
         "label": "Area surveyed (ha) (fauna)"
      },
      "outputType": "RLP - Fauna survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Area surveyed (ha) (fauna)",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Fauna survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Number of fauna surveys conducted"
      },
      "outputType": "RLP - Fauna survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of fauna surveys conducted",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Fire management",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaTreatedHa",
            "type": "SUM"
         }],
         "label": "Area (ha) treated by fire management action"
      },
      "outputType": "RLP - Fire management",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) treated by fire management action",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Flora survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaCoveredByFloraSurveyHa",
            "type": "SUM"
         }],
         "label": "Area surveyed (ha) (flora)"
      },
      "outputType": "RLP - Flora survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Area surveyed (ha) (flora)",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Flora survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Number of flora surveys conducted"
      },
      "outputType": "RLP - Flora survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of flora surveys conducted",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Habitat augmentation",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaHa",
            "type": "SUM"
         }],
         "label": "Area (ha) of augmentation"
      },
      "outputType": "RLP - Habitat augmentation",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) of augmentation",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Habitat augmentation",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfStructures",
            "type": "SUM"
         }],
         "label": "Number of structures or installations"
      },
      "outputType": "RLP - Habitat augmentation",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of structures or installations",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Identifying sites",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberSitesIdentified",
            "type": "SUM"
         }],
         "label": "Number of potential sites identified"
      },
      "outputType": "RLP - Identifying sites",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of potential sites identified",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Improving hydrological regimes",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.hydrologicalRegimeDetails.numberOfActions",
            "type": "SUM"
         }],
         "label": "Number of treatments implemented to improve water management"
      },
      "outputType": "RLP - Improving hydrological regimes",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of treatments implemented to improve water management",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Improving land management practices",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaHa",
            "type": "SUM"
         }],
         "label": "Area (ha) covered by practice change"
      },
      "outputType": "RLP - Improving land management practices",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) covered by practice change",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Disease management",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaHa",
            "type": "SUM"
         }],
         "label": "Area (ha) treated for disease"
      },
      "outputType": "RLP - Disease management",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) treated for disease",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Negotiations",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberGroupsNegotiatedWith",
            "type": "SUM"
         }],
         "label": "Number of groups negotiated with"
      },
      "outputType": "RLP - Negotiations",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of groups negotiated with",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Obtaining approvals",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberApprovalsObtained",
            "type": "SUM"
         }],
         "label": "Number of relevant approvals obtained"
      },
      "outputType": "RLP - Obtaining approvals",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of relevant approvals obtained",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Pest animal survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Area (ha) surveyed for pest animals"
      },
      "outputType": "RLP - Pest animal survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) surveyed for pest animals",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Pest animal survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Number of pest animal surveys conducted"
      },
      "outputType": "RLP - Pest animal survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of pest animal surveys conducted",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Plant survival survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaHa",
            "type": "SUM"
         }],
         "label": "Area surveyed (ha) for plant survival"
      },
      "outputType": "RLP - Plant survival survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Area surveyed (ha) for plant survival",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Plant survival survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Number of plant survival surveys conducted"
      },
      "outputType": "RLP - Plant survival survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of plant survival surveys conducted",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Project planning",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberPlanningDocuments",
            "type": "SUM"
         }],
         "label": "Number of planning and delivery documents for delivery of the project services and monitoring"
      },
      "outputType": "RLP - Project planning",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of planning and delivery documents for delivery of the project services and monitoring",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Project planning",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfDaysProjectPlanning",
            "type": "SUM"
         }],
         "label": "Number of days project planning / preparation"
      },
      "outputType": "RLP - Project planning",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of days project planning / preparation",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Remediating riparian and aquatic areas",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaRemediatedHa",
            "type": "SUM"
         }],
         "label": "Area (ha) remediated"
      },
      "outputType": "RLP - Remediating riparian and aquatic areas",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) remediated",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Remediating riparian and aquatic areas",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.lengthRemediatedKm",
            "type": "SUM"
         }],
         "label": "Length (km) remediated"
      },
      "outputType": "RLP - Remediating riparian and aquatic areas",
      "entityTypes": ["RLP Output Report"],
      "label": "Length (km) remediated",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Weed treatment",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "Initial",
               "property": "data.weedTreatmentDetails.activityType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.weedTreatmentDetails.areaHa",
               "type": "SUM"
            }]
         }],
         "label": "Area (ha) treated for weeds - initial"
      },
      "outputType": "RLP - Weed treatment",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) treated for weeds - initial",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Weed treatment",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "filter": {
               "filterValue": "Follow-up",
               "property": "data.weedTreatmentDetails.activityType",
               "type": "filter"
            },
            "childAggregations": [{
               "property": "data.weedTreatmentDetails.areaHa",
               "type": "SUM"
            }]
         }],
         "label": "Area (ha) treated for weeds - follow-up"
      },
      "outputType": "RLP - Weed treatment",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) treated for weeds - follow-up",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Revegetating habitat",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaHa",
            "type": "SUM"
         }],
         "label": "Area of habitat revegetated (ha)"
      },
      "outputType": "RLP - Revegetating habitat",
      "entityTypes": ["RLP Output Report"],
      "label": "Area of habitat revegetated (ha)",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Revegetating habitat",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfDaysCollectingSeed",
            "type": "SUM"
         }],
         "label": "Number of days collecting seed"
      },
      "outputType": "RLP - Revegetating habitat",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of days collecting seed",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Revegetating habitat",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfDaysPropogatingPlants",
            "type": "SUM"
         }],
         "label": "Number of days propagating plants"
      },
      "outputType": "RLP - Revegetating habitat",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of days propagating plants",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Revegetating habitat",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaMaintainedHa",
            "type": "SUM"
         }],
         "label": "Area (ha) of revegetated habitat maintained"
      },
      "outputType": "RLP - Revegetating habitat",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) of revegetated habitat maintained",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Site preparation",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.areaHa",
            "type": "SUM"
         }],
         "label": "Area (ha) of site preparation"
      },
      "outputType": "RLP - Site preparation",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) of site preparation",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Site preparation",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfDaysPreparingSites",
            "type": "SUM"
         }],
         "label": "Number of days preparing site/s"
      },
      "outputType": "RLP - Site preparation",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of days preparing site/s",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Skills and knowledge survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Number of skills and knowledge surveys conducted"
      },
      "outputType": "RLP - Skills and knowledge survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of skills and knowledge surveys conducted",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Soil testing",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSoilTests",
            "type": "SUM"
         }],
         "label": "Number of soil tests conducted in targeted areas"
      },
      "outputType": "RLP - Soil testing",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of soil tests conducted in targeted areas",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Emergency Interventions",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfInterventions",
            "type": "SUM"
         }],
         "label": "Number of interventions"
      },
      "outputType": "RLP - Emergency Interventions",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of interventions",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Water quality survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Area (ha) surveyed for water quality"
      },
      "outputType": "RLP - Water quality survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) surveyed for water quality",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Water quality survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Number of water quality surveys"
      },
      "outputType": "RLP - Water quality survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of water quality surveys",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Weed distribution survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Area (ha) surveyed for weeds"
      },
      "outputType": "RLP - Weed distribution survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Area (ha) surveyed for weeds",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   },
   {
      "configuration": {
         "filter": {
            "filterValue": "RLP - Weed distribution survey",
            "property": "name",
            "type": "filter"
         },
         "childAggregations": [{
            "property": "data.numberOfSurveys",
            "type": "SUM"
         }],
         "label": "Number of weed distribution surveys conducted"
      },
      "outputType": "RLP - Weed distribution survey",
      "entityTypes": ["RLP Output Report"],
      "label": "Number of weed distribution surveys conducted",
      "units": "",
      "category": "RLP",
      "isOutputTarget": true,
      "status": "active"
   }
]