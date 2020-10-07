/*
 * Copyright (C) 2013 Atlas of Living Australia
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
 */

package au.org.ala.ecodata

import grails.converters.JSON
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.servlet.HttpHeaders
import org.springframework.http.MediaType

import javax.servlet.http.HttpServletResponse
/**
 * Helper class for invoking web services.
 */
class WebService {

    // Used to avoid a circular dependency during initialisation
    def getUserService() {
        return grailsApplication.mainContext.userService
    }
    
    def grailsApplication

    def get(String url, boolean includeUserId) {
        def conn = null
        try {
            conn = configureConnection(url, includeUserId)
            return responseText(conn)
        } catch (SocketTimeoutException e) {
            def error = [error: "Timed out calling web service. URL= ${url}."]
            log.error error
            return error
        } catch (Exception e) {
            def error = [error: "Failed calling web service. ${e.getClass()} ${e.getMessage()} URL= ${url}.",
                    statusCode: conn?.responseCode?:"",
                    detail: conn?.errorStream?.text]
            log.error error, e
            return error
        }
    }

    private int defaultTimeout() {
        grailsApplication.config.webservice.readTimeout as int
    }

    private URLConnection configureConnection(String url, boolean includeUserId, Integer timeout = null) {
        URLConnection conn = new URL(url).openConnection()

        def readTimeout = timeout?:defaultTimeout()
        conn.setConnectTimeout(grailsApplication.config.webservice.connectTimeout as int)
        conn.setReadTimeout(readTimeout)

        if (includeUserId) {
            def user = getUserService().getUser()
            if (user) {
                conn.setRequestProperty(grailsApplication.config.app.http.header.userId, user.userId)
            }

        }
        conn
    }

    /**
     * Proxies a request URL but doesn't assume the response is text based. (Used for proxying requests to
     * ecodata for excel-based reports)
     */
    def proxyGetRequest(HttpServletResponse response, String url, boolean includeUserId = true, boolean includeApiKey = false, List headers = [HttpHeaders.CONTENT_DISPOSITION]) {

        HttpURLConnection conn = configureConnection(url, includeUserId)
        if (includeApiKey) {
            conn.setRequestProperty("Authorization", grailsApplication.config.api_key);
        }

        response.setContentType(conn.getContentType())
        response.setContentLength(conn.getContentLength())

        headers.each { header ->
            response.setHeader(header, conn.getHeaderField(header))
        }
        response.status = conn.responseCode
        response.outputStream << conn.inputStream

    }

    def get(String url) {
        return get(url, true)
    }


    def getJson(String url, Integer timeout = null, Map headers = null) {
        def conn = null
        try {
            conn = configureConnection(url, false, timeout)
            if (headers) {
                headers.each {key, value  ->
                    conn.setRequestProperty(key, value)
                }
            }

            def json = responseText(conn)
            return JSON.parse(json)
        } catch (ConverterException e) {
            def error = ['error': "Failed to parse json. ${e.getClass()} ${e.getMessage()} URL= ${url}."]
            log.error error
            return error
        } catch (SocketTimeoutException e) {
            def error = [error: "Timed out getting json. URL= ${url}."]
            println error
            return error
        } catch (ConnectException ce) {
            log.info "Exception class = ${ce.getClass().name} - ${ce.getMessage()}"
            def error = [error: "ecodata service not available. URL= ${url}."]
            println error
            return error
        } catch (Exception e) {
            log.info "Exception class = ${e.getClass().name} - ${e.getMessage()}"
            def error = [error: "Failed to get json from web service. ${e.getClass()} ${e.getMessage()} URL= ${url}.",
                         statusCode: conn?.responseCode?:"",
                         detail: conn?.errorStream?.text]
            log.error error
            return error
        }
    }

    def getJsonRepeat(String url, int repeatCount = 12, Integer timeout = null){

        def status
        int repeat = 0;
        while((status = getJson(url,timeout))?.error && repeat < repeatCount){
            sleep(5000)
            repeat++
        }
        status
    }
    /**
     * Reads the response from a URLConnection taking into account the character encoding.
     * @param urlConnection the URLConnection to read the response from.
     * @return the contents of the response, as a String.
     */
    def responseText(urlConnection) {

        def charset = 'UTF-8' // default
        def contentType = urlConnection.getContentType()
        if (contentType) {
            def mediaType = MediaType.parseMediaType(contentType)
            charset = (mediaType.charSet)?mediaType.charSet.toString():'UTF-8'
        }
        return urlConnection.content.getText(charset)
    }

    /**
     * Do post call with params
     * @param url the URLConnection to read the response from.
     * @param params
     * @return the contents of the response as a map
     */
    Map doPostWithParams(String url, Map params, boolean suppressLog = false) {
        def conn = null
        def charEncoding = 'utf-8'
        try {
            String query = ""
            boolean first = true
            for (String name:params.keySet()) {
                query+=first?"?":"&"
                first = false
                query+=name.encodeAsURL()+"="+params.get(name).encodeAsURL()
            }
            conn = new URL(url+query).openConnection()
            conn.setRequestMethod("POST")
            conn.setDoOutput(true)
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), charEncoding)

            wr.flush()
            def resp = conn.inputStream.text
            wr.close()
            return [resp: JSON.parse(resp?:"{}")]
        } catch (SocketTimeoutException e) {
            def error = [error: "Timed out calling web service. URL= ${url}."]
            if (!suppressLog) {
                log.error(error, e)
            }

            return error
        } catch (Exception e) {
            def error = [error: "Failed calling web service. ${e.getMessage()} URL= ${url}.",
                         statusCode: conn?.responseCode?:"",
                         detail: conn?.errorStream?.text]
            if (!suppressLog) {
                log.error(error, e)
            }
            return error
        }
    }

    Map doPost(String url, Map postBody) {
        HttpURLConnection conn = null
        def charEncoding = 'utf-8'
        try {
            conn = new URL(url).openConnection()
            conn.setRequestMethod("POST")
            conn.setDoOutput(true)
            conn.setRequestProperty("Content-Type", "application/json;charset=${charEncoding}");
            conn.setRequestProperty("Authorization", "${grailsApplication.config.api_key}");

            def user = getUserService().getCurrentUserDetails()
            if (user && user.userId) {
                conn.setRequestProperty(grailsApplication.config.app.http.header.userId, user.userId)
                conn.setRequestProperty("Cookie", "ALA-Auth="+java.net.URLEncoder.encode(user.userName, charEncoding))
            }
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), charEncoding)
            wr.write((postBody as JSON).toString())
            wr.flush()
            def resp = conn.inputStream.text
            wr.close()
            return [resp: JSON.parse(resp?:"{}"), headers: conn.getHeaderFields()] // fail over to empty json object if empty response string otherwise JSON.parse fails
        } catch (SocketTimeoutException e) {
            def error = [error: "Timed out calling web service. URL= ${url}."]
            log.error(error, e)
            return error
        } catch (Exception e) {
            def error = [error: "Failed calling web service. ${e.getMessage()} URL= ${url}.",
                    statusCode: conn?.responseCode?:"",
                    detail: conn?.errorStream?.text]
            log.error(error, e)
            return error
        }
    }

    Map doPost(String url, String postBody, boolean includeAuthKey = true, Map headers = null, boolean addALACookie = true) {
        HttpURLConnection conn = null
        def charEncoding = 'utf-8'
        try {
            conn = new URL(url).openConnection()
            conn.setRequestMethod("POST")
            conn.setDoOutput(true)
            conn.setRequestProperty("Content-Type", "text/plain;charset=${charEncoding}");
            if (includeAuthKey) {
                conn.setRequestProperty("Authorization", "${grailsApplication.config.api_key}");

            }

            if (addALACookie) {
                def user = getUserService().getCurrentUserDetails()
                if (user && user.userId) {
                    conn.setRequestProperty(grailsApplication.config.app.http.header.userId, user.userId)
                    conn.setRequestProperty("Cookie", "ALA-Auth=" + java.net.URLEncoder.encode(user.userName, charEncoding))
                }
            }

            if (headers) {
                headers.each {key, value  ->
                    conn.setRequestProperty(key, value)
                }
            }

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), charEncoding)
            wr.write(postBody)
            wr.flush()
            def resp = conn.inputStream.text
            wr.close()
            return [resp: JSON.parse(resp?:"{}"), headers: conn.getHeaderFields()] // fail over to empty json object if empty response string otherwise JSON.parse fails
        } catch (SocketTimeoutException e) {
            def error = [error: "Timed out calling web service. URL= ${url}."]
            log.error(error, e)
            return error
        } catch (Exception e) {
            def error = [error: "Failed calling web service. ${e.getMessage()} URL= ${url}.",
                         statusCode: conn?.responseCode?:"",
                         detail: conn?.errorStream?.text]
            log.error(error, e)
            return error
        }
    }

    def doDelete(String url) {
        url += (url.indexOf('?') == -1 ? '?' : '&') + "api_key=${grailsApplication.config.api_key}"
        def conn = null
        try {
            conn = new URL(url).openConnection()
            conn.setRequestMethod("DELETE")
            conn.setRequestProperty("Authorization", grailsApplication.config.api_key);
            def user = getUserService().getUser()
            if (user) {
                conn.setRequestProperty(grailsApplication.config.app.http.header.userId, user.userId)
            }
            return conn.getResponseCode()
        } catch(Exception e){
            println e.message
            return 500
        } finally {
            if (conn != null){
                conn?.disconnect()
            }
        }
    }

    def doDelete(String url, Map headers) {
        def conn = null
        try {
            conn = new URL(url).openConnection()
            conn.setRequestMethod("DELETE")
            if(headers) {
                headers.each {key, value  ->
                    conn.setRequestProperty(key, value)
                }
            }

            return conn.getResponseCode()
        } catch(Exception e){
            println e.message
            return 500
        } finally {
            if (conn != null){
                conn?.disconnect()
            }
        }
    }

    def extractIdFromLocationHeader(Map result) {
        return result?.headers?.location?.first().toString().tokenize('/').last()
    }

}
