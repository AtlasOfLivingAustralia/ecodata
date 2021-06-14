var projects = db.project.find({$or:[{'custom.details.events.scheduledDate':''}, {'custom.details.events.scheduledDate':'NaN-NaN-NaNTNaN:NaN:NaNZ'}]});
while (projects.hasNext()) {
    var project = projects.next();

    var events = project.custom.details.events;
    for (var i=0; i<events.length; i++) {
        if (events[i].scheduledDate == '' || events[i].scheduledDate == 'NaN-NaN-NaNTNaN:NaN:NaNZ') {
            events[i].scheduledDate = null;
        }
    }
    db.project.save(project);
}