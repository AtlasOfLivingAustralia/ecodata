package au.org.ala.ecodata

import geb.Browser
import geb.spock.GebReportingSpec
import org.apache.log4j.Logger
//import pages.HomePage
import spock.lang.Shared

/**
 * Helper class for functional tests in fieldcapture.
 */
class FunctionalTestHelper extends GebReportingSpec {

    static Logger log = Logger.getLogger(FunctionalTestHelper.class)

    @Shared def testConfig

    def setupSpec() {
        def filePath = new File('grails-app/conf/application.groovy').toURI().toURL()
        def configSlurper = new ConfigSlurper(System.properties.get('grails.env'))
        testConfig = configSlurper.parse(filePath)
        def props = new Properties()
        if (testConfig.default_config) {
            def externalConfigFile = new File(testConfig.default_config)
            if (externalConfigFile.exists()) {
                externalConfigFile.withInputStream {
                    props.load(it)
                }
            }
            testConfig.merge(configSlurper.parse(props))
        }
    }

    void useDataSet(String dataSetName) {

        def dataSetPath = getClass().getResource("/resources/"+dataSetName+"/").getPath()

        log.info("Using dataset from: ${dataSetPath}")
        def userName = System.getProperty('grails.mongo.username') ?: ""
        def password = System.getProperty('grails.mongo.password') ?: ""
        int exitCode = "./scripts/loadFunctionalTestData.sh ${dataSetPath} ${userName} ${password}".execute().waitFor()
        if (exitCode != 0) {
            throw new RuntimeException("Loading data set ${dataSetPath} failed.  Exit code: ${exitCode}")
        }
    }

    def logout(Browser browser) {
        if ($('#logout-btn').displayed) {
            $('#logout-btn').click()
            //waitFor { at HomePage }
        }
        else {
            logoutViaUrl(browser)
        }

    }

    def logoutViaUrl(browser) {
        String serverUrl = getConfig().baseUrl ?: testConfig.grails.serverURL
        String logoutUrl = "${serverUrl}/logout/logout?appUrl=${serverUrl}"
        browser.go logoutUrl
    }

}
