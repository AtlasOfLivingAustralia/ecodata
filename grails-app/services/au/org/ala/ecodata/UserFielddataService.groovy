package au.org.ala.ecodata
import groovy.json.JsonSlurper
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair

class UserFielddataService {

    def serviceMethod() {}

    def grailsApplication

    def webService

    private def userListMap = [:]

    private def userEmailMap = [:]

    /**
     * Returns null if lookup fails
     * @param email
     * @return
     */
    def syncUserIdLookup(email){
       def result = doPost(grailsApplication.config.userDetailsSingleUrl, ["userName": email])
       if (!result.error){
          result.resp.userId
       } else {
           null
       }
    }

    def refreshUserDetails(){
        try {
            def replacementEmailMap = [:]
            def replacementIdMap = [:]
            def userListJson = doPost(grailsApplication.config.userDetailsUrl)
            log.info "Refreshing user lists....."
            if (userListJson && !userListJson.error) {
                userListJson.resp.each {
                    replacementEmailMap.put(it.email.toLowerCase(),  it.id);
                    replacementIdMap.put(it.id.toString(), it.firstName + " " + it.lastName);
                }
                log.info "Refreshing user lists.....count: " + replacementEmailMap.size()
                synchronized (this){
                    this.userEmailMap = replacementEmailMap
                    this.userListMap = replacementIdMap
                }
            } else {
                log.info "error -  " + userListJson.getClass() + ":"+ userListJson
            }
        } catch (Exception e) {
            log.error ("Cache refresh error" + e.message, e)
        }
    }

    def getUserEmailToIdMap() {
        this.userEmailMap
    }

    def getUserNamesForIdsMap() {
        this.userListMap
    }

    def doPost(String url) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost post = new HttpPost(url)
        try {
            def response = httpclient.execute(post)
            def content = response.getEntity().getContent()
            def jsonSlurper = new JsonSlurper()
            def json = jsonSlurper.parse(new InputStreamReader(content))
            return [error:  null, resp: json]
        } catch (SocketTimeoutException e) {
            def error = [error: "Timed out calling web service. URL= \${url}."]
            log.error(error.error)
            return [error: error]
        } catch (Exception e) {
            def error = [error: "Failed calling web service. ${e.getClass()} ${e.getMessage()} ${e} URL= ${url}."]
            println error.error
            return [error: error]
        } finally {
            post.releaseConnection()
        }
    }

    def doPost(String url, Map kvpairs) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpPost post = new HttpPost(url)

        //add parameters
        List<NameValuePair> nvps = new ArrayList<NameValuePair>()
        kvpairs.each {k,v -> nvps.add(new BasicNameValuePair(k,v)) }
        post.setEntity(new UrlEncodedFormEntity(nvps))

        try {
            def response = httpclient.execute(post)
            def content = response.getEntity().getContent()
            def jsonSlurper = new JsonSlurper()
            def json = jsonSlurper.parse(new InputStreamReader(content))
            return [error:  null, resp: json]
        } catch (SocketTimeoutException e) {
            def error = [error: "Timed out calling web service. URL= \${url}."]
            log.error(error.error)
            return [error: error]
        } catch (Exception e) {
            def error = [error: "Failed calling web service. ${e.getClass()} ${e.getMessage()} ${e} URL= ${url}."]
            println error.error
            return [error: error]
        } finally {
            post.releaseConnection()
        }
    }
}