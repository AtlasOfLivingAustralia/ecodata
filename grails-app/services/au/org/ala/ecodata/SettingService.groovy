package au.org.ala.ecodata

class SettingService {

    public static final String SETTING_KEY_ABOUT_TEXT = "fielddata.about.text"
    public static final String SETTING_KEY_DESCRIPTION_TEXT = "fielddata.description.text"
    public static final String SETTING_KEY_FOOTER_TEXT = "fielddata.footer.text"
    public static final String SETTING_KEY_ANNOUNCEMENT_TEXT = "fielddata.announcement.text"
    public static final String SETTING_KEY_HELP_TEXT = "fielddata.help.text"
    public static final String SETTING_KEY_CONTACTS_TEXT = "fielddata.contacts.text"
    public static final String SETTING_KEY_INTRO_TEXT = "fielddata.introduction.text"

    // Default footer text - note that each line has two spaces at the end of it per Markdown syntax for <br />
    public static final String DEFAULT_FOOTER_TEXT = """

This site has been developed by the Atlas of Living Australia in 2013. Report issues to [mailto:MERIT@environment.gov.au](MERIT@environment.gov.au)’.
<div class="large-space-before">© 2013 [Commonwealth of Australia](http://www.nrm.gov.au/about/copyright.html)</a></div>
"""
    // Default about text (Markdown)
    public static final String DEFAULT_ABOUT_TEXT = """
The Monitoring, Evaluation, Reporting and Improvement Tool (MERIT) was developed by the [Atlas of Living Australia](http://www.ala.org.au) in
2013 in conjunction with the Department of the Environment.

For grant recipients, MERIT provides a simple user interface for reporting on NRM projects funded by the Australian
Government. MERIT allows for a simpler and more complete project record, and can show a direct link between project
activities and regional or national biodiversity conservation.

For the Australian Government, MERIT provides greater transparency, increased efficiencies and the ability to use
project data for more comprehensive reporting of NRM programme achievements. MERIT also provides the opportunity
for public learning within the NRM community through access to a broad range of project and programme information.
MERIT was released in December 2013, but will continue to be refined and updated based on user feedback to make
the system easier to use and relevant to more people.

At this stage, MERIT will be used for reporting for projects funded under the following previous NRM programmes:

* Caring for our Country Target Area Grants
* Caring for our Country Regional Delivery
* Caring for our Country Reef Rescue
* Caring for our Country Community Environment Grants
* Biodiversity Fund Round 1
* Biodiversity Fund Round 2
* Biodiversity Fund Investing in Tasmania's Native Forests
* Biodiversity Fund Northern Australia Targeted Investment
"""

    // Default description text (Markdown)
    public static final String DEFAULT_DESCRIPTION_TEXT =  """
The online monitoring, evaluation, reporting and improvement tool (MERIT) is now available for grant recipients to start reporting.
MERIT has been developed for the project and programme reporting requirements of Australian Government NRM programmes.

MERIT allows grant recipients to record and upload data about the progress of their projects on a continual basis and to
submit reports online. It will also increase information sharing within NRM communities and the broader public.

Developed in collaboration with the [Atlas of Living Australia](http://www.ala.org.au), MERIT will enhance the reporting
process by allowing simpler yet more complete project records and showing direct links between project activities and
contributions to Australia’s biodiversity conservation work.
    """

    // Default contacts text (Markdown)
    public static final String DEFAULT_CONTACTS_TEXT =  """
### MERIT enquiries

Grant recipients, please contact your Departmental grant manager or an Australian Government natural resource management officer for general MERI plan and reporting questions.

For general MERIT enquiries please email us at [MERIT@environment.gov.au](mailto:MERIT@environment.gov.au) or call 1800 552 008.

NRM programme enquiries
Contact details for NRM programme enquiries are available here.
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

    /**
     * @deprecated
     * @param name
     * @return
     */
    public String getSettingText(String name) {
        def keyMap = getKeyMapForName(name)
        return getSetting(keyMap?.name, keyMap?.defaultValue)
    }

    public String getSettingTextForKey(String key) {
        def defaultValue = getKeyMapForKey(key)
        return getSetting(key, defaultValue?:'')
    }


    public void setSettingText(String content, String key) {
        setSetting(key, content)
    }

    /**
     * @deprecated
     * @param settingName
     * @return
     */
    def getKeyMapForName(settingName) {
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
            case "help":
                keyMap.key = SETTING_KEY_HELP_TEXT
                keyMap.defaultValue = "User guides, online tutorials and frequently asked questions (FAQ) are available here to assist you with using MERIT."
                break
            case "contacts":
                keyMap.key = SETTING_KEY_CONTACTS_TEXT
                keyMap.defaultValue = DEFAULT_CONTACTS_TEXT
                break
            default:
                log.warn "Unknown setting type in setSettingText()"
        }

        keyMap
    }

    def getKeyMapForKey(key) {
        def defaultValue
        switch (key) {
            case SETTING_KEY_ABOUT_TEXT:
                defaultValue = DEFAULT_ABOUT_TEXT
                break
            case SETTING_KEY_DESCRIPTION_TEXT:
                defaultValue = DEFAULT_DESCRIPTION_TEXT
                break
            case SETTING_KEY_FOOTER_TEXT:
                defaultValue = DEFAULT_FOOTER_TEXT
                break
            case SETTING_KEY_ANNOUNCEMENT_TEXT:
                defaultValue = ""
                break
            case SETTING_KEY_HELP_TEXT:
                defaultValue = "User guides, online tutorials and frequently asked questions (FAQ) are available here to assist you with using MERIT."
                break
            case SETTING_KEY_INTRO_TEXT:
                defaultValue = "TBC"
                break
            case SETTING_KEY_CONTACTS_TEXT:
                defaultValue = DEFAULT_CONTACTS_TEXT
                break
            default:
                log.info "Unknown setting type in setSettingText()"
        }

        defaultValue
    }

}
