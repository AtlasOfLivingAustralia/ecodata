load("uuid.js");

var programs = db.program.find({status:{$ne:'deleted'}});
while (programs.hasNext()) {
    var program = programs.next();
    if (!program.config) {
        program.config = {};
    }
    if (!program.config.excludes) {
        program.config.excludes = [];
    }
    if (program.name != 'Regional Land Partnerships') {

        if (program.config.excludes) {
            program.config.excludes.push('DATA_SETS');
        }
        else {
            program.config.excludes = ['DATA_SETS'];
        }
    }

    db.program.save(program);
}


var name = 'Regional Fund for Wildlife and Habitat Bushfire Recovery (the Regional Fund) - States';

var programQuery = db.program.find({name: name});
var program;
if (programQuery.hasNext()) {
    program = programQuery.next();
    print("Program "+name+" already exists")
} else {
    print("Creating program "+name)
    var parent = db.program.find({name:'Bushfire Recovery for Species and Landscapes Program'}).next();
    program = db.program.find({name: 'Competitive Grants Tranche 1'}).next();
    delete program._id;
    program.parent = parent._id;
    program.programId = UUID.generate();

    program.name = name;

    db.program.save(program);
}

program.config.excludeFinancialYearData = true;
program.config.activities = [
    {
        "name": "Herbivore and/or predator control"
    },
    {
        "name": "Weed control and/or revegetation"
    },
    {
        "name": "Fire management and planning"
    },
    {
        "name": "Species and ecological community specific interventions"
    },
    {
        "name": "Traditional Owner led healing of country"
    },
    {
        "name": "Erosion control"
    },
    {
        "name": "Refugia management"
    }
];
program.config.excludes = [];
program.config.meriPlanContents = [
    {
        "template": "assets",
        "model": {
            "placeHolder": "[Free text; for species please enter common and scientific name; one asset per line]"
        }
    },
    {
        "template" : "activities",
        "model" : {
            "singleSelection" : true,
            "noneSelectedMessage" : "No priority actions have been nominated for this project",
            "title" : "Priority actions",
            "explanation" : "Please select from the drop-down options which of the following regional investment strategy objectives are applicable to this project"
        }
    },
    {
        "template": "outcomeStatements",
        "model": {
            "subtitle": "Please provide short term outcome statements. Short term outcomes statements should: <br/>- Contribute to the regional investment strategy;<br/>- Outline the degree of impact having undertaken the actions within the project timeframe;<br/>- Be expressed as a SMART statement (Specific, Measurable, Attainable, Realistic and Time-bound); and<br/>- Ensure the outcomes are measurable with consideration to the monitoring methodology provided below.",
            "placeholder": "By 30 June 2021, [Free text]",
            "title": "Short term outcome statements"
        }
    },
    {
        "template": "description",
        "model": {
            "maxSize": "1000",
            "placeholder": "[Free text; limit response to 1000 characters (approx. 150 words)]",
            "explanation": " Please provide a short description of this project. The project description should be succinct and state what will be done and why it will be done. This project description will be visible on the project overview page in MERIT"
        }
    },
    {
        "template": "projectMethodology",
        "model": {
            "maxSize": "4000",
            "title": "Project methodology",
            "tableHeading": "Please describe the methodology that will be used to achieve the project’s short-term outcome statements.",
            "placeHolder": "[Free text; limit response to 4000 characters (approx. 650 words)]"
        }
    },
    {
        "template": "monitoringIndicators",
        "model": {
            "approachHeading": "Describe the project monitoring indicator(s) approach",
            "indicatorHeading": "Identify the project monitoring indicator(s)",
            "indicatorHelpText": "List the measurable indicators of project success that will be monitored. Indicators should link back to the outcome statements and have units of measure. Indicators should measure both project outputs (e.g. area (ha) of rabbit control, length (km) of predator proof fencing) and change the project is aiming to achieve (e.g. Change in abundance of X threatened species at Y location, Change in vegetation cover (%), etc).",
            "approachHelpText": "How will the indicator be monitored? Briefly describe the method to be used to monitor the indicator (including timing of monitoring, who will collect/collate / analyse data, etc)",
            "indicatorPlaceHolder": "[Free text]",
            "approachPlaceHolder": "[Free text]",
            "title": "Project Monitoring Indicators"
        }
    },
    {
        "template": "adaptiveManagement",
        "model": {
            "title": "Project Review, Evaluation and Improvement Methodology and Approach",
            "explanation": "Outline the methods and processes that will enable adaptive management during the lifetime of this project"
        }
    },
    {
        "template": "projectPartnerships",
        "model": {
            "namePlaceHolder": "[Free text]",
            "partnershipPlaceHolder": "[Free text]"
        }
    },
    {
        "template": "consultation",
        "model": {
            "title":"Consultation",
            "placeHolder": "[Free text]",
            "explanation": "Please provide details of consultation with relevant state / territory agencies and NRM organisations to identify any duplication between activities proposed in the Activity and any other government-funded actions already underway in the project location. Where duplication has been identified, please describe how this has been resolved. If a modification to the Activity is required, you must submit a written request for a variation to the Department."
        }
    },
    {
        "template": "serviceTargets",
        "model": {
            "title":"Activities and minimum targets",
            "serviceName": "Activity"
        }
    },
    {
        "template": "meriBudget",
        "model": {
            "title":"Activities and minimum targets",
            "serviceName": "Activity",
            "showActivityColumn": true,
            "itemName": "Budget item"
        }
    }
];
program.config.projectReports = [
    {
        "reportType": "Activity",
        "reportDescriptionFormat": "Progress Report %1d",
        "reportNameFormat": "Progress Report %1d",
        "description": "",
        "category": "Progress Reports",
        "reportingPeriodInMonths": 6,
        "activityType": "State Intervention Progress Report",
        "reportsAlignedToCalendar": true,
        "firstReportingPeriodEnd": "2021-06-30T14:00:00Z",
        "canSubmitDuringReportingPeriod": true
    },
    {
        "reportType": "Single",
        "firstReportingPeriodEnd": "2022-06-30T14:00:00Z",
        "reportDescriptionFormat": "Final Report",
        "reportNameFormat": "Final Report",
        "reportingPeriodInMonths": 18,
        "multiple": false,
        "description": "",
        "category": "Final Report",
        "reportsAlignedToCalendar": false,
        "activityType": "Final Report"
    }
];

db.program.save(program);

var name = 'Regional Fund for Wildlife and Habitat Bushfire Recovery (the Regional Fund) - NRM';


var programQuery = db.program.find({name: name});
var program;
if (programQuery.hasNext()) {
    program = programQuery.next();
    print("Program "+name+" already exists")
} else {
    print("Creating program "+name)
    var parent = db.program.find({name:'Bushfire Recovery for Species and Landscapes Program'}).next();
    program = db.program.find({name: 'Regional Fund for Wildlife and Habitat Bushfire Recovery (the Regional Fund) - States'}).next();
    delete program._id;
    program.parent = parent._id;
    program.programId = UUID.generate();

    program.name = name;

    db.program.save(program);
}
program.config.excludeFinancialYearData = true;
delete program.config.activities;
program.config.excludes = [];
program.config.meriPlanContents = [
    {
        "template":"programOutcome"
    },
    {
        "template":"additionalOutcomes"
    },
    {
        "template": "assets",
        "model": {
            "placeHolder": "[Free text; for species please enter common and scientific name; one asset per line]"
        }
    },
    {
        "template": "outcomeStatements",
        "model": {
            "subtitle": "Please provide short term outcome statements. Short term outcomes statements should: <br/>- outline the degree of impact having undertaken the actions within the project timeframe;<br/>- be expressed as a SMART statement (Specific, Measurable, Attainable, Realistic and Time-bound); and<br/>- ensure the outcomes are measurable with consideration to the monitoring methodology provided below.",
            "placeholder": "By 30 June 2021, [Free text]",
            "title": "Short term outcome statements"
        }
    },
    {
        "template" : "name",
        "model" : {
            "placeHolder" : "[150 characters]"
        }
    },
    {
        "template": "description",
        "model": {
            "maxSize": "1000",
            "placeholder": "[Free text; limit response to 1000 characters (approx. 150 words)]",
            "explanation": " Please provide a short description of this project. This project description will be visible on the project overview page in MERIT"
        }
    },
    {
        "template": "projectMethodology",
        "model": {
            "maxSize": "4000",
            "title": "Project methodology",
            "tableHeading": "Please describe the methodology that will be used to achieve the project's short term outcome statements. To help demonstrate best practice delivery approaches and cost effectiveness of methodologies used, include details of the specific delivery mechanisms to leverage change (e.g. delivery method, approach and justification)",
            "placeHolder": "[Free text; limit response to 4000 characters (approx. 650 words)]"
        }
    },
    {
        "template": "monitoringBaseline"
    },
    {
        "template": "monitoringIndicators",
        "model": {
            "approachHeading": "Describe the project monitoring indicator(s) approach",
            "indicatorHeading": "Identify the project monitoring indicator(s)",
            "indicatorHelpText": "List the measurable indicators of project success that will be monitored. Indicators should link back to the outcome statements and have units of measure. Indicators should measure both project outputs (e.g. area (ha) of rabbit control, length (km) of predator proof fencing) and change the project is aiming to achieve (e.g. Change in abundance of X threatened species at Y location, Change in vegetation cover (%), etc).",
            "approachHelpText": "How will the indicator be monitored? Briefly describe the method to be used to monitor the indicator (including timing of monitoring, who will collect/collate / analyse data, etc)",
            "indicatorPlaceHolder": "[Free text]",
            "approachPlaceHolder": "[Free text]",
            "title": "Project Monitoring Indicators"
        }
    },
    {
        "template": "adaptiveManagement",
        "model": {
            "title": "Project Review, Evaluation and Improvement Methodology and Approach",
            "explanation": "Outline the methods and processes that will enable adaptive management during the lifetime of this project"
        }
    },
    {
        "template": "projectPartnerships",
        "model": {
            "namePlaceHolder": "[Free text]",
            "partnershipPlaceHolder": "[Free text]"
        }
    },
    {
        "template": "consultation",
        "model": {
            "title":"Consultation",
            "placeHolder": "[Free text]",
            "explanation": "Please provide details of consultation with relevant state / territory agencies and NRM organisations to identify any duplication between activities proposed in the Activity and any other government-funded actions already underway in the project location. Where duplication has been identified, please describe how this has been resolved. If a modification to the Activity is required, you must submit a written request for a variation to the Department."
        }
    },
    {
        "template": "serviceTargets",
        "model": {
            "title":"Activities and minimum targets",
            "serviceName": "Activity"
        }
    },
    {
        "template": "meriBudget",
        "model": {
            "title":"Activities and minimum targets",
            "serviceName": "Activity",
            "showActivityColumn": true,
            "itemName": "Budget item"
        }
    }
];
program.outcomes = [
    {
        "priorities": [
            {
                "category": "Ramsar"
            }
        ],

        "type":"secondary",
        "targeted": true,
        "shortDescription": "Ramsar Sites",
        "category": "environment",
        "outcome": "1. By 2023, there is restoration of, and reduction in threats to, the ecological character of Ramsar sites, through the implementation of priority actions"
    },
    {
        "priorities": [
            {
                "category": "Threatened Species"
            }
        ],
        "targeted": true,
        "shortDescription": "Threatened Species Strategy",
        "category": "environment",
        "outcome": "2. By 2023, the trajectory of species targeted under the Threatened Species Strategy, and other EPBC Act priority species, is stabilised or improved."
    },
    {
        "priorities": [
            {
                "category": "World Heritage Sites"
            }
        ],
        "targeted": true,
        "shortDescription": "World Heritage Areas",
        "category": "environment",
        "outcome": "3. By 2023, invasive species management has reduced threats to the natural heritage Outstanding Universal Value of World Heritage properties through the implementation of priority actions."
    },
    {
        "priorities": [
            {
                "category": "Threatened Ecological Communities"
            }
        ],
        "targeted": true,
        "shortDescription": "Threatened Ecological Communities",
        "category": "environment",
        "outcome": "4. By 2023, the implementation of priority actions is leading to an improvement in the condition of EPBC Act listed Threatened Ecological Communities."
    },
    {
        "priorities": [
            {
                "category": "Land Management"
            }
        ],
        "type":"secondary",
        "targeted": true,
        "shortDescription": "Soil Condition",
        "category": "agriculture",
        "outcome": "5. By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation."
    },
    {
        "priorities": [
            {
                "category": "Sustainable Agriculture"
            }
        ],
        "type":"secondary",
        "shortDescription": "Climate / Weather Adaption",
        "category": "agriculture",
        "outcome": "6. By 2023, there is an increase in the capacity of agriculture systems to adapt to significant changes in climate and market demands for information on provenance and sustainable production."
    }
];
program.config.projectReports=[];
db.program.save(program);