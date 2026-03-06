package au.org.ala.ecodata

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class FileSystemService implements StorageService {
    @Autowired
    GrailsApplication grailsApplication

    @Override
    String saveFile(String path, String filename, InputStream inputStream, long contentLength) {
        return saveFile(path, filename, inputStream)
    }


    String saveFile(String path, String filename, InputStream inputStream) {
        def uploadDir = new File(fullPath(path, ''))
        if (!uploadDir.exists()) {
            FileUtils.forceMkdir(uploadDir)
        }

        File destination = new File(fullPath(path, filename))
        new FileOutputStream(destination).withStream { it << inputStream }

        return filename
    }

    @Override
    InputStream getFile(String path, String filename) {
        if (fileExists(path, filename)) {
            String fullPath = fullPath(path, filename)
            File file = new File(fullPath)
            return file.newInputStream()
        }
    }

    @Override
    boolean deleteFile(String path, String filename) {
        File fileToDelete = new File(fullPath(path, filename))
        fileToDelete.delete()
    }

    @Override
    boolean fileExists(String path, String filename) {
        String fullPath = fullPath(path, filename)
        File file = new File(fullPath)
        return  file.exists()
    }

    @Override
    void archiveFile(String path, String filename) {
        File fileToArchive = new File(fullPath(path, filename))

        if (fileToArchive.exists()) {
            File archiveDir = new File("${grailsApplication.config.getProperty('app.file.archive.path')}/${path}")
            // This overwrites an archived file with the same name.
            FileUtils.copyFileToDirectory(fileToArchive, archiveDir)
            FileUtils.deleteQuietly(fileToArchive)
        } else {
            log.warn("Unable to archive file ${filename}: the file ${fileToArchive.absolutePath} does not exist.")
        }
    }

    /**
     * Returns the path the document by combining the path and filename with the directory where documents
     * are uploaded.
     * Optionally uses the canonical form of the uploads directory to assist validation.
     */
    String fullPath(String path = "", String filename, boolean useCanonicalFormOfUploadPath = false) {
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
    @Override
    boolean validateDocumentFilePath(String path, String filename) {
        String file = fullPath(path, filename, true)
        return new File(file).getCanonicalPath() == file
    }

}
