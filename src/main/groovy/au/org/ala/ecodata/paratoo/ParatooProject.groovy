package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.AccessLevel
import au.org.ala.ecodata.ActivityForm
import au.org.ala.ecodata.Project
import au.org.ala.ecodata.Service
import au.org.ala.ecodata.Site

/** DTO for a response to the paratoo app */
class ParatooProject {

    static String READ_ONLY = 'authenticated'
    static String EDITABLE = 'project_admin'

    String id
    String name
    AccessLevel accessLevel
    Project project
    List<ActivityForm> protocols
    Map projectArea = null
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

    String getParatooAccessLevel() {
        String paratooAccessLevel = READ_ONLY
        switch (accessLevel) {
            case AccessLevel.admin:
            case AccessLevel.caseManager:
                paratooAccessLevel = EDITABLE
                break;
        }
        paratooAccessLevel
    }
}
