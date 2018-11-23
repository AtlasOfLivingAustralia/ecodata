var outputs = db.output.find({'data.grazingPeriods.growthStage':'Hoggart'});
while (outputs.hasNext()) {
    var output = outputs.next();
    for (var i=0; i<output.data.grazingPeriods.length; i++) {
        if (output.data.grazingPeriods[i].growthStage == 'Hoggart') {
            output.data.grazingPeriods[i].growthStage = 'Hogget';
        }
    }
    db.output.save(output);
}