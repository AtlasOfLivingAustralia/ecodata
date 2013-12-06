package au.org.ala.ecodata

import grails.converters.JSON

class SettingController {

    def settingService

    def ajaxSetSettingText() {
        def jsonMap = request.JSON
        if (!jsonMap.containsKey("settingText") || !jsonMap.containsKey("key") ) {
            response.status = 500
            render([error:'Setting text not set! Missing params [key|settingText]'] as JSON)
            return
        }
        def content = jsonMap.settingText as String
        def key = jsonMap.key as String
        settingService.setSettingText(content, key)
        render([message:'ok'] as JSON)
    }

    /**
     * @deprecated - keeping just in case // TODO do thorough search and remove any references to this
     * @param id
     * @return
     */
    def ajaxGetSettingText(String id) {
        def results = [settingText: settingService.getSettingText(id)]
        render(results as JSON)
    }

    def ajaxGetSettingTextForKey() {
        def key = params.key
        def results =  [settingText: settingService.getSettingTextForKey(key), key: key]
        render(results as JSON)
    }

}
