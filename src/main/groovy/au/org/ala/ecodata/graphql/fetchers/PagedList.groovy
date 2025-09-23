package au.org.ala.ecodata.graphql.fetchers

import grails.gorm.PagedResultList
import groovy.transform.CompileStatic

@CompileStatic
class PagedList {
    private PagedResultList resultList

    PagedList(PagedResultList resultList) {
        this.resultList = resultList
    }

    List getResults() {
        resultList
    }

    int getTotalCount() {
        resultList.totalCount
    }
}
