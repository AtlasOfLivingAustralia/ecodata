package au.org.ala.ecodata

class SettingService {

    public static final String SETTING_KEY_ABOUT_TEXT = "fielddata.about.text"
    public static final String SETTING_KEY_FOOTER_TEXT = "fielddata.footer.text"
    public static final String SETTING_KEY_ANNOUNCEMENT_TEXT = "fielddata.announcement.text"

    // Default footer text - note that each line has two spaces at the end of it per Markdown syntax for <br />
    public static final String DEFAULT_FOOTER_TEXT = """
Caring for our Country is an Australian Government initiative jointly administered by the Australian Government
[Department of Agriculture, Fisheries and Forestry](http://www.daff.gov.au) and the [Department of Sustainability, Environment, Water, Population and Communities](http://www.environment.gov.au/index.html)
This site is a prototype developed by the Atlas of Living Australia in 2013. Report issues to [support@ala.org.au](mailto:support@ala.org.au)

<div class="large-space-before">© 2013 <a href="/about/copyright.html">Commonwealth of Australia</a></div>
<div class="stay-connected pull-right">
    <h2 style="display:none;">Stay connected</h2>
    <ul class="horizontal">
        <li class="email"> <a href="http://www.nrm.gov.au/news/subscribe.html">Subscribe to receive<br>email alerts</a> </li>
        <li class="facebook"> <a href="http://www.facebook.com/CaringforourCountry">Join us on Facebook</a> </li>
        <li class="twitter"> <a href="http://twitter.com/#!/C4oC">Follow us on Twitter</a> </li>
        <li class="rss"> <a href="http://www.nrm.gov.au/news/news.xml">Subscribe to RSS</a><br><a class="what" href="http://www.nrm.gov.au/news/rss.html">(what is RSS?)</a></li>
    </ul>
</div>
"""
    // Default about text (Markdown)
    public static final String DEFAULT_ABOUT_TEXT = """
Developed in collaboration with the [Atlas of Living Australia](http://www.ala.org.au/), MERIT will enhance the
reporting process by allowing simpler yet more complete project records and showing direct links between project
activities and contributions to Australia’s biodiversity conservation work.

At this stage MERIT includes the following programmes:

* Caring for our Country Target Area Grants
* Caring for our Country Regional Delivery
* Caring for our Country Reef Rescue
* Caring for our Country Community Environment Grants
* Biodiversity Fund Round 1
* Biodiversity Fund Round 2
* Biodiversity Fund Investing in Tasmania's Native Forests
* Biodiversity Fund Northern Australia Targeted Investment
"""

    def getSetting(String key, String defaultValue="") {
        if (!key) {
            return defaultValue
        }

        def setting = Setting.findByKey(key)
        // if user saves an empty value in Admin -> Settings, then default value is used
        if (setting && setting.value?.trim()) {
            return setting.value
        }
        return defaultValue
    }

    def setSetting(String key, String value) {
        def setting = Setting.findByKey(key)
        if (!setting) {
            setting = new Setting()
            setting.key = key
        }
        setting.value = value
        setting.save(flush: true, failOnError: true)
    }

    public String getSettingText(String name) {
        def keyMap = getKeyForName(name)
        return getSetting(keyMap?.key, keyMap?.defaultValue)
    }

    public void setSettingText(String name, String content) {
        def keyMap = getKeyForName(name)
        setSetting(keyMap?.key, content)
    }

    def getKeyForName(settingName) {
        def keyMap = [:]
        switch (settingName) {
            case "about":
                keyMap.key = SETTING_KEY_ABOUT_TEXT
                keyMap.defaultValue = DEFAULT_ABOUT_TEXT
                break
            case "footer":
                keyMap.key = SETTING_KEY_FOOTER_TEXT
                keyMap.defaultValue = DEFAULT_FOOTER_TEXT
                break
            case "announcement":
                keyMap.key = SETTING_KEY_ANNOUNCEMENT_TEXT
                keyMap.defaultValue = ""
                break
            default:
                log.warn "Unknown setting type in setSettingText()"
        }

        keyMap
    }

}
