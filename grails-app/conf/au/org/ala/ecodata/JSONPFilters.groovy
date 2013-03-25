package au.org.ala.ecodata

import grails.converters.JSON

class JSONPFilters {

    def filters = {
        ws(uri: '/ws/**') {
            after = { model ->
                //log.debug "Model is ${model}"
                def ct = response.getContentType()
                if(ct?.contains("application/json") && model){
                    String resp = model as JSON
                    if(params.callback) {
                        resp = params.callback + "(" + resp + ")"
                    }
                    render (contentType: "application/json", text: resp)
                    false
                }
            }
        }
    }
}
