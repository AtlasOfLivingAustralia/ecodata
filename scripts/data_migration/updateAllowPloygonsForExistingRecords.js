/**
* Only for migrating the old data to the new site implementation
* Qifeng Bai
*  
*/
//1, This script is used to set two new fields: allowPolygons and allowPoints to the existing records
// For the existing records which allowAddtionalSurverySites is true, the set allowPolygons to true and allowPoints to true

//2, update projectActivities which allowAddtionalSurveySites does not exist.

function updateAllowPolygonsForProjectActviity(){

    print('Update allowPolygons  to false, and allowPoints to true, if allowAdditionalSurveySites is not defined')
    var cPA_N =  db.projectActivity.find({allowAdditionalSurveySites:{$exists:false}})
    cPA_N.forEach(function(pa){
        print('updating project activity without allowAddtionalSurveySites: '+ pa.projectActivityId)
        })
        
    print('Update allowPolygons and allowPoints to true, if allowAdditionalSurveySites is true')
    var cPA_N =  db.projectActivity.find({allowAdditionalSurveySites:true})
    cPA_N.forEach(function(pa){
        print('updating project activity with allowAddtionSurveySites is true: '+ pa.projectActivityId)
        })   
    
    db.projectActivity.update({allowAdditionalSurveySites:{$exists:false}},{$set:{allowPolygons:false,allowPoints:true,allowAdditionalSurveySites:false}},{multi:true})    
    db.projectActivity.update({allowAdditionalSurveySites:true},{$set:{allowPolygons:true,allowPoints:true}},{multi:true})
}

//Found some activities which has  both location and lat/lng of this projectActivivity
function removeSiteIdForTheProjectActivity(pai){
    var pId= (db.getCollection('projectActivity').findOne({projectActivityId:'59754a0b-478c-4815-b368-eef5f9edfb07'})).projectId;
    var project = db.project.findOne({projectId:pId})
    var psid = project.projectSiteId
    if (psid){
            print('Found project site id: '+ psid)
            var cAct = db.activity.find({projectActivityId:pai})
            while (cAct.hasNext()){
                var activity = cAct.next()    
                var outputs = db.output.find({activityId:activity.activityId})              
                outputs.forEach(function(output){
                    if (output.data){
                         if (output.data.location && output.data.locationLongitude && output.data.locationLatitude){ 
                                if(psid == output.data.location){
                                        print("remove location from activity: " +output.activityId)                     
                                        db.output.update({outputId:output.outputId},{$unset:{'data.location':""}})                                        
                                    }                            
                         }
                    }          
                })
            }
    }else{
        print('Error: SiteId is not found for project activity: '+pai)
    }


}

updateAllowPolygonsForProjectActviity()
//removeSiteIdForTheProjectActivity('59754a0b-478c-4815-b368-eef5f9edfb07')