var espActivityTypes = ['ESP PMU or Zone reporting', 'ESP SMU Reporting'];
for (var i=0; i<espActivityTypes.length; i++) {
    var activityType = espActivityTypes[i];

    var activities = db.activity.find({type:activityType});
    while (activities.hasNext()) {
        var act = activities.next();

        if (act.siteId) {
            var site = db.site.find({siteId:act.siteId});
            if (site.hasNext()) {
                var s = site.next();
                act.description = act.description + " for site "+s.name;

                db.activity.save(act);
            }
            else {
                print("WARNING: No site found with site id "+act.siteId);
            }
        }
        else {
            print("WARNING: No site id for activity "+act.activityId);
        }
    }

}
