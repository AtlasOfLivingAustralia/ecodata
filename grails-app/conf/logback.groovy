import grails.util.BuildSettings
import grails.util.Environment
import org.springframework.boot.logging.logback.ColorConverter
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.Charset

conversionRule 'clr', ColorConverter
conversionRule 'wex', WhitespaceThrowableProxyConverter

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')

        pattern =
                '%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} ' + // Date
                        '%clr(%5p) ' + // Log level
                        '%clr(---){faint} %clr([%15.15t]){faint} ' + // Thread
                        '%clr(%-40.40logger{39}){cyan} %clr(:){faint} ' + // Logger
                        '%m%n%wex' // Message
    }
}

def targetDir = (System.getProperty('ecodata.logs') ?: '. /logs')
if (Environment.isDevelopmentMode() && targetDir != null) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
}

if (targetDir != null) {
    appender("ES-INDEXING", FileAppender) {
        file = "${targetDir}/elasticsearch-indexing.log"
        append = false
        encoder(PatternLayoutEncoder) {
            pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} - %msg%n"
        }
    }
    logger("EsIndexing", INFO, ['ES-INDEXING'], false)
}
root(ERROR, ['STDOUT'])


final error = [

]

final warn = [

        'au.org.ala.cas.client',
        'au.org.ala.cas.util',
        'org.apache.coyote.http11.Http11Processor'
]

final info  = [
        'asset.pipeline',
      //  'au.org.ala',
        'grails.app',
        'grails.plugins.mail',
        'grails.plugins.quartz',
        'grails.mongodb',
        'org.quartz',
        'org.springframework'
]

final esInfo  = [
        'au.org.ala.ecodata.ElasticSearchService'
]

final debug = [
        'grails.mongodb',
        'grails.gorm',
      //  'org.grails.datastore.gorm',
        'au.org.ala.ecodata'
//        'grails.app.services.au.org.ala.volunteer.ExportService',
//        'grails.app.controllers.au.org.ala.volunteer.ProjectController',
//        'grails.plugin.cache'
//        'org.apache.http.headers',
//        'org.apache.http.wire',
//        'org.hibernate.SQL',
//        'org.springframework.cache',
//        'net.sf.ehcache',
//        'org.jooq.tools.LoggerListener'
]

final trace = [
//        'org.hibernate.type'
]

for (def name : error) logger(name, ERROR)
for (def name : warn) logger(name, WARN)
for (def name: info) logger(name, INFO)
for (def name: debug) logger(name, DEBUG)
for (def name: trace) logger(name, TRACE)
for (def name: esInfo) logger(name, INFO, ["ES-INDEXING"])