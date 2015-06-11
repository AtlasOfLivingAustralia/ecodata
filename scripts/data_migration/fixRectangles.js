function representsRectangle(arr) {
    // must have 5 points
    if (arr.length !== 5) {
        return false;
    }

    if (arr[0][0] != arr[1][0]) {
        return false;
    }
    if (arr[2][0] != arr[3][0]) {
        return false;
    }
    if (arr[0][1] != arr[3][1]) {
        return false;
    }
    if (arr[1][1] != arr[2][1]) {
        return false;
    }
    return true
}

var sites = db.site.find({'extent.source':'drawn'});
while (sites.hasNext()) {
    var site = sites.next();
    var coordinates = site.extent.geometry.coordinates;
    if (coordinates.length == 1) {
        coordinates = coordinates[0];
    }
    if (representsRectangle(coordinates)) {
        if (coordinates[3][0] == coordinates[4][0] && coordinates[3][1] == coordinates[4][1]) {
            print("incorrectly closed polygon for site: "+site.siteId);
            coordinates[4][0] = coordinates[0][0];
            coordinates[4][1] = coordinates[0][1];

            db.site.update({siteId:site.siteId}, {$set:{'extent.geometry.coordinates':[coordinates]}});
        }

    }

}