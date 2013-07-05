package au.org.ala.ecodata

/**
 * Full text search service
 *
 * Currently using GORM but TODO integrate with SOLR or ElasticSearch, et al.
 *
 * @author "Nick dos Remedios <nick.dosremedios@csiro.au>"
 */
class SearchService {
    static transactional = false

    def findForQuery(String query, params) {
        def likeQuery = "%" + query + "%"
        def offset = params.offset?.toInteger()?:0
        def max = params.max?.toInteger()?:10
        params.offset = null // force full list
        params.max = null    // force full list

        // Projects
        def pc = Project.createCriteria()
        def pList = pc.list (params) {
            or {
                ilike('name', likeQuery )
                ilike('description', likeQuery )
                ilike('organisationName', likeQuery )
            }
        }
        //def pCount = pList.getTotalCount()

        // Activities
        def ac = Activity.createCriteria()
        def aList = ac.list (params) {
            or {
                ilike('name', likeQuery )
                ilike('description', likeQuery )
                ilike('type', likeQuery )
                ilike('notes', likeQuery )
            }
        }
        //def aCount = aList.getTotalCount()

        // Sites
        def sc = Site.createCriteria()
        def sList = sc.list (params) {
            or {
                ilike('name', likeQuery )
                ilike('description', likeQuery )
                ilike('notes', likeQuery )
                ilike('recordingMethod', likeQuery )
                ilike('landTenure', likeQuery )
                ilike('protectionMechanism', likeQuery )
            }
        }
        //def sCount = sList.getTotalCount()

        // Merge results (not sorted)
        pList += aList
        pList += sList
        def finalList = []
        def totalCount = 0

        // manually paginate results
        if (pList.size() > 0) {
            totalCount = pList.size()
            def lowerRange = offset
            def upperRange = ((offset + max) < totalCount) ? (offset + max) -1 : totalCount - 1
            log.error "totalCount = " + totalCount + " || lowerRange = " + lowerRange + " || upperRange = " + upperRange
            finalList = pList[lowerRange..upperRange]
        }

        // reset param values
        params.offset = offset
        params.max = max

        // return data structure
        [
            query: likeQuery,
            totalRecords: totalCount,
            max: params.max,
            offset: params.offset,
            sort: params.sort,
            order: params.order,
            results: finalList
        ]
    }
}
