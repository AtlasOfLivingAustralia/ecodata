package au.org.ala.ecodata.paratoo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
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
            version_org: version_org,
            system_core: system_core,
            version_core: version_core
        ]
    }

    static ParatooProvenance fromMap(Map data) {
        new ParatooProvenance(
            version_app: data.version_app,
            version_core_documentation: data.version_core_documentation,
            system_app: data.system_app,
            system_org: data.system_org,
            version_org: data.version_org,
            system_core: data.system_core,
            version_core: data.version_core
        )
    }
}
