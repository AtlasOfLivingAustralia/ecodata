load('../misc/uuid.js');

var hubs = db.setting.find({key:/hub\.configuration/});
while (hubs.hasNext()) {
    var hub = hubs.next();

    var hubJson = JSON.parse(hub.value);
    hubJson.hubId = UUID.generate();
    hubJson.urlPath = hubJson.id;
    delete hubJson.id;

    hubJson.status = 'active';

    db.hub.save(hubJson);

    db.setting.remove(hub);
}