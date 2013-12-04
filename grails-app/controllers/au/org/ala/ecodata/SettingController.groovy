package au.org.ala.ecodata

import grails.converters.JSON

class SettingController {

    def settingService

    def ajaxSetSettingText(String id) {
        def jsonMap = request.JSON
        if (!id || !jsonMap.containsKey("settingText")) {
            response.status = 500
            render([error:'Setting text not set!'] as JSON)
            return
        }
        settingService.setSettingText(id, jsonMap.settingText as String)
        render([message:'ok'] as JSON)
    }

    def ajaxGetSettingText(String id) {
        def results = [settingText: settingService.getSettingText(id)]
        render(results as JSON)
    }

}
