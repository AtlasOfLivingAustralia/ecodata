load('uuid.js');
var sessionId = 'CB5EC7ED75D90F983E32BC5C7D973C30';
var affectedOutputs = db.output.find({name:'Single Sighting - Advanced', status:{$ne:'deleted'}, 'data.species.guid':{$exists:true}, 'data.species.outputSpeciesId':{$exists:false}});

while (affectedOutputs.hasNext()) {
    var output = affectedOutputs.next();

    output.data.species.outputSpeciesId = UUID.generate();
    db.output.save(output);

    print("curl --cookie JSESSIONID="+sessionId+" -D headers.txt 'http://devt.ala.org.au:8080/ecodata/admin/regenerateRecordsForOutput?outputId="+output.outputId+"'");

}