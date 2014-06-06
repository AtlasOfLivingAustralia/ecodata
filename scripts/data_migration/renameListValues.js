load('renameListValue.js');

// Admin activities
renameNestedListValue('Administration Activities', 'adminActions', 'adminActionType', 'Submit project progress / annual/final report', 'Submit project progress / annual / final report');

// Revegetation Details
renameListValue('Revegetation Details', 'connectivityIndex', 'Isolated forest or woodland remnant', 'Isolated forest or woodland remnant >1km from other remnants');
renameListValue('Revegetation Details', 'connectivityIndex', 'Isolated grassland', 'Isolated grassland >1km from other remnants');
renameListValue('Revegetation Details', 'connectivityIndex', 'Connected to patch of more than 1000 ha', 'Patch <1km from a patch of more than 1000ha');
renameListValue('Revegetation Details', 'connectivityIndex', 'Connected to patch of 100 to 1000 ha', 'Patch <1km from a patch of 100 to 1000ha');
renameListValue('Revegetation Details', 'connectivityIndex', 'Connected to patch of 25 to 100 ha', 'Patch <1km from a patch of 25 to 100ha');
renameListValue('Revegetation Details', 'connectivityIndex', 'Connected to patch of less than 25 ha', 'Patch <1km from a patch of less than 25ha');

renameArrayTypedListValue('Revegetation Details', 'environmentalBenefits', 'Habitat restoration - home range improvement', 'Home range / extent improvement');
renameArrayTypedListValue('Revegetation Details', 'environmentalBenefits', 'Habitat enhancement - improved migration paths', 'Improved habitat connectivity');
renameArrayTypedListValue('Revegetation Details', 'environmentalBenefits', 'Streambank protection', 'Riparian rehabilitation');

// Site planning details
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Infrastructure Establishment', 'Public Access and Infrastructure');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Other Conservation Works for Threatened Species', 'Conservation Works for Threatened Species');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Pest and Disease Management', 'Pest Management');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Public Access Management', 'Public Access and Infrastructure');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Fauna (biological) survey', 'Fauna survey - general');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Flora (biological) survey', 'Flora survey - general');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Pest animal assessment', 'Pest Animal Assessment');

// Pest Monitoring Details
renameNestedListValue('Pest Observation and Monitoring Details', 'pestObservationMonitoringDetails', 'pestPopulationDensityClass', '< 10 individuals / Ha', '< 10 individuals or colonies / Ha');
renameNestedListValue('Pest Observation and Monitoring Details', 'pestObservationMonitoringDetails', 'pestPopulationDensityClass', '11 - 100 individuals / Ha', '11 - 100 individuals or colonies / Ha');
renameNestedListValue('Pest Observation and Monitoring Details', 'pestObservationMonitoringDetails', 'pestPopulationDensityClass', '101 - 1000 individuals / Ha', '101 - 1000 individuals or colonies / Ha');

// Plan Management Details


// Water management details
renameArrayTypedListValue('Water Management Details', 'hydrologicalStructuresOnsite', 'Constructed channel', 'Channel - constructed');
renameArrayTypedListValue('Water Management Details', 'hydrologicalStructuresOnsite', 'Natural channel', 'Channel - natural');
renameArrayTypedListValue('Water Management Details', 'hydrologicalStructuresOnsite', 'Habitat management for enhancing species resilience & breeding', 'Enhancing species resilience & breeding');

// Stock Management Details
renameArrayTypedListValue('Stock Management Details', 'stockManagementReason', 'Other (describe in notes)', 'Other (specify in notes)');

// Other conservation works
renameNestedListValue('Conservation Works Details', 'conservationWorks', 'conservationActionType', 'Ex-situ breeding program', 'Ex-situ breeding / propagation program');
renameNestedListValue('Conservation Works Details', 'conservationWorks', 'protectionMechanism', 'Termed agreement not on title binding (eg Land management Agreement)', 'Termed agreement not on title - binding (eg Land management agreement)');
renameNestedListValue('Conservation Works Details', 'conservationWorks', 'protectionMechanism', 'Not on title non-binding (eg Wildlife Refuge)', 'Not on title - non-binding (eg Wildlife Refuge)');

// Site preparation actions
renameNestedListValue('Site Preparation Actions', 'actionsList', 'groundPreparationWorks', 'Herbicide only', 'Herbicide broadscale only');
renameNestedListValue('Site Preparation Actions', 'actionsList', 'groundPreparationWorks', 'Herbicide and rip', 'Herbicide broadscale and rip');
renameNestedListValue('Site Preparation Actions', 'actionsList', 'associatedActivity', 'Infrastructure Establishment', 'Public Access and Infrastructure');
renameNestedListValue('Site Preparation Actions', 'actionsList', 'associatedActivity', 'Public Access Management', 'Public Access and Infrastructure');

// Evidence of Weed Management
renameArrayTypedListValue('Evidence of Weed Treatment', 'evidenceOfPreviousWeedTreatment', 'Root/stem rot present', 'Root / stem rot present');
renameArrayTypedListValue('Evidence of Weed Treatment', 'recommendedWeedTreatment', 'Manual control - Grubbing/chipping', 'Manual control - Grubbing / chipping');

// Erosion Management Details
renameArrayTypedListValue('Erosion Management Details', 'erosionTreatmentMethod', 'Farming practice change - improved crop management (contour plowing/bunding)', 'Farming practice change - improved crop management (contour plowing / bunding)');
renameArrayTypedListValue('Erosion Management Details', 'erosionType', 'Mass movement - landslide/landslip', 'Mass movement - landslide / landslip');
renameArrayTypedListValue('Erosion Management Details', 'erosionControlStructures', 'Channel/bank lining - solid barrier', 'Channel / bank lining - solid barrier');
renameArrayTypedListValue('Erosion Management Details', 'erosionControlStructures', 'Channel/bank lining - cellular confinement systems', 'Channel / bank lining - cellular confinement systems');
