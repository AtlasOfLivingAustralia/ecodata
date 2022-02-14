package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.*
import au.org.ala.ecodata.graphql.models.Schema
import au.org.ala.ecodata.graphql.models.Summary
import grails.core.GrailsApplication
import grails.util.Holders
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.apache.commons.lang.WordUtils
import org.elasticsearch.action.search.SearchResponse
import org.springframework.context.MessageSource

import static au.org.ala.ecodata.Status.ACTIVE

class ManamgementUnitFetcher implements DataFetcher<List<Activity>> {

    @Override
    List<ManagementUnit> get(DataFetchingEnvironment environment) throws Exception {

        String muId = environment.arguments.managementUnitId
        String name = environment.arguments.name
        String startDate = environment.arguments.startDate
        String endDate = environment.arguments.endDate

        def list = ManagementUnit.createCriteria().list() {
            if(muId){
                eq ("managementUnitId", muId)
            }
            if(name){
                like ("name", "%" + name + "%")
            }
            if(startDate){
                ge ("startDate", Date.parse("yyyy-MM-dd", startDate))
            }
            if(endDate){
                ge ("endDate", Date.parse("yyyy-MM-dd", endDate))
            }
        }
        return list
    }
}
