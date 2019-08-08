var outputs = db.output.find({name:'RLP - Negotiations', 'data.negotiations':{$exists:true}, status:'active'});

while (outputs.hasNext()) {
    var output = outputs.next();

    for (var i=0; i<output.data.negotiations.length; i++) {
        var neg = output.data.negotiations[i];
        var activity = db.activity.find({activityId:output.activityId}).next();
        if (neg.groupsOrIndividuals == 'Groups') {
            neg.numberOfGroupsNegotiatedWith = neg.numberOfGroupsOrIndividuals;
            neg.numberOfIndividualsNegotiatedWith = 0;

            print("Updated project "+activity.projectId+" "+activity.description+" "+neg.groupsOrIndividuals+" "+neg.numberOfGroupsOrIndividuals);

            delete neg.groupsOrIndividuals;
            delete neg.numberOfGroupsOrIndividuals;

            db.output.save(output);

        }
        else if (neg.groupsOrIndividuals == 'Individuals') {
            neg.numberOfGroupsNegotiatedWith = 0;
            neg.numberOfIndividualsNegotiatedWith = neg.numberOfGroupsOrIndividuals;


            print("Updated project "+activity.projectId+" "+activity.description+" "+neg.groupsOrIndividuals+" "+neg.numberOfGroupsOrIndividuals);

            delete neg.groupsOrIndividuals;
            delete neg.numberOfGroupsOrIndividuals;

            db.output.save(output);
        }
        else {
            print("Warn: "+neg.groupsOrIndividuals);
        }
    }
}

