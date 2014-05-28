var outputs = db.output.find({name:'Event Details'});
while (outputs.hasNext()) {
    var output = outputs.next();
    output.data.events= [];

    var purposes = output.data.eventPurpose;
    var hoursPerEvent = 0;
    if (output.data.eventDurationHrs) {
        hoursPerEvent = output.data.eventDurationHrs / purposes.length;
    }

    for (var i=0; i<purposes.length; i++) {
        var event = {};
        event.eventType = 'Other (specify in notes)';
        event.eventPurpose = purposes[i];
        event.eventTopics = output.data.eventTopics;
        event.eventDurationHrs = hoursPerEvent;
        event.industryType = output.data.industryType;

        output.data.events.push(event);
    }

    delete output.data.eventPurpose;
    delete output.data.eventTopics;
    delete output.data.eventDurationHrs;
    delete output.data.industryType;

    db.output.save(output);

}