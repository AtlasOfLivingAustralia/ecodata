package au.org.ala.ecodata.converter

import groovy.util.logging.Log4j
import org.apache.commons.lang.StringUtils
import grails.util.Holders

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
                'id'                 : 'sciStarterId',
                'title'              : 'name',
                'tags'               : [
                        'name'     : 'keywords',
                        'transform': { props, target ->
                            StringUtils.join(props.tags ?: [], ',')
                        }],
                'search_terms'       : [
                        'name'     : 'keywords',
                        'transform': { props, target ->
                            target.keywords + props.search_terms
                        }],
                'goal'               : 'aim',
                'task'               : 'task',
                'project_owner_email': 'managerEmail',
                'description'        : 'description',
                'url'                : 'urlWeb',
                'image'              : [
                        'name'     : 'image',
                        'transform': { props, target ->
                            "${Holders.grailsApplication.config.scistarter.baseUrl}/${props.url}"
                        }],
                'difficulty'         : 'difficulty',
                'begin_date'         : [
                        'name'     : 'plannedStartDate',
                        'transform': { props, target ->
                            SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd');
                            if (props.begin_date) {
                                return sdf.parse(props.begin_date)
                            }
                        }],
                'end_date'           : [
                        'name'     : 'plannedEndDate',
                        'transform': { props, target ->
                            SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd');
                            if (props.end_date) {
                                return sdf.parse(props.end_date)
                            }
                        }],
                'state'              : 'state',
                'image_credit'       : 'attribution',
                'presenter'          : 'organisationName'
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
                "aim"                    : "",
                "description"            : "",
                "difficulty"             : "Medium",
                "gear"                   : "",
                "getInvolved"            : "",
                "keywords"               : "",
                "manager"                : null,
                "plannedStartDate"       : null,
                "plannedEndDate"         : null,
                "projectType"            : "citizenScience",
                "scienceType"            : "biodiversity",
                "task"                   : null,
                "projectSiteId"          : null,
                "organisationId"         : null,
                "organisationName"       : null,
                "sciStarterId"           : null,
                "isSciStarter"           : true,
                "attribution"            : null,
                "managerEmail"           : null,
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
}
