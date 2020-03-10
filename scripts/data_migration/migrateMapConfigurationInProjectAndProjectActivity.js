/**
 * Script to migrate mapConfiguration in survey and works project. Run it only once since data can be overwritten.
 * It adds the following properties
 *  - surveySiteOption - default 'sitepick'
 *  - allowLine - default false
 *  - addCreatedSiteToListOfSelectedSites
 *
 * Usage - mongo localhost:27017/ecodata migrateMapConfigurationInProjectAndProjectActivity.js
 */

var save = true;
var forceUpdate = true;
var SITE_CREATE = 'sitecreate', SITE_PICK = 'sitepick', SITE_PICK_CREATE = 'sitepickcreate';
var pas = db.projectActivity.find({});
print("Number of ProjectActivity(s) to migrate - " + pas.count());
var paCounter = 0;
while (pas.hasNext()) {
    var pa = pas.next();
    print(pa.projectActivityId);
    updateMapConfiguration(pa);

    if (save) {
        db.projectActivity.save(pa);
        paCounter++;
    }
}

print("Number of ProjectActivity(s) updated " + paCounter);

var projects = db.project.find({mapConfiguration: {$ne: null}, isMERIT: false});
print("Number of Project(s) to migrate - " + projects.count());
var projectCounter = 0;
while (projects.hasNext()) {
    var project = projects.next();
    print(project.projectId);
    updateMapConfiguration(project.mapConfiguration);

    if (save) {
        db.project.save(project);
        projectCounter++;
    }
}

print("Number of Project(s) updated " + projectCounter);

function updateMapConfiguration (obj) {
    if (forceUpdate || (obj.surveySiteOption == undefined))
        obj.surveySiteOption = initSurveySiteOption(obj);

    if (forceUpdate || (obj.allowLine == undefined))
        obj.allowLine = false;

    if (forceUpdate || (obj.addCreatedSiteToListOfSelectedSites == undefined)) {
        var addCreatedSiteToListOfSelectedSites = obj.allowAdditionalSurveySites;
        if (obj.surveySiteOption === SITE_PICK_CREATE)
            obj.addCreatedSiteToListOfSelectedSites = addCreatedSiteToListOfSelectedSites;
        else
            obj.addCreatedSiteToListOfSelectedSites = false;
    }

    validateMapConfigurationSettings(obj);
};

function initSurveySiteOption (obj) {
    var surveySiteOptions = [];
    if (obj.allowPoints || obj.allowPolygons || obj.allowLine) {
        surveySiteOptions.push(SITE_CREATE);
    }

    if (obj.sites && obj.sites.length > 0) {
        surveySiteOptions.push(SITE_PICK);
    }

    if (surveySiteOptions.length > 1) {
        return SITE_PICK_CREATE;
    }
    else if (surveySiteOptions.length == 1) {
        return surveySiteOptions[0];
    }
    else {
        if (obj.projectActivityId) {
            print ( "Project activity - " + obj.projectActivityId + " - setting surveySiteOption to 'sitepick'." );
        } else {
            print ( "Project - " + obj.projectId + " - cannot find an adequate surveySiteOption." );
        }

        return SITE_PICK;
    }
};

function validateMapConfigurationSettings(obj) {
    switch (obj.surveySiteOption) {
        case SITE_CREATE:
            clearSelectedSites(obj);
            break;
        case SITE_PICK:
            clearOptionToCreateSite(obj);
            break;
        case SITE_PICK_CREATE:
            // do nothing
            break;
    }
};

function clearSelectedSites (obj) {
        obj.sites = [];
};

function clearOptionToCreateSite (obj) {
    obj.allowLine = false;
    obj.allowPolygons = false;
    obj.allowPoints = false;
    obj.addCreatedSiteToListOfSelectedSites = false;
};