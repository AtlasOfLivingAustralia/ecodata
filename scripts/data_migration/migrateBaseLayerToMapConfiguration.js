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
 * Created by Temi on 21/1/20.
 */

var save = true;
var forceUpdate = true;
var projectActivityBaseLayerValues = db.runCommand({
    "distinct": "projectActivity",
    "key": "baseLayersName",
    "query": {"baseLayersName": {$ne: null}}
}).values;

print("Distinct base layer values - " + projectActivityBaseLayerValues.join(' , '));

var pas = db.projectActivity.find({baseLayersName: {$ne: null}});
print("Number of ProjectActivity(s) to migrate - " + pas.count());
var paCounter = 0;
while (pas.hasNext()) {
    var pa = pas.next();
    convertProjectActivityBaseLayerConfig(pa);
    if (save) {
        db.projectActivity.save(pa);
        print(pa.projectActivityId);
        paCounter++;
    }
}

print("Number of ProjectActivity(s) updated " + paCounter);

var projects = db.project.find({baseLayer: {$ne: null}, isMERIT: false});
print("Number of Project(s) to migrate - " + projects.count());
var projectCounter = 0;
while (projects.hasNext()) {
    var project = projects.next();
    convertProjectBaseLayerConfig(project);
    if (save) {
        print(project.projectId);
        db.project.save(project);
        projectCounter++;
    }
}

print("Number of Project(s) updated " + projectCounter);

function convertProjectBaseLayerConfig(project) {
    if (forceUpdate || project.mapLayersConfig === undefined) {
        project.mapLayersConfig = {baseLayers: [], overlays: []};
        switch (project.baseLayer) {
            case 'minimal':
                project.mapLayersConfig.baseLayers.push(setBaseLayerAsDefault(getBaseLayer('minimal')));
                project.mapLayersConfig.baseLayers.push(getBaseLayer('worldimagery'));
                break;
            case 'worldimagery':
                project.mapLayersConfig.baseLayers.push(setBaseLayerAsDefault(getBaseLayer('worldimagery')));
                project.mapLayersConfig.baseLayers.push(getBaseLayer('minimal'));
                break;
            default:
                print(project.baseLayersName + " value not found - projectActivityId " + project.projectId);
                break;
        }
    }

    return project;   
}

function convertProjectActivityBaseLayerConfig(pa) {
    if (forceUpdate || pa.mapLayersConfig === undefined) {
        pa.mapLayersConfig = {baseLayers: [], overlays: []};
        switch (pa.baseLayersName) {
            case 'Google Maps':
                pa.mapLayersConfig.baseLayers.push(setBaseLayerAsDefault(getBaseLayer('googleroadmap')));
                pa.mapLayersConfig.baseLayers.push(getBaseLayer('googlehybrid'));
                pa.mapLayersConfig.baseLayers.push(getBaseLayer('googleterrain'));
                break;
            case 'Open Layers':
            case 'Default':
                pa.mapLayersConfig.baseLayers.push(setBaseLayerAsDefault(getBaseLayer('minimal')));
                break;
            default:
                print(pa.baseLayersName + " value not found - projectActivityId " + pa.projectActivityId);
                break;
        }
    }

    return pa;
}

function getBaseLayer(baseLayer) {
    var config;
    switch (baseLayer) {
        case 'googlehybrid':
            config = {
                'code': 'googlehybrid',
                'displayText': 'Google hybrid',
                'isSelected': false
            };
            break;
        case 'googleroadmap':
            config = {
                'code': 'googleroadmap',
                'displayText': 'Google roadmap',
                'isSelected': false
            };
            break;
        case 'googleterrain':
            config = {
                'code': 'googleterrain',
                'displayText': 'Google terrain',
                'isSelected': false
            };
            break;
        case 'topographic':
            config = {
                'code': 'topographic',
                'displayText': 'ESRI Topographic',
                'isSelected': false
            };
            break;
        case 'detailed':
            config = {
                'code': 'detailed',
                'displayText': 'Detailed',
                'isSelected': false
            };
            break;
        case 'worldimagery':
            config = {
                'code': 'worldimagery',
                'displayText': 'Satellite',
                'isSelected': true
            };
            break;
        case 'minimal':
        default:
            config = {
                'code': 'minimal',
                'displayText': 'Road map',
                'isSelected': false
            };
            break;
    }

    return config;
}

function setBaseLayerAsDefault(config) {
    if (config) {
        config.isSelected = true;
    }

    return config;
}