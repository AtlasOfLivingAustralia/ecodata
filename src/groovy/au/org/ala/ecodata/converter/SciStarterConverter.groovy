package au.org.ala.ecodata.converter

import grails.util.Holders
import groovy.util.logging.Log4j
import org.apache.commons.lang.StringUtils

import java.text.SimpleDateFormat

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
 * Created by Temi on 9/03/16.
 *
 * A class which maps SciStarter properties to Biocollect properties.
 * A property value is either copied exactly or it can be transformed to a new value. Transformation example is date,
 * tags etc.
 * A transformation example. If you have a list of values and need to covert to a comma separated string, the below
 * mapping is how it can be done.
 * Map mapping = [      'tags'               : [
                        'name'     : 'keywords',
                        'transform': { props, target ->
                            StringUtils.join(props.tags ?: [], ',')
                        }]]
 */

@Log4j
class SciStarterConverter {
    public static convert(Map sciStarter, Map override = [:]) {
        Map mapping = [
                'id'          : 'sciStarterId',
                'title'       : [
                        'name'     : 'name',
                        'transform': { props, target ->
                            props.title?.trim()
                        }],
                'tags'        : [
                        'name'     : 'keywords',
                        'transform': { props, target ->
                            StringUtils.join(props.tags ?: [], ',')
                        }],
                'search_terms': [
                        'name'     : 'keywords',
                        'transform': { props, target ->
                            if (target.keywords && props.search_terms) {
                                target.keywords + "," + props.search_terms
                            } else {
                                target.keywords + props.search_terms
                            }
                        }],
                'goal'        : 'aim',
                'task'        : 'task',
                'description' : 'description',
                'url'         : 'urlWeb',
                'image'       : [
                        'name'     : 'image',
                        'transform': { props, target ->
                            if (props.image?.contains(Holders.grailsApplication.config.scistarter.baseUrl)) {
                                props.image
                            } else {
                                "${Holders.grailsApplication.config.scistarter.baseUrl}/${props.image}"
                            }
                        }],
                'difficulty'  : 'difficulty',
                'begin_date'  : [
                        'name'     : 'plannedStartDate',
                        'transform': { props, target ->
                            SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd');
                            if (props.begin_date) {
                                sdf.parse(props.begin_date)
                            } else {
                                new Date()
                            }
                        }],
                'end_date'    : [
                        'name'     : 'plannedEndDate',
                        'transform': { props, target ->
                            SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd');
                            if (props.end_date) {
                                sdf.parse(props.end_date)
                            }
                        }],
                'state'       : 'state',
                'image_credit': 'attribution',
                'presenter'   : 'organisationName',
                'topics'      : [
                        'name'     : 'scienceType',
                        'transform': { props, target ->
                            List approvedScienceType = Holders.grailsApplication.config.biocollect.scienceType
                            List lowerScienceType = approvedScienceType.collect { it?.toLowerCase() }
                            List scienceTypes = []
                            props?.topics?.each { String type ->
                                String lowerType = type?.toLowerCase()
                                if (lowerType in lowerScienceType) {
                                    scienceTypes.push(lowerType)
                                }
                            }

                            scienceTypes
                        }
                ]
        ];

        // default values
        Map target = [
                "funding"                : 0,
                "hasParticipantCost"     : false,
                "hasTeachingMaterials"   : false,
                "isCitizenScience"       : true,
                "isDIY"                  : false,
                "isDataSharing"          : false,
                "isExternal"             : true,
                "isMERIT"                : false,
                "isMetadataSharing"      : true,
                "isSuitableForChildren"  : false,
                "name"                   : null,
                "promoteOnHomepage"      : "no",
                "status"                 : "active",
                "associatedOrgs"         : [],
                "collectoryInstitutionId": null,
                "termsOfUseAccepted"     : true,
                "dataSharing"            : "Disabled",
                "aim"                    : "Imported from SciStarter",
                "description"            : "Imported from SciStarter",
                "difficulty"             : "Medium",
                "gear"                   : "",
                "getInvolved"            : "",
                "keywords"               : "",
                "manager"                : null,
                "plannedStartDate"       : null,
                "plannedEndDate"         : null,
                "projectType"            : "citizenScience",
                "scienceType"            : [],
                "task"                   : null,
                "projectSiteId"          : null,
                "organisationId"         : null,
                "organisationName"       : null,
                "sciStarterId"           : null,
                "isSciStarter"           : true,
                "attribution"            : null,
                "managerEmail"           : "contact@scistarter.com",
                "urlWeb"                 : null,
                "image"                  : null,
                "state"                  : null
        ] << override;

        // iterate through mapping variable and copy or tranform the value
        mapping.each { key, value ->
            log.debug(key)
            if (value instanceof Map) {
                if (value.transform) {
                    target[value.name] = value.transform(sciStarter, target);
                }
            }

            if (value instanceof String) {
                if (sciStarter[key] != null) {
                    target[value] = sciStarter[key]
                }
            }
        }

        target
    }

    static Map siteMapping(Map props) {
        Map site = [
                "projects"    : [
                ],
                "isSciStarter": true,
                "status"      : "active",
                "poi"         : [],
                "geoIndex"    : [
                        "type"       : null,
                        "coordinates": null
                ],
                "extent"      : [
                        "source"  : "drawn",
                        "geometry": [
                                "centre"     : null,
                                "type"       : null,
                                "coordinates": null
                        ]
                ],
                "externalId"  : "",
                "description" : null,
                "name"        : null,
                "notes"       : "",
                "type"        : "surveyArea"
        ]

        switch (props.geometry.type) {
            case 'MultiPolygon':
                site.name = props.name;
                // possible data loss here. converting multipolygon to polygon since biocollect/merit does not support it.
                site.geoIndex.coordinates = site.extent.geometry.coordinates = props.geometry.coordinates[0]
                site.geoIndex.type = site.extent.geometry.type = "Polygon"
                break
        }

        site
    }

    /**
     * check if the SciStarter meets import conditions.
     * 1. if project topic is in the white list of topics
     * @return
     */
    static Boolean canImportProject(Map project) {
        List topicWhiteList = Holders.grailsApplication.config.biocollect.scienceType
        List intersect = project.topics?.intersect(topicWhiteList)
        if (intersect?.size() > 0) {
            return true
        } else {
            return false
        }
    }
}
