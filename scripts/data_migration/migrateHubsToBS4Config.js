var updated = 0;
var hubs = db.hub.find({});
print(`Count ${hubs.count()}`)
while (hubs.hasNext()) {
    var hub = hubs.next();
    var updateProps = {},
        type, update = false;

    print(`Hub - ${hub.urlPath}`);

    if (hub.urlPath == 'merit')
        continue;

    if (hub.skin != 'bs4') {
        updateProps.skin = 'bs4';
        update = true;
        print(`Hub ${hub.urlPath} - ${hub.hubId} - has skin ${hub.skin}`);
    }

    type = hub.templateConfiguration && hub.templateConfiguration.header && hub.templateConfiguration.header.type;
    if (type === 'biocollect') {
        updateProps["templateConfiguration.header.type"] = 'custom'
        update = true;
        print(`Hub ${hub.urlPath} - ${hub.hubId} - has header ${type}`);
    }

    if (update) {
        db.hub.update({hubId: hub.hubId}, {$set: updateProps});
        updated++;
    }
}

print(`Updated ${updated} hubs`);