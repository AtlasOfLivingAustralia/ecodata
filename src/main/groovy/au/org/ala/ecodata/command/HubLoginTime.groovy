package au.org.ala.ecodata.command


import au.org.ala.ecodata.Hub
import grails.databinding.BindingFormat
import grails.validation.Validateable

/** Command object for {@link au.org.ala.ecodata.UserController#recordLoginTime} */
class HubLoginTime implements Validateable {
    String userId
    String hubId
    @BindingFormat('iso8601')
    Date loginTime

    static constraints = {
        userId nullable: true
        loginTime nullable: true
        hubId validator: { Hub.findByHubId(it) != null }
    }
}
