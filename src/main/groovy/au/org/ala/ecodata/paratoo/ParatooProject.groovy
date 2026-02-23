package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.AccessLevel
import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.Program
import au.org.ala.ecodata.Project
import au.org.ala.ecodata.Service
import au.org.ala.ecodata.Site
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** DTO for a response to the paratoo app */
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooProject {
    static final String PROGRAM_CONFIG_PARATOO_ITEM = 'supportsParatoo'
    static final String PARATOO_DEFAULT_MODULES = 'paratooDefaultModules'
    static final List DEFAULT_MODULES =
            ['Plot Selection and Layout', 'Plot Description', 'Opportune']

    String id
    String name
    // Used when creating voucher labels
    String grantID
    Project project
    List<ActivityForm> protocols
    Map projectArea = null
    Site projectAreaSite = null
    List<Site> plots = null
    Program program
    List<String> roles = []

    private Map getConfig() {
        Map config = program?.getInheritedConfig() ?: [:]
        // Support per-project overrides of config
        if (project.config) {
            config.putAll(project.config)
        }
        config
    }

    List<String> getDefaultModules() {
        Map config = getConfig()
        List modules = DEFAULT_MODULES
        if (config.containsKey(PARATOO_DEFAULT_MODULES)) {
            modules = getConfig()?.get(PARATOO_DEFAULT_MODULES)
        }
        modules
    }

    boolean isParatooEnabled() {
        // The Monitor/Paratoo app is "write only" (i.e. there is no view mode for the data), so we don't support
        // the read only role
        getConfig()?.get(PROGRAM_CONFIG_PARATOO_ITEM) && roles
    }

    List<Map> getDataSets() {
        project?.custom?.dataSets
    }

    List<Service> findProjectServices() {
        project.findProjectServices()
    }

    List<String> getMonitoringProtocolCategories() {
        project.getMonitoringProtocolCategories()
    }

}
