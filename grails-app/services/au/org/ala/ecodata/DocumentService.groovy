package au.org.ala.ecodata

class DocumentService {

    static final ACTIVE = "active"

    def commonService, grailsApplication
    
    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param document an Document instance
     * @param levelOfDetail list of features to include
     * @return map of properties
     */
    def toMap(document, levelOfDetail = []) {
        def dbo = document.getProperty("dbo")
        def mapOfProperties = dbo.toMap()
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        // construct document url based on the current configuration
        mapOfProperties.url = grailsApplication.config.app.uploads.url + mapOfProperties.filename
        mapOfProperties.findAll {k,v -> v != null}
    }

    def get(id, levelOfDetail = []) {
        def o = Document.findByDocumentIdAndStatus(id, ACTIVE)
        return o ? toMap(o, levelOfDetail) : null
    }

    def getAll(boolean includeDeleted = false, levelOfDetail = []) {
        includeDeleted ?
            Document.list().collect { toMap(it, levelOfDetail) } :
            Document.findAllByStatus(ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    def findAllForProjectId(id, levelOfDetail = []) {
        Document.findAllByProjectIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    /**
     * todo: NOTE that this does not yet copy the file - it assumes the file has
     * been placed in the uploads directory
     * @param props
     * @return
     */
    def create(props) {
        def d = new Document(documentId: Identifiers.getNew(true,''))
        props.remove 'documentId'
        try {
            commonService.updateProperties(d, props)
            return [status:'ok',documentId:d.documentId]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            Document.withSession { session -> session.clear() }
            def error = "Error creating document for ${props.filename} - ${e.message}"
            log.error error
            return [status:'error',error:error]
        }
    }

    def update(props, id) {
        def d = Document.findByDocumentId(id)
        if (d) {
            try {
                commonService.updateProperties(d, props)
                return [status:'ok']
            } catch (Exception e) {
                Document.withSession { session -> session.clear() }
                def error = "Error updating document ${id} - ${e.message}"
                log.error error
                return [status:'error',error:error]
            }
        } else {
            def error = "Error updating document - no such id ${id}"
            log.error error
            return [status:'error',error:error]
        }
    }

}
