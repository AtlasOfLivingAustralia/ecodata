grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.8
grails.project.source.level = 1.8
//grails.project.war.file = "target/${appName}-${appVersion}.war"
//grails.server.port.http = 8079

// uncomment (and adjust settings) to fork the JVM to isolate classpaths
grails.project.fork = [
   run: false,
   test: false // [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true] // configure settings for the test-app JVM
]

clover {
    on = false // Slows down testing individual classes too much.  Override by passing -clover.on to test-app e.g. grails test-app -clover.on unit:
    reports.dir = "target/clover/report"
    // One of these is a very large 3rd party timezone list and instrumenting the AuditService causes tests to fail.
    excludes = ['**/TimezoneMapper.java', '**/AuditService.groovy', '**/PermissionsController.groovy']
    reporttask = { ant, binding, self ->
        ant.mkdir(dir: "${clover.reports.dir}")
        ant.'clover-report' {

            ant.current(outfile: "${clover.reports.dir}") {
                format(type: "html")
                ant.columns {
                    lineCount()
                    complexity()
                    filteredElements(format: "bar")
                    uncoveredElements(format: "raw")
                    totalElements(format: "raw")
                    totalPercentageCovered()
                }
            }
        }
        ant.'clover-check'(target: "29%", haltOnFailure: true) { }
    }
}

grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
        excludes 'xercesImpl', 'xml-apis'

    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    //checksums true // Whether to verify checksums on resolve

    repositories {
        mavenLocal()
        mavenRepo "http://nexus.ala.org.au/content/groups/public/"
    }

    def seleniumVersion = "2.21.0"
    def geoToolsVersion = "11.2"
    def imgscalrVersion = "4.2"

    dependencies {
        compile "org.elasticsearch:elasticsearch:1.7.1"

        // Schema validation for external web service clients
        compile ("com.github.fge:json-schema-validator:2.1.6") {
            excludes "mailapi"
        }
		compile "com.itextpdf:itextpdf:5.5.1"
        compile "org.apache.httpcomponents:httpmime:4.2.1"

        compile "org.geotools.xsd:gt-xsd-kml:${geoToolsVersion}"
        compile "org.geotools:gt-shapefile:${geoToolsVersion}"
        compile "org.geotools:gt-geojson:${geoToolsVersion}"
        compile "org.geotools:gt-epsg-hsql:${geoToolsVersion}"

        compile "org.imgscalr:imgscalr-lib:${imgscalrVersion}"
        compile "com.drewnoakes:metadata-extractor:2.10.1"
        compile 'org.apache.poi:ooxml-schemas:1.4'
        compile 'org.apache.poi:poi:4.1.1'
        compile 'org.apache.poi:poi-ooxml:4.1.1'
        compile 'org.apache.poi:poi-ooxml-schemas:4.1.1'

        compile 'org.codehaus.jackson:jackson-core-asl:1.9.13'
        compile 'org.codehaus.jackson:jackson-mapper-asl:1.9.13'

        compile 'org.mongodb:mongo-java-driver:2.13.3'

        compile 'com.twelvemonkeys.imageio:imageio-jpeg:3.3.2'

        runtime "javax.transaction:jta:1.1" // Required as a side effect of ehcache field walking.


        test 'org.grails:grails-datastore-test-support:1.0.2-grails-2.4'
        test 'com.github.fakemongo:fongo:1.5.4'
        test 'org.openclover:clover:4.3.0'

    }

    plugins {
        compile ":asset-pipeline:1.9.9" // 2.13 has issues with java7, possibly there is a middle ground between 1.9.9 and 2.13

        // Uncomment these to enable additional asset-pipeline capabilities
        //compile ":sass-asset-pipeline:2.13.1"
        //compile ":less-asset-pipeline:2.13.1"
        //compile ":coffee-asset-pipeline:2.13.1"
        //compile ":handlebars-asset-pipeline:2.13.1"
        runtime ":csv:0.3.1"

        // This version of ala-auth plugin pulls in dependencies we are implicitly relying on - updating to 2.x
        // results in compile time errors.
        runtime ":ala-auth:1.3.1"
        runtime ":ala-bootstrap2:1.2"
        runtime ":cors:1.1.8"

        compile ":mongodb:3.0.3"
        compile ":quartz:1.0.2"
        compile ":excel-export:0.2.1"
        compile ":excel-import:1.0.1"
        compile ":mail:1.0.7"

        build ":tomcat:7.0.55.2"
        build ":release:3.0.1"
        compile ':cache:1.1.8'
        compile ":cache-ehcache:1.0.5"

        test 'org.grails.plugins:clover:4.3.0'
        
    }


}
