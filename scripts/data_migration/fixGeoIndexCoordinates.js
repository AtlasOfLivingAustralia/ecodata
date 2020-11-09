var sites = db.site.find({status: 'active', geoIndex: {$ne: null}});
print("Number of sites to check - " + sites.count());

var count = 0,
    checkingSite;

while (sites.hasNext()) {
    var site = sites.next();
    checkingSite = site.siteId;
    convertToNumber(site.geoIndex.coordinates, site);
    db.site.save(site);
}

function convertToNumber (coordinates, site) {
    if (coordinates) {
        for (var i = 0; i < coordinates.length; i++) {
            var number = coordinates[i],
                type = typeof number;
            switch (type) {
                case "object":
                    convertToNumber(number, site);
                    break;
                case "string":
                    coordinates[i] = Number.parseFloat(number);
                    if (checkingSite == site.siteId) {
                        count ++;
                    }
                    else {
                        checkingSite = "";
                    }
                    break;
            }
        }

        return coordinates;
    }
}

print("Number of sites converted - " + count);