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
        response.setContentType("application/json; charset=\"UTF-8\"")
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

    @RequireApiKey
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
    @RequireApiKey
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

    @RequireApiKey
    def createPoi(String id) {
        def props = request.JSON

        if (!id) {
            render status:400, text:'Site ID is mandatory'
            return
        }
        def result = siteService.createPoi(id, props)
        if (result.status == 'ok') {
            def message = [message:'created', poiId: result.poiId]
            asJson(message)
        }
        else {
            render status:400, text:result.error
        }
    }

    def list2() {
        def sites = Site.list()
        [sites: sites]
    }
}
