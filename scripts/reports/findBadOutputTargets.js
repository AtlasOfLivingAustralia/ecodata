var targets = [
    {
        output: "Revegetation Details",
        isOutputTarget: true,
        category: "Revegetation",
        listName: "",
        description: "The area of land actually revegetated with native species by the planting of seedlings, sowing of seed and managed native regeneration actions.",
        displayType: "",
        name: "areaRevegHa",
        aggregationType: "SUM",
        label: "Area of revegetation works (Ha)",
        units: "Ha",
        gmsId: "RVA"
    },
    {
        output: "Revegetation Details",
        isOutputTarget: true,
        category: "Revegetation",
        description: "The total number of seedlings planted.",
        displayType: "",
        name: "totalNumberPlanted",
        aggregationType: "SUM",
        label: "Number of plants planted",
        units: "",
        gmsId: "RVN"
    },
    {
        output: "Revegetation Details",
        isOutputTarget: true,
        category: "Revegetation",
        description: "The total weight of seed sown in revegetation plots.",
        displayType: "",
        name: "totalSeedSownKg",
        aggregationType: "SUM",
        label: "Kilograms of seed sown",
        units: "Kg",
        gmsId: "RVSS"
    },
    {
        output: "Revegetation Details",
        groupBy: "output:matureHeight",
        isOutputTarget: true,
        category: "Revegetation",
        listName: "planting",
        description: "Total number of plants planted which will grow to a mature height of greater than 2 metres. This does not include natural regeneration or plants established from the sowing of seed.",
        displayType: "",
        name: "numberPlanted",
        aggregationType: "SUM",
        label: "No. of plants planted > 2 metres in height",
        filterBy: "> 2 metres",
        gmsId: "RVT2A"
    },
    {
        output: "Revegetation Details",
        groupBy: "output:matureHeight",
        isOutputTarget: true,
        category: "Revegetation",
        listName: "planting",
        description: "Kilograms of seed sown of species expected to grow < 2 metres in height",
        displayType: "",
        name: "seedSownKg",
        aggregationType: "SUM",
        label: "Kilograms of seed sown of species expected to grow > 2 metres in height",
        filterBy: "< 2 metres",
        units: "Kg",
        gmsId: "RVS2B"
    },
    {
        output: "Revegetation Details",
        groupBy: "output:matureHeight",
        isOutputTarget: true,
        category: "Revegetation",
        listName: "planting",
        description: "Total number of plants planted which will grow to a mature height of less than 2 metres. This does not include natural regeneration or plants established from the sowing of seed.",
        displayType: "",
        name: "numberPlanted",
        aggregationType: "SUM",
        label: "No. of plants planted < 2 metres in height",
        filterBy: "< 2 metres",
        gmsId: "RVT2B"
    },
    {
        output: "Seed Collection Details",
        isOutputTarget: true,
        category: "Revegetation",
        listName: "",
        description: "The total weight of seed collected for storage or propagation.",
        displayType: "",
        name: "totalSeedCollectedKg",
        aggregationType: "SUM",
        label: "Total seed collected (Kg)",
        units: "Kg",
        gmsId: "SDC"
    },
    {
        output: "Site Preparation Actions",
        isOutputTarget: true,
        category: "Sites and Activity Planning",
        description: "Sum of the area of land specifically recorded as having preparatory works undertaken in advance of another associated activity. Site preparation works include activities such as fencing, weed or pest treatment, drainage or soil moisture management actions, etc.",
        displayType: "",
        name: "preparationAreaTotal",
        aggregationType: "SUM",
        label: "Total area prepared (Ha) for follow-up treatment actions",
        units: "Ha",
        gmsId: "STP"
    },
    {
        output: "Participant Information",
        isOutputTarget: true,
        category: "Community Engagement and Capacity Building",
        listName: "",
        description: "The number of participants at activities and events who are not employed on projects. Individuals attending multiple activities will be counted for their attendance at each event.",
        displayType: "",
        name: "totalParticipantsNotEmployed",
        aggregationType: "SUM",
        label: "No of volunteers participating in project activities",
        units: ""
    },
    {
        output: "Participant Information",
        isOutputTarget: true,
        category: "Indigenous Capacity",
        description: "The number of Indigenous people attending and/or participating in activities and events. Individuals attending multiple activities will be counted for their attendance at each event.",
        displayType: "",
        name: "numberOfIndigenousParticipants",
        aggregationType: "SUM",
        label: "No of Indigenous participants at project events. ",
        units: ""
    },
    {
        output: "Participant Information",
        isOutputTarget: true,
        category: "Community Engagement and Capacity Building",
        description: "The total number of unique individuals who attended at least one project event. This measure is an attempt to determine new vs repeat participation as a measure of the effectiveness of projects/programmes in reaching out and engaging with their communities.",
        displayType: "",
        name: "totalParticipantsNew",
        aggregationType: "SUM",
        label: "Total No. of new participants (attending project events for the first time)",
        units: ""
    },
    {
        output: "Event Details",
        isOutputTarget: true,
        category: "Community Engagement and Capacity Building",
        listName: "events",
        description: "Count of the number of community participation and engagement events run.",
        displayType: "",
        name: "eventType",
        aggregationType: "COUNT",
        label: "Total No. of community participation and engagement events run",
        gmsId: "CPEE"
    },
    {
        output: "Fire Management Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        description: "The area of the fire ground actually burnt. This may be different to the total area managed.",
        displayType: "",
        name: "areaOfFireHa",
        aggregationType: "SUM",
        label: "Burnt area (Ha)",
        units: "Ha",
        gmsId: "FMA"
    },
    {
        output: "Weed Observation and Monitoring Details",
        isOutputTarget: true,
        category: "Invasive Species Management - Weeds",
        listName: "",
        description: "Count of the number of weed observation, mapping and monitoring activities undertaken.",
        displayType: "",
        name: "weedObservationMonitoringDetails",
        aggregationType: "COUNT",
        label: "No. of activities undertaking weed monitoring",
        gmsId: "WMM WSA"
    },
    {
        output: "Weed Treatment Details",
        groupBy: "output:treatmentEventType",
        isOutputTarget: true,
        category: "Invasive Species Management - Weeds",
        description: "The total area of weeds for which an initial treatment was undertaken.",
        displayType: "",
        name: "areaTreatedHa",
        aggregationType: "SUM",
        label: "Total new area treated (Ha)",
        filterBy: "Initial treatment",
        units: "Ha",
        gmsId: "WDT"
    },
    {
        output: "Stock Management Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        description: "Total area managed with conservation grazing",
        displayType: "",
        name: "areaOfStockManagmentHa",
        aggregationType: "SUM",
        label: "Area managed with conservation grazing (Ha)",
        units: "Ha",
        gmsId: "CGM"
    },
    {
        output: "Water Management Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        listName: "",
        description: "The area of land actively managed with water management activities for enhanced environmental values.",
        displayType: "",
        name: "managedArea",
        aggregationType: "SUM",
        label: "Area (Ha) managed for water values",
        units: "Ha",
        gmsId: "WMA"
    },
    {
        output: "Training Details",
        isOutputTarget: true,
        category: "Community Engagement and Capacity Building",
        description: "Sum of the number of people who complete formal/accredited training courses.",
        displayType: "",
        name: "totalCompletingCourses",
        aggregationType: "SUM",
        label: "Total No. of people completing formal training courses",
        gmsId: "TSD"
    },
    {
        output: "Debris Removal Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        description: "The total mass of debris removed.",
        displayType: "",
        name: "debrisWeightTonnes",
        aggregationType: "SUM",
        label: "Weight of debris removed (Tonnes)",
        units: "Tonnes",
        gmsId: "DRW"
    },
    {
        output: "Debris Removal Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        description: "The total volume of debris removed.",
        displayType: "",
        name: "debrisVolumeM3",
        aggregationType: "SUM",
        label: "Volume of debris removed (m3)",
        units: "m3",
        gmsId: "DRV"
    },
    {
        output: "Erosion Management Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        listName: "",
        description: "The total number of structures installed to to control erosion.",
        displayType: "",
        name: "erosionStructuresInstalled",
        aggregationType: "SUM",
        label: "Total No. of erosion control structures installed"
    },
    {
        output: "Erosion Management Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        description: "Total lineal length of stream frontage and/or coastline which has been treated with erosion management actions.",
        displayType: "",
        name: "erosionLength",
        aggregationType: "SUM",
        label: "Length of stream/coastline treated (Km)",
        units: "Km",
        gmsId: "EML"
    },
    {
        output: "Erosion Management Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        description: "The total area of erosion treated by management actions.",
        displayType: "",
        name: "erosionAreaTreated",
        aggregationType: "SUM",
        label: "Erosion area treated (Ha)",
        units: "Ha",
        gmsId: "EMA"
    },
    {
        output: "Access Control Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        description: "Count of the number of activities implementing access control works",
        displayType: "",
        name: "structuresInstalled",
        aggregationType: "COUNT",
        label: "No. of activities implementing access control works",
        gmsId: "PAM"
    },
    {
        output: "Access Control Details",
        isOutputTarget: true,
        category: "Natural Resources Management",
        listName: "",
        description: "Area in hectares that is being protected as a direct result of installing access control structures.",
        displayType: "",
        name: "accessAreaProtected",
        aggregationType: "SUM",
        label: "Area protected by access control installations (Ha)"
    },
    {
        output: "Management Practice Change Details",
        isOutputTarget: true,
        category: "Farm Management Practices",
        description: "The total number of unique/individual farming/fisher entities (farming/fishing businesses) that have made changes towards more sustainable management and primary production practices.",
        displayType: "",
        name: "totalEntitiesAdoptingChange",
        aggregationType: "SUM",
        label: "Total No. of farming entities adopting sustainable practice change",
        gmsId: "MPCFE"
    },
    {
        output: "Management Practice Change Details",
        isOutputTarget: true,
        category: "Farm Management Practices",
        description: "The surface area of land/water over which improved management practices have been undertaken/implemented as a result of government funded projects. ",
        displayType: "",
        name: "totalChangePracticeTreatedArea",
        aggregationType: "SUM",
        label: "Area of land (Ha) on which improved management practices have been implemented",
        units: "Ha",
        gmsId: "MPC"
    },
    {
        output: "Management Practice Change Details",
        isOutputTarget: true,
        category: "Farm Management Practices",
        description: "The surface area of land/water which is now managed using sustainable practices and resulting from implementation of funded projects.",
        displayType: "",
        name: "benefitAreaHa",
        aggregationType: "SUM",
        label: "Area of land (Ha) changed to sustainable practices",
        units: "Ha",
        gmsId: "MPCSP"
    },
    {
        output: "Conservation Works Details",
        groupBy: "",
        isOutputTarget: true,
        category: "Biodiversity Management",
        listName: "protectionMechanisms",
        description: "The total number of agreements/protection mechanisms implemented to conserve/protect species.",
        displayType: "",
        name: "numberOfProtectionMechanisms",
        aggregationType: "SUM",
        label: "No. of protection mechanisms implemented"
    },
    {
        output: "Conservation Works Details",
        isOutputTarget: true,
        category: "Biodiversity Management",
        listName: "protectionMechanisms",
        description: "Sum of the areas covered by all Agreement mechanisms. Multiple mechanisms covering the same area may result in area of coverage being double-counted.",
        displayType: "",
        name: "areaUnderAgreement",
        aggregationType: "SUM",
        label: "Area (Ha) covered by Agreement mechanisms"
    },
    {
        output: "Plant Propagation Details",
        isOutputTarget: true,
        category: "Revegetation",
        description: "The total number of plants which have been propagated and nurtured to the point of being ready for planting out.",
        displayType: "",
        name: "totalNumberGrown",
        aggregationType: "SUM",
        label: "Total No. of plants grown and ready for planting ",
        gmsId: "PPRP"
    },
    {
        output: "Fauna Survey Details",
        isOutputTarget: true,
        category: "Biodiversity Management",
        listName: "",
        description: "The number of activities undertaken of the type 'Fauna Survey - general'.",
        displayType: "",
        name: "totalNumberOfOrganisms",
        aggregationType: "COUNT",
        label: "No. of fauna surveys undertaken",
        gmsId: "FBS"
    },
    {
        output: "Flora Survey Details",
        isOutputTarget: true,
        category: "Biodiversity Management",
        listName: "",
        description: "The number of activities undertaken of the type 'Flora Survey - general'.",
        displayType: "",
        name: "totalNumberOfOrganisms",
        aggregationType: "COUNT",
        label: "No. of flora surveys undertaken",
        gmsId: "FRBS"
    },
    {
        output: "Indigenous Employment",
        isOutputTarget: true,
        category: "Indigenous Capacity",
        listName: "",
        description: "No. of Indigenous people employed PT (rangers)",
        displayType: "",
        name: "noOfIndigenousRangersPt",
        aggregationType: "SUM",
        label: "No. of Indigenous people employed PT (rangers)"
    },
    {
        output: "Indigenous Employment",
        isOutputTarget: true,
        category: "Indigenous Capacity",
        listName: "",
        description: "No. of Indigenous people employed FT (rangers)",
        displayType: "",
        name: "noOfIndigenousRangersFt",
        aggregationType: "SUM",
        label: "No. of Indigenous people employed FT (rangers)"
    },
    {
        output: "Indigenous Employment",
        isOutputTarget: true,
        category: "Indigenous Capacity",
        listName: "",
        description: "No. of Indigenous people employed PT (non-rangers)",
        displayType: "",
        name: "noOfIndigenousNonRangersPt",
        aggregationType: "SUM",
        label: "No. of Indigenous people employed PT (non-rangers)"
    },
    {
        output: "Indigenous Employment",
        isOutputTarget: true,
        category: "Indigenous Capacity",
        listName: "",
        description: "No. of Indigenous people employed FT (non-rangers)",
        displayType: "",
        name: "noOfIndigenousNonRangersFt",
        aggregationType: "SUM",
        label: "No. of Indigenous people employed FT (non-rangers)"
    },
    {
        output: "Indigenous Businesses",
        isOutputTarget: true,
        category: "Indigenous Capacity",
        description: "Count of the number of unique (new) Indigenous enterprises established as a result of project implementation",
        displayType: "",
        name: "totalNewEnterprises",
        aggregationType: "SUM",
        label: "No. of new enterprises established"
    },
    {
        output: "Indigenous Businesses",
        isOutputTarget: true,
        category: "Indigenous Capacity",
        description: "Count of the number of unique engagements of Indigenous enterprises which have been formalised in contracts/agreements.",
        displayType: "",
        name: "totalIndigenousEnterprisesEngaged",
        aggregationType: "SUM",
        label: "No. of formal (contractual) engagements with Indigenous enterprises"
    },
    {
        output: "Pest Management Details",
        groupBy: "output:treatmentType",
        isOutputTarget: true,
        category: "Invasive Species Management - Pests & Diseases",
        listName: "pestManagement",
        description: "The total area over which pest treatment activities have been undertaken. Note that re-treatments of the same area may be double-counted.",
        displayType: "",
        name: "areaTreatedHa",
        aggregationType: "SUM",
        label: "Area covered (Ha) by pest treatment actions",
        filterBy: "Initial treatment",
        units: "Ha",
        gmsId: "PMA"
    },
    {
        output: "Pest Management Details",
        isOutputTarget: true,
        category: "Invasive Species Management - Pests & Diseases",
        listName: "pestManagement",
        description: "The total number of individual pest animals killed and colonies (ants, wasps, etc.) destroyed.",
        displayType: "",
        name: "pestAnimalsTreatedNo",
        aggregationType: "SUM",
        label: "Total No. of individuals or colonies of pest animals destroyed",
        gmsId: "PMQ"
    },
    {
        output: "Disease Management Details",
        groupBy: "",
        isOutputTarget: true,
        category: "Invasive Species Management - Pests & Diseases",
        listName: "",
        description: "Total area treated / quarantined",
        displayType: "",
        name: "areaTreatedHa",
        aggregationType: "SUM",
        label: "Total area (Ha) treated / quarantined",
        gmsId: "DMA"
    },
    {
        output: "Pest Observation and Monitoring Details",
        isOutputTarget: true,
        category: "Invasive Species Management - Pests & Diseases",
        listName: "pestObservationMonitoringDetails",
        description: "The total number of actions undertaken to monitor pest species.",
        displayType: "",
        name: "assessmentMethod",
        aggregationType: "COUNT",
        label: "No. of pest species monitoring actions undertaken",
        gmsId: "PSA"
    },
    {
        output: "Vegetation Monitoring Results",
        isOutputTarget: true,
        category: "Revegetation",
        description: "The total number of follow-up activities undertaken to monitor the success of revegetation actions carried out under sponsored projects.",
        displayType: "",
        name: "revegetationType",
        aggregationType: "SUM",
        label: "Total number of revegetation monitoring activities undertaken",
        gmsId: "PSS"
    },
    {
        output: "Vegetation Monitoring Results",
        groupBy: "output:matureHeight",
        isOutputTarget: true,
        category: "Revegetation",
        listName: "revegetationMonitoring",
        description: "Total No. of plants surviving with mature height > 2 metres",
        displayType: "",
        name: "numberSurviving",
        aggregationType: "SUM",
        label: "Total No. of plants surviving with mature height > 2 metres",
        filterBy: "> 2 metres",
        gmsId: "PSC"
    },
    {
        output: "Vegetation Monitoring Results",
        isOutputTarget: true,
        category: "Revegetation",
        listName: "revegetationMonitoring",
        description: "Estimate of the average survivability of both tubestock and seedstock expressed as a percentage of plants planted. For seedstock, this includes both counted and estimated germination establishment rates.",
        displayType: "",
        name: "survivalRate",
        aggregationType: "AVERAGE",
        label: "Average survivability of tubestock and seedstock (%)"
    },
    {
        output: "Water Quality Measurements",
        isOutputTarget: true,
        category: "Natural Resources Management",
        listName: "",
        description: "The number of water quality monitoring activities undertaken.",
        displayType: "",
        name: "instrumentCalibration",
        aggregationType: "COUNT",
        label: "No. of water quality monitoring events undertaken",
        gmsId: "WQSA"
    },
    {
        output: "Sampling Site Information",
        isOutputTarget: true,
        category: "Biodiversity - Site Condition Assessment",
        listName: "",
        description: "Total number of site assessments undertaken using the Commonwealth government vegetation assessment methodology.",
        displayType: "",
        name: "assessmentEventType",
        aggregationType: "COUNT",
        label: "No. of site assessments undertaken using the Commonwealth government vegetation assessment methodology",
        gmsId: "VAC"
    },
    {
        output: "Fence Details",
        isOutputTarget: true,
        category: "Biodiversity Management",
        description: "The total length of fencing erected.",
        displayType: "",
        name: "lengthOfFence",
        aggregationType: "SUM",
        label: "Total length of fence (Km)",
        units: "kilometres",
        gmsId: "FNC"
    },
    {
        output: "Fence Details",
        isOutputTarget: true,
        category: "Biodiversity Management",
        listName: "",
        description: "The area in hectares enclosed within a protective fence",
        displayType: "",
        name: "fenceAreaProtected",
        aggregationType: "SUM",
        label: "Area protected by fencing (Ha)"
    }
];

var projects = db.project.find({});

var badTargets = [];
while (projects.hasNext()) {
    var project = projects.next();

    if (project.outputTargets) {
        for (var i=0; i<project.outputTargets.length; i++) {
            var target = project.outputTargets[i];

            if (!target.hasOwnProperty('outcomeTarget')) {

                var found = false;
                for (var j=0; j<targets.length; j++) {
                    if (targets[j].output === target.outputLabel && targets[j].label === target.scoreLabel) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    print("Project "+project.projectId+" has output target that doesn't match");
                    badTargets.push(target);
                }
            }
        }
    }
}

var distinctBadTargets = [];
for (var k=0; k<badTargets.length; k++) {

    var found = false;
    for (var l=0; l<distinctBadTargets.length; l++) {
        if (distinctBadTargets[l].outputLabel === badTargets[k].outputLabel && distinctBadTargets[l].scoreLabel === badTargets[k].scoreLabel) {
            found = true;
            break;
        }
    }
    if (!found) {
        distinctBadTargets.push(badTargets[k]);
    }

}

printjson(distinctBadTargets);