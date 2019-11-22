/**
 * Add to scores for initial and follow up Length of pest animal control
 */
load('uuid.js');

//current _id is long
var last_id = db.score.find().sort({_id: -1}).limit(1)

var uuid_followup_length = UUID.generate()
var followup_length_score = {
    "_id" : last_id++,
    "category" : "RLP",
    "configuration" : {
        "label" : "Length (km) treated for pest animals - follow-up",
        "childAggregations" : [
            {
                "filter" : {
                    "filterValue" : "RLP - Output Report Adjustment",
                    "type" : "filter",
                    "property" : "name"
                },
                "childAggregations" : [
                    {
                        "filter" : {
                            "filterValue" : uuid_followup_length,
                            "type" : "filter",
                            "property" : "data.adjustments.scoreId"
                        },
                        "childAggregations" : [
                            {
                                "property" : "data.adjustments.adjustment",
                                "type" : "SUM"
                            }
                        ]
                    }
                ]
            },
            {
                "filter" : {
                    "filterValue" : "RLP - Pest animal management",
                    "property" : "name",
                    "type" : "filter"
                },
                "childAggregations" : [
                    {
                        "filter" : {
                            "filterValue" : "Follow-up",
                            "property" : "data.areasControlled.initialOrFollowup",
                            "type" : "filter"
                        },
                        "childAggregations" : [
                            {
                                "property" : "data.areasControlled.lengthInvoicedKm",
                                "type" : "SUM"
                            }
                        ]
                    }
                ]
            }
        ]
    },
    "displayType" : "",
    "entity" : "Activity",
    "entityTypes" : [],
    "isOutputTarget" : true,
    "label" : "Length (km) treated for pest animals - follow-up",
    "outputType" : "RLP - Pest animal management",
    "scoreId" : uuid_followup_length,
    "status" : "active",
    "units" : ""
}

var uuid_init_length = UUID.generate()
var init_length_score = {
    "_id" : last_id++,
    "category" : "RLP",
    "configuration" : {
        "label" : "Length (km) treated for pest animals - initial",
        "childAggregations" : [
            {
                "filter" : {
                    "filterValue" : "RLP - Output Report Adjustment",
                    "type" : "filter",
                    "property" : "name"
                },
                "childAggregations" : [
                    {
                        "filter" : {
                            "filterValue" : uuid_init_length,
                            "type" : "filter",
                            "property" : "data.adjustments.scoreId"
                        },
                        "childAggregations" : [
                            {
                                "property" : "data.adjustments.adjustment",
                                "type" : "SUM"
                            }
                        ]
                    }
                ]
            },
            {
                "filter" : {
                    "filterValue" : "RLP - Pest animal management",
                    "property" : "name",
                    "type" : "filter"
                },
                "childAggregations" : [
                    {
                        "filter" : {
                            "filterValue" : "Initial",
                            "property" : "data.areasControlled.initialOrFollowup",
                            "type" : "filter"
                        },
                        "childAggregations" : [
                            {
                                "property" : "data.areasControlled.lengthInvoicedKm",
                                "type" : "SUM"
                            }
                        ]
                    }
                ]
            }
        ]
    },
    "displayType" : "",
    "entity" : "Activity",
    "entityTypes" : [],
    "isOutputTarget" : true,
    "label" : "Length (km) treated for pest animals - initial",
    "outputType" : "RLP - Pest animal management",
    "scoreId" : uuid_init_length,
    "status" : "active",
    "units" : "km"
}



db.score.insert(followup_length_score)
db.score.insert(init_length_score)

