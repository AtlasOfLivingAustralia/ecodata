grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"
//grails.server.port.http = 8079

// uncomment (and adjust settings) to fork the JVM to isolate classpaths
grails.project.fork = [
   run: false,
   test: false // [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true] // configure settings for the test-app JVM
]
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
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.

        // ElasticSearch
        compile "org.elasticsearch:elasticsearch:1.7.1"

        // Schema validation for external web service clients
        compile "com.github.fge:json-schema-validator:2.1.6"
		compile "com.itextpdf:itextpdf:5.5.1"
        compile "org.apache.httpcomponents:httpmime:4.2.1"

        compile "org.geotools.xsd:gt-xsd-kml:${geoToolsVersion}"
        compile "org.geotools:gt-shapefile:${geoToolsVersion}"
        compile "org.geotools:gt-geojson:${geoToolsVersion}"
        compile "org.geotools:gt-epsg-hsql:${geoToolsVersion}"

        compile "org.imgscalr:imgscalr-lib:${imgscalrVersion}"
        compile "org.apache.poi:ooxml-schemas:1.0"

        runtime "javax.transaction:jta:1.1" // Required as a side effect of ehcache field walking.


        test 'org.grails:grails-datastore-test-support:1.0.2-grails-2.4'
        test 'com.github.fakemongo:fongo:1.5.4'

        //compile 'org.codehaus.gpars:gpars:0.11' // Breaks Grails test runner
//        compile 'org.apache.activemq:activemq-core:5.3.0'

//        runtime 'org.springframework:spring-jms:3.0.2.RELEASE'
//        runtime 'org.springframework:spring-beans:3.0.2.RELEASE'
//        runtime 'org.springframework:spring-core:3.0.2.RELEASE'
    }

    plugins {
        runtime ":jquery:1.11.1"
        runtime ":resources:1.2.8"
        runtime ":csv:0.3.1"
        runtime ":ala-auth:1.3.1"
        runtime ":ala-bootstrap2:1.2"

        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0"
        //runtime ":cached-resources:1.0"
        //runtime ":yui-minify-resources:0.1.5"

        compile ":mongodb:3.0.2"
        compile ":quartz:1.0.2"
        compile ":excel-export:0.2.1"
        compile ":excel-import:1.0.1"

        build ":tomcat:7.0.55"
        build ":release:3.0.1"
        compile ':cache:1.1.8'
        compile ":cache-ehcache:1.0.5"
        
    }
}
