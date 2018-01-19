/**
* Only for migrating the old data to the new site implementation
* Qifeng Bai
*  
*/
//1, This script is used to set two new fields: allowPolygons and allowPoints to the existing records
// For the existing records which allowAddtionalSurverySites is true, the set allowPolygons to true and allowPoints to true

//2, update projectActivities which allowAddtionalSurveySites does not exist.

function updateAllowPolygonsForProjectActviity(){

    print('Update allowPolygons  to false, and allowPoints to true, if allowAdditionalSurveySites is not defined/false')
    var cPA_N =  db.projectActivity.find({$or:[{allowAdditionalSurveySites:{$exists:false}}, {allowAdditionalSurveySites:false}]})
    cPA_N.forEach(function(pa){
        var project = db.project.findOne({projectId:pa.projectId});
        print('updating project activity without allowAddtionalSurveySites: ' + pa.name +" : " + pa.projectActivityId + " for project:" + project.name +" : " +project.projectId)
      })
        
    print('Update allowPolygons and allowPoints to true, if allowAdditionalSurveySites is true')
    var cPA_N =  db.projectActivity.find({allowAdditionalSurveySites:true})
    cPA_N.forEach(function(pa){
        var project = db.project.findOne({projectId:pa.projectId});
        print('updating project activity with allowAddtionSurveySites is true: '+ pa.name +" : " +  pa.projectActivityId + " for project:" + project.name +" : " +project.projectId)
        })   
    
    db.projectActivity.update({$or:[{allowAdditionalSurveySites:{$exists:false}}, {allowAdditionalSurveySites:false}]},{$set:{allowPolygons:false,allowPoints:true,allowAdditionalSurveySites:false}},{multi:true})    
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

function findOutputsWithBoth(){
    var projects =  db.project.find({isMERIT:false})
    var total = 0;
    projects.forEach(function(project){
        var projectId = project.projectId;
        var activites = db.activity.find({projectId : projectId})
        var outputsInproject = 0;
        activites.forEach(function(activity){
            var activityId = activity.activityId;
            var outputs = db.output.find({$and:[{activityId: activityId}, {'data.locationLatitude':{$ne:null}} , {'data.locationLongitude':{$ne:null}}, {'data.location':{$ne:null} }]})
            var oc = outputs.count();
            if (oc > 0){
               total = total+oc;
               outputsInproject += oc;
               db.output.update({$and:[{activityId: activityId}, {'data.locationLatitude':{$ne:null}} , {'data.locationLongitude':{$ne:null}}, {'data.location':{$ne:null} }]},{$unset:{'data.location':""}})
            }
        })

        if (outputsInproject>0){
            print (project.name  +'\t' + "https://biocollect.ala.org.au/project/index/"+projectId + '\t' + outputsInproject )
        }
    })
}


//updateAllowPolygonsForProjectActviity()
findOutputsWithBoth()
print ('Excution is done')

//removeSiteIdForTheProjectActivity('59754a0b-478c-4815-b368-eef5f9edfb07')