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
