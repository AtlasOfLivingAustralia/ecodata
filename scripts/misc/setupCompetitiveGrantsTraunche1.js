load("uuid.js");

var name = 'Competitive Grants Tranche 1';

var programQuery = db.program.find({name: name});
var program;
if (programQuery.hasNext()) {
    program = programQuery.next();
} else {
    program = db.program.find({name: 'Greening Australia'}).next();
    delete program._id;
    program.programId = UUID.generate();

    program.name = name;

    db.program.save(program);
}
program.config.excludeFinancialYearData = true;
program.config.meriPlanContents = [
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
        "template": "monitoringIndicators",
        "model": {
            "approachHeading": "Describe the project monitoring indicator(s) approach",
            "indicatorHeading": "Identify the project monitoring indicator(s)",
            "indicatorHelpText": "",
            "approachHelpText": "",
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

db.program.save(program);