
def appName = 'ecodata'

environments {
    development {
        grails {
            mongodb {
                host = "localhost"
                port = "27017"
                databaseName = "ecodata"
            }
        }
    }
    test {
        grails {
            mongodb {
                host = "localhost"
                port = "27017"
                databaseName = "ecodata-test"
            }
        }
    }
    meritfunctionaltest {
        grails {
            mongodb {
                host = "localhost"
                port = "27017"
                databaseName = "ecodata-functional-test"
            }
        }
    }
    production {
        grails {
            mongodb {
                host = "localhost"
                port = "27017"
                databaseName = "ecodata"
                options {
                    autoConnectRetry = true
                    connectionsPerHost = 100
                }
            }
        }
        server.servlet.session.cookie.secure = true
    }
}

grails.mongodb.default.mapping = {
    version false
    '*'(reference:true)
}


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
    webservice.readTimeout = 30000
}
// spatial services
if (!spatial.baseUrl) {
    spatial.baseUrl = "https://spatial-test.ala.org.au"
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
if(!paratoo.location.excluded) {
    paratoo.location.excluded = ['location.vegetation-association-nvis']
}
access.expiry.maxEmails=500


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

homepageIdx {
    elasticsearch   {
        fieldsAndBoosts {
            name = 50
            description = 30
            organisationName = 30
        }
    }
}

// Specifies the spatial portal layers that will be intersected with sites to provide the geographic faceting
// on the home and search pages.
// Each gridded facet becomes a search facet.
// Each entry under the grouped facets becomes a facet, with the possible facet terms provided by intersecting the site
// with each layer in the group.
// The special facets are gridded facets that are too large for the spatial portal so are managed locally by ecodata.
// Please note that changes to these facets require that all sites in the system be re-processed - this can be
// done using the admin tools in fieldcapture.

//TODO update elect field for electorate boundaries update
app {
    facets {
        geographic {
            contextual {
                state = 'cl927'
                nrm = 'cl10946'
                lga = 'cl959'
                ibra = 'cl20'
                imcra4_pb = 'cl21'
                elect = 'cl10921'
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
    ala.baseURL = "https://www.ala.org.au"
}
bie.ws.url = "https://bie-ws.ala.org.au/"
bie.url = "https://bie.ala.org.au/"
namesmatching.url = "https://namematching-ws-test.ala.org.au/"
namematching.strategy = ["exactMatch", "vernacularMatch"]

if (!collectory.baseURL) {
    //collectory.baseURL = "https://collectory-dev.ala.org.au/"
    collectory.baseURL = "https://collections-test.ala.org.au/"
    collectory.dataProviderUid.merit = 'dp245'
    collectory.dataProviderUid.biocollect = 'dp244'
    collectory.collectoryIntegrationEnabled = false
}
if (!headerAndFooter.baseURL) {
    headerAndFooter.baseURL = "https://www.ala.org.au/commonui-bs3"//"https://www2.ala.org.au/commonui"
}
if (!security.apikey.serviceUrl) {
    security.apikey.serviceUrl = 'https://auth-test.ala.org.au/apikey/ws/check?apikey='
}
if (!biocacheService.baseURL) {
    biocacheService.baseURL = 'https://biocache.ala.org.au/ws'
}
if (!imagesService.baseURL) {
    imagesService.baseURL = 'https://images-dev.ala.org.au'
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

ecodata.documentation.exampleProjectUrl = 'http://ecodata-test.ala.org.au/ws/activitiesForProject/746cb3f2-1f76-3824-9e80-fa735ae5ff35'
// Used by ParatooService to sync available protocols
paratoo.core.baseUrl = 'https://dev.core-api.monitor.tern.org.au/api'
paratoo.excludeInterventionProtocols = true
paratoo.core.documentationUrl = '/documentation/swagger.json'

auth.baseUrl = 'https://auth-test.ala.org.au'
userDetails.web.url = "${auth.baseUrl}/userdetails/"
userDetails.api.url = "${auth.baseUrl}/userdetails/userDetails/"

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


security {
    cas {
        enabled = false
        appServerName = 'http://localhost:8080' // or similar, up to the request path part
        casServerUrlPrefix = 'https://auth-test.ala.org.au/cas'
        loginUrl = 'https://auth-test.ala.org.au/cas/login'
        logoutUrl = 'https://auth-test.ala.org.au/cas/logout'
        casServerName = 'https://auth-test.ala.org.au'
        uriFilterPattern = ['/admin/*', '/activityForm/*', '/graphql/*']
        authenticateOnlyIfLoggedInPattern = "/graphql/*"
        uriExclusionFilterPattern = ['/assets/.*','/images/.*','/css/.*','/js/.*','/less/.*', '/activityForm/get.*']
    }
    oidc {
        enabled = true
        discoveryUri = 'https://auth-test.ala.org.au/cas/oidc/.well-known'
        clientId = 'changeMe'
        secret = 'changeMe'
        scope = 'openid,profile,ala,roles'
        connectTimeout = 20000
        readTimeout = 20000
    }
    jwt {
        enabled = true
        discoveryUri = 'https://auth-test.ala.org.au/cas/oidc/.well-known'
        requiredClaims = ["sub", "iat", "exp", "jti", "client_id"]
        urlPatterns = ["/ws/graphql/*"]
        requiredScopes = []
        connectTimeoutMs = 20000
        readTimeoutMs = 20000
    }
}
webservice.jwt = false
webservice['jwt-scopes'] = "ala/internal users/read ala/attrs"
webservice['client-id']='changeMe'
webservice['client-secret'] = 'changeMe'

grails.gorm.graphql.browser = true

environments {
    development {
        grails.logging.jul.usebridge = true
        ecodata.use.uuids = false
        app.external.model.dir = "~/data/ecodata/models/"
        app.file.upload.path="~/data/ecodata/uploads"
        app.file.archive.path="~/data/ecodata/archives"
        temp.dir="~/data/ecodata/tmp"
        app.elasticsearch.indexAllOnStartup = false
        app.elasticsearch.indexOnGormEvents = true
        server.host="localhost"
        server.port=8080
        grails.hostname = "${server.host}"
        grails.serverURL = "http://${grails.hostname}:${server.port}"
        app.uploads.url = "/document/download/"
        grails.mail.host="localhost"
        grails.mail.port=1025


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
        grails.hostname = "localhost"
        // Only for travis CI, they must be overriden by ecodata-config.properties
        serverName = "http://${grails.hostname}:8080"
        grails.app.context = "ecodata"
        grails.serverURL = serverName + "/" + grails.app.context
        app.uploads.url = "${grails.serverURL}/document/download?filename="

        app.elasticsearch.indexOnGormEvents = true
        app.elasticsearch.indexAllOnStartup = false // Makes integration tests slow to start
        app.elasticsearch.location = "./target/elasticsearch/"
        app.file.upload.path = "./target/uploads"
        app.file.archive.path = "./target/archive"
        String casBaseUrl = "http://locahost:8018"
        userDetails.admin.url = "${casBaseUrl}/userdetails/ws/admin"

        wiremock.port = 8018
        security.cas.bypass = true
        security.cas.casServerUrlPrefix="http://localhost:${wiremock.port}/cas"
        security.cas.loginUrl="${security.cas.casServerUrlPrefix}/login"
    }
    meritfunctionaltest {
        grails.cache.config = {
            diskStore {
                path '/tmp'
            }
        }
        security.cas.bypass = true
        grails.logging.jul.usebridge = true
        ecodata.use.uuids = false
        app.external.model.dir = "./models/"
        grails.serverURL = "http://localhost:8080"
        app.uploads.url = "${grails.serverURL}/document/download?filename="

        app.elasticsearch.indexOnGormEvents = true
        app.elasticsearch.indexAllOnStartup = true
        app.file.upload.path = "./build/uploads"
        app.file.archive.path = "./build/archive"

        wiremock.port = 8018
        security.oidc.discoveryUri = "http://localhost:${wiremock.port}/cas/oidc/.well-known"
        security.oidc.allowUnsignedIdTokens = true
        def casBaseUrl = "http://localhost:${wiremock.port}"
        security.cas.casServerName="${casBaseUrl}"
        security.cas.contextPath=""
        security.cas.casServerUrlPrefix="${casBaseUrl}/cas"
        security.cas.loginUrl="${security.cas.casServerUrlPrefix}/login"
        security.cas.casLoginUrl="${security.cas.casServerUrlPrefix}/login"

        userDetails.web.url = "${casBaseUrl}/userdetails/"
        userDetails.api.url = "${casBaseUrl}/userdetails/"
        userDetails.admin.url = "${casBaseUrl}/userdetails/ws/admin"
        security.apikey.serviceUrl = "${casBaseUrl}/apikey/ws/check?apikey="

        grails.mail.host = 'localhost'
        grails.mail.port = 3025 // com.icegreen.greenmail.util.ServerSetupTest.SMTP.port
        // Schedule the audit thread frequently during functional tests to get less indexing errors because
        // the data was cleaned up before the audit ran
        audit.thread.schedule.interval = 500l;

        paratoo.core.baseUrl = "http://localhost:${wiremock.port}/monitor"
    }
    production {
        grails.logging.jul.usebridge = false
        app.elasticsearch.indexAllOnStartup = false // Makes deployments too slow
        app.external.model.dir = "/data/ecodata/models/"
    }
}

facets.data = [
        [
                name: "projectNameFacet",
                title: 'Project',
                dataType: 'text',
                helpText: 'Name of the project'
        ],
        [
                name:  "organisationNameFacet",
                title: 'Organisation',
                dataType: 'text',
                helpText: 'Organisations either running projects or associated with projects (eg. as partners).'
        ],
        [
                name: "projectActivityNameFacet",
                title: 'Survey name',
                dataType: 'text',
                helpText: 'Name of survey'
        ],
        [
                name: "recordNameFacet",
                title:  'Species name',
                dataType: 'text',
                helpText: 'Sighting\'s scientific name'
        ],
        [
                name: "userId",
                title:  'Owner',
                dataType: 'text',
                helpText: 'User who created the record'
        ],
        [
                name: "embargoed",
                dataType: 'text',
                title:  'Unpublished records'
        ],
        [
                name:  "activityLastUpdatedMonthFacet",
                title:  'Month',
                dataType: 'text',
                helpText: 'Month the record was last edited'
        ],
        [
                name:  "activityLastUpdatedYearFacet",
                title:  'Year',
                dataType: 'text',
                helpText: 'Year the record was last edited'
        ],
        [
                name:  "surveyMonthFacet",
                title:  'Month',
                dataType: 'text',
                helpText: 'Month the sighting was observed'
        ],
        [
                name:  "surveyYearFacet",
                title:  'Year',
                dataType: 'text',
                helpText: 'Year the sighting was observed'
        ],
        [
                name:  "individualCount",
                title:  'Presence or Absence',
                dataType: 'number',
                helpText: 'Is species present or absent',
                facetTermType: 'PresenceOrAbsence'
        ],
        [
                name: "associatedProgramFacet",
                title: 'Program Name',
                dataType: 'text',
                helpText: 'The administrative Program under which a project is being run.'
        ],
        [
                name: "siteNameFacet",
                title: 'Site Name',
                dataType: 'text',
                helpText: 'A site at which data has been collected for one or projects.'
        ],
        [
                name: "associatedSubProgramFacet",
                title: 'Sub Program',
                dataType: 'text',
                helpText: 'Titles of sub-programmes under listed programmes.'
        ],
        [
                name: "verificationStatusFacet",
                title: 'Verification status',
                dataType: 'text',
                helpText: 'Verification status of an activity'
        ],
        [
                name: "methodType",
                title: 'Method type',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "spatialAccuracy",
                title: 'Spatial accuracy confidence',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "speciesIdentification",
                title: 'Species identification confidence',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "temporalAccuracy",
                title: 'Temporal accuracy confidence',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "nonTaxonomicAccuracy",
                title: 'Non-taxonomic data accuracy',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "dataQualityAssuranceMethods",
                title: 'Data quality assurance method',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "isDataManagementPolicyDocumented",
                title: 'Is data management policy documented?',
                dataType: 'text',
                helpText: ''
        ]

]

facets.project = [
        [
                name: "isExternal",
                title: 'Project',
                dataType: 'text',
                helpText: 'Name of the project'
        ],
        [
                name: "isBushfire",
                title: 'Bushfire Recovery',
                dataType: 'text',
                helpText: 'Project associated to bushfire recovery'
        ],
        [
                name: "organisationFacet",
                title: 'Organisation',
                dataType: 'text',
                helpText: 'Organisations either running projects or associated with projects (eg. as partners).'
        ],
        [
                name: "uNRegions",
                title: 'UN Regions',
                dataType: 'text',
                helpText: 'The continental regions in which projects occur according to the United Nations regional classification scheme.'
        ],
        [
                name: "countries",
                title: 'Countries',
                dataType: 'text',
                helpText: 'Countries in which people can participate in the project.'
        ],
        [
                name: "origin",
                title: 'Source System',
                dataType: 'text',
                helpText: 'The project catalogue system in which the project is registered.'
        ],
        [
                name: "scienceType",
                title: 'Science Type',
                dataType: 'text',
                helpText: 'Categories of science which survey-based projects are addressing.'
        ],
        [
                name: "tags",
                title: 'Tags',
                dataType: 'text',
                helpText: 'Classifications for citizen science projects to assist decision making for participation'
        ],
        [
                name: "difficulty",
                title: 'Difficulty',
                dataType: 'text',
                helpText: 'A general level of difficulty for citizen science participation'
        ],
        [
                name: "status",
                title: 'Status',
                dataType: 'text',
                helpText: 'Active projects are still running, whereas \'completed\' projects have ended and are no longer \'active\''
        ],
        [
                name: "typeOfProject",
                title: 'Project Types',
                dataType: 'text',
                helpText: 'The project type reflects the hub in which projects were created, but will be either \'survey-based\' (Citizen and Ecological science) or \'schedule-based\' (Works and MERIT) formats for recording data.'
        ],
        [
                name: "ecoScienceType",
                title: 'Science Type',
                dataType: 'text',
                helpText: 'Categories of science which survey-based projects are addressing.'
        ],
        [
                name: "associatedProgramFacet",
                title: 'Program Name',
                dataType: 'text',
                helpText: 'The administrative Program under which a project is being run.'
        ],
        [
                name: "siteNameFacet",
                title: 'Site Name',
                dataType: 'text',
                helpText: 'A site at which data has been collected for one or projects.'
        ],
        [
                name: "associatedSubProgramFacet",
                title: 'Sub Program',
                dataType: 'text',
                helpText: 'Titles of sub-programmes under listed programmes.'
        ],
        [
                name: "plannedStartDate",
                title: 'Project Start Date',
                dataType: 'date',
                helpText: 'Selects projects that start between the specified date range.',
                facetTermType: 'Date'
        ],
        [
                name: "fundingSourceFacet",
                title: 'Funding source',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "reportingThemesFacet",
                title: 'Reporting theme',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "typeFacet",
                title: 'Activity type',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "assessment",
                title: 'Assessment',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "stateFacet",
                title: 'State',
                dataType: 'text',
                helpText: 'Australian State'
        ],
        [
                name: "lgaFacet",
                title: 'LGA',
                dataType: 'text',
                helpText: 'Local Government Areas'
        ],
        [
                name: "nrmFacet",
                title: 'Management Areas',
                dataType: 'text',
                helpText: 'Natural Resource Management Areas'
        ],
        [
                name: "mvgFacet",
                title: 'Major Vegetation Group',
                dataType: 'text',
                helpText: 'National Vegetation Information System Major Vegetation Group'
        ],
        [
                name: "mainThemeFacet",
                title: 'Reporting Theme',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "ibraFacet",
                title: 'Biogeographic Regions',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "imcra4_pbFacet",
                title: 'Marine Regions',
                helpText: ''
        ],
        [
                name: "otherFacet",
                title: 'Other Regions',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "electFacet",
                title: 'Federal Electorates',
                dataType: 'text',
                helpText: ''
        ],
        [
                name: "cmzFacet",
                title: 'CMZ',
                dataType: 'text',
                helpText: 'Conservation Management Zones'
        ],
        [
                name: "meriPlanAssetFacet",
                title: 'Assets Addressed',
                dataType: 'text',
                helpText: 'Assets addressed in the MERI plan'
        ],
        [
                name: "projLifecycleStatus",
                title: 'Project Lifecycle Status',
                dataType: 'text',
                helpText: 'Filters projects by project lifecycle status'
        ],
        [
                name: "partnerOrganisationTypeFacet",
                title: 'Partner Organisations',
                dataType: 'text',
                helpText: 'Organisation type of partner organisations'
        ],
        [
                name: "industryFacet",
                title: "Industry",
                helpText: "The industries relevant to the project"
        ],
        [
                name: "bushfireCategoriesFacet",
                title: "Bushfire Categories",
                helpText: "The bushfire categories relevant to the project"
        ]
]

license.default = "https://creativecommons.org/licenses/by-nc/3.0/au/"
projectActivity.notifyOnChange=true
biocollect.baseURL="https://biocollect.ala.org.au"
biocollect.projectActivityDataURL="${biocollect.baseURL}/bioActivity/projectRecords"

// elasticsearch cluster setting
// can transport layer connection be made from apps outside JVM
elasticsearch.local = true
elasticsearch.primary = true

// geo server config
geoServer.enabled = false
geoServer.baseURL = "http://localhost:8081/geoserver"
geoServer.workspace = "ecodata"
geoServer.username = "admin"
geoServer.password = "geoserver"
geoServer.datastore = "pasearch"
geoServer.defaultIndexName = "pasearch"
geoServer.pasearch.geometryTypeField = "sites.geometryType"
geoServer.homepage.geometryTypeField = "projectArea.geometryType"
geoServer.pasearch.geoIndexField = "projectArea.geoIndex"
geoServer.homepage.geoIndexField = "sites.geoIndex"
geoServer.defaultDataType = "pa"
geoServer.elasticHome = "localhost"
geoServer.elasticPort = "9300"
geoServer.clusterName = "elasticsearch"
geoServer.readTimeout = 600000
geoServer.layerNames = [
        "_general" : [
                "pa": [ name: "general", attributes: ['sites.geoIndex', 'sites.geometryType']],
                "project": [ name: "generalproject", attributes: ['projectArea.geoIndex', 'projectArea.geometryType']],
        ],
        "_info"    : [
                "pa": [ name: "layerinfo",
                                       attributes: [
                                               'sites.geoIndex',
                                               'dateCreated',
                                               'projectId',
                                               'thumbnailUrl',
                                               'activityId',
                                               'recordNameFacet',
                                               'projectActivityNameFacet',
                                               'projectNameFacet',
                                               'surveyMonthFacet',
                                               'surveyYearFacet',
                                               'sites.geometryType'
                                       ]
                ],
                "project": [ name: "layerinfoproject",
                             attributes: [
                                     'projectArea.geoIndex',
                                     'projectArea.geometryType',
                                     'name',
                                     'aim',
                                     'projectId',
                                     'imageUrl',
                                     'logoAttribution',
                                     'plannedStartDate',
                                     'plannedEndDate'
                             ]
                ],
        ],
        "_time": [
                "pa": [ name: "time", attributes: ['sites.geoIndex', 'sites.geometryType']]
        ],
        "_indices": [
                "pa": [ name: "colour_by", attributes: ['sites.geometryType']],
                "project": [ name: "colour_byproject", attributes: ['projectArea.geometryType']],
        ]
]

geoServer.layerConfiguration = [
        "pasearch": [
                "name": "layerName",
                "nativeName": "layerNativeName",
                "title": "BioCollect survey activity",
                "keywords": ["activity", "survey", "biocollect"],
                "timeEnabled": false,
                "timeAttribute": "dateCreated",
                "attributes": [
                        [
                                "name": "sites.geoIndex",
                                "shortName": "sites.geoIndex",
                                "useShortName": false,
                                "type": "org.locationtech.jts.geom.Geometry",
                                "use": true,
                                "defaultGeometry": true,
                                "geometryType": "GEO_SHAPE",
                                "srid": "4326",
                                "stored": false,
                                "nested": false,
                                "binding": "org.locationtech.jts.geom.Geometry",
                                "nillable": true,
                                "minOccurs": 0,
                                "maxOccurs": 1
                        ]
                ]
        ],
        "homepage": [
                "name": "layerName",
                "nativeName": "layerNativeName",
                "title": "BioCollect survey activity",
                "keywords": ["activity", "survey", "biocollect"],
                "timeEnabled": false,
                "timeAttribute": "dateCreated",
                "attributes": [
                        [
                                "name": "projectArea.geoIndex",
                                "shortName": "projectArea.geoIndex",
                                "useShortName": false,
                                "type": "org.locationtech.jts.geom.Geometry",
                                "use": true,
                                "defaultGeometry": true,
                                "geometryType": "GEO_SHAPE",
                                "srid": "4326",
                                "stored": false,
                                "nested": false,
                                "binding": "org.locationtech.jts.geom.Geometry",
                                "nillable": true,
                                "minOccurs": 0,
                                "maxOccurs": 1
                        ]
                ]
        ]
]

if (!geoserver.facetTermColour) {
    geoserver.facetTermColour = [
            '#e6194b', '#3cb44b', '#ffe119', '#4363d8', '#f58231', '#911eb4', '#46f0f0', '#f032e6',
            '#bcf60c', '#fabebe', '#008080', '#e6beff', '#9a6324', '#fffac8', '#800000', '#aaffc3', '#808000',
            '#ffd8b1', '#000075', '#808080', '#ffffff', '#000000'
    ]
}

if (!geoserver.facetRangeColour) {
    geoserver.facetRangeColour = [
            '#FD3C07', '#FC4606', '#FC5105', '#FC5B05', '#FC6604', '#FC7004', '#FC7B03', '#FC8503', '#FC9002', '#FC9A02',
            '#FCA501', '#FCB001', '#FCB70C', '#FCBE17', '#FCC523', '#FDCC2E', '#FDD339', '#FDDB45', '#FDE250', '#FEE95B',
            '#FEF067', '#FEF772', '#FFFF7E'
    ]
}

geohash.lookupTable = [
        [
                length: 1,
                width: 5009400,
                height: 4992600,
                area:25009930440000
        ],
        [
                length: 2,
                width: 1252300,
                height: 624100,
                area: 781560430000
        ],
        [
                length: 3,
                width: 156500,
                height: 156000,
                area: 24414000000
        ],
        [
                length: 4,
                width: 39100,
                height: 19500,
                area: 762450000
        ],
        [
                length: 5,
                width: 4900,
                height: 4900,
                area: 24010000
        ],
        [
                length: 6,
                width: 1200,
                height: 609.4,
                area: 731280
        ],
        [
                length: 7,
                width: 152.9,
                height: 152.4,
                area: 23301.96
        ],
        [
                length: 8,
                width: 38.2,
                height: 19,
                area: 725.8
        ],
        [
                length: 9,
                width: 4.8,
                height: 4.8,
                area: 23.04
        ],
        [
                length: 10,
                width: 1.2,
                height: 0.0595,
                area: 0.0714
        ],
        [
                length: 11,
                width: 0.0149,
                height: 0.0149,
                area: 0.00022201
        ],
        [
                length: 12,
                width: 0.0037,
                height: 0.0019,
                area: 0.00000703
        ]

]

geohash.maxNumberOfGrids = 250
// Sets the maximum precision geohash grid.
// Using higher precision will be able to narrow the record to precise location. Use lower precision if the aim is to
// hide exact location.
geohash.maxLength =  5

if(!additionalFieldsForDataTypes){
    additionalFieldsForDataTypes = [
            'species': [
                    'name': 'Scientific name field',
                    'type': 'species',
                    'fields': [
                        [
                                'name': 'name',
                                'label': 'Name',
                                "dataType": "text"
                        ],
                        [
                                'name': 'scientificName',
                                'label': 'Scientific name',
                                "dataType": "text"
                        ],
                        [
                                'name': 'commonName',
                                'label': 'Common name',
                                "dataType": "text"
                        ],
                        [
                                'name': 'guid',
                                'label': 'ALA identifier',
                                "dataType": "text"
                        ]
                    ]
            ],
            'image': [
                    'name': 'Image field',
                    'type': 'image',
                    'fields': [
                        [
                                'name': 'url',
                                'label': 'Image URL',
                                "dataType": "text"
                        ],
                        [
                                'name': 'licence',
                                'label': 'Licence',
                                "dataType": "text",
                                'constraints': [
                                        'CC BY 3.0',
                                        'CC BY 0',
                                        'CC BY 4.0',
                                        'CC BY-NC'
                                ]
                        ],
                        [
                                'name': 'name',
                                'label': 'Image name',
                                "dataType": "text"
                        ],
                        [
                                'name': 'filename',
                                'label': 'Image filename',
                                "dataType": "text"
                        ],
                        [
                                'name': 'attribution',
                                'label': 'Attribution',
                                "dataType": "text"
                        ],
                        [
                                'name': 'notes',
                                'label': 'Notes',
                                "dataType": "text"
                        ],
                        [
                                'name': 'projectId',
                                'label': 'Project Id',
                                "dataType": "text"
                        ],
                        [
                            'name': 'projectName',
                            'label': 'Project name',
                            "dataType": "text"
                        ],
                        [
                            'name': 'dateTaken',
                            'label': 'Date taken',
                            "dataType": "date"
                        ]
                    ]
            ],
            'geoMap' : [
                    'name': 'Geo map field',
                    'type': 'geoMap',
                    'fields': [
                                [
                                        'name': "",
                                        'label': 'Site identifier (siteId)',
                                        "dataType": "text"
                                ],
                                [
                                        'name': "Latitude",
                                        'label': 'Latitude',
                                        "dataType": "number",
                                        "validate": "min[-90],max[90]"
                                ],
                                [
                                        'name': "Longitude",
                                        'label': 'Longitude',
                                        "dataType": "number",
                                        "validate": "min[-180],max[180]"
                                ]
                        ]
            ]
    ]
}

// Dummy / default username and password for elasticsearch, will be ignored if the server is not setup for
// basic authentication.
elasticsearch {
    username = 'elastic'
    password = 'password'
}

// paratoo / monitor

paratoo.defaultPlotLayoutDataModels =  [
                [
                        dataType: "geoMap",
                        name: "plot_layout",
                        validate: "required"
                ],
                [
                        dataType: "list",
                        name: "plot_visit",
                        validate: "required",
                        columns: [
                                [
                                        dataType: "date",
                                        name: "end_date",
                                        dwcAttribute: "eventDate"
                                ],
                                [
                                        dataType: "text",
                                        name: "visit_field_name"
                                ],
                                [
                                        dataType: "date",
                                        name: "start_date",
                                        dwcAttribute: "eventDate"
                                ]
                        ]
                ]
        ]

paratoo.defaultPlotLayoutViewModels = [
                [
                        type: "row",
                        items: [
                                [
                                        type: "col",
                                        items: [
                                                [
                                                        type: "section",
                                                        title: "Plot Visit",
                                                        preLabel: "Plot Visit",
                                                        boxed: true,
                                                        items: [
                                                                [
                                                                        type: "repeat",
                                                                        source: "plot_visit",
                                                                        userAddedRows: false,
                                                                        items: [
                                                                                [
                                                                                        type: "row",
                                                                                        class: "output-section",
                                                                                        items: [
                                                                                                [
                                                                                                        type: "col",
                                                                                                        items: [
                                                                                                                [
                                                                                                                        type: "date",
                                                                                                                        source: "end_date",
                                                                                                                        preLabel: "End Date"
                                                                                                                ],
                                                                                                                [
                                                                                                                        type: "text",
                                                                                                                        source: "visit_field_name",
                                                                                                                        preLabel: "Visit Field Name"
                                                                                                                ],
                                                                                                                [
                                                                                                                        type: "date",
                                                                                                                        source: "start_date",
                                                                                                                        preLabel: "Start Date"
                                                                                                                ]
                                                                                                        ]
                                                                                                ]
                                                                                        ]
                                                                                ]
                                                                        ]
                                                                ]
                                                        ]
                                                ],
                                                [
                                                        type: "geoMap",
                                                        source: "plot_layout",
                                                        orientation: "vertical"
                                                ]
                                        ]
                                ]
                        ]
                ]
        ]
paratoo.species.specialCases = ["Other", "N/A"]
