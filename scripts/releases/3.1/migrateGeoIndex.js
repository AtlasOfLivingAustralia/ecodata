var sites = db.site.find({'geoIndex.type':'Circle'});
while (sites.hasNext()) {
    var site = sites.next();
    site.geoIndex.radius = site.geoIndex.radius.toString();
    db.site.save(site);
}

db.site.update({'extent.geometry.area':''}, {$unset:{'extent.geometry.area':true}}, {multi:true});
db.site.update({'extent.geometry.area':NumberLong(0)}, {$set:{'extent.geometry.area':0}}, {multi:true});

var sites = db.site.find({'geoIndex.coordinates':{$type:'string'}});

function migrateArray(coords, site) {

    for (var i=0; i<coords.length; i++) {
        if (Array.isArray(coords[i])) {
            migrateArray(coords[i])
        }
        else {
            coords[i] = Number(coords[i]);
            if (isNaN(coords[i])) {

                return false;
            }
        }

    }
    return true;
}

// Convert strings to numbers
while (sites.hasNext()) {
    var site = sites.next();

    var coordinates = site.geoIndex.coordinates;

    if (migrateArray(coordinates)) {
        db.site.save(site);
    }
    else {
        print("Warning: "+site+" has invalid coordinates ");
        printjson(site.geoIndex.coordinates);
    }


}
