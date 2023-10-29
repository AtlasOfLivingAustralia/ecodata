function updateSetting(settingKey, settingValue) {
    let setting = db.setting.findOne({key: settingKey});
    if (!setting) {
        db.setting.insertOne({key: settingKey, value: settingValue});
    } else {
        db.setting.replaceOne({key: settingKey}, {key: settingKey, value: settingValue});
    }
}


const protocols = [
    {
        "id": 1,
        "attributes": {
            "name": "Opportunistic Observations",
            "module": "opportunistic observations",
            "endpointPrefix": "/opportunistic-observations",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "multiple": true,
                    "modelName": "opportunistic-observation",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:00.909Z",
            "updatedAt": "2023-03-27T04:44:00.909Z"
        }
    },
    {
        "id": 2,
        "attributes": {
            "name": "Vegetation Mapping",
            "module": "vegetation mapping",
            "endpointPrefix": "/vegetation-mapping-surveys",
            "version": 1,
            "description": "Vegetation Mapping description",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "vegetation-mapping-survey"
                },
                {
                    "multiple": true,
                    "modelName": "vegetation-mapping-observation",
                    "newInstanceForRelationOnAttributes": [
                        "vegetation_mapping_species_covers"
                    ]
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:00.921Z",
            "updatedAt": "2023-03-27T04:44:00.921Z"
        }
    },
    {
        "id": 3,
        "attributes": {
            "name": "Plot Layout",
            "module": "no module",
            "endpointPrefix": "/plot-layouts",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-layout"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:00.932Z",
            "updatedAt": "2023-03-27T04:44:00.932Z"
        }
    },
    {
        "id": 4,
        "attributes": {
            "name": "Drone Survey",
            "module": "no module",
            "endpointPrefix": "/drone-surveys",
            "version": 1,
            "description": "Drone Survey",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "drone-survey"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:00.942Z",
            "updatedAt": "2023-03-27T04:44:00.942Z"
        }
    },
    {
        "id": 5,
        "attributes": {
            "name": "Dev sandbox",
            "module": "no module",
            "endpointPrefix": "/dev-sandbox-surveys",
            "version": 1,
            "description": "An all-in-one Project for testing all Protocols",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "dev-sandbox-survey"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:00.952Z",
            "updatedAt": "2023-03-27T04:44:00.952Z"
        }
    },
    {
        "id": 6,
        "attributes": {
            "name": "Photopoints – Full Protocol: DSLR Panoramas",
            "module": "photo points",
            "endpointPrefix": "/photopoints-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "photopoints-survey",
                    "protocol-variant": "DSLR Full",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:00.961Z",
            "updatedAt": "2023-03-27T04:44:00.961Z"
        }
    },
    {
        "id": 7,
        "attributes": {
            "name": "Photopoints - Lite Protocol: Compact Panoramas",
            "module": "photo points",
            "endpointPrefix": "/photopoints-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "photopoints-survey",
                    "protocol-variant": "Compact Lite",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:00.971Z",
            "updatedAt": "2023-03-27T04:44:00.971Z"
        }
    },
    {
        "id": 8,
        "attributes": {
            "name": "Photopoints - Lite Protocol: Device Panoramas",
            "module": "photo points",
            "endpointPrefix": "/photopoints-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "photopoints-survey",
                    "protocol-variant": "On-device Lite",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:00.981Z",
            "updatedAt": "2023-03-27T04:44:00.981Z"
        }
    },
    {
        "id": 9,
        "attributes": {
            "name": "Floristics – Full Protocol",
            "module": "floristics",
            "endpointPrefix": "/floristics-veg-survey-fulls",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "floristics-veg-survey-full",
                    "protocol-variant": "full"
                },
                {
                    "multiple": true,
                    "modelName": "floristics-veg-voucher-full",
                    "protocol-variant": "full",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:00.990Z",
            "updatedAt": "2023-03-27T04:44:00.990Z"
        }
    },
    {
        "id": 10,
        "attributes": {
            "name": "Floristics – Lite Protocol",
            "module": "floristics",
            "endpointPrefix": "/floristics-veg-survey-lites",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "floristics-veg-survey-lite",
                    "protocol-variant": "lite"
                },
                {
                    "multiple": true,
                    "required": false,
                    "modelName": "floristics-veg-voucher-lite",
                    "protocol-variant": "lite",
                    "usesCustomComponent": "true"
                },
                {
                    "multiple": true,
                    "required": false,
                    "modelName": "floristics-veg-virtual-voucher"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.001Z",
            "updatedAt": "2023-03-27T04:44:01.001Z"
        }
    },
    {
        "id": 11,
        "attributes": {
            "name": "Plant Tissue Vouchering – Full Protocol",
            "module": "plant tissue vouchering",
            "endpointPrefix": "/floristics-veg-genetic-voucher-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "floristics-veg-genetic-voucher-survey",
                    "protocol-variant": "full"
                },
                {
                    "multiple": true,
                    "modelName": "floristics-veg-genetic-voucher",
                    "protocol-variant": "full",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.012Z",
            "updatedAt": "2023-03-27T04:44:01.012Z"
        }
    },
    {
        "id": 12,
        "attributes": {
            "name": "Plant Tissue Vouchering – Lite Protocol",
            "module": "plant tissue vouchering",
            "endpointPrefix": "/floristics-veg-genetic-voucher-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "floristics-veg-genetic-voucher-survey",
                    "protocol-variant": "lite"
                },
                {
                    "multiple": true,
                    "modelName": "floristics-veg-genetic-voucher",
                    "protocol-variant": "lite",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.024Z",
            "updatedAt": "2023-03-27T04:44:01.024Z"
        }
    },
    {
        "id": 13,
        "attributes": {
            "name": "Cover - Full Protocol",
            "module": "cover",
            "endpointPrefix": "/cover-point-intercept-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "cover-point-intercept-survey",
                    "protocol-variant": "full"
                },
                {
                    "multiple": true,
                    "modelName": "cover-point-intercept-point",
                    "protocol-variant": "full",
                    "usesCustomComponent": "true",
                    "newInstanceForRelationOnAttributes": [
                        "species_intercepts"
                    ]
                },
                {
                    "multiple": true,
                    "modelName": "fire-char-observation",
                    "defaultHidden": true,
                    "isSubmoduleStep": true,
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.036Z",
            "updatedAt": "2023-03-27T04:44:01.036Z"
        }
    },
    {
        "id": 14,
        "attributes": {
            "name": "Cover - Lite Protocol",
            "module": "cover",
            "endpointPrefix": "/cover-point-intercept-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "cover-point-intercept-survey",
                    "protocol-variant": "lite"
                },
                {
                    "multiple": true,
                    "modelName": "cover-point-intercept-point",
                    "protocol-variant": "lite",
                    "usesCustomComponent": "true",
                    "newInstanceForRelationOnAttributes": [
                        "species_intercepts"
                    ]
                },
                {
                    "multiple": true,
                    "modelName": "fire-char-observation",
                    "defaultHidden": true,
                    "isSubmoduleStep": true,
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.047Z",
            "updatedAt": "2023-03-27T04:44:01.047Z"
        }
    },
    {
        "id": 15,
        "attributes": {
            "name": "Basal Area – Full DBH",
            "module": "basal area",
            "endpointPrefix": "/basal-area-dbh-measure-survey-fulls",
            "version": 1,
            "description": "Basal Area DBH Measure Full Survey",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "basal-area-dbh-measure-survey-full",
                    "protocol-variant": "full"
                },
                {
                    "multiple": true,
                    "modelName": "basal-area-dbh-measure-observation-full",
                    "protocol-variant": "full",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.060Z",
            "updatedAt": "2023-03-27T04:44:01.060Z"
        }
    },
    {
        "id": 16,
        "attributes": {
            "name": "Basal Area – Lite DBH",
            "module": "basal area",
            "endpointPrefix": "/basal-area-dbh-measure-survey-lites",
            "version": 1,
            "description": "Basal Area DBH Measure Lite Survey",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "basal-area-dbh-measure-survey-lite",
                    "protocol-variant": "lite"
                },
                {
                    "multiple": true,
                    "modelName": "basal-area-dbh-measure-observation-lite",
                    "protocol-variant": "lite",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.072Z",
            "updatedAt": "2023-03-27T04:44:01.072Z"
        }
    },
    {
        "id": 17,
        "attributes": {
            "name": "Basal Area – Basal Wedge",
            "module": "basal area",
            "endpointPrefix": "/basal-wedge-surveys",
            "version": 1,
            "description": "Basal Area – Basal Wedge",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "basal-wedge-survey",
                    "protocol-variant": "wedge"
                },
                {
                    "multiple": true,
                    "modelName": "basal-wedge-observation",
                    "protocol-variant": "wedge"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.084Z",
            "updatedAt": "2023-03-27T04:44:01.084Z"
        }
    },
    {
        "id": 18,
        "attributes": {
            "name": "Coarse Woody Debris",
            "module": "coarse woody debris",
            "endpointPrefix": "/coarse-woody-debris-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "coarse-woody-debris-survey"
                },
                {
                    "multiple": true,
                    "modelName": "coarse-woody-debris-observation",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.097Z",
            "updatedAt": "2023-03-27T04:44:01.097Z"
        }
    },
    {
        "id": 19,
        "attributes": {
            "name": "Recruitment – Age Structure Protocol",
            "module": "recruitment",
            "endpointPrefix": "/recruitment-field-surveys",
            "version": 1,
            "description": "Recruitment – Age Structure Protocol",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "recruitment-field-survey"
                },
                {
                    "multiple": true,
                    "modelName": "recruitment-growth-stage",
                    "usesCustomComponent": true
                },
                {
                    "multiple": true,
                    "modelName": "recruitment-sapling-and-seedling-count",
                    "usesCustomComponent": true
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.109Z",
            "updatedAt": "2023-03-27T04:44:01.109Z"
        }
    },
    {
        "id": 20,
        "attributes": {
            "name": "Recruitment – Survivorship Protocol",
            "module": "recruitment",
            "endpointPrefix": "/recruitment-survivorship-surveys",
            "version": 1,
            "description": "Recruitment – Survivorship Protocol",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "recruitment-survivorship-survey",
                    "usesCustomComponent": true
                },
                {
                    "multiple": true,
                    "modelName": "recruitment-survivorship-observation",
                    "usesCustomComponent": true
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.122Z",
            "updatedAt": "2023-03-27T04:44:01.122Z"
        }
    },
    {
        "id": 21,
        "attributes": {
            "name": "Soils – Plot Soil Description",
            "module": "soils",
            "endpointPrefix": "/plot-soil-description-surveys",
            "version": 1,
            "description": "Records location, erosion, microrelief, drainage, disturbance and soil surface condition when dry.",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-soil-description-survey",
                    "usesCustomComponent": "true"
                },
                {
                    "multiple": true,
                    "modelName": "microrelief-observation"
                },
                {
                    "multiple": true,
                    "modelName": "erosion-observation"
                },
                {
                    "multiple": true,
                    "modelName": "surface-coarse-fragments-observation"
                },
                {
                    "modelName": "rock-outcrop-observation"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.135Z",
            "updatedAt": "2023-03-27T04:44:01.135Z"
        }
    },
    {
        "id": 22,
        "attributes": {
            "name": "Soils – Soil Pit Characterisation",
            "module": "soils",
            "endpointPrefix": "/soil-pit-characterisation-surveys",
            "version": 1,
            "description": "Characterisation of the upper soil profile, by exposing the profile to a depth of 1 m+ at the southwest corner of the plot and collecting data and/or soil samples and at 10 cm increments, taking care not to sample across soil horizons. Critical to describe, measure and sample the soil profile, and assign an Australian Soil Classification.",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "soil-pit-characterisation-survey"
                },
                {
                    "minimum": 1,
                    "multiple": true,
                    "modelName": "soil-horizon-observation",
                    "newInstanceForRelationOnAttributes": [
                        "soil_horizon_mottle",
                        "soil_horizon_coarse_fragment",
                        "soil_horizon_structure",
                        "soil_horizon_segregation",
                        "soil_horizon_void",
                        "soil_horizon_samples"
                    ]
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.186Z",
            "updatedAt": "2023-03-27T04:44:01.186Z"
        }
    },
    {
        "id": 23,
        "attributes": {
            "name": "Soils – Soil Sub-pits",
            "module": "soils",
            "endpointPrefix": "/soil-sub-pits-surveys",
            "version": 1,
            "description": "Collection of soil samples to capture variability in the upper soil profile, including exposing the profile to a depth of 30 cm at nine locations with differing microhabitat.",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "soil-sub-pits-survey"
                },
                {
                    "maximum": 9,
                    "minimum": 9,
                    "multiple": true,
                    "modelName": "soil-sub-pit",
                    "newInstanceForRelationOnAttributes": [
                        "soil_sub_pits_horizons"
                    ]
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.198Z",
            "updatedAt": "2023-03-27T04:44:01.198Z"
        }
    },
    {
        "id": 24,
        "attributes": {
            "name": " Soils – Soil Bulk Density",
            "module": "soils",
            "endpointPrefix": "/soil-bulk-density-surveys",
            "version": 1,
            "description": " Soils – Soil Bulk Density",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "soil-bulk-density-survey"
                },
                {
                    "maximum": 3,
                    "minimum": 3,
                    "multiple": true,
                    "modelName": "soil-bulk-density-sample"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.210Z",
            "updatedAt": "2023-03-27T04:44:01.210Z"
        }
    },
    {
        "id": 25,
        "attributes": {
            "name": "Soils – Soil Metagenomics",
            "module": "soils",
            "endpointPrefix": "/soil-metagenomics-surveys",
            "version": 1,
            "description": "Collection of nine soil surface samples to 3 cm in differing microhabitats, or microhabitat of target species, taxa, or groups within the plot for downstream metagenomics analysis (generally collected immediately before the soil sub-site samples are collected – if undertaking the Soil sub-site characterisation protocol).",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "soil-metagenomics-survey"
                },
                {
                    "maximum": 9,
                    "minimum": 9,
                    "multiple": true,
                    "modelName": "soil-metagenomics-sample-observation"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.222Z",
            "updatedAt": "2023-03-27T04:44:01.222Z"
        }
    },
    {
        "id": 26,
        "attributes": {
            "name": "Soils – Lite Protocol",
            "module": "soils",
            "endpointPrefix": "/soil-lite-surveys",
            "version": 1,
            "description": "Lite protocol",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "soil-lite-survey"
                },
                {
                    "maximum": 3,
                    "minimum": 3,
                    "multiple": true,
                    "modelName": "soil-lite-sample"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.234Z",
            "updatedAt": "2023-03-27T04:44:01.234Z"
        }
    },
    {
        "id": 27,
        "attributes": {
            "name": "Vertebrate Fauna – Bird Survey",
            "module": "vertebrate fauna",
            "endpointPrefix": "/bird-surveys",
            "version": 1,
            "description": "A Bird Fauna Survey",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "weather-observation"
                },
                {
                    "modelName": "bird-survey",
                    "usesCustomComponent": "true"
                },
                {
                    "multiple": true,
                    "modelName": "bird-survey-observation"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.246Z",
            "updatedAt": "2023-03-27T04:44:01.246Z"
        }
    },
    {
        "id": 28,
        "attributes": {
            "name": "Invertebrate Fauna – Wet Pitfall Trapping: Full Protocol",
            "module": "invertebrate fauna",
            "endpointPrefix": "/invertebrate-wet-pitfall-traps",
            "version": 1,
            "description": "Consist of a container positioned flush with the ground surface, filled with a liquid preservative to  kill and preserve invertebrates falling into the trap",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "invertebrate-wet-pitfall-trap",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.257Z",
            "updatedAt": "2023-03-27T04:44:01.257Z"
        }
    },
    {
        "id": 29,
        "attributes": {
            "name": "Invertebrate Fauna – Malaise Trapping",
            "module": "invertebrate fauna",
            "endpointPrefix": "/invertebrate-malaise-trappings",
            "version": 1,
            "description": "Tent-like traps that intercept insects in flight, consist of a central wall and side walls made of fine black mesh to reduce visibility, and white tent material on the roof.",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "invertebrate-malaise-trapping"
                },
                {
                    "multiple": true,
                    "modelName": "invertebrate-malaise-trap",
                    "usesCustomComponent": "true",
                    "newInstanceForRelationOnAttributes": [
                        "samples"
                    ]
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.268Z",
            "updatedAt": "2023-03-27T04:44:01.268Z"
        }
    },
    {
        "id": 30,
        "attributes": {
            "name": "Invertebrate Fauna – Pan Trapping",
            "module": "invertebrate fauna",
            "endpointPrefix": "/invertebrate-pan-trappings",
            "version": 1,
            "description": "Small coloured bowls or ‘pans’  either filled with diluted dishwashing liquid for rapid sampling or undiluted propylene glycol for sampling over a longer duration.",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "invertebrate-pan-trapping"
                },
                {
                    "multiple": true,
                    "modelName": "invertebrate-pan-trap",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.280Z",
            "updatedAt": "2023-03-27T04:44:01.280Z"
        }
    },
    {
        "id": 31,
        "attributes": {
            "name": "Invertebrate Fauna – Active Search",
            "module": "invertebrate fauna",
            "endpointPrefix": "/invertebrate-active-searches",
            "version": 1,
            "description": "Involves looking on, under, in and around different habitat features and different apparatus to collect different invertebrate taxa within a set time.",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "invertebrate-active-search",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "invertebrate-active-search-setup",
                    "newInstanceForRelationOnAttributes": [
                        "apparatus",
                        "active_search_photos",
                        "samples"
                    ]
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.291Z",
            "updatedAt": "2023-03-27T04:44:01.291Z"
        }
    },
    {
        "id": 32,
        "attributes": {
            "name": "Plot Location",
            "module": "no module",
            "endpointPrefix": "/plot-locations",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.301Z",
            "updatedAt": "2023-03-27T04:44:01.301Z"
        }
    },
    {
        "id": 33,
        "attributes": {
            "name": "Plot Definition",
            "module": "plot description",
            "endpointPrefix": "/plot-definition-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-definition-survey"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.313Z",
            "updatedAt": "2023-03-27T04:44:01.313Z"
        }
    },
    {
        "id": 34,
        "attributes": {
            "name": "Condition Point Intercept Survey",
            "module": "condition",
            "endpointPrefix": "/condition-point-intercept-surveys",
            "version": 1,
            "description": "Condition Point Intercept Survey",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "condition-point-intercept-survey"
                },
                {
                    "multiple": true,
                    "modelName": "condition-tree-survey",
                    "usesCustomComponent": "true"
                },
                {
                    "multiple": true,
                    "modelName": "condition-shrub-survey"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.323Z",
            "updatedAt": "2023-03-27T04:44:01.323Z"
        }
    },
    {
        "id": 35,
        "attributes": {
            "name": "Intervention Project",
            "module": "intervention",
            "endpointPrefix": "/intervention-general-project-informations",
            "version": 1,
            "description": "Intervention General Project",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "intervention-general-project-information"
                },
                {
                    "multiple": true,
                    "modelName": "intervention",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.336Z",
            "updatedAt": "2023-03-27T04:44:01.336Z"
        }
    },
    {
        "id": 36,
        "attributes": {
            "name": "Plot Visit",
            "module": "no module",
            "endpointPrefix": "/plot-visits",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-visit"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.350Z",
            "updatedAt": "2023-03-27T04:44:01.350Z"
        }
    },
    {
        "id": 37,
        "attributes": {
            "name": "Site Description and Layout",
            "module": "no module",
            "endpointPrefix": "/plot-locations",
            "version": 1,
            "description": "Define and layout a plot",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location"
                },
                {
                    "modelName": "plot-layout"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.362Z",
            "updatedAt": "2023-03-27T04:44:01.362Z"
        }
    },
    {
        "id": 38,
        "attributes": {
            "name": "Dev Sandbox Bulk Survey",
            "module": "no module",
            "endpointPrefix": "/dev-sandbox-bulk-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "dev-sandbox-bulk-survey"
                },
                {
                    "multiple": true,
                    "modelName": "dev-sandbox-bulk-observation"
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.378Z",
            "updatedAt": "2023-03-27T04:44:01.378Z"
        }
    },
    {
        "id": 39,
        "attributes": {
            "name": "Fire Survey",
            "module": "fire",
            "endpointPrefix": "/fire-surveys",
            "version": 1,
            "description": "Fire Survey",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "plot-location",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-layout",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "plot-visit",
                    "usesCustomComponent": "true"
                },
                {
                    "modelName": "fire-survey"
                },
                {
                    "multiple": true,
                    "modelName": "fire-point-intercept-point",
                    "usesCustomComponent": "true",
                    "newInstanceForRelationOnAttributes": [
                        "fire_species_intercepts"
                    ]
                },
                {
                    "multiple": true,
                    "modelName": "fire-char-observation",
                    "usesCustomComponent": "true"
                }
            ],
            "isHidden": true,
            "createdAt": "2023-03-27T04:44:01.391Z",
            "updatedAt": "2023-03-27T04:44:01.391Z"
        }
    },
    {
        "id": 40,
        "attributes": {
            "name": "Fauna Protocol Survey",
            "module": "targeted surveys",
            "endpointPrefix": "/fauna-protocol-surveys",
            "version": 1,
            "description": "Record survey type, survey effort and any observations of target fauna species or suitable habitat within the project area",
            "isWritable": true,
            "workflow": [
                {
                    "maximum": 5,
                    "multiple": true,
                    "modelName": "targeted-species-fauna"
                },
                {
                    "modelName": "fauna-protocol-survey",
                    "usesCustomComponent": "true"
                },
                {
                    "multiple": true,
                    "required": false,
                    "modelName": "fauna-observation-active",
                    "defaultHidden": true,
                    "usesCustomComponent": "true",
                    "newInstanceForRelationOnAttributes": [
                        "fauna_vouchers"
                    ]
                },
                {
                    "multiple": true,
                    "required": false,
                    "modelName": "fauna-observation-passive",
                    "defaultHidden": true,
                    "usesCustomComponent": "true"
                },
                {
                    "multiple": true,
                    "required": false,
                    "modelName": "fauna-passive-check",
                    "defaultHidden": true,
                    "usesCustomComponent": "true",
                    "newInstanceForRelationOnAttributes": [
                        "observations"
                    ]
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.405Z",
            "updatedAt": "2023-03-27T04:44:01.405Z"
        }
    },
    {
        "id": 41,
        "attributes": {
            "name": "Flora Protocol Survey",
            "module": "targeted surveys",
            "endpointPrefix": "/flora-protocol-surveys",
            "version": 1,
            "description": "Record survey effort and any observations of target flora species, populations or suitable habitat within the project area",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "flora-protocol-survey"
                },
                {
                    "maximum": 5,
                    "multiple": true,
                    "modelName": "targeted-species-flora"
                },
                {
                    "multiple": true,
                    "modelName": "flora-observation",
                    "usesCustomComponent": "true",
                    "newInstanceForRelationOnAttributes": [
                        "flora_vouchers"
                    ]
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.418Z",
            "updatedAt": "2023-03-27T04:44:01.418Z"
        }
    },
    {
        "id": 42,
        "attributes": {
            "name": "Ecological Community Protocol Survey",
            "module": "targeted surveys",
            "endpointPrefix": "/ecological-community-protocol-surveys",
            "version": 1,
            "description": "Record survey effort and any observations of target ecological communities within the project area",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "ecological-community-protocol-survey"
                },
                {
                    "multiple": true,
                    "modelName": "ecological-community-observation",
                    "usesCustomComponent": "true",
                    "newInstanceForRelationOnAttributes": [
                        "ecological_vouchers"
                    ]
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.430Z",
            "updatedAt": "2023-03-27T04:44:01.430Z"
        }
    },
    {
        "id": 43,
        "attributes": {
            "name": "Camera Trap Deployment Survey",
            "module": "camera traps",
            "endpointPrefix": "/camera-trap-surveys",
            "version": 1,
            "description": "",
            "isWritable": true,
            "workflow": [
                {
                    "modelName": "camera-trap-survey",
                    "usesCustomComponent": "true"
                },
                {
                    "multiple": true,
                    "modelName": "camera-trap-deployment-point",
                    "usesCustomComponent": "true",
                    "newInstanceForRelationOnAttributes": [
                        "features",
                        "camera_trap_information",
                        "camera_trap_settings"
                    ]
                }
            ],
            "isHidden": false,
            "createdAt": "2023-03-27T04:44:01.441Z",
            "updatedAt": "2023-03-27T04:44:01.441Z"
        }
    }
];

const settingValue = JSON.stringify(protocols);
const protocolSettingKey = 'paratoo.protocols';

updateSetting(protocolSettingKey, settingValue);

let protocolMapping =
    {
        "3": [13, 15, 23, 30],
        "13": [13, 15, 23, 30],
        "36": [13, 15, 23, 30],
        "37": [13, 15, 23, 30],
        "40": [13],
        "41": [15],
        "42": [15],
        "vegetation mapping": [15],
        "floristics": [15],
        "plant tissue vouchering": [15],
        "cover": [15],
        "basal area": [15],
        "coarse woody debris": [15],
        "recruitment": [15],
        "soils": [30],
        "vertebrate fauna": [13, 23],
        "invertebrate fauna": [13],
        "condition": [15]
    };
const mappingValue = JSON.stringify(protocolMapping);
const protocolMappingSettingKey = 'paratoo.service_protocol_mapping';

updateSetting(protocolMappingSettingKey, mappingValue);