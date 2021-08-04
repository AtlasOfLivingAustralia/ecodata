package au.org.ala.ecodata

import grails.converters.JSON

class JSONPInterceptor {

    public JSONPInterceptor() {
        match uri: '/ws/**'
    }

    boolean before() { true }

    boolean after = { model ->
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
        true
    }

    void afterView() { }

 /*   def filters = {
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
    }*/
}
