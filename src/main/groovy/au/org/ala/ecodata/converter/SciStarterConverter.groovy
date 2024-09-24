package au.org.ala.ecodata.converter

import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
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

@Slf4j
class SciStarterConverter {

    public static final String NO_ORGANISATION_NAME = "Organisation not provided"

    public static convert(Map sciStarter, Map override = [:]) {
        Map mapping = [
                'legacy_id'          : 'externalId',
                'name'       : [
                        'name'     : 'name',
                        'transform': { props, target ->
                            props.name?.trim()
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
                'url'         : [
                        'name': 'urlWeb',
                        'transform': { props, target ->
                            try {
                                String url = props.url.replace("www.", "").replaceAll("\n","")

                                new URL(url)
                                url
                            } catch (error) {
                                log.info("${props.url} is not a valid URL, returning null...")
                                null
                            }
                        }
                ],
                'image'       : 'image',
                'difficulty'  : [
                        'name'     : 'difficulty',
                        'transform': { props, target ->
                            [
                                    'easy': 'Easy',
                                    'medium': 'Medium',
                                    'difficult': 'Hard',
                                    'unknown': null
                            ][props.difficulty ? props.difficulty.label.toLowerCase() : 'unknown']
                        }
                ],
                'begin'  : [
                        'name'     : 'plannedStartDate',
                        'transform': { props, target ->
                            props.begin ? new Date(props.begin.longValue()) : new Date()
                        }],
                'end'    : [
                        'name'     : 'plannedEndDate',
                        'transform': { props, target ->
                            props.end ? new Date(props.end.longValue()) : new Date()
                        }],
                'created'        : [
                        'name'     : 'dateCreated',
                        'transform': { props, target ->
                            props.created ? new Date(props.created.longValue()) : new Date()
                        }],
                'updated'     : [
                        'name'     : 'lastUpdated',
                        'transform': { props, target ->
                            props.updated ? new Date(props.updated.longValue()) : new Date()
                        }],
                'state'       : 'state',
                'image_credit': 'attribution',
                'presenter'   : 'organisationName',
                'topics'      : [ // TODO: check topic mapping parity
                        'name'     : 'scienceType',
                        'transform': { props, target ->
                            List approvedScienceType = Holders.grailsApplication.config.getProperty('biocollect.scienceType', List)
                            List scienceTypes = []
                            props?.topics?.each { topic ->
                                String lowerType = topic.label.toLowerCase()
                                approvedScienceType?.each { String scienceType ->
                                    if (scienceType.toLowerCase() == lowerType) {
                                        scienceTypes.push(scienceType)
                                    }
                                }
                            }

                            scienceTypes
                        }
                ],
                "origin"      : "origin",
                "country"     : [ // TODO: check topic mapping parity
                        name       : "countries",
                        'transform': { props, target ->
                            List countries = Holders.grailsApplication.config.getProperty('countries', List)
                            String country
                            if (props.country instanceof String) {
                                country = countries.find { String cntry ->
                                    cntry.toLowerCase() == props.country?.toLowerCase()
                                }
                            }

                            List countryAsList = country ? [country] : []

                            if (country?.size()) {
                                return countryAsList
                            } else {
                                return ["United States of America (USA)"]
                            }
                        }
                ],
                "united_nations_region"  : [
                        name       : "uNRegions",
                        'transform': { props, target ->
                            List uNRegions = Holders.grailsApplication.config.getProperty('uNRegions', List)
                            List matchedRegions = []
                            props.UN_regions?.each { String region ->
                                String matchedRegion = uNRegions.find { String UN_region ->
                                    UN_region.toLowerCase() == region.toLowerCase()
                                }

                                if (matchedRegion) matchedRegions.push(matchedRegion)
                            }

                            if (matchedRegions.size()) {
                                return matchedRegions
                            } else {
                                return ["Americas – Northern America"]
                            }
                        }
                ]
        ]

        // default values
        Map target = [
                "funding"                : 0,
                "hasTeachingMaterials"   : false,
                "isCitizenScience"       : true,
                "isDIY"                  : false,
                "isDataSharing"          : false,
                "isExternal"             : true,
                "isMERIT"                : false,
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
                "organisationName"       : NO_ORGANISATION_NAME,
                "externalId"             : null,
                "isSciStarter"           : true,
                "attribution"            : null,
                "managerEmail"           : "contact@scistarter.com",
                "urlWeb"                 : null,
                "image"                  : null,
                "state"                  : null,
                "importDate"             : new Date(),
                "origin"                 : "scistarter",
                "countries"              : [],
                "unRegions"              : []
        ] << override;

        // iterate through mapping variable and copy or tranform the value
        mapping.each { key, value ->
            log.debug(key.toString())
            if (value instanceof Map) {
                if (value.transform) {
                    target[value.name] = value.transform(sciStarter, target);
                }
            }

            if (value instanceof String) {
                def sciStarterValue = sciStarter[key]
                if (sciStarterValue != null && !sciStarterValue.toString().isEmpty()) {
                    target[value] = sciStarter[key]
                }
            }
        }

        target
    }

    static Map siteMapping(Map project) {
        JsonSlurper slurper = new JsonSlurper();
        Map geometry = slurper.parseText(project.regions)

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
                "type"        : "projectArea"
        ]

        switch (geometry.type) {
            case 'MultiPolygon':
                site.name = project.regions_description;
                // possible data loss here. converting multipolygon to polygon since biocollect/merit does not support it.
                site.geoIndex.coordinates = site.extent.geometry.coordinates = geometry.coordinates[0]
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
        List topicWhiteList = Holders.grailsApplication.config.getProperty('biocollect.scienceType', List)
        List intersect = project.topics?.intersect(topicWhiteList)
        if (intersect?.size() > 0) {
            return true
        } else {
            return false
        }
    }
}
