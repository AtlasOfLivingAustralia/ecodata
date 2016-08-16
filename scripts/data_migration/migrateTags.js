/*
 * Copyright (C) 2016 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Created by Temi on 15/08/2016.
 */

var fields = ['hasParticipantCost', 'isSuitableForChildren', 'isDIY', 'isHome', 'hasTeachingMaterials', 'isContributingDataToAla']
var condition = {
    "$or" : [{
        'hasParticipantCost':true
    },{
        'isSuitableForChildren':true
    },{
        'isDIY':true
    },{
        'isHome':true
    },{
        'hasTeachingMaterials':true
    },{
        'isContributingDataToAla':true
    }]
}

var unset = {
    'hasParticipantCost':"",
    'isSuitableForChildren':"",
    'isDIY':"",
    'isHome':"",
    'hasTeachingMaterials':"",
    'isContributingDataToAla':""
}

var projects = db.project.find(condition)
while (projects.hasNext()) {
    var project = projects.next();
    if(!project.tags){
        var tags = []
        for(var i = 0; i < fields.length; i++){
            var field = fields[i]
            if (project[field]) {
                tags.push(field)
            }

            delete  project[field]
        }


        db.project.update({projectId:project.projectId},{$set:{tags: tags}})
        print("updated project " + project.projectId)
    }
}