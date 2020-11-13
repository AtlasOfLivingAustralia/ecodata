mr = db.runCommand({
    "mapreduce" : "project",
    "map" : function() {
        if (this.status != 'deleted') {
            for (var key in this) { emit(key, null); }
        }

    },
    "reduce" : function(key, stuff) { return null; },
    "out": "project" + "_keys"
});
db.project_keys.distinct('_id');