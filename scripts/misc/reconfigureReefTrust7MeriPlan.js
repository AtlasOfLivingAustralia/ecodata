function updateProgramConfig(programName, meriPlanContents, meriPlanTemplate){
    var programQuery = db.program.find({name: programName});
    if (programQuery.hasNext()){
        var program = programQuery.next();
        program.config.meriPlanContents = meriPlanContents
        program.config.meriPlanTemplate = meriPlanTemplate
        db.program.save(program);
    }

}

var programName1 = "Reef Trust 7 - Water Quality"
var programname2 = "Reef Trust 7 - Coastal Habitat and Species"
var meriPlanTemplate = "configurableMeriPlan"
var meriPlanContent = [
    {
        "template": "programOutcome"
    },
    {
        "template": "additionalOutcomes"
    },
    {
        "template": "outcomeStatements",
        "model": {
            "helpText": "Short term outcomes statements should: <br/><ul> <li>Contribute to the 5-year Outcome (e.g. what degree of impact are you expecting from the Project's interventions )</li> <li>Outline the degree of impact having undertaken the Services for  up to 3 years, for example 'area of relevant vegetation type has increased'.</li><li> Be expressed as a SMART statement (Specific, Measurable, Attainable, Realistic and Time-bound). Ensure the outcomes are measurable with consideration to the monitoring methodology provided below.</li></ul><b>Please Note: </b> for Project three years or less in duration, a short-term Project outcome achievable at the Project's completion must be set.",
            "subtitle": "Short-terms outcome statement/s",
            "title": "Project Outcomes"
        }
    },
    {
        "template": "mediumTermOutcomes"
    },
    {
        "template": "sectionHeading",
        "model": {
            "heading": "Project Details"
        }
    },

    {
        "template": "name",
        "model": {
            "placeHolder": "[150 Characters]",
            "tableFormatting": true
        }
    },
    {
        "template": "description",
        "model": {
            "placeHolder": "Please provide a short description of this project. This project description will be visible on the project overview page in MERIT [Free text, limit response to 1000 characters (approx. 150 words)]",
            "maxSize":"1000",
            "tableFormatting": true

        }
    },
    {
        "template": "rationale"
    },
    {
        "template": "keyThreats"
    },
    {
        "template": "projectMethodology",
        "model": {
            "helpText": "Describe the methodology that will be used to achieve the project outcomes. To help demonstrate best practice delivery approaches and cost effectiveness of methodologies used, include details of the specific delivery mechanisms to leverage change (e.g. delivery method, approach and justification, and any assumptions).",
            "maxSize": "4000",
            "tableHeading": "Project methodology (4000 character limit [approx 650 words])",
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
            "monitoringValidation": true,
            "indicatorHeading": "Project monitoring indicators"
        }
    },
    {
        "template": "projectReview"
    },
    {
        "template": "nationalAndRegionalPlans"
    },
    {
        "template": "serviceTargets",
        "model": {
            "title": "Project services and minimum targets",
            "serviceName": "Activity"
        }
    }
];
print("Configuring Meri Plan for this : "+ programName1 + "program")
updateProgramConfig(programName1, meriPlanContent, meriPlanTemplate)
print("Configuring Meri Plan for this : "+ programname2 + "program")
updateProgramConfig(programname2, meriPlanContent, meriPlanTemplate)

