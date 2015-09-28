package au.org.ala.ecodata

/**
 * Created by sat01a on 14/02/15.
 */
class Pagination {

    private static final int NUMBER_OF_SURROUNDING_PAGES = 2
    public static int DEFAULT_PER_PAGE = 25
    private static final int MAXIMUM_DIFFERENCE_BETWEEN_PAGING_LINKS = NUMBER_OF_SURROUNDING_PAGES * 2
    private static final int NUMBER_OF_PAGING_LINKS = MAXIMUM_DIFFERENCE_BETWEEN_PAGING_LINKS + 1
    private int resultsPerPage
    private long totalResults
    private int requestedPage
    private long previousIndex
    private long beginIndex
    private long endIndex
    private long nextIndex
    private long paginationLastPage

    public Pagination( requestedPage,  totalResults,  resultsPerPage) {
        setTotalResults(totalResults)
        setRequestedPage(requestedPage)
        setResultsPerPage(resultsPerPage)
        setPreviousIndex(getPagingPrevIndex())
        setNextIndex(getPagingNextIndex())
        setPaginationLastPage(getLastPage())
        setBeginIndex(getPagingBeginIndex())
        setEndIndex(getPagingEndIndex())
    }

    public void setRequestedPage(int requestedPage) {
        this.requestedPage = requestedPage
    }

    public long getRequestedPage() {
        return requestedPage
    }

    public void setTotalResults(long totalResults) {
        this.totalResults = totalResults
    }

    public long getTotalResults() {
        return totalResults
    }

    public long getLastPage() {
        long lastPage = totalResults / resultsPerPage
        if (totalResults % resultsPerPage != 0) {
            lastPage += 1
        }
        if (lastPage == 0) {
            lastPage = 1
        }
        return lastPage
    }

    public long getPagingBeginIndex() {
        if (requestedPage <= NUMBER_OF_SURROUNDING_PAGES) {
            return 1
        }

        long lastPage = getLastPage()
        if (requestedPage > lastPage - NUMBER_OF_SURROUNDING_PAGES) {
            return Math.max(1,lastPage - MAXIMUM_DIFFERENCE_BETWEEN_PAGING_LINKS)
        }
        return requestedPage - NUMBER_OF_SURROUNDING_PAGES
    }

    public long getPagingEndIndex() {
        long lastPage = getLastPage()
        if (requestedPage > lastPage - NUMBER_OF_SURROUNDING_PAGES) {
            return lastPage
        }
        if (requestedPage <= NUMBER_OF_SURROUNDING_PAGES) {
            return Math.min(lastPage, NUMBER_OF_PAGING_LINKS)
        }
        return requestedPage + NUMBER_OF_SURROUNDING_PAGES
    }

    public int getPagingPrevIndex() {
        requestedPage <= 1 ? 1 : requestedPage - 1
    }

    public long getPagingNextIndex() {
        long lastPage = getLastPage()
        requestedPage >= lastPage ? lastPage : requestedPage + 1
    }

    public void setPreviousIndex(int previousIndex) {
        this.previousIndex = previousIndex
    }

    public long getPreviousIndex() {
        return previousIndex
    }

    public void setNextIndex(long nextIndex) {
        this.nextIndex = nextIndex
    }

    public long getNextIndex() {
        return nextIndex
    }

    public void setPaginationLastPage(long paginationLastPage) {
        this.paginationLastPage = paginationLastPage
    }

    public long getPaginationLastPage() {
        return paginationLastPage
    }

    public void setBeginIndex(long beginIndex) {
        this.beginIndex = beginIndex
    }

    public long getBeginIndex() {
        return beginIndex
    }

    public void setEndIndex(long endIndex) {
        this.endIndex = endIndex
    }

    public long getEndIndex() {
        return endIndex
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage
    }

    public int getResultsPerPage() {
        return resultsPerPage
    }

    def getPagination(){
        [resultsPerPage :getResultsPerPage(),
         totalResults : getTotalResults(),
         beginIndex : getBeginIndex(),
         previousIndex : getPreviousIndex(),
         requestedPage : getRequestedPage(),
         nextIndex:  getNextIndex(),
         endIndex : getEndIndex(),
         paginationLastPage : getPaginationLastPage()
        ]
    }
}
