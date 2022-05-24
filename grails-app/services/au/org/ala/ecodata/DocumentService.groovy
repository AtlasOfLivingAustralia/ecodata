package au.org.ala.ecodata

import com.itextpdf.text.PageSize
import com.itextpdf.text.html.simpleparser.HTMLWorker
import com.itextpdf.text.pdf.PdfWriter
import grails.core.GrailsApplication
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.grails.datastore.mapping.query.api.BuildableCriteria

import java.text.DateFormat
import java.text.SimpleDateFormat

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED

class DocumentService {

    static final FLAT = 'flat'
    static final LINKTYPE = "link"
    static final LOGO = 'logo'
    static final FILE_LOCK = new Object()

    static final DIRECTORY_PARTITION_FORMAT = 'yyyy-MM'
    static  final MOBILE_APP_ROLE = [ "android",
                                     "blackberry",
                                     "iTunes",
                                     "windowsPhone"]

    CommonService commonService
    GrailsApplication grailsApplication
    ActivityService activityService

    /**
     * Converts the domain object into a map of properties, including
     * dynamic properties.
     * @param document an Document instance
     * @param levelOfDetail list of features to include
     * @return map of properties
     */
    def toMap(Document document, levelOfDetail = []) {
        def mapOfProperties = document instanceof Document ? GormMongoUtil.extractDboProperties(document.getProperty("dbo")) : document
        def id = mapOfProperties["_id"].toString()
        mapOfProperties["id"] = id
        mapOfProperties.remove("_id")
        // construct document url based on the current configuration
        mapOfProperties.url = document.url
        if (document?.type == Document.DOCUMENT_TYPE_IMAGE) {
            mapOfProperties.thumbnailUrl = document.thumbnailUrl
        }
        mapOfProperties.publiclyViewable = document.isPubliclyViewable()
        mapOfProperties.findAll {k,v -> v != null}
    }

    def get(id, levelOfDetail = []) {
        def o = Document.findByDocumentIdAndStatus(id, ACTIVE)
        return o ? toMap(o, levelOfDetail) : null
    }

    def getByStatus(id, levelOfDetail = []) {
        def o = Document.findByDocumentId(id)
        return o ? toMap(o, levelOfDetail) : null
    }

    def getAll(boolean includeDeleted = false, levelOfDetail = []) {
        includeDeleted ?
            Document.findAllByTypeNotEqual(LINKTYPE).collect { toMap(it, levelOfDetail) } :
            Document.findAllByStatusAndTypeNotEqual(ACTIVE, LINKTYPE).collect { toMap(it, levelOfDetail) }
    }

    def getAllLinks(levelOfDetail = []) {
        Document.findAllByType(LINKTYPE).collect { toMap(it, levelOfDetail) }
    }

    def findAllForProjectId(id, levelOfDetail = [], version = null) {
        if (version) {
            def all = AuditMessage.findAllByProjectIdAndEntityTypeAndDateLessThanEquals(id, Document.class.name,
                    new Date(version as Long), [sort:'date', order:'desc'])
            def documents = []
            def found = []
            all?.each {
                if (!found.contains(it.entityId)) {
                    found << it.entityId
                    if (it.entity.status == ACTIVE && it.entity.type != LINKTYPE &&
                            (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                        documents << toMap(it.entity, levelOfDetail)
                    }
                }
            }
            documents
        } else {
            Document.findAllByProjectIdAndStatusAndTypeNotEqual(id, ACTIVE, LINKTYPE).collect { toMap(it, levelOfDetail) }
        }
    }

    def findAllLinksForProjectId(id, levelOfDetail = [], version = null) {
        if (version) {
            def all = AuditMessage.findAllByProjectIdAndEntityTypeAndDateLessThanEquals(id, Document.class.name,
                    new Date(version as Long), [sort: 'date', order: 'desc'])
            def links = []
            def found = []
            all?.each {
                if (!found.contains(it.entityId)) {
                    found << it.entityId
                    if (it.entity.status == ACTIVE && it.entity.type == LINKTYPE &&
                            (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                        links << toMap(it.entity, levelOfDetail)
                    }
                }
            }
            links
        } else {
            Document.findAllByProjectIdAndType(id, LINKTYPE).collect { toMap(it, levelOfDetail) }
        }
    }

    def findAllForProjectIdAndIsPrimaryProjectImage(id, levelOfDetail = []) {
		Document.findAllByProjectIdAndStatusAndIsPrimaryProjectImage(id, ACTIVE,true).collect { toMap(it, levelOfDetail) }
	}

    def findAllForActivityId(id, levelOfDetail = [], version = null) {
        if (version) {
            def projectId = activityService.get(id)
            def all = AuditMessage.findAllByProjectIdAndEntityTypeAndDateLessThanEquals(projectId, Document.class.name,
                    new Date(version as Long), [sort:'date', order:'desc'])
            def outputs = []
            def found = []
            all?.each {
                if (!found.contains(it.entityId)) {
                    found << it.entityId
                    if (it.entity.activityId == id && it.entity.status == ACTIVE &&
                            (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                        outputs << toMap(it.entity, levelOfDetail)
                    }
                }
            }

            outputs
        } else {
            Document.findAllByActivityIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
        }
    }

    def findAllForSiteId(id, levelOfDetail = [], version = null) {
        if (version) {
            def documentIds = Document.findAllBySiteId(id).collect { it.documentId }
            def all = AuditMessage.findAllByEntityIdInListAndEntityTypeAndDateLessThanEquals(documentIds, Document.class.name,
                    new Date(version as Long), [sort:'date', order:'desc'])
            def documents = []
            def found = []
            all?.each {
                if (!found.contains(it.entityId)) {
                    found << it.entityId
                    if (it.entity.status == ACTIVE &&
                            (it.eventType == AuditEventType.Insert || it.eventType == AuditEventType.Update)) {
                        documents << toMap(it.entity, levelOfDetail)
                    }
                }
            }

            documents
        } else {
            Document.findAllBySiteIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
        }
    }

    def findAllForOutputId(id, levelOfDetail = []) {
        Document.findAllByOutputIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }
    def findAllForProjectActivityId(id, levelOfDetail = []) {
        Document.findAllByProjectActivityIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    String findImageUrlForProjectId(id, boolean isThumbnail = true){
        Document primaryImageDoc;
        Document logoDoc = Document.findByProjectIdAndRoleAndStatus(id, LOGO, ACTIVE);
        String urlImage;
        urlImage = logoDoc?.url
        if (urlImage) {
            if (isThumbnail) {
                urlImage = logoDoc.getThumbnailUrl()
            }
        }

        if(!urlImage){
            primaryImageDoc = Document.findByProjectIdAndIsPrimaryProjectImage(id, true)
            urlImage = primaryImageDoc?.url;
        }
        urlImage
    }

    String getLogoAttributionForProjectId(String id){
        Document.findByProjectIdAndRoleAndStatus(id, LOGO, ACTIVE)?.attribution
    }

    /**
     * @param criteria a Map of property name / value pairs.  Values may be primitive types or arrays.
     * Multiple properties will be ANDed together when producing results.
     *
     * @return a map with two keys: "count": the total number of results, "documents": a list of the documents that match the supplied criteria
     */
    public Map search(Map searchCriteria, Integer max = 100, Integer offset = 0, String sort = null, String orderBy = null) {

        BuildableCriteria criteria = Document.createCriteria()
        List documents = criteria.list(max:max, offset:offset) {
            searchCriteria.each { prop,value ->

                if (value instanceof List) {
                    inList(prop, value)
                }
                else {
                    eq(prop, value)
                }
            }
            ne("status", DELETED)
            if (sort) {
                order(sort, orderBy?:'asc')
            }

        }
        [documents:documents.collect{toMap(it)}, count:documents.totalCount]
    }


    /**
     * Creates a new Document object associated with the supplied file.
     * @param props the desired properties of the Document.
     * @param fileIn an InputStream attached to the file to save.  This will be saved to the uploads directory.
     */
    def create(props, fileIn) {
        def d = new Document(documentId: Identifiers.getNew(true,''))
        props.remove('url')
        props.remove('thumbnailUrl')

        try {
            d.save([failOnError: true]) // The document appears to need to be associated with a session before setting any dynamic properties. The exact reason for this is unclear - I am unable to reproduce in a test app.
            props.remove 'documentId'

            if (fileIn) {
                DateFormat dateFormat = new SimpleDateFormat(DIRECTORY_PARTITION_FORMAT)
                def partition = dateFormat.format(new Date())
				if(props.saveAs?.equals("pdf")){
					props.filename = saveAsPDF(fileIn, partition, props.filename,false)
				}
				else {
                    props.filename = saveFile(partition, props.filename, fileIn, false, props.type)
                }
                props.filepath = partition
            }

            if (props.activityId) {
                props.reportId = Report.findByActivityId(props.activityId)?.reportId
            }

            commonService.updateProperties(d, props)
            return [status:'ok',documentId:d.documentId, url:d.url]
        } catch (Exception e) {
            // clear session to avoid exception when GORM tries to autoflush the changes
            e.printStackTrace()

            Document.withSession { session -> session.clear() }
            def error = "Error creating document for ${props.filename} - ${e.message}"
            log.error error
            return [status:'error',error:error]
        }
    }

    /**
     * Updates a new Document object and optionally it's attached file.
     * @param props the desired properties of the Document.
     * @param fileIn (optional) an InputStream attached to the file to save.  If supplied, this will overwrite any
     * file with the same name in the uploads directory.
     */
    def update(props, id, fileIn = null) {
        def d = Document.findByDocumentId(id)
        if (d) {
            try {
                if (fileIn) {
                    props.filename = saveFile(d.filepath, props.filename, fileIn, true, d.type)
                }
                props.remove('url')
                props.remove('thumbnailUrl')
                commonService.updateProperties(d, props)
                return [status:'ok',documentId:d.documentId, url:d.url]
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

    /**
     * Saves the contents of the supplied InputStream to the file system, using the supplied filename.  If overwrite
     * is false and a file with the supplied filename exists, a new filename will be generated by pre-pending the
     * filename with a counter.
     * @param filename the name to save the file.
     * @param fileIn an InputStream containing the contents of the file to save.
     * @param overwrite true if an existing file should be overwritten.
     * @param type the type of file being saved (image types will have thumbnails created after saving)
     * @return the filename (not the full path) the file was saved using.  This may not be the same as the supplied
     * filename in the case that overwrite is false.
     */
    private String saveFile(String filepath, String filename, InputStream fileIn, boolean overwrite, String type = null) {
        if (fileIn) {
            synchronized (FILE_LOCK) {
                //create upload dir if it doesn't exist...
                def uploadDir = new File(fullPath(filepath, ''))

                if(!uploadDir.exists()){
                    FileUtils.forceMkdir(uploadDir)
                }

                if (!overwrite) {
                    filename = nextUniqueFileName(filepath, filename)
                }

                File destination = new File(fullPath(filepath, filename))
                new FileOutputStream(destination).withStream { it << fileIn }

                if (type == Document.DOCUMENT_TYPE_IMAGE) {

                    filename = processImage(filepath, filename, destination, overwrite)
                }
            }
        }
        return filename
    }

    private String processImage(String filepath, String filename, File destination, boolean overwrite) {
        File processed = new File(fullPath(filepath, Document.PROCESSED_PREFIX + filename))
        boolean result = ImageUtils.reorientImage(destination, processed)
        if (result) {
            // If the image was processed, used the processed image when making the thumbnail.
            filename = Document.PROCESSED_PREFIX + filename
        }

        makeThumbnail(filepath, filename, overwrite)
        filename
    }

    /**
     * Creates a thumbnail of the image stored at the location specified by filepath and filename.
     * @param filepath the path (relative to root document storage) at which the file can be found.
     * @param filename the name of the file.
     *
     * @return The thumbnail file or null for no thumbnail
     */
    File makeThumbnail(filepath, filename, overwrite = true) {
        File sFile = new File(fullPath(filepath, filename))
        if (!sFile.exists())
            return null

        File tnFile = new File(fullPath(filepath, Document.THUMBNAIL_PREFIX+filename))
        if (tnFile.exists()) {
            if (!overwrite) {
                return tnFile
            }
            else {
                tnFile.delete()
            }
        }

        return ImageUtils.makeThumbnail(sFile, tnFile, 300)
    }

	/**
	 * Saves the contents of the supplied string to the file system as pdf using the supplied filename.  If overwrite
	 * is false and a supplied file name exits, a new filename will be generated by pre-pending the
	 * filename with a counter.
	 * @param content file contents
	 * @param fileName file name
	 * @param overwrite true if an existing file should be overwritten.
	 * @return the filename (not the full path) the file was saved using.  This may not be the same as the supplied
	 * filename in the case that overwrite is false.
	 */
	private saveAsPDF(content, filepath, filename, overwrite){
		synchronized (FILE_LOCK) {
			def uploadDir = new File(fullPath(filepath, ''))
			if(!uploadDir.exists()){
				FileUtils.forceMkdir(uploadDir)
			}
			// make sure we don't overwrite the file.
			if (!overwrite) {
				filename = nextUniqueFileName(filepath, filename)
			}
			OutputStream file = new FileOutputStream(new File(fullPath(filepath, filename)));

			//supply outputstream to itext to write the PDF data,
			com.itextpdf.text.Document document = new com.itextpdf.text.Document();
			document.setPageSize(PageSize.LETTER.rotate());
			PdfWriter.getInstance(document, file);
			document.open();
			HTMLWorker htmlWorker = new HTMLWorker(document);
			StringWriter writer = new StringWriter();
			IOUtils.copy(content, writer);
			htmlWorker.parse(new StringReader(writer.toString()));
			document.close();
			file.close();
			return filename

		}

	}

    /**
     * We are preserving the file name so the URLs look nicer and the file extension isn't lost.
     * As filename are not guaranteed to be unique, we are pre-pending the file with a counter if necessary to
     * make it unique.
     */
    private String nextUniqueFileName(filepath, filename) {
        int counter = 0;
        String newFilename = filename
        while (new File(fullPath(filepath, newFilename)).exists()) {
            newFilename = "${counter}_${filename}"
            counter++;
        };
        return newFilename;
    }

    /**
     * Returns the path the document by combining the path and filename with the directory where documents
     * are uploaded.
     * Optionally uses the canonical form of the uploads directory to assist validation.
     */
    String fullPath(String filepath, String filename, boolean useCanonicalFormOfUploadPath = false) {
        String path = filepath ?: ''
        if (path) {
            path = path+File.separator
        }
        String uploadPath = grailsApplication.config.getProperty('app.file.upload.path')
        if (useCanonicalFormOfUploadPath) {
            uploadPath = new File(uploadPath).getCanonicalPath()
        }
        return uploadPath + File.separator + path  + filename
    }

    /**
     * This method compares the canonical path to a document with the path potentially supplied by the
     * user and returns false if they don't match.  This is to prevent attempts at file system traversal.
     */
    boolean validateDocumentFilePath(String path, String filename) {
        String file = fullPath(path, filename, true)
        new File(file).getCanonicalPath() == file
    }

    void deleteAllForProject(String projectId, boolean destroy = false) {
        delete("projectId", projectId, destroy)
    }

    void deleteAllForSite(String siteId, boolean destroy = false) {
        delete("siteId", siteId, destroy)
    }

    private void delete(String owner, String ownerId, boolean destroy = false) {
        List<String> documentIds = Document.withCriteria {
            eq owner, ownerId
            projections {
                property("documentId")
            }
        }

        documentIds?.each { deleteDocument(it, destroy) }
    }

    void deleteDocument(String documentId, boolean destroy = false) {
        Document document = Document.findByDocumentId(documentId)
        if (document) {
            if (destroy) {
                document.delete()
                deleteFile(document)
            } else {
                document.status = DELETED
                archiveFile(document)
                document.save(flush: true)
            }
        }
    }

    /**
     * Deletes the file associated with the supplied Document from the file system.
     * @param document identifies the file to delete.
     * @return true if the delete operation was successful.
     */
    boolean deleteFile(Document document) {
        File fileToDelete = new File(fullPath(document.filepath, document.filename))
        fileToDelete.delete();
    }

    /**
     * Move the document's file to the 'archive' directory. This is used when the Document is being soft deleted.
     * The file should only be deleted if the Document has been 'hard' deleted.
     * @param document the Document entity representing the file to be moved
     * @return the new absolute location of the file
     */
    void archiveFile(Document document) {
        File fileToArchive = new File(fullPath(document.filepath, document.filename))

        if (fileToArchive.exists()) {
            File archiveDir = new File("${grailsApplication.config.app.file.archive.path}/${document.filepath}")
            // This overwrites an archived file with the same name.
            FileUtils.copyFileToDirectory(fileToArchive, archiveDir)
            FileUtils.deleteQuietly(fileToArchive)
        } else {
            log.warn("Unable to move file for document ${document.documentId}: the file ${fileToArchive.absolutePath} does not exist.")
        }
    }

    def findAllByOwner(ownerType, owner, includeDeleted = false) {
        def query = Document.createCriteria()

        def results = query {
           ne('type', LINKTYPE)
           eq(ownerType, owner)
           if (!includeDeleted) {
               ne('status', DELETED)
           }
        }

        results.collect{toMap(it, 'flat')}
    }

    def findAllLinksByOwner(ownerType, owner, includeDeleted = false) {
        def query = Document.createCriteria()

        def results = query {
            eq('type', LINKTYPE)
            eq(ownerType, owner)
            if (!includeDeleted) {
                ne('status', DELETED)
            }
        }

        results.collect{toMap(it, 'flat')}
    }

    Boolean isMobileAppForProject(Map project){
        List links = project.links
        Boolean isMobileApp = false;
        isMobileApp = links?.any {
            it.role in MOBILE_APP_ROLE;
        }
        isMobileApp;
    }

    /**
     * Remove necessary properties from a document that is embargoed.
     * @param doc
     * @return doc
     */
    public Map embargoDocument (Map doc){
        List blackListProps = ['thumbnailUrl','url','dataTaken','attribution','notes','filename','filepath','documentId']
        doc.isEmbargoed = true;
        blackListProps.each { item ->
            doc.remove(item)
        }
        doc
    }

    /**
     * Reads the contents of a file associated with a Document and return content as JSON
     */
    def readJsonDocument(Map document) {
        String fullPath = this.fullPath(document.filepath, document.filename)
        File file = new File(fullPath)

        if (!file.exists()) {
            return  [error: fullPath + ' does not exist!']
        }
        def jsonSlurper = new JsonSlurper()
        def data = jsonSlurper.parse(file)
        return data
    }

    void doWithAllDocuments(Closure action) {
        def offset = 0
        def batchSize = 100

        def count = batchSize // For first loop iteration
        while (count == batchSize) {
            List documents = Document.findAllByStatus('active', [offset: offset, max: batchSize]).collect {
                action.call(toMap(it))
            }

            count = documents.size()
            offset += batchSize
            Document.withSession { session -> session.clear() }
        }
    }
}
