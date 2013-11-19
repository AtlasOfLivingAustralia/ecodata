package au.org.ala.ecodata

import grails.converters.JSON

class SettingController {

    def settingService

    def ajaxGetAboutPageText() {
        def results = [aboutText: settingService.aboutPageText]
        render(results as JSON)
    }

    def ajaxSetAboutPageText() {
        def jsonMap = request.JSON
        if (jsonMap.aboutText) {
            settingService.setAboutPageText(jsonMap.aboutText as String)
        }
        render([message:'ok'] as JSON)
    }

    def ajaxGetFooterText() {
        def results = [footerText: settingService.footerText]
        render(results as JSON)
    }

    def ajaxSetFooterText() {
        def jsonMap = request.JSON
        if (jsonMap.footerText) {
            settingService.setFooterText(jsonMap.footerText as String)
        }
        render([message:'ok'] as JSON)
    }

}
