/*
 * Copyright (C) 2020 Atlas of Living Australia
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
 * Created by Temi on 7/8/20.
 */

// DO NOT RUN IT ON PRODUCTION OR TEST SYSTEM
var softDelete = "soft",
    hardDelete = "hard",
    deleteType = hardDelete,
    sleepTime = 10000,
    sleepSeconds = sleepTime/1000,
    justOne = true;
function getNumberOfDocumentsToDelete() {
    var projects = db.project.distinct('projectId', {isMERIT: true});
    var sites = db.site.distinct('siteId', {projects: {$in: projects}});
    return {projects: projects, sites: sites};
}

function deleteDocuments(entityType, query) {
    switch (deleteType) {
        case softDelete:
        default:
            print("Begining soft delete");
            db[entityType].update(query, {$set:{"status": "deleted"}}, {multi: true});
            print("Completed soft delete");
            break;
        case hardDelete:
            print("About to permenantly delete " + entityType + " in " + sleepSeconds + " seconds. Abort now if unintended.");
            sleep(sleepTime);
            print("Begining delete");
            db[entityType].remove(query, justOne);
            print("Completed delete");
            break;
    }
}
if(deleteType == hardDelete) {
    print("Are you sure you want to permanently delete MERIT data from database? You have " + sleepSeconds +" seconds to abort running this script.");
    sleep(sleepTime);
} else {
    print("Are you sure you want to soft delete MERIT data from database? You have " + sleepSeconds +" seconds to abort running this script.");
    sleep(sleepTime);
}

var documents = getNumberOfDocumentsToDelete();
print("Number of project to delete - " + documents.projects.length);
print(documents.projects);
deleteDocuments('project', {projectId: {$in: documents.projects}});

print("Number of sites to delete - " + documents.sites.length);
print(documents.sites);
deleteDocuments('site', {siteId: {$in: documents.sites}});
