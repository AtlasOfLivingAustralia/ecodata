package au.org.ala.ecodata.paratoo

import au.org.ala.ecodata.ActivityForm

/** DTO for a response to the paratoo app */
class ParatooProject {

    String id
    String name

    List<ActivityForm> protocols
    Map projectArea = null
    List<Map> plots = null

    List<Map> dataSets
}
