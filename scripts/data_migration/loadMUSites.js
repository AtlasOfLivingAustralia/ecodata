load('../misc/uuid.js');
load('mu2018.js');

print(mugeojson.features.length);
print(db.program.count());
for (var i=0; i<mugeojson.features.length; i++) {
    var mu = mugeojson.features[i];

    var name = mu.properties.NLP_MU;
    //print(name);

    if (name == 'Marine NRM') {
        name = 'Marine Natural Resource Management';
    }

    var program = db.program.find({name:name});
    if (!program.hasNext()) {
        print("No program found for: "+name);
    }
    else {
        program = program.next();

        var now = new Date();
        var site = {
            name:name,
            type:'programArea',
            dateCreated: now,
            lastUpdated: now,
            extent: {
                geometry: mu.geometry,
                source:"upload"
            },
            status:'active',
            siteId:UUID.generate()
        };
        site.extent.geometry.state = mu.properties.STATE;
        db.site.insert(site);
        // Create site, assign ID to program.
        db.program.update({programId:program.programId}, {$set:{programSiteId:site.siteId}});

    }

}
