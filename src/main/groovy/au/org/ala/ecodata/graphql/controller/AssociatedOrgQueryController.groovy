package au.org.ala.ecodata.graphql.controller


import au.org.ala.ecodata.AssociatedOrg
import au.org.ala.ecodata.Organisation
import au.org.ala.ecodata.OrganisationService
import groovy.transform.CompileStatic
import org.dataloader.DataLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

import java.util.concurrent.CompletableFuture

@CompileStatic
@Controller
class AssociatedOrgQueryController {

    @Autowired
    OrganisationService organisationService


    @SchemaMapping(typeName = "AssociatedOrg", field = "organisation")
    CompletableFuture<Organisation> organisation(AssociatedOrg associatedOrg, DataLoader<String, Organisation> organisationDataLoader) {
        organisationDataLoader.load(associatedOrg.organisationId)
    }
}
