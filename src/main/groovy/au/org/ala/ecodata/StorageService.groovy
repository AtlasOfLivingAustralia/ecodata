package au.org.ala.ecodata

interface StorageService {

    String saveFile(String path, String filename, InputStream inputStream, long contentLength)
    InputStream getFile(String path, String filename)
    boolean deleteFile(String path, String filename)
    boolean fileExists(String path, String filename)
    void archiveFile(String path, String filename)

    /**
     * We are preserving the file name so the URLs look nicer and the file extension isn't lost.
     * As filename are not guaranteed to be unique, we are pre-pending the file with a counter if necessary to
     * make it unique.
     */
    default String nextUniqueFileName(String path, String filename) {
        int counter = 0
        String newFilename = filename
        while (fileExists(path, newFilename)) {
            newFilename = "${counter}_${filename}"
            counter++
        }

        return newFilename
    }
}