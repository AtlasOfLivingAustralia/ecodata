package pages

class AdminTools extends ReloadablePage {

    static url = "admin/tools"

    static at = { waitFor { title.startsWith("Tools - Admin - Data capture - Atlas of Living Australia")}}

    static content = {
        reindexButton { $('#btnReIndexAll') }
        clearMetaDataCacheButton { $("#btnClearMetadataCache") }
    }

    void clearMetadata(){
        waitFor {clearMetaDataCacheButton.displayed}
        clearMetaDataCacheButton.click()
        waitFor { hasBeenReloaded() }
    }

    void reindex() {
        reindexButton().click()
    }

    void clearCache() {
        waitFor { $("#btnClearMetadataCache").displayed }
        $("#btnClearMetadataCache").click()
        waitFor { hasBeenReloaded() }
    }

}
