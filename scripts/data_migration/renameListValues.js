load('renameListValue.js');

// Admin activities
renameNestedListValue('Administration Activities', 'adminActions', 'adminActionType', 'Submit project progress / annual/final report', 'Submit project progress / annual / final report');

// Site planning details
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Infrastructure Establishment', 'Public Access and Infrastructure');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Other Conservation Works for Threatened Species', 'Conservation Works for Threatened Species');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Pest and Disease Management', 'Pest Management');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Public Access Management', 'Public Access and Infrastructure');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Fauna (biological) survey', 'Fauna survey - general');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Flora (biological) survey', 'Flora survey - general');
renameNestedListValue('Site Planning Details', 'plannedActions', 'plannedActivityType', 'Pest animal assessment', 'Pest Animal Survey');

// Pest Monitoring Details
renameNestedListValue('Pest Observation and Monitoring Details', 'pestObservationMonitoringDetails', 'pestPopulationDensityClass', '< 10 individuals / Ha', '< 10 individuals or colonies / Ha');
renameNestedListValue('Pest Observation and Monitoring Details', 'pestObservationMonitoringDetails', 'pestPopulationDensityClass', '11 - 100 individuals / Ha', '11 - 100 individuals or colonies / Ha');
renameNestedListValue('Pest Observation and Monitoring Details', 'pestObservationMonitoringDetails', 'pestPopulationDensityClass', '101 - 1000 individuals / Ha', '101 - 1000 individuals or colonies / Ha');

// Plan Management Details
renameListValue('Plan Development Details', 'typeOfPlan', 'Property or Reserve plan', 'Property, Reserve or Site development plan');

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

// Evidence of Weed Treatment
renameArrayTypedListValue('Evidence of Weed Treatment', 'evidenceOfPreviousWeedTreatment', 'Root/stem rot present', 'Root / stem rot present');
renameArrayTypedListValue('Evidence of Weed Treatment', 'recommendedWeedTreatment', 'Manual control - Grubbing/chipping', 'Manual control - Grubbing / chipping');

// Erosion Management Details
renameArrayTypedListValue('Erosion Management Details', 'erosionTreatmentMethod', 'Farming practice change - improved crop management (contour plowing/bunding)', 'Farming practice change - improved crop management (contour plowing / bunding)');
renameArrayTypedListValue('Erosion Management Details', 'erosionType', 'Mass movement - landslide/landslip', 'Mass movement - landslide / landslip');
renameArrayTypedListValue('Erosion Management Details', 'erosionControlStructures', 'Channel/bank lining - solid barrier', 'Channel / bank lining - solid barrier');
renameArrayTypedListValue('Erosion Management Details', 'erosionControlStructures', 'Channel/bank lining - cellular confinement systems', 'Channel / bank lining - cellular confinement systems');

// Risk Assessment
renameNestedListValue('Threatening Processes & Site Condition Risks', 'riskTable', 'riskType', 'Fishing / aquaculture impacts - drifting ghost nets', 'Fishing / aquaculture impacts - drifting nets / debris');
renameNestedListValue('Threatening Processes & Site Condition Risks', 'riskTable', 'riskType', 'Mistletoe or other parasite/fungal infestations', 'Mistletoe or other parasite / fungal infestations');
renameNestedListValue('Threatening Processes & Site Condition Risks', 'riskTable', 'riskType', 'Soil compaction - animal/human/machinery', 'Soil compaction - animal / human / machinery');
renameNestedListValue('Threatening Processes & Site Condition Risks', 'riskTable', 'riskType', 'Soil nutrient enrichment/eutrophication', 'Soil nutrient enrichment / eutrophication');
renameNestedListValue('Threatening Processes & Site Condition Risks', 'riskTable', 'riskType', 'Weedicides/pesticides or chemical contamination', 'Weedicides / pesticides or chemical contamination');

// Planned Monitoring Approach
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Fauna survey (biological)', 'Fauna Survey - general');
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Flora survey (biological)', '');
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Flora Survey - general');
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Pest animal assessment & monitoring', 'Pest Animal Survey');
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Vegetation condition assessment - BioCondition (QLD)', 'Vegetation Assessment - Commonwealth government methodology');
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Vegetation condition assessment - Biodiversity Fund (DoE)', 'Vegetation Assessment - Commonwealth government methodology')
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Vegetation condition assessment - BioMetric (NSW)', 'Vegetation Assessment - Commonwealth government methodology')
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Vegetation condition assessment - Bushland Condition Monitoring (SA)', 'Vegetation Assessment - Commonwealth government methodology')
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Vegetation condition assessment - Habitat Hectares (VIC)', 'Vegetation Assessment - Commonwealth government methodology')
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Vegetation condition assessment - Native Vegetation Condition Assessment and Monitoring (WA)', 'Vegetation Assessment - Commonwealth government methodology')
renameNestedListValue('Planned Monitoring Approach', 'plannedActions', 'plannedActivityType', 'Vegetation condition assessment - TasVeg (TAS)', 'Vegetation Assessment - Commonwealth government methodology');

// Plant Propagation Details
renameNestedListValue('Plant Propagation Details', 'plantPropagation', 'germinationMethod', 'vegetative - cuttings - direct striking', 'Vegetative - cuttings - direct striking');
renameNestedListValue('Plant Propagation Details', 'plantPropagation', 'germinationMethod', 'vegetative - cuttings - pre-rooting', 'Vegetative - cuttings - pre-rooting');
renameNestedListValue('Plant Propagation Details', 'plantPropagation', 'germinationMethod', 'vegetative - layering', 'Vegetative - layering');
renameNestedListValue('Plant Propagation Details', 'plantPropagation', 'germinationMethod', 'vegetative - tissue culture', 'Vegetative - tissue culture');

// Evidence of Pest Management
renameArrayTypedListValue('Evidence of Pest Management Activity', 'previousPestTreatmentAssessmentMethod', 'Spotlight / daylight population survey - target species (specify details in notes)', 'Method-based population survey - target species abundance (specify method in notes)');
renameArrayTypedListValue('Evidence of Pest Management Activity', 'previousPestTreatmentAssessmentMethod', 'Spotlight / daylight population survey - food species (specify details in notes)', 'Method-based population survey - food species abundance (specify method in notes)');

