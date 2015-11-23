
var projects = db.project.find({status:{$ne:'deleted'}, associatedSubProgram:/Target Area/});

while (projects.hasNext()) {
    var project = projects.next();
    var activities = db.activity.find({projectId:project.projectId, status:{$ne:'deleted'}, publicationStatus:'published'});

    while (activities.hasNext()) {
        var totals = {totalParticipantsNotEmployed:0, totalParticipantsNew:0};
        var activity = activities.next();
        var outputs = db.output.find({name:'Participant Information', activityId:activity.activityId, status:{$ne:'deleted'}});

        while (outputs.hasNext()) {
            var output = outputs.next();
            var tpne = output.data.totalParticipantsNotEmployed;
            if (typeof tpne === 'string') {
                tpne = parseFloat(tpne.replace(',',''))
            }
            totals.totalParticipantsNotEmployed += tpne || 0;
            var tpn = output.data.totalParticipantsNew;
            if (typeof tpn === 'string') {
                tpn = parseFloat(tpn.replace(",",''));
            }
            totals.totalParticipantsNew += tpn || 0;
        }

        if (totals.totalParticipantsNew || totals.totalParticipantsNotEmployed) {
            print(project.projectId+','+ activity.activityId+','+totals.totalParticipantsNotEmployed+','+totals.totalParticipantsNew);
        }

    }
}





