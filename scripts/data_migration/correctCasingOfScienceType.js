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
 * Created by Temi on 18/08/2016.
 */
var scienceType = [
    "Animals",
    "Agricultural & veterinary science",
    "Astronomy",
    "Biology",
    "Biodiversity",
    "Biogeography",
    "Birds",
    "Chemical sciences",
    "Climate & meteorology",
    "Ecology",
    "Ecology & Environment",
    "Fire Ecology",
    "Genetics",
    "Geology & soils",
    "Geomorphology",
    "Indigenous science",
    "Indigenous knowledge",
    "Information & computing sciences",
    "Insects & Pollinators",
    "Long-Term Species Monitoring",
    "Marine & Terrestrial",
    "Medical & human health",
    "Nature & Outdoors",
    "NRM",
    "Ocean",
    "Physical science",
    "Social sciences",
    "Symbyotic Interactions",
    "Technology",
    "Water"
]
var scienceTypeLowerCase = {}

scienceType.forEach(function (it, index) {
    scienceTypeLowerCase[it.toLowerCase()] = index
})
var projects = db.project.find({scienceType:{$ne:null}})
while (projects.hasNext()) {
    var project = projects.next();
    var correctScienceType = []

    if(project.scienceType && project.scienceType.length){
        project.scienceType.forEach(function (it) {
            var type = scienceType[scienceTypeLowerCase[it]]
            if(type != null || type != undefined){
                correctScienceType.push(type)
            } else {
                print('Could not find match for - ' + it)
            }
        });
        db.project.update({projectId:project.projectId},{$set:{scienceType:correctScienceType}})
        print("updated name -" + project.name + ", id -" +project.projectId)
    }
}