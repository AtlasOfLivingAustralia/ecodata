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

    public static final String NO_ORGANISATION_NAME = "Organisation not provided"

    public static convert(Map sciStarter, Map override = [:]) {
        Map mapping = [
                'id'          : 'externalId',
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
                            if (Holders.grailsApplication.config.scistarter.forceHttpsUrls == 'true') {
                                try {
                                    URL oldUrl = new URL(props.image)
                                    URL newUrl = new URL("https", oldUrl.getHost(), oldUrl.getPort(), oldUrl.getFile())
                                    newUrl.toString()
                                } catch (MalformedURLException e) {
                                    "${Holders.grailsApplication.config.scistarter.baseUrl}/${props.image}"
                                }
                            } else if (!props.image?.equals(null) && props.image?.contains(Holders.grailsApplication.config.scistarter.baseUrl)) {
                                props.image
                            } else if (!props.image?.equals(null)) {
                                "${Holders.grailsApplication.config.scistarter.baseUrl}/${props.image}"
                            } else {
                                return null
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
                'date'        : [
                        'name'     : 'dateCreated',
                        'transform': { props, target ->
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            if (props.date) {
                                sdf.parse(props.date)
                            }
                        }],
                'updated'     : [
                        'name'     : 'lastUpdated',
                        'transform': { props, target ->
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            if (props.updated) {
                                sdf.parse(props.updated)
                            }
                        }],
                'state'       : 'state',
                'image_credit': 'attribution',
                'presenter'   : 'organisationName',
                'topics'      : [
                        'name'     : 'scienceType',
                        'transform': { props, target ->
                            List approvedScienceType = Holders.grailsApplication.config.biocollect.scienceType
                            List scienceTypes = []
                            props?.topics?.each { String type ->
                                String lowerType = type?.toLowerCase()
                                approvedScienceType?.each { String scienceType ->
                                    if(scienceType.toLowerCase() == lowerType){
                                        scienceTypes.push(scienceType)
                                    }
                                }
                            }

                            scienceTypes
                        }
                ],
                "origin"      : "origin",
                "country"     : [
                        name       : "countries",
                        'transform': { props, target ->
                            List countries = Holders.grailsApplication.config.countries
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
                "UN_regions"  : [
                        name       : "uNRegions",
                        'transform': { props, target ->
                            List uNRegions = Holders.grailsApplication.config.uNRegions
                            List matchedRegions = []
                            props.UN_regions?.each { String region ->
                                String matchedRegion = uNRegions.find { String UN_region ->
                                    UN_region.toLowerCase() == region.toLowerCase()
                                }

                                if (matchedRegion) {
                                    matchedRegions.push(matchedRegion)
                                }
                            }

                            if(matchedRegions.size()){
                                return matchedRegions
                            } else {
                                return ["Americas – Northern America"]
                            }
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
                "unRegions"             : []
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
                def sciStarterValue = sciStarter[key]
                if (sciStarterValue != null && !sciStarterValue.toString().isEmpty()) {
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
                "type"        : "projectArea"
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
