//var program = 'Biodiversity Fund';

var program = 'Caring for Our Country 2';
var subprogram = 'Regional Delivery 1318';
var projects = db.project.distinct('projectId', {associatedProgram:program, associatedSubProgram:subprogram, status:{$ne:'deleted'}});
var allEvents = [];
for (var i=0; i<projects.length; i++) {
    var projectId = projects[i]; 
    var messages = db.auditMessage.find({projectId:projectId, eventType:'Update', entityType:'au.org.ala.ecodata.Project', 'entity.planStatus':{$in:['approved', 'not approved', 'submitted']}
}).sort({date:1});
    var projectEvents = [];
    var status = 'none';
    var currentDate = ISODate("2000-01-01T00:00:00Z");
    while (messages.hasNext()) {
        var message = messages.next();
        var millis = currentDate.getTime();
        var messageMillis = message.date.getTime();
        var messageStatus = message.entity.planStatus;
        if (Math.abs(millis-messageMillis) > 60000 && status != messageStatus) {
            currentDate = message.date;
            status = message.entity.planStatus;
            projectEvents.push(      
               {date:message.date, projectId:message.projectId, status:message.entity.planStatus});
        }
    }
    allEvents.push(projectEvents);
}
print('Project ID, Status, Date Status Changed');
for (var j=0; j<allEvents.length; j++) {
    for (var k =0; k<allEvents[j].length; k++) {
        var e = allEvents[j][k];
        var s;
        if (e.status == 'not approved') s='Returned';
        if (e.status == 'approved') s='Approved';
        if (e.status == 'submitted') s='Submitted';
        print(e.projectId+','+s+','+e.date);
    }
}

