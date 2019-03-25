var scores = db.score.find({category:'RLP'});
while (scores.hasNext()) {
    var score = scores.next();

    var adjustmentConfiguration = {
        filter: {
            filterValue: 'RLP - Output Report Adjustment',
            type:'filter',
            property:'name'
        },
        childAggregations: [
            {
                filter: {
                    filterValue: score.scoreId,
                    type:'filter',
                    property:'data.adjustments.scoreId'
                },
                childAggregations: [
                    {
                        property:'data.adjustments.adjustment',
                        type:'SUM'
                    }
                ]

            }
        ]
    };

    var original = score.configuration;
    if (original.childAggregations.length > 1) {
        throw "Uhoh";
    }

    score.configuration = {
        label: original.label,
        childAggregations : [
            adjustmentConfiguration,
            {
                filter:original.filter,
                childAggregations: original.childAggregations
            }
        ]
    };

    db.score.save(score);

}
