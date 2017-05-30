/******************************************************************************\
 *  CONFIG MANAGEMENT
 \******************************************************************************/
def appName = 'ecodata'
def ENV_NAME = "${appName.toUpperCase()}_CONFIG"
default_config = "/data/${appName}/config/${appName}-config.properties"
if (!grails.config.locations || !(grails.config.locations instanceof List)) {
    grails.config.locations = []
}

if (System.getenv(ENV_NAME) && new File(System.getenv(ENV_NAME)).exists()) {
    println "[${appName}] Including configuration file specified in environment: " + System.getenv(ENV_NAME);
    grails.config.locations.add "file:" + System.getenv(ENV_NAME)
} else if (System.getProperty(ENV_NAME) && new File(System.getProperty(ENV_NAME)).exists()) {
    println "[${appName}] Including configuration file specified on command line: " + System.getProperty(ENV_NAME);
    grails.config.locations.add "file:" + System.getProperty(ENV_NAME)
} else if (new File(default_config).exists()) {
    println "[${appName}] Including default configuration file: " + default_config;
    grails.config.locations.add "file:" + default_config
} else {
    println "[${appName}] No external configuration file defined."
}

// The layers config is best expressed in groovy format.
def layers_config = "/data/${appName}/config/layers-config.groovy"
if (new File(layers_config).exists()) {
    grails.config.locations.add "file:" + layers_config
}



println "[${appName}] (*) grails.config.locations = ${grails.config.locations}"

grails.project.groupId = 'au.org.ala' // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = true
grails.mime.types = [
        all          : '*/*',
        atom         : 'application/atom+xml',
        css          : 'text/css',
        csv          : 'text/csv',
        form         : 'application/x-www-form-urlencoded',
        html         : ['text/html', 'application/xhtml+xml'],
        js           : 'text/javascript',
        json         : ['application/json', 'text/json'],
        multipartForm: 'multipart/form-data',
        rss          : 'application/rss+xml',
        text         : 'text/plain',
        xlsx         : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        xls          : 'application/vnd.ms-excel',
        xml          : ['text/xml', 'application/xml'],
        shp          : 'application/zip'
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
grails.web.disable.multipart = false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

grails.mongo.default.mapping = {
    version false
}

cors.url.pattern = ['/document/download']

/******************************************************************************\
 *  APPLICATION CONFIG
 \*****************************************************************************/

//the ID of the default project to use for ad-hoc record submissions
records.default.projectId = "4084c7ea-94f6-42f2-9c65-da18dcabf08"
records.default.dataResourceId = "dr364"

if (!app.dump.location) {
    app.dump.location = "/data/ecodata/dump/"
}
if (!app.elasticsearch.location) {
    app.elasticsearch.location = "/data/ecodata/elasticsearch/"
}
if (!app.elasticsearch.indexAllOnStartup) {
    app.elasticsearch.indexAllOnStartup = true
}
if (!app.elasticsearch.indexOnGormEvents) {
    app.elasticsearch.indexOnGormEvents = true
}
if (!app.http.header.userId) {
    app.http.header.userId = "X-ALA-userId"
}
if (!app.file.upload.path) {
    app.file.upload.path = "/data/ecodata/uploads"
}
if (!app.file.archive.path) {
    app.file.archive.path = "/data/ecodata/archive"
}
if (!app.external.api.version) {
    app.external.api.version = 'draft'
}

if (!webservice.connectTimeout) {
    webservice.connectTimeout = 10000
}
if (!webservice.readTimeout) {
    webservice.readTimeout = 20000
}
// spatial services
if (!spatial.baseUrl) {
    spatial.baseUrl = "http://spatial-dev.ala.org.au"
}
if (!spatial.intersectUrl) {
    spatial.intersectUrl = spatial.baseUrl + '/ws/intersect/'
}
if (!spatial.intersectBatchUrl) {
    spatial.intersectBatchUrl = spatial.baseUrl + '/ws/intersect/batch/'
}
if (!google.geocode.url) {
    google.geocode.url = "https://maps.googleapis.com/maps/api/geocode/json?sensor=false&latlng="
}
if (!temp.file.cleanup.days) {
    temp.file.cleanup.days = 1
}


if (!biocollect.scienceType) {
    biocollect.scienceType = [
            "Animals",
            "Agricultural & veterinary science",
            "Astronomy",
            "Biology",
            "Biodiversity",
            "Biogeography",
            "Birds",
            "Chemical sciences",
            "Climate & meteorology",
            "Ecology",
            "Ecology & Environment",
            "Fire Ecology",
            "Genetics",
            "Geology & soils",
            "Geomorphology",
            "Indigenous science",
            "Indigenous knowledge",
            "Information & computing sciences",
            "Insects & Pollinators",
            "Long-Term Species Monitoring",
            "Marine & Terrestrial",
            "Medical & human health",
            "Nature & Outdoors",
            "NRM",
            "Ocean",
            "Physical science",
            "Social sciences",
            "Symbyotic Interactions",
            "Technology",
            "Water"
    ]
}

if(!biocollect.dataCollectionWhiteList){
    biocollect.dataCollectionWhiteList = [
            "Animals",
            "Biodiversity",
            "Biogeography",
            "Birds",
            "Ecology",
            "Ecology & Environment",
            "Fire Ecology",
            "Insects & Pollinators",
            "Long-Term Species Monitoring",
            "NRM",
            "Predator-Prey Interactions",
            "Symbyotic Interactions"
    ]
}

if (!biocollect.ecoScienceType) {
    biocollect.ecoScienceType = [
            "Agroecology",
            "Behavioural Ecology",
            "Biodiversity Inventory",
            "Species Composition",
            "Structural Assemblage",
            "Biogeography",
            "Bioregional Inventory",
            "Chemical Ecology",
            "Competition/Resource Partitioning",
            "Decomposition",
            "Disease Ecology",
            "Disturbances",
            "Ecological Succession",
            "Ecophysiology",
            "Ecosystem Modelling",
            "Ecotoxicology",
            "Evolutionary Ecology",
            "Fire Ecology",
            "Functional Ecology",
            "Global Ecology",
            "Herbivory",
            "Landscape Ecology",
            "Long-Term Community Monitoring",
            "Long-Term Species Monitoring",
            "Macroecology",
            "Molecular Ecology",
            "None",
            "Other",
            "Paleoecology",
            "Pollination",
            "Population Dynamics",
            "Predator-Prey Interactions",
            "Productivity",
            "Restoration Ecology",
            "Soil Ecology",
            "Species Decline",
            "Species Distribution Modelling",
            "Symbyotic Interactions",
            "Urban Ecology"
    ]
}

if (!uNRegions) {
    uNRegions = [
            "Africa",
            "Americas – South America",
            "Americas – Central America",
            "Americas – Northern America",
            "Asia",
            "Europe",
            "Oceania"
    ]
}

if (!countries) {
    countries = [
            "Worldwide",
            "---------",
            "Afghanistan",
            "Albania",
            "Algeria",
            "Andorra",
            "Angola",
            "Antigua and Barbuda",
            "Argentina",
            "Armenia",
            "Australia",
            "Austria",
            "Azerbaijan",
            "Bahamas",
            "Bahrain",
            "Bangladesh",
            "Barbados",
            "Belarus",
            "Belgium",
            "Belize",
            "Benin",
            "Bhutan",
            "Bolivia",
            "Bosnia and Herzegovina",
            "Botswana",
            "Brazil",
            "Brunei",
            "Bulgaria",
            "Burkina Faso",
            "Burundi",
            "Cabo Verde",
            "Cambodia",
            "Cameroon",
            "Canada",
            "Central African Republic (CAR)",
            "Chad",
            "Chile",
            "China",
            "Colombia",
            "Comoros",
            "Democratic Republic of the Congo",
            "Costa Rica",
            "Cote d'Ivoire",
            "Croatia",
            "Cuba",
            "Cyprus",
            "Czech Republic",
            "Denmark",
            "Djibouti",
            "Dominica",
            "Dominican Republic",
            "Ecuador",
            "Egypt",
            "El Salvador",
            "Equatorial Guinea",
            "Eritrea",
            "Estonia",
            "Ethiopia",
            "Fiji",
            "Finland",
            "France",
            "Gabon",
            "Gambia",
            "Georgia",
            "Germany",
            "Ghana",
            "Greece",
            "Grenada",
            "Guatemala",
            "Guinea",
            "Guinea-Bissau",
            "Guyana",
            "Haiti",
            "Honduras",
            "Hungary",
            "Iceland",
            "India",
            "Indonesia",
            "Iran",
            "Iraq",
            "Ireland",
            "Israel",
            "Italy",
            "Jamaica",
            "Japan",
            "Jordan",
            "Kazakhstan",
            "Kenya",
            "Kiribati",
            "Kosovo",
            "Kuwait",
            "Kyrgyzstan",
            "Laos",
            "Latvia",
            "Lebanon",
            "Lesotho",
            "Liberia",
            "Libya",
            "Liechtenstein",
            "Lithuania",
            "Luxembourg",
            "Macedonia",
            "Madagascar",
            "Malawi",
            "Malaysia",
            "Maldives",
            "Mali",
            "Malta",
            "Marshall Islands",
            "Mauritania",
            "Mauritius",
            "Mexico",
            "Micronesia",
            "Moldova",
            "Monaco",
            "Mongolia",
            "Montenegro",
            "Morocco",
            "Mozambique",
            "Myanmar (Burma)",
            "Namibia",
            "Nauru",
            "Nepal",
            "Netherlands",
            "New Zealand",
            "Nicaragua",
            "Niger",
            "Nigeria",
            "North Korea",
            "Norway",
            "Oman",
            "Pakistan",
            "Palau",
            "Palestine",
            "Panama",
            "Papua New Guinea",
            "Paraguay",
            "Peru",
            "Philippines",
            "Poland",
            "Portugal",
            "Qatar",
            "Romania",
            "Russia",
            "Rwanda",
            "Saint Kitts and Nevis",
            "Saint Lucia",
            "Saint Vincent and the Grenadines",
            "Samoa",
            "San Marino",
            "Sao Tome and Principe",
            "Saudi Arabia",
            "Senegal",
            "Serbia",
            "Seychelles",
            "Sierra Leone",
            "Singapore",
            "Slovakia",
            "Slovenia",
            "Solomon Islands",
            "Somalia",
            "South Africa",
            "South Korea",
            "South Sudan",
            "Spain",
            "Sri Lanka",
            "Sudan",
            "Suriname",
            "Swaziland",
            "Sweden",
            "Switzerland",
            "Syria",
            "Taiwan",
            "Tajikistan",
            "Tanzania",
            "Thailand",
            "Timor-Leste",
            "Togo",
            "Tonga",
            "Trinidad and Tobago",
            "Tunisia",
            "Turkey",
            "Turkmenistan",
            "Tuvalu",
            "Uganda",
            "Ukraine",
            "United Arab Emirates (UAE)",
            "United Kingdom (UK)",
            "United States of America (USA)",
            "Uruguay",
            "Uzbekistan",
            "Vanuatu",
            "Vatican City (Holy See)",
            "Venezuela",
            "Vietnam",
            "Yemen",
            "Zambia",
            "Zimbabwe"
    ]
}

if(!spatial.geoJsonEnvelopeConversionThreshold){
    spatial.geoJsonEnvelopeConversionThreshold = 1_000_000
}

if(!biocollect.facets.project){
    biocollect.facets.project = [
        "organisationFacet",
        "uNRegions",
        "countries",
        "origin",
        "scienceType",
        "tags",
        "difficulty",
        "status",
        "typeOfProject",
        "ecoScienceType",
        "associatedProgramFacet",
        "siteNameFacet",
        "associatedSubProgramFacet",
        "plannedStartDate"
    ]
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
            contextual {
                state = 'cl927'
                nrm = 'cl2120'
                lga = 'cl959'
                ibra = 'cl20'
                imcra4_pb = 'cl21'
                elect = 'cl10874'
                cmz = 'cl2112'
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
    collectory.baseURL = "http://collectory-dev.ala.org.au/"
    collectory.dataProviderUid.merit = 'dp245'
    collectory.dataProviderUid.biocollect = 'dp244'
    collectory.collectoryIntegrationEnabled = false
}
if (!headerAndFooter.baseURL) {
    headerAndFooter.baseURL = "http://www2.ala.org.au/commonui"
}
    if (!security.apikey.serviceUrl) {
    security.apikey.serviceUrl = 'https://auth.ala.org.au/apikey/ws/check?apikey='
}
if (!biocacheService.baseURL) {
    biocacheService.baseURL = 'http://biocache.ala.org.au/ws'
}
if (!imagesService.baseURL) {
    imagesService.baseURL = 'http://images-dev.ala.org.au'
}
if (!security.cas.bypass) {
    security.cas.bypass = false
}
if (!security.cas.adminRole) {
    security.cas.adminRole = "ROLE_ADMIN"
}
if (!ecodata.use.uuids) {
    ecodata.use.uuids = false
}
if (!userDetailsSingleUrl) {
    userDetailsSingleUrl = "https://auth.ala.org.au/userdetails/userDetails/getUserDetails"
}
if (!userDetailsUrl) {
    userDetailsUrl = "http://auth.ala.org.au/userdetails/userDetails/getUserListFull"
}

if (!authGetKeyUrl) {
    authGetKeyUrl = "https://m.ala.org.au/mobileauth/mobileKey/generateKey"
}

if (!authCheckKeyUrl) {
    authCheckKeyUrl = "https://m.ala.org.au/mobileauth/mobileKey/checkKey"
}


if (!grails.cache.ehcache) {
    grails {
        cache {
            enabled = true
            ehcache {
                cacheManagerName = appName + '-ehcache'
                reloadable = true
                diskStore = '/data/${appName}/ehcache'
            }
        }
    }
}
grails.cache.config = {

    provider {
        name "${appName}-ehcache"
    }
    diskStore {
        path "/data/${appName}/ehcache"
    }
    cache {
        name 'userDetailsCache'
        timeToLiveSeconds 60 * 60 * 24
        maxElementsInMemory 2000
        maxElementsOnDisk 2000
        overflowToDisk true
        diskPersistent true
    }
}

environments {
    development {
        grails.logging.jul.usebridge = true
        ecodata.use.uuids = false
        app.external.model.dir = "/data/ecodata/models/"
        grails.hostname = "devt.ala.org.au"
        app.elasticsearch.indexAllOnStartup = false
        app.elasticsearch.indexOnGormEvents = true
    }
    test {
        // Override disk store so the travis build doesn't fail.
        grails.cache.config = {
            diskStore {
                path '/tmp'
            }
        }
        grails.logging.jul.usebridge = true
        ecodata.use.uuids = false
        app.external.model.dir = "./models/"
        grails.hostname = "devt.ala.org.au"
        // Only for travis CI, they must be overriden by ecodata-config.properties
        serverName = "http://${grails.hostname}:8080"
        grails.app.context = "ecodata"
        grails.serverURL = serverName + "/" + grails.app.context
        app.uploads.url = "${grails.serverURL}/document/download?filename="

        app.elasticsearch.indexOnGormEvents = true
        app.elasticsearch.indexAllOnStartup = false // Makes integration tests slow to start
        app.elasticsearch.location = "./target/elasticsearch/"
        app.file.upload.path = "./target/uploads"
        app.file.upload.path = "./target/archive"
    }
    production {
        grails.logging.jul.usebridge = false
        app.elasticsearch.indexAllOnStartup = false // Makes deployments too slow
    }
}

// log4j configuration - this can be overridden in the external configuration
if (!logging.dir) {
    logging.dir = (System.getProperty('catalina.base') ? System.getProperty('catalina.base') + '/logs' : '/var/log/tomcat7')
}
def loggingDir = logging.dir
//if logging not available (e.g. Travis) log to /tmp and avoid errors
if (!new File(loggingDir).exists()) {
    loggingDir = "/tmp"
}

println "[${appName}] Logging to ${loggingDir}"

log4j = {
    appenders {
        environments {
            development {
                console name: "stdout",
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n"),
                        threshold: org.apache.log4j.Level.DEBUG
                rollingFile name: "ecodataLog",
                        maxFileSize: 104857600,
                        file: loggingDir + "/ecodata.log",
                        threshold: org.apache.log4j.Level.INFO,
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n")
                rollingFile name: "stacktrace",
                        maxFileSize: 104857600,
                        file: loggingDir + "/ecodata-stacktrace.log"
            }
            test {
                console name: "stdout",
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n"),
                        threshold: org.apache.log4j.Level.DEBUG
                rollingFile name: "ecodataLog",
                        maxFileSize: 104857600,
                        file: loggingDir + "/ecodata-test.log",
                        threshold: org.apache.log4j.Level.INFO,
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n")
                rollingFile name: "stacktrace",
                        maxFileSize: 104857600,
                        file: loggingDir + "/ecodata-test-stacktrace.log"
            }
            production {
                rollingFile name: "ecodataLog",
                        maxFileSize: 104857600,
                        file: loggingDir + "/ecodata.log",
                        threshold: org.apache.log4j.Level.INFO,
                        layout: pattern(conversionPattern: "%d %-5p [%c{1}]  %m%n")
                rollingFile name: "stacktrace",
                        maxFileSize: 104857600,
                        file: loggingDir + "/ecodata-stacktrace.log"
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
                    'grails.app.filters.au.org.ala.ecodata',
//                    'au.org.ala.cas.client'*/
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

    debug 'grails.app.controllers.au.org.ala', 'au.org.ala.ecodata' //, 'grails.plugin.cache'

    error 'org.codehaus.groovy.grails.web.servlet',        // controllers
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
