load('../../utils/audit.js');
const intersection = "intersectionAreaByFacets"
let lookupTable = {
    "state": {
        "Northern Territory": ["Northern Territory (including Coastal Waters)", "NT"],
        "Tasmania": ["Tasmania (including Coastal Waters)", "TAS"],
        "New South Wales": ["New South Wales (including Coastal Waters)", "NSW"],
        "Victoria": ["Victoria (including Coastal Waters)", "VIC"],
        "Queensland": ["Queensland (including Coastal Waters)", "QLD"],
        "South Australia": ["South Australia (including Coastal Waters)", "SA"],
        "Australian Capital Territory": ["ACT"],
        "Western Australia": ["Western Australia (including Coastal Waters)", "WA"]
    }
}

const propertiesToStandardize = ["state", "elect"];
function standardiseSpatialLayerObjectName(name, property) {
    if (name) {
        let lookupTableForProperty = lookupTable[property];
        name = name.trim().toLowerCase();
        if (lookupTableForProperty) {
            let manyToOneMappingTable = `${property}Synonym`;
            if (!lookupTable[manyToOneMappingTable]) {
                const mappings = {};
                const keys= Object.keys(lookupTableForProperty || {})
                for (let i = 0; i < keys.length; i++) {
                    let key = keys[i];
                    let values = lookupTableForProperty[key];
                    values.forEach(value => {
                        mappings[value.toLowerCase()] = key;
                    });
                }

                lookupTable[manyToOneMappingTable] = mappings;
            }

            if (lookupTable[manyToOneMappingTable][name])
                return lookupTable[manyToOneMappingTable][name];
        }

        return name.replace(/\b\w/g, char => char.toUpperCase());
    }

    return null;
}

/**
 *
 * @param geometry = {
 *     "state": ["New South Wales"],
 *     "elect": "Page"
 * }
 * @param updated
 * @returns {*}
 */
function standardiseFacetValues(geometry, updated) {
    propertiesToStandardize.forEach(property => {
        let value = geometry[property];
        if (value) {
            if (typeof value === "string"){
                value = geometry[property] = [value];
                updated = true;
            }

            if (value.length > 0) {
                value.forEach((item, index) => {
                    let standardizedValue = standardiseSpatialLayerObjectName(item, property);
                    if (standardizedValue !== item) {
                        value[index] = standardizedValue;
                        updated = true;
                    }
                });
            }
        }
    });

    return updated;
}

/**
 * @param geometry = {
 *    "state": ["New South Wales"],
 *     "intersectionAreaByFacets": {
 *           "state": {
 *             "CURRENT": {
 *               "New South Wales": 0
 *             },
 *             "cl927": {
 *               "New South Wales": 0
 *             }
 *           },
 *           "elect": {
 *             "CURRENT": {
 *               "Page": 0
 *             },
 *             "cl11163": {
 *               "Page": 0
 *             }
 *           }
 *         }
 * }
 * @param updated
 * @returns updated
 */
function standardiseIntersectionAreaByFacets(geometry, updated) {
    let intersectionAreaByFacets = geometry[intersection];
    if (intersectionAreaByFacets) {
        var facets = Object.keys(intersectionAreaByFacets);
        for (let i = 0; i < facets.length; i++) {
            let facet = facets[i];
            let layersSpatialNamesAndArea = intersectionAreaByFacets[facet];
            let layers = Object.keys(layersSpatialNamesAndArea);
            for (let j = 0; j < layers.length; j++) {
                let layer = layers[j];
                let spatialNamesAndArea = layersSpatialNamesAndArea[layer];
                let spatialNames = Object.keys(spatialNamesAndArea);
                let newSpatialValuesAndArea = {};
                for (let k = 0; k < spatialNames.length; k++) {
                    let newSpatialValue = standardiseSpatialLayerObjectName(spatialNames[k], facet);
                    newSpatialValuesAndArea[newSpatialValue] = spatialNamesAndArea[spatialNames[k]];
                    updated = true;
                }

                layersSpatialNamesAndArea[layer] = newSpatialValuesAndArea;
            }
        }
    }

    return updated;
}

function standardiseGeographicInfo(geographicInfo) {
    if (geographicInfo) {
        geographicInfo.primaryState = standardiseSpatialLayerObjectName(geographicInfo.primaryState, "state");
        if (geographicInfo.otherStates) {
            geographicInfo.otherStates = geographicInfo.otherStates.map(state => standardiseSpatialLayerObjectName(state, "state"));
        }

        geographicInfo.primaryElectorate = standardiseSpatialLayerObjectName(geographicInfo.primaryElectorate, "elect");
        if (geographicInfo.otherElectorates) {
            geographicInfo.otherElectorates = geographicInfo.otherElectorates.map(elect => standardiseSpatialLayerObjectName(elect, "elect"));
        }
    }

    return geographicInfo;
}

db.site.find({"site.extent.geometry": {$exists: true}}).forEach(site => {
    let updated = false;
    let geometry = site.extent && site.extent.geometry;
    if (geometry) {
        updated = standardiseFacetValues(geometry, updated);
        updated = standardiseIntersectionAreaByFacets(geometry, updated);

        if (updated) {
            print(`Updating site ${site.siteId}`);
            db.site.updateOne({siteId: site.siteId}, {$set: {"extent.geometry": geometry}});
            audit(site, site.siteId, 'au.org.ala.ecodata.Site', 'system');
        }
    }
});

print("Completed sites");

db.project.find({
    "geographicInfo": {$exists: true}
}).forEach(project => {
   print(`Updating project ${project.projectId}`);
   if(project.geographicInfo) {
       project.geographicInfo = standardiseGeographicInfo(project.geographicInfo);
       db.project.updateOne({projectId: project.projectId}, {$set: {"geographicInfo": project.geographicInfo}});
       audit(project, project.projectId, 'au.org.ala.ecodata.Project', 'system');
   }
});

print("Completed projects");