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
   test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true] // configure settings for the test-app JVM
]
grails.project.dependency.resolver = "maven"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
        excludes 'xercesImpl'

    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    //checksums true // Whether to verify checksums on resolve

    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()

        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
		mavenRepo "http://mvnrepository.com"
        mavenRepo "http://maven.ala.org.au/repository"
        mavenRepo "http://oss.sonatype.org/content/repositories/releases/"
        mavenRepo "http://download.osgeo.org/webdav/geotools/"
    }

    def seleniumVersion = "2.21.0"
    def geoToolsVersion = "11.2"
    def imgscalrVersion = "4.2"
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.

        test "org.gebish:geb-spock:0.9.3"
        test("org.seleniumhq.selenium:selenium-htmlunit-driver:$seleniumVersion") {
            exclude "xml-apis"
        }
        test("org.seleniumhq.selenium:selenium-chrome-driver:$seleniumVersion")
        test("org.seleniumhq.selenium:selenium-firefox-driver:$seleniumVersion")

        // ElasticSearch
        compile "org.elasticsearch:elasticsearch:1.1.2"

        // Schema validation for external web service clients
        compile "com.github.fge:json-schema-validator:2.1.6"
		compile "com.itextpdf:itextpdf:5.5.1"
        compile "org.apache.httpcomponents:httpmime:4.1.2"

        compile "org.geotools.xsd:gt-xsd-kml:${geoToolsVersion}"
        compile "org.geotools:gt-shapefile:${geoToolsVersion}"
        compile "org.geotools:gt-geojson:${geoToolsVersion}"
        compile "org.geotools:gt-epsg-hsql:${geoToolsVersion}"

        compile "org.imgscalr:imgscalr-lib:${imgscalrVersion}"


        test 'org.grails:grails-datastore-test-support:1.0.1-grails-2.4'
        test 'com.github.fakemongo:fongo:1.5.4'

    }

    plugins {
        runtime ":jquery:1.11.1"
        runtime ":resources:1.2.8"
        runtime ":csv:0.3.1"
        runtime ":ala-web-theme:1.0.0"

        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0"
        //runtime ":cached-resources:1.0"
        //runtime ":yui-minify-resources:0.1.5"

        compile ":mongodb:3.0.2"
        compile ":quartz:1.0.2"
        compile ":excel-export:0.2.0"
        compile ":excel-import:1.0.1"

        build ":tomcat:7.0.52.1"
        build ":release:3.0.1"
        compile ':cache:1.1.8'
        compile ":cache-ehcache:1.0.2"

        test ":geb:0.9.3"


    }
}
