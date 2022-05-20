var updated = 0;
var hubs = db.hub.find({});
print(`Count ${hubs.count()}`)
while (hubs.hasNext()) {
    var hub = hubs.next();
    var updateProps = {},
        type;

    print(`Hub - ${hub.urlPath}`);

    if (hub.urlPath === 'merit' || hub.urlPath === undefined)
        continue;

    if (hub.skin !== 'bs4') {
        updateProps.skin = 'bs4';
        print(`Hub ${hub.urlPath} - ${hub.hubId} - has skin ${hub.skin}`);
    }

    type = hub.templateConfiguration && hub.templateConfiguration.header && hub.templateConfiguration.header.type;
    if (type === 'biocollect' || type === "") {
        updateProps["templateConfiguration.header.type"] = 'custom'
        print(`Hub ${hub.urlPath} - ${hub.hubId} - has header ${type}`);
    }

    copyStyleSheet(hub, updateProps);

    if (Object.keys(updateProps).length) {
        db.hub.update({hubId: hub.hubId}, {$set: updateProps});
        updated++;
    }
}

function copyStyleSheet (hub, updateProps) {
    var config = hub.templateConfiguration;
    var styles = config.styles;

    if(!styles.homepageButtonBackgroundColor && styles.navBackgroundColor){
        updateProps["templateConfiguration.styles.homepageButtonBackgroundColor"] = styles.navBackgroundColor;
    }

    if(!styles.homepageButtonTextColor && styles.navTextColor){
        updateProps["templateConfiguration.styles.homepageButtonTextColor"] = styles.navTextColor;
    }

    if(!styles.primaryDarkColor && styles.primaryButtonBackgroundColor) {
        updateProps["templateConfiguration.styles.primaryDarkColor"] = styles.primaryButtonBackgroundColor;
        updateProps["templateConfiguration.styles.primaryColor"] = styles.primaryButtonBackgroundColor;
    }

    if (!styles.darkColor && styles.defaultButtonBackgroundColor)
        updateProps["templateConfiguration.styles.darkColor"] = styles.defaultButtonBackgroundColor;
}

print(`Updated ${updated} hubs`);
