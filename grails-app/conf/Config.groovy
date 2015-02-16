/******************************************************************************\
 *  CONFIG MANAGEMENT
 \******************************************************************************/
def appName = 'ecodata'
def ENV_NAME = "${appName.toUpperCase()}_CONFIG"
default_config = "/data/${appName}/config/${appName}-config.properties"
if(!grails.config.locations || !(grails.config.locations instanceof List)) {
    grails.config.locations = []
}

// add ala skin conf (needed for version >= 0.1.10)
grails.config.locations.add("classpath:ala-config.groovy")

if(System.getenv(ENV_NAME) && new File(System.getenv(ENV_NAME)).exists()) {
    println "[${appName}] Including configuration file specified in environment: " + System.getenv(ENV_NAME);
    grails.config.locations.add "file:" + System.getenv(ENV_NAME)
} else if(System.getProperty(ENV_NAME) && new File(System.getProperty(ENV_NAME)).exists()) {
    println "[${appName}] Including configuration file specified on command line: " + System.getProperty(ENV_NAME);
    grails.config.locations.add "file:" + System.getProperty(ENV_NAME)
} else if(new File(default_config).exists()) {
    println "[${appName}] Including default configuration file: " + default_config;
    grails.config.locations.add "file:" + default_config
} else {
    println "[${appName}] No external configuration file defined."
}

println "[${appName}] (*) grails.config.locations = ${grails.config.locations}"

grails.project.groupId = 'au.org.ala' // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
    all:           '*/*',
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    xlsx:          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    xls:           'application/vnd.ms-excel',
    xml:           ['text/xml', 'application/xml']
]

/******************************************************************************\
 *  RELOADABLE CONFIG
\******************************************************************************/
reloadable.cfgs = ["file:/data/${appName}/config/${appName}-config.properties"]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

grails.mongo.default.mapping = {
    version false
}

/******************************************************************************\
 *  APPLICATION CONFIG
 \*****************************************************************************/
if(!app.dump.location){
    app.dump.location = "/data/ecodata/dump/"
}
if(!app.elasticsearch.location){
    app.elasticsearch.location = "/data/ecodata/elasticsearch/"
}
if(!app.elasticsearch.indexAllOnStartup){
    app.elasticsearch.indexAllOnStartup = true
}
if(!app.elasticsearch.indexOnGormEvents){
    app.elasticsearch.indexOnGormEvents = true
}
if(!app.http.header.userId){
    app.http.header.userId = "X-ALA-userId"
}
if(!app.file.upload.path){
    app.file.upload.path = "/data/ecodata/uploads"
}
if(!app.external.api.version){
    app.external.api.version = 'draft'
}

if(!webservice.connectTimeout){
    webservice.connectTimeout = 10000
}
if(!webservice.readTimeout){
    webservice.readTimeout = 20000
}
// spatial services
if(!spatial.baseUrl){
    spatial.baseUrl = "http://spatial-dev.ala.org.au"
}
if (!spatial.intersectUrl) {
    spatial.intersectUrl = spatial.baseUrl + '/ws/intersect/'
}
if (!spatial.intersectBatchUrl) {
    spatial.intersectBatchUrl = spatial.baseUrl + '/ws/intersect/batch/'
}
if(!google.geocode.url){
    google.geocode.url = "https://maps.googleapis.com/maps/api/geocode/json?sensor=false&latlng="
}

// Specifies the spatial portal layers that will be intersected with sites to provide the geographic faceting
// on the home and search pages.
// Each gridded facet becomes a search facet.
// Each entry under the grouped facets becomes a facet, with the possible facet terms provided by intersecting the site
// with each layer in the group.
// The special facets are gridded facets that are too large for the spatial portal so are managed locally by ecodata.
// Please note that changes to these facets require that all sites in the system be re-processed - this can be
// done using the admin tools in fieldcapture.
app {
    facets {
        geographic {
            gridded {
                state = 'cl927'
                nrm = 'cl916'
                lga = 'cl959'
                ibra = 'cl20'
                imcra4_pb = 'cl21'
                elect = 'cl958';
            }
            grouped {
                other {
                    australian_coral_ecoregions = 'cl917'
                    ramsar = 'cl935'
                    diwa_type_criteria = 'cl901'
                    tams_reserves = 'cl1054'
                    nswlls = 'cl2012'
                    eez_poly = 'cl929'
                    ntd = 'cl2009'
                    alcw4 = 'cl990'
                    ger_initiative = 'cl2049'
                    ipa_7aug13 = 'cl2015'
                    ilua = 'cl2010'
                    east_afa_final = 'cl900'

                }
                gerSubRegion {
                    gerBorderRanges = 'cl1062'
                    gerIllawarraToShoalhaven = 'cl1064'
                    gerSouthernHighlands = 'cl1070'
                    gerHunterValley = 'cl1063'
                    gerJaliigirr = 'cl1065'
                    gerKanangraBoydToWyangalaLink = 'cl2048'
                    gerKosciuszkoToCoast = 'cl1067'
                    gerSlopesToSummit = 'cl1069'
                    gerVictoriaBioLinks = 'cl2046'
                }
            }
            special {
                mvg = '/data/nvis_grids/mvg'
                mvs = '/data/nvis_grids/mvs'
            }
        }
    }
}


/******************************************************************************\
 *  EXTERNAL SERVERS
\******************************************************************************/
if (!ala.baseURL) {
    ala.baseURL = "http://www.ala.org.au"
}
if (!collectory.baseURL) {
    collectory.baseURL = "http://collections.ala.org.au/"
}
if (!headerAndFooter.baseURL) {
    headerAndFooter.baseURL = "http://www2.ala.org.au/commonui"
}
if (!security.apikey.serviceUrl) {
    security.apikey.serviceUrl = 'http://auth.ala.org.au/apikey/ws/check?apikey='
}
if(!security.cas.bypass){
    security.cas.bypass = false
}
if(!security.cas.adminRole){
    security.cas.adminRole = "ROLE_ADMIN"
}
if(!ecodata.use.uuids){
    ecodata.use.uuids = false
}

if (!grails.cache.ehcache) {
    grails {
        cache {
            ehcache {
                cacheManagerName = appName + '-ehcache'
                reloadable = false
            }
        }
    }
}

environments {
    development {
        grails.logging.jul.usebridge = true
        ecodata.use.uuids = false
        app.external.model.dir = "/data/ecodata/models/"
        grails.hostname = "devt.ala.org.au"
        serverName = "http://${grails.hostname}:8080"
        grails.app.context = "ecodata"
        grails.serverURL = serverName + "/" + grails.app.context
        security.cas.appServerName = serverName
        security.cas.contextPath = "/" + appName
        app.uploads.url = "${grails.serverURL}/document/download?filename="
        app.elasticsearch.indexAllOnStartup = true
        app.elasticsearch.indexOnGormEvents = true
    }
    test {
        rails.logging.jul.usebridge = true
        ecodata.use.uuids = false
        app.external.model.dir = "/data/ecodata/models/"
        grails.hostname = "devt.ala.org.au"
        serverName = "http://${grails.hostname}:8080"
        grails.app.context = "ecodata"
        grails.serverURL = serverName + "/" + grails.app.context
        security.cas.appServerName = serverName
        security.cas.contextPath = "/" + appName
        app.uploads.url = "${grails.serverURL}/document/download?filename="
        app.elasticsearch.indexOnGormEvents = true
        app.elasticsearch.indexAllOnStartup = false // Makes integration tests slow to start
    }
    production {
        grails.logging.jul.usebridge = false
    }
}

// log4j configuration

//this can be overridden in the external configuration
if (!logging.dir) {
    logging.dir = (System.getProperty('catalina.base') ? System.getProperty('catalina.base') + '/logs'  : '/var/log/tomcat7')
}
def loggingDir = logging.dir
log4j = {
    appenders {
        environments{
            development {
                console name: "stdout",
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n"),
                        threshold: org.apache.log4j.Level.DEBUG
                rollingFile name: "ecodataLog",
                        maxFileSize: 104857600,
                        file: loggingDir+"/ecodata.log",
                        threshold: org.apache.log4j.Level.INFO,
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n")
                rollingFile name: "stacktrace",
                        maxFileSize: 104857600,
                        file: loggingDir+"/ecodata-stacktrace.log"
            }
            test {
                console name: "stdout",
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n"),
                        threshold: org.apache.log4j.Level.DEBUG
                rollingFile name: "ecodataLog",
                        maxFileSize: 104857600,
                        file: loggingDir+"/ecodata-test.log",
                        threshold: org.apache.log4j.Level.INFO,
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n")
                rollingFile name: "stacktrace",
                        maxFileSize: 104857600,
                        file: loggingDir+"/ecodata-test-stacktrace.log"
            }
            production {
                rollingFile name: "ecodataLog",
                        maxFileSize: 104857600,
                        file: loggingDir+"/ecodata.log",
                        threshold: org.apache.log4j.Level.INFO,
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n")
                rollingFile name: "stacktrace",
                        maxFileSize: 104857600,
                        file: loggingDir+"/ecodata-stacktrace.log"
            }
        }
    }

    environments {
        development {
            all additivity: false, stdout: [
                    'grails.app.controllers.au.org.ala.ecodata',
                    'grails.app.domain.au.org.ala.ecodata',
                    'grails.app.services.au.org.ala.ecodata',
                    'grails.app.taglib.au.org.ala.ecodata',
                    'grails.app.conf.au.org.ala.ecodata',
                    'grails.app.filters.au.org.ala.ecodata'/*,
                    'au.org.ala.cas.client'*/
            ]
        }
    }

    all additivity: false, ecodataLog: [
            'grails.app.controllers.au.org.ala.ecodata',
            'grails.app.domain.au.org.ala.ecodata',
            'grails.app.services.au.org.ala.ecodata',
            'grails.app.taglib.au.org.ala.ecodata',
            'grails.app.conf.au.org.ala.ecodata',
            'grails.app.filters.au.org.ala.ecodata'
    ]

    debug 'grails.app.controllers.au.org.ala','au.org.ala.ecodata'

    error  'org.codehaus.groovy.grails.web.servlet',        // controllers
            'org.codehaus.groovy.grails.web.pages',          // GSP
            'org.codehaus.groovy.grails.web.sitemesh',       // layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping',        // URL mapping
            'org.codehaus.groovy.grails.commons',            // core / classloading
            'org.codehaus.groovy.grails.plugins',            // plugins
            'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'
}
