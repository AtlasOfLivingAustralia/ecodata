load("uuid.js");

let outcomes = [
    {
        "priorities": [
            {
                "category": "Land Management"
            }
        ],
        "targeted": true,
        "shortDescription": "Soil Condition",
        "supportsMultiplePrioritiesAsPrimary": true,
        "type": "primary",
        "category": "agriculture",
        "outcome": "5. By 2023, there is an increase in the awareness and adoption of land management practices that improve and protect the condition of soil, biodiversity and vegetation."
    },
    {
        "priorities": [
            {
                "category": "Sustainable Agriculture"
            }
        ],
        "shortDescription": "Climate / Weather Adaption",
        "supportsMultiplePrioritiesAsPrimary": true,
        "type": "primary",
        "category": "agriculture",
        "outcome": "6. By 2023, there is an increase in the capacity of agriculture systems to adapt to significant changes in climate and market demands for information on provenance and sustainable production."
    },
    {
        "priorities": [
            {
                "category": "Bushfires"
            }
        ],
        "shortDescription": "Bushfire Recovery",
        "type": "secondary",
        "category": "bushfires",
        "outcome": "Enhance the recovery and maximise the resilience of fire affected priority species, ecological communities and other natural assets within the seven regions impacted by the 2019-20 bushfires",
        "supportsMultiplePrioritiesAsSecondary": true
    }
];

let config = {
    "meriPlanContents": [
        {
            "template": "programOutcome",
            "model": {
                "maximumPriorities": "1000"
            }
        },
        {
            "template": "fdFund"
        },
        {
            "template": "sectionHeading",
            "model": {
                "heading": "Project Outcomes"
            }
        },
        {
            "template": "outcomeStatements",
            "model": {
                "helpText": "Please provide outcome statements. Outcomes statements should: <br/>- outline the degree of impact having undertaken the actions within the project timeframe;<br/>- be expressed as a SMART statement (Specific, Measurable, Attainable, Realistic and Time-bound); and<br/>- ensure the outcomes are measurable with consideration to the monitoring methodology provided below.",
                "subtitle": "Short-term outcome statement/s 1000 character limit [approx. 150 words]",
                "placeholder": "By 30 June 2021, [Free text]"
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
                "tableFormatting": true,
                "placeHolder": "[150 characters]"
            }
        },
        {
            "template": "description",
            "model": {
                "tableFormatting": true,
                "maxSize": "1000",
                "placeholder": "Please provide a short description of this project. This project description will be visible on the project overview page in MERIT [Free text; limit response to 1000 characters (approx. 150 words)]"
            }
        },
        {
            "template": "rationale",
            "model": {
                "tableFormatting": true,
                "maxSize": "3000"
            }
        },
        {
            "template": "projectMethodology",
            "model": {
                "maxSize": "4000",
                "title": "Project methodology",
                "tableHeading": "Please describe the methodology that will be used to achieve the project's outcome statements. To help demonstrate best practice delivery approaches and cost effectiveness of methodologies used, include details of the specific delivery mechanisms to leverage change (e.g. delivery method, approach and justification)",
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
                "indicatorHeading": "Project monitoring indicators",
                "indicatorHelpText": "List the measurable indicators of project success that will be monitored. Indicators should link back to the outcome statements and have units of measure. Indicators should measure both project outputs (e.g. area (ha) of rabbit control, length (km) of predator proof fencing) and change the project is aiming to achieve (e.g. Change in abundance of X threatened species at Y location, Change in vegetation cover (%), etc).",
                "approachHelpText": "How will the indicator be monitored? Briefly describe the method to be used to monitor the indicator (including timing of monitoring, who will collect/collate / analyse data, etc)",
                "indicatorPlaceHolder": "[Free text]",
                "approachPlaceHolder": "[Free text]"
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
                "showTargetDate": true,
                "title": "Project services and minimum targets",
                "serviceName": "Service"
            }
        }
    ],
    "excludes": [],
    "visibility": "public",
    "organisationRelationship": "Service Provider",
    "excludeFinancialYearData": true,
    "requiresActivityLocking": true,
    "nrm": [
        {
            "name": "More primary producers adopt whole-of-system approaches to NRM to improve the natural resource base, for long-term productivity and landscape health (also and FDF Outcome)"
        },
        {
            "name": "Partnerships and engagement is built between stakeholders responsible for managing natural resources"
        },
        {
            "name": "More primary producers and agricultural communities are experimenting with adaptive or transformative NRM practices, systems and approaches that link and contribute to building drought resilience"
        },
        {
            "name": "Improved NRM in agricultural landscapes for increased capacity to prepare and respond to drought"
        }
    ],
    "fdf": [
        {
            "name": "More primary producers preserve natural capital while also improving productivity and profitability"
        },
        {
            "name": "More primary producers adopt risk management practices to improve their sustainability and resilience"
        },
        {
            "name": "More primary producers adopt whole-of-system approaches to NRM to improve the natural resource base, for long-term productivity and landscape health"
        }
    ],
    "projectTemplate": "rlp",
    "activityPeriodDescriptor": "Outputs report #",
    "meriPlanTemplate": "configurableMeriPlan",
    "riskAndThreatTypes": [
        "Performance",
        "Work Health and Safety",
        "People resources",
        "Financial",
        "External stakeholders",
        "Natural Environment"
    ],
    "projectReports": [
        {
            "reportType": "Activity",
            "firstReportingPeriodEnd": "2021-06-30T14:00:00Z",
            "reportDescriptionFormat": "Progress Report %1d",
            "reportNameFormat": "Progress Report %1d",
            "reportingPeriodInMonths": 6,
            "description": "",
            "category": "Progress Reports",
            "activityType": "State Intervention Progress Report",
            "reportsAlignedToCalendar": true,
            "canSubmitDuringReportingPeriod": true
        },
        {
            "firstReportingPeriodEnd": "2021-06-30T14:00:00Z",
            "reportType": "Administrative",
            "reportDescriptionFormat": "Annual Progress Report %2$tY - %3$tY for %4$s",
            "reportNameFormat": "Annual Progress Report %2$tY - %3$tY",
            "reportingPeriodInMonths": 12,
            "description": "",
            "category": "Annual Progress Reporting",
            "activityType": "Bushfires Annual Report"
        },
        {
            "reportType": "Single",
            "firstReportingPeriodEnd": "2021-06-30T14:00:00Z",
            "reportDescriptionFormat": "Outcomes Report 1 for %4$s",
            "reportNameFormat": "Outcomes Report 1",
            "reportingPeriodInMonths": 36,
            "multiple": false,
            "description": "Before beginning Outcomes Report 1, please go to the Data set summary tab and complete a form for each data set collected for this project. Help with completing this form can be found in Section 10 of the [RLP MERIT User Guide](http://www.nrm.gov.au/my-project/monitoring-and-reporting-plan/merit)",
            "category": "Outcomes Report 1",
            "reportsAlignedToCalendar": false,
            "activityType": "RLP Short term project outcomes"
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
    ],
    "navigationMode": "returnToProject"
};

let name = "Future Drought Fund";
var program = db.program.find({name: name});
var now = ISODate();
var p = {
    name: name, programId: UUID.generate(), dateCreated: now, lastUpdate: now
}
if (!program.hasNext()) {
    db.program.insert(p);
}else{
    print("Program Already Exist: " + name)
}

name = "Natural Resource Management - Landscape"

var program = db.program.find({name: name});
var parent = db.program.find({name: "Future Drought Fund"}).next();
var now = ISODate();
var p = {
    name: name, programId: UUID.generate(), dateCreated: now, lastUpdate: now, parent: parent._id
}
if (!program.hasNext()) {
    db.program.insert(p);
}else{
    print("Program Already Exist: " + name)
}
print("Adding config data into program:  " + name)
var program = db.program.find({name: name});
while(program.hasNext()){
    var p = program.next();
    p.config = config;
    p.outcomes = outcomes;
    db.program.save(p);
}
