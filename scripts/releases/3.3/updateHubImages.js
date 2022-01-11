

function modifyImageUrlIfNecessary(imageUrl) {

    if ((imageUrl.indexOf("https://ecodata") >= 0) && imageUrl.indexOf('/uploads') > 0) {
        return '/document/download' + imageUrl.substring(imageUrl.indexOf('/uploads')+8);
    }
    return imageUrl;
}

var hubs = db.hub.find({status:{$ne:'deleted'}});
while (hubs.hasNext()) {
    var hub = hubs.next();
    var changed = false;
    print("Processing hub: "+hub.urlPath);
    if (hub.bannerUrl) {
        var newUrl = modifyImageUrlIfNecessary(hub.bannerUrl);
        if (newUrl != hub.bannerUrl) {
            print("Updating bannerUrl: "+hub.bannerUrl+" to "+newUrl);
            hub.bannerUrl = newUrl;
            changed = true;
        }
    }
    if (hub.logoUrl) {
        newUrl = modifyImageUrlIfNecessary(hub.logoUrl);
        if (newUrl != hub.logoUrl) {
            print("Updating logoUrl: "+hub.logoUrl+" to "+newUrl);
            hub.logoUrl = newUrl;
            changed = true;
        }
    }
    if (hub.templateConfiguration && hub.templateConfiguration.banner && hub.templateConfiguration.banner.images) {
        for (var i=0; i<hub.templateConfiguration.banner.images.length; i++) {
            if (hub.templateConfiguration.banner.images[i].url) {
                newUrl = modifyImageUrlIfNecessary(hub.templateConfiguration.banner.images[i].url);
                if (newUrl != hub.templateConfiguration.banner.images[i].url) {
                    print("Updating banner image url: "+hub.templateConfiguration.banner.images[i].url+" to "+newUrl);
                    hub.templateConfiguration.banner.images[i].url = newUrl;
                    changed = true;
                }
            }

        }
    }

    if (changed) {
        db.hub.save(hub);
    }
}