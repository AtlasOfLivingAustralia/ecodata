package au.org.ala.ecodata.paratoo

class ParatooProvenance {

    String system_app
    String version_app
    String version_core_documentation

    String system_core
    String version_core

    String system_org
    String version_org

    Map toMap() {
        [
            version_app: version_app,
            version_core_documentation: version_core_documentation,
            system_app: system_app,
            system_org: system_org,
            version_org: version_org
        ]
    }
}
