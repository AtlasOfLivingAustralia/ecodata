package au.org.ala.ecodata

import grails.util.Holders
/*
 * Copyright (C) 2022 Atlas of Living Australia
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
 * Created by Temi on 14/2/22.
 */
/**
 * DocumentUrlBuilder is used on elasticserch an search hit to construct full URL path for documents withs relative URL path.
 * Full URL path is constructed by adding hostname to relative path. Hostname is identified at {@link DocumentHostInterceptor#before}
 */
class DocumentUrlBuilder {
    /**
     * Update document URL for a list of properties. Property can either be simple or nested.
     * Nested property can be added by separating key by '.'. For example, 'documents.thumbnail' can be used for a document
     * like the following.
     * <pre>
     *     [
     *      'documents':
     *          [
     *              [ 'thumbnail': '/document/download/2021-04/123.jpeg' ]
     *          ]
     *     ]
     * </pre>
     * @param source - elasticsearch hit's source
     * @param hostName - host name to update document URL
     * @return
     */
    static Map updateDocumentURL (Map source, String hostName) {
        def paths = Holders.grailsApplication.config.getProperty("app.path.document.url", List)
        if (hostName) {
            paths.each { String path ->
                String [] parts = path.split('\\.')
                List listOfKeys = Arrays.asList(parts)
                updateDocumentURLForPath(source, listOfKeys, hostName)
            }
        }

        source
    }

    /**
     * Traverse the source object to update a simple or nested property.
     * @param source
     * @param keys
     * @param hostName - host name to update document URL
     * @return
     */
    static def updateDocumentURLForPath(source, List keys, String hostName){
        def temp = source
        def previous = source
        List navigatedPath = []
        keys?.each{ key ->
            if(temp instanceof Map){
                previous = temp
                temp = temp[key]
            } else if(temp instanceof List){
                temp.each { map ->
                    updateDocumentURLForPath(map, keys - navigatedPath, hostName)
                }

                temp = null
            } else {
                temp = null
            }

            navigatedPath.add(key)
        }

        if(temp != null){
            if(temp instanceof String){
                previous[navigatedPath.last()] = addHostName(temp, hostName)
            }
        }

        source
    }

    /**
     * Add host name to relative path only
     * @param value
     * @param hostName - host name to update document URL
     * @return
     */
    static String addHostName(String value, String hostName) {
        String uploadPath = Holders.grailsApplication.config.getProperty("app.uploads.url", String)
        if (uploadPath.startsWith("/") && value.startsWith(uploadPath) && hostName) {
            return hostName + value
        }

        value
    }
}
