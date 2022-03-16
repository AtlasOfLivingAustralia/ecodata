package au.org.ala.ecodata.graphql.fetchers

import au.org.ala.ecodata.*
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment

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
