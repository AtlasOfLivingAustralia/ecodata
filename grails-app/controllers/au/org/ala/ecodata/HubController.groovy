package au.org.ala.ecodata

import grails.converters.JSON

import static au.org.ala.ecodata.Status.*

@RequireApiKey
class HubController  {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [update: "POST", delete: "DELETE"]

    HubService hubService

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        //response.setContentType("application/json;charset=UTF-8")
        render model as JSON
    }

    def index(String id) {
        Boolean includeDeleted = params.boolean('includeDeleted', false)

        if (!id) {
            List hubs = Hub.findAllByStatusNotEqual(DELETED, params)

            Map map = ['list': hubs]

           // render map as JSON
         /*  withFormat {
               // html bookList: books
                json { render map as JSON }
                //xml { render hubs as XML }
                '*' { render map as JSON }
            }*/
            //render([list: hubs]) as JSON

            asJson([list: hubs])
        } else {
            Map hub = hubService.get(id, includeDeleted)
            if (hub) {
                render hubService.toMap(hub) as JSON
            } else {
                render (status: 404, text: 'No such id')
            }
        }
    }

    def findByUrlPath(String id) {
        Map hub = hubService.findByUrlPath(id)
        if (hub) {
            render hub as JSON
        } else {
            render (status: 404, text: 'No hub exists with urlPath='+id)
        }
    }

    def update(String id) {
        Map props = request.JSON
        Map result, message
        if (id) {
            result = hubService.update(id, props)
            message = [message: 'updated']
        }
        else {
            result = hubService.create(props)
            message = [message: 'created', hubId: result.hubId]
        }
        if (result.status == 'ok') {
            asJson(message)
        } else {
            log.error result.errors.toString()
            render status:400, text: result.errors
        }
    }

    def delete(String id) {
        hubService.delete(id)
    }
}
