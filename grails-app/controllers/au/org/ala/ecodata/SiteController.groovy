package au.org.ala.ecodata

import grails.converters.JSON

class SiteController {

    def siteService, commonService

    static final RICH = "rich"
    static final BRIEF = 'brief'
    static final RAW = 'raw'
    static final SCORES = 'scores'

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json")
        model
    }

    static ignore = ['action','controller','id']

    def index() {
        log.debug "Total sites = ${Site.count()}"
        render "${Site.count()} sites"
    }

    def list() {
        def list = []
        def sites = params.includeDeleted ? Site.list() :
            Site.findAllByStatus('active')
        sites.each { site ->
            list << siteService.toMap(site)
        }
        list.sort {it.name}
        render list as JSON
    }

    def get(String id) {
        def levelOfDetail = []
        if (params.brief || params.view == BRIEF) { levelOfDetail << BRIEF }
        if (params.rich || params.view == RICH) { levelOfDetail << RICH }
        if (params.raw || params.view == RAW) { levelOfDetail << RAW }
        if (params.scores || params.view == SCORES) { levelOfDetail << SCORES }
        if (!id) {
            def list = []
            def sites = params.includeDeleted ? Site.list() :
                Site.findAllByStatus('active')
            sites.each { site ->
                list << siteService.toMap(site, levelOfDetail)
            }
            list.sort {it.name}
            asJson([list:list])
        } else {
            def s = siteService.get(id, levelOfDetail)
            if (s) {
                asJson s
            } else {
                render (status: 404, text: 'No such id')
            }
        }
    }

    def delete(String id) {
        def s = Site.findBySiteId(id)
        if (s) {
            if (params.destroy) {
                s.delete()
            } else {
                s.status = 'deleted'
                s.save(flush: true)
            }
            render (status: 200, text: 'deleted')
        } else {
            render (status: 404, text: 'No such id')
        }
    }

    /**
     * Update a site.
     *
     * @param id - identifies the resource
     * @return
     */
    def update(String id) {
        def props = request.JSON
        log.debug props
        def result
        def message
        if (id) {
            result = siteService.update(props,id)
            message = [message: 'updated']
        }
        else {
            result = siteService.create(props)
            message = [message: 'created', siteId: result.siteId]
        }
        if (result.status == 'ok') {
            asJson(message)
        } else {
            log.error result.error
            render status:400, text: result.error
        }
    }

    /*def loadTestData() {
        def testFile = new File('/data/fieldcapture/site-test-data.csv')
        testFile.eachCsvLine { tokens ->
            if (tokens[2] == 'Bushbids') {
                Site s = new Site(name: tokens[4],
                        siteId: Identifiers.getNew(
                                grailsApplication.config.ecodata.use.uuids, tokens[4]),
                        description: tokens[15],
                        notes: tokens[13])
                s.save(flush: true)
                s['organisationName'] = tokens[1]
                s['projectName'] = tokens[2]
                def lat = tokens[10]
                def lng = tokens[9]
                if (lat && lng && lat != '0' && lng != '0') {
                    s.location = [[
                            name: 'centre',
                            type: 'locationTypePoint',
                            data: [
                                decimalLatitude: lat,
                                decimalLongitude: lng
                            ]
                    ]]
                } else {
                    s.location = []
                }

                if (s.name == 'ASH-MACC-A - 1') {
                    // add an activity
                    Activity a1 = new Activity(
                            activityId: Identifiers.getNew(
                               grailsApplication.config.ecodata.use.uuids, ''),
                            siteId: s.siteId,
                            type: 'DECCW vegetation assessment'
                    )
                    a1.save(flush: true, failOnError: true)
                    s.addToActivities(a1)

                    // add an output
                    Output o = new Output(
                            outputId: Identifiers.getNew(
                                    grailsApplication.config.ecodata.use.uuids, ''),
                            activityId: a1.activityId,
                            assessmentDate: a1.startDate,
                            collector: 'Wally'
                    )
                    o.save(flush: true, failOnError: true)
                    o.errors.each {
                        log.debug it
                    }
                    o.data = [weedAbundanceAndThreatScore: [
                            [name: 'Blackberry', areaCovered: '30%', coverRating: 4, invasiveThreatCategory: 5],
                            [name: 'Bridal Creeper', areaCovered: '3 plants', coverRating: 1, invasiveThreatCategory: 5]
                    ]]
                    a1.addToOutputs(o)
                    a1.save(failOnError: true)
                }

                s.save()
            }
        }
        render "${Site.count()} sites"
    }*/

    def testInsert() {
        //def point = new Coordinate(decimalLatitude: '-35.4', decimalLongitude: '145.3')
        def point = [decimalLatitude: '-35.4', decimalLongitude: '145.3']
        def locations = [point]
        Site s = new Site(siteId: "test3", name: 'Test site2',
                location: locations)
        s.save(flush: true)
        if (s.hasErrors()) {
            s.errors.each { println it }
        }
        render "${Site.count()} sites"
    }

    def testShow(id) {
        params.each { println it }
        println "id = ${id}"
        def s = Site.findBySiteId(id)
        if (s) {
            render s as JSON
        } else {
            render (status: 404, text: 'No such id')
        }
    }

    def list2() {
        def sites = Site.list()
        [sites: sites]
    }

    def clear() {
        Site.list().each {it.delete(flush: true)}
        render "${Site.count()} sites"
    }
}
