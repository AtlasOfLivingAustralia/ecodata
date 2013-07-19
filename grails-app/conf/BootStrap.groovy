import au.org.ala.ecodata.ElasticSearchService

class BootStrap {

    def elasticSearchService

    def init = { servletContext ->
        elasticSearchService.initialize()
        elasticSearchService.indexAll()
    }
    def destroy = {
        elasticSearchService.destroy()
    }
}
