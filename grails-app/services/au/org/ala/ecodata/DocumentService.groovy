package au.org.ala.ecodata
import com.itextpdf.text.PageSize
import com.itextpdf.text.html.simpleparser.HTMLWorker
import com.itextpdf.text.pdf.PdfWriter
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.imgscalr.Scalr

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.text.DateFormat
import java.text.SimpleDateFormat

import static au.org.ala.ecodata.Status.ACTIVE
import static au.org.ala.ecodata.Status.DELETED

class DocumentService {

    static final LINKTYPE = "link"
    static final LOGO = 'logo'
    static final FILE_LOCK = new Object()

    static final DIRECTORY_PARTITION_FORMAT = 'yyyy-MM'
    static  final MOBILE_APP_ROLE = [ "android",
                                     "blackberry",
                                     "iTunes",
                                     "windowsPhone"]

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
        mapOfProperties.url = document.url
        if (document.isImage()) {
            mapOfProperties.thumbnailUrl = document.thumbnailUrl
        }
        mapOfProperties.findAll {k,v -> v != null}
    }

    def get(id, levelOfDetail = []) {
        def o = Document.findByDocumentIdAndStatus(id, ACTIVE)
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

    def findAllForProjectId(id, levelOfDetail = []) {
        Document.findAllByProjectIdAndStatusAndTypeNotEqual(id, ACTIVE, LINKTYPE).collect { toMap(it, levelOfDetail) }
    }

    def findAllLinksForProjectId(id, levelOfDetail = []) {
        Document.findAllByProjectIdAndType(id, LINKTYPE).collect { toMap(it, levelOfDetail) }
    }

    def findAllForProjectIdAndIsPrimaryProjectImage(id, levelOfDetail = []) {
		Document.findAllByProjectIdAndStatusAndIsPrimaryProjectImage(id, ACTIVE,true).collect { toMap(it, levelOfDetail) }
	}

    def findAllForActivityId(id, levelOfDetail = []) {
        Document.findAllByActivityIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    def findAllForSiteId(id, levelOfDetail = []) {
        Document.findAllBySiteIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    def findAllForOutputId(id, levelOfDetail = []) {
        Document.findAllByOutputIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }
    def findAllForProjectActivityId(id, levelOfDetail = []) {
        Document.findAllByProjectActivityIdAndStatus(id, ACTIVE).collect { toMap(it, levelOfDetail) }
    }

    String findImageUrlForProjectId(id, levelOfDetail = []){
        Document primaryImageDoc;
        Document logoDoc = Document.findByProjectIdAndRoleAndStatus(id, LOGO, ACTIVE);
        String urlImage;
        urlImage = logoDoc?.url ? logoDoc.getThumbnailUrl() : ''
        if(!urlImage){
            primaryImageDoc = Document.findByProjectIdAndIsPrimaryProjectImage(id, true)
            urlImage = primaryImageDoc?.url;
        }
        urlImage
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
            ne("status", DELETED)
            searchCriteria.each { prop,value ->

                if (value instanceof List) {
                    inList(prop, value)
                }
                else {
                    eq(prop, value)
                }
            }
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
                    props.filename = saveFile(partition, props.filename, fileIn, false)
                    if (props.type == Document.DOCUMENT_TYPE_IMAGE) {
                        makeThumbnail(partition, props.filename)
                    }
                }
                props.filepath = partition
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
                    props.filename = saveFile(d.filepath, props.filename, fileIn, true)
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
     * @return the filename (not the full path) the file was saved using.  This may not be the same as the supplied
     * filename in the case that overwrite is false.
     */
    private String saveFile(filepath, filename, fileIn, overwrite) {
        if (fileIn) {
            synchronized (FILE_LOCK) {
                //create upload dir if it doesnt exist...
                def uploadDir = new File(fullPath(filepath, ''))

                if(!uploadDir.exists()){
                    FileUtils.forceMkdir(uploadDir)
                }

                if (!overwrite) {
                    filename = nextUniqueFileName(filepath, filename)
                }
                new FileOutputStream(fullPath(filepath, filename)).withStream { it << fileIn }
            }
        }
        return filename
    }

    /**
     * Creates a thumbnail of the image stored at the location specified by filepath and filename.
     * @param filepath the path (relative to root document storage) at which the file can be found.
     * @param filename the name of the file.
     */
    def makeThumbnail(filepath, filename, overwrite = true) {

        File tnFile = new File(fullPath(filepath, Document.THUMBNAIL_PREFIX+filename))
        if (tnFile.exists()) {
            if (!overwrite) {
                return
            }
            else {
                tnFile.delete();
            }
        }

        def ext = FilenameUtils.getExtension(filename)
        BufferedImage img = ImageIO.read(new File(fullPath(filepath, filename)))
        BufferedImage tn = Scalr.resize(img, 300, Scalr.OP_ANTIALIAS)
        try {
            def success = ImageIO.write(tn, ext, tnFile)
            log.debug "Thumbnailing: " + success
        } catch(IOException e) {
            e.printStackTrace()
            log.error "Write error for " + tnFile.getPath() + ": " + e.getMessage()
        }

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

    String fullPath(String filepath, String filename) {
        String path = filepath ?: ''
        if (path) {
            path = path+File.separator
        }
        return grailsApplication.config.app.file.upload.path + '/' + path  + filename
    }

    void deleteAllForProject(String projectId, boolean destroy = false) {
        List<String> documentIds = Document.withCriteria {
            eq "projectId", projectId
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

            FileUtils.moveFileToDirectory(fileToArchive, archiveDir, true)
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

}
