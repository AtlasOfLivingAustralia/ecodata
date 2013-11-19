package au.org.ala.ecodata

class SettingService {

    public static final String SETTING_KEY_ABOUT_TEXT = "fielddata.about.text"
    public static final String SETTING_KEY_FOOTER_TEXT = "fielddata.footer.text"

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

    def getSetting(String key, String defaultValue="") {
        if (!key) {
            return defaultValue
        }

        def setting = Setting.findByKey(key)
        if (setting) {
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

    public String getAboutPageText() {
        return getSetting(SETTING_KEY_ABOUT_TEXT, "This system was developed by the Atlas of Living Australia in 2013 in conjunction with the Department of Sustainability, Environment, Water, Population and Communities.") as String
    }

    public void setAboutPageText(String content) {
        setSetting(SETTING_KEY_ABOUT_TEXT, content)
    }

    public String getFooterText() {
        return getSetting(SETTING_KEY_FOOTER_TEXT, DEFAULT_FOOTER_TEXT)
    }

    public void setFooterText(String content) {
        setSetting(SETTING_KEY_FOOTER_TEXT, content)
    }

}
