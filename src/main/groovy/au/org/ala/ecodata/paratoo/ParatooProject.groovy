package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.AccessLevel
import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.Project
import au.org.ala.ecodata.Service
import au.org.ala.ecodata.Site
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** DTO for a response to the paratoo app */
@JsonIgnoreProperties(['metaClass', 'errors', 'expandoMetaClass'])
class ParatooProject {

    static String EDITOR = 'authenticated'
    static String ADMIN = 'project_admin'
    static String PUBLIC = 'public'

    String id
    String name
    AccessLevel accessLevel
    Project project
    List<ActivityForm> protocols
    Map projectArea = null
    Site projectAreaSite = null
    List<Site> plots = null

    List<Map> getDataSets() {
        project?.custom?.dataSets
    }

    List<Service> findProjectServices() {
        project.findProjectServices()
    }

    List<String> getMonitoringProtocolCategories() {
        project.getMonitoringProtocolCategories()
    }

    String getParatooRole() {
        String paratooRole
        switch (accessLevel) {
            case AccessLevel.admin:
            case AccessLevel.caseManager:
                paratooRole = ADMIN
                break
            case AccessLevel.projectParticipant:
            case AccessLevel.editor:
                paratooRole = EDITOR
                break
            default:
                paratooRole = PUBLIC
        }
        paratooRole
    }

    boolean isParaooAdmin() {
        getParatooRole() == ADMIN
    }

}
