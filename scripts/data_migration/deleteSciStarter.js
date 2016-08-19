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
 * Created by Temi on 8/08/2016.
 */
var sitesToDelete = []
function updateOrDeleteSite(site){
    var validProjectIds = []
    site.projects && site.projects.forEach(function (projectId) {
        if (doesProjectExist(projectId)) {
            validProjectIds.push (projectId)
        }
    })

    if(validProjectIds.length){
        print("Updating site -" + site.name + " site id - " + site.siteId)
        db.site.update({siteId: site.siteId},{$set:{projects:validProjectIds}})
    } else {
        print("Deleting site -" + site.name + " site id - " + site.siteId)
        db.site.remove({siteId: site.siteId})
    }
}

function doesProjectExist(projectId) {
    var project = db.project.find({projectId: projectId})
    if (project.hasNext()) {
        return true
    }

    return false
}


var projects = db.project.find({isSciStarter:true})
while (projects.hasNext()) {
    var project = projects.next();
    var projectId = project.projectId
    db.project.remove({projectId: project.projectId})
    var sites = db.site.find({projects:{$in:[projectId]}})
    while (sites.hasNext()){
        var site = sites.next()
        updateOrDeleteSite(site)
    }
    print("Deleted project id - " + project.projectId)
}