package au.org.ala.ecodata
import grails.converters.JSON
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.elasticsearch.action.search.SearchResponse

import static au.org.ala.ecodata.ElasticIndex.PROJECT_ACTIVITY_INDEX

class DocumentController {

    def documentService
    ElasticSearchService elasticSearchService

    static allowedMethods = [save: "POST", update: "POST", delete: "DELETE", search:"POST", listImages: "POST"]

    // JSON response is returned as the unconverted model with the appropriate
    // content-type. The JSON conversion is handled in the filter. This allows
    // for universal JSONP support.
    def asJson = { model ->
        response.setContentType("application/json; charset=UTF-8")
        model
    }

    def index() {
        log.debug "Total documents (including links) = ${Document.count()}"
        render "${Document.count()} documents"
    }

    def get(String id) {
        def detail = []
        if (id) {
            def doc = documentService.get(id, detail)
            if (doc) {
                asJson doc
            } else {
                render status:404, text: 'No such id'
            }
        } else if (params.links as boolean) {
            def list = documentService.getAllLinks(params.view)
            //log.debug list
            asJson([list: list])
        } else {
            def list = documentService.getAll(params.includeDeleted as boolean, params.view)
            list.sort {it.name}
            //log.debug list
            asJson([list: list])
        }
    }

    def getFile() {
        if (params.id) {
            Map document = documentService.get(params.id)

            if (!document) {
                response.status = 404
                render status:404, text: 'No such id'
            } else {
                String path = "${grailsApplication.config.app.file.upload.path}${File.separator}${document.filepath}${File.separator}${document.filename}"

                File file = new File(path)

                if (!file.exists()) {
                    response.status = 404
                    return null
                }

                if (params.forceDownload?.toBoolean()) {
                    // set the content type to octet-stream to stop the browser from auto playing known types
                    response.setContentType('application/octet-stream')
                } else {
                    response.setContentType(document.contentType ?: 'application/octet-stream')
                }
                response.outputStream << new FileInputStream(file)
                response.outputStream.flush()

                return null
            }
        } else {
            response.status = 400
            render status:400, text: 'id is a required parameter'
        }
    }

    def find(String entity, String id) {
        if (params.links as boolean) {
            def result = documentService.findAllLinksByOwner(entity+'Id', id)
            asJson([documents:result])
        } else {
            def result = documentService.findAllByOwner(entity+'Id', id)
            asJson([documents:result])
        }
    }

    /**
     * Request body should be JSON formatted of the form:
     * {
     *     "property1":value1,
     *     "property2":value2,
     *     etc
     * }
     * where valueN may be a primitive type or array.
     * The criteria are ANDed together.
     *
     * the properties "max" and "offset", if they are supplied, will be used as pagination parameters.  Otherwise
     * the defaults max=100 and offset=0 will be used.
     *
     * If a property is supplied that isn't a property of the project, it will not cause
     * an error, but no results will be returned.  (this is an effect of mongo allowing
     * a dynamic schema)
     *
     * @return a JSON object with attributes: "count": the total number of documents that matched the criteria, "documents": the list of documents that match the supplied criteria
     */
    @RequireApiKey
    def search() {
        def searchCriteria = request.JSON
        def max = searchCriteria.remove('max') as Integer
        def offset = searchCriteria.remove('offset') as Integer
        String sort = searchCriteria.remove('sort')
        String order = searchCriteria.remove('order')

        def searchResults = documentService.search(searchCriteria, max, offset, sort, order)
        asJson searchResults
    }

    @RequireApiKey
    def delete(String id) {
        Document document = Document.findByDocumentId(id)
        if (document) {
            if (document.type == documentService.LINKTYPE) {
                document.delete()
            } else {
                boolean destroy = params.destroy == null ? false : params.destroy.toBoolean()

                documentService.deleteDocument(id, destroy)
            }
            render (status: 200, text: 'deleted')
        } else {
            response.status = 404
            render status:404, text: 'No such id'
        }
    }

    /**
     * Creates or updates a Document object via an HTTP multipart request.
     * This method currently expects:
     * 1) For an update, the document ID should be in the URL path.
     * 2) The document metadata is supplied (JSON encoded) as the value of the
     * "document" HTTP parameter.  To create a text file from a supplied string, supply the filename and content
     * as JSON properties.
     * 3) The file contents to be supplied as the value of the "files" HTTP parameter.  This is optional for
     * an update.
     * @param id The ID of an existing document to update.  If not present, a new Document will be created.
     */
    @RequireApiKey
    def update(String id) {
        def props, file = null
        def stream = null
        if (request.respondsTo('getFile')) {
            file = request.getFile('files')
            props = JSON.parse(params.document)
            if (!props.contentType && file) {
               props.contentType = file.contentType
            }
            stream = file?.inputStream
        }
        else {
            props = request.JSON
            if (props.content) {
                stream = new StringReader(props.content)
                props.remove('content')
            }
        }
        def result
        def message

        if (id) {
            result = documentService.update(props,id, stream)
            message = [message: 'updated', documentId: result.documentId, url:result.url]
        } else {
            result = documentService.create(props, stream)
            message = [message: 'created', documentId: result.documentId, url:result.url]
        }
        if (result.status == 'ok') {
            response.status = 200
            render message as JSON
        } else {
            //Document.withSession { session -> session.clear() }
            log.error result.error
            render status:400, text: result.error
        }
    }

    /**
     * Serves up a file named by the supplied filename HTTP parameter.  It is mostly as a convenience for development
     * as the files will be served by Apache in prod.
     */
    def download() {

        if (!params.filename) {
            response.status = 400
            return null
        }

        File file = new File(documentService.fullPath('', params.filename))

        if (!file.exists()) {
            response.status = 404
            return null
        }

        // Probably should store the mime type in the document, however in prod the files will be served up by
        // Apache so this doesn't have to be perfect.
        def contentType = URLConnection.guessContentTypeFromName(file.name)
        response.setContentType(contentType?:'application/octet-stream')
        response.outputStream << new FileInputStream(file)
        response.outputStream.flush()

        return null
    }

    /**
     * get images for associated activities in a paged fashion.
     * embargoed images will not be shown.
     * @return
     */
    @RequireApiKey
    def listImages(){
        Map searchCriteria = request.JSON
        Map mongoSearch = [:]
        Map documentResult
        GrailsParameterMap params
        mongoSearch.type = searchCriteria.remove('type');
        mongoSearch.role = searchCriteria.remove('role');
        params = new GrailsParameterMap(searchCriteria, request)
        documentResult = listImagesForView(mongoSearch, params)
        asJson  documentResult
    }

    /**
     * queries the activity list first. Then get images for those activities from mongo
     * @param searchCriteria
     * @param mongoSearch
     * @return
     */
    private Map listImagesForView(Map mongoSearch, GrailsParameterMap params) {
        List activityIds
        Map searchResults, activityMetadata = [:]
        Map documentResult
        elasticSearchService.buildProjectActivityQuery(params)
        SearchResponse results = elasticSearchService.search(params.query, params, PROJECT_ACTIVITY_INDEX);
        activityIds = results?.hits?.hits?.collect { document ->
            document.source.activityId
        }
        results?.hits?.hits?.each { document ->
            activityMetadata[document.source.activityId] = [
                    activityId: document.source.activityId,
                    activityName: document.source.name,
                    projectId: document.source.projectActivity.projectId,
                    projectName: document.source.projectActivity.projectName
            ]
        }

        mongoSearch.activityId = activityIds;
        searchResults = documentService.search(mongoSearch)
        searchResults?.documents.each { document ->
            activityMetadata[document.activityId]?.each{ metadata ->
                document[metadata.key] = metadata.value;
            }
        }

        documentResult = [documents: searchResults?.documents, total: results.hits?.totalHits()]
        documentResult
    }

}
