
print ("Fix record.eventDate ")

use ecodata

db.record.find(
    { eventDate: {$regex: /^(mon|tue|wed|thu|fri|sat|sun)/ , $options: "i"} },
    {eventDate:1}).forEach(function(doc) {
    var inputString = doc.eventDate

    // Get the first part if it is a ; separated list of dates
    var breakUpIndex = inputString.indexOf(";")
    if(breakUpIndex >= 0 ) {
        inputString = inputString.substr(0,breakUpIndex)
    }

    // Manually convert the two known timezones, Mongo won't parse those as the timezone abbreviations are not unique
    var isoDateString =  new Date(inputString.replace("AEDT", "+11").replace("AEST", "+10") ).toJSON()

    print ("Before: " + doc.eventDate + " After: " + isoDateString)

    // Comment out for a dry run

    db.record.update({"_id":doc._id},{$set:{"eventDate":isoDateString}} );

} )

print ("Finish ")