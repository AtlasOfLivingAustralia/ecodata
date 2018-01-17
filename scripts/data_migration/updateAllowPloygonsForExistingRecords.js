/**
* Only for migrating the old data to the new site implementation
* Qifeng Bai
*  
*/
//This script is used to set two new fields: allowPolygons and allowPoints to the existing records
//For the existing records which allowAddtionalSurverySites is true, the set allowPolygons to true and allowPoints to false
// {allowPloygons:{$exists:false}} is used to double check, in case some existing surveies have been mannually updated.

function updateAllowPolygonsForProjectActviity(){
    db.getCollection('projectActivity').update({$and:[{allowAdditionalSurveySites:true},{allowPolygons:{$exists:false}}]},{$set:{allowPolygons:true,allowPoints:false}},{multi:true})
}

//Found some activities which has  both location and lat/lng of this projectActivivity
function removeSiteIdForTheProjectActivity(pai){
    var cAct = db.activity.find({projectActivityId:pai})
    while (cAct.hasNext()){
        var activity = cAct.next()    
        var outputs = db.output.find({activityId:activity.activityId})
        outputs.forEach(function(output){
            if (output.data){
                 if (output.data.location && output.data.locationLongitude && output.data.locationLatitude){                     
                     print("remove location from: " +output.activityId)                     
                     db.output.update({outputId:output.outputId},{$unset:{'data.location':""}}) 
                 }
            }          
        })
    }
}

removeSiteIdForTheProjectActivity('59754a0b-478c-4815-b368-eef5f9edfb07')