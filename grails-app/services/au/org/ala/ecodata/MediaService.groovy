package au.org.ala.ecodata

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringEscapeUtils

class MediaService {

    static transactional = false

    def grailsApplication

    def THUMB = [suffix: "__thumb", size: 100f ]
    def SMALL = [suffix: "__small", size: 314f ]
    def LARGE = [suffix: "__large", size: 650f ]

    def serviceMethod() {}

    def removeImage(filePath){
        def thumb = getFileForFormat(filePath,THUMB)
        def small = getFileForFormat(filePath,SMALL)
        def large = getFileForFormat(filePath,LARGE)
        def raw = new File(filePath)
        if(thumb.exists()) thumb.delete() //FileUtils.forceDelete(thumb)
        if(small.exists()) small.delete() //FileUtils.forceDelete(small)
        if(large.exists()) large.delete() //FileUtils.forceDelete(large)
        if(raw.exists()) raw.delete() //FileUtils.forceDelete(raw)
    }

    def setupMediaUrls(mapOfProperties){
        if (mapOfProperties["associatedMedia"] != null){

            if (isCollectionOrArray(mapOfProperties["associatedMedia"])){

                def imagesArray = []
                def originalsArray = []

                mapOfProperties["associatedMedia"].each {

                    def imagePath = it.replaceAll(grailsApplication.config.app.file.upload.path,
                            grailsApplication.config.app.uploads.url)
                    def extension = FilenameUtils.getExtension(imagePath)
                    def pathWithoutExt = imagePath.substring(0, imagePath.length() - extension.length() - 1 )
                    def image = [
                            thumb : pathWithoutExt + "__thumb."+extension,
                            small : pathWithoutExt + "__small."+extension,
                            large : pathWithoutExt + "__large."+extension,
                            raw : imagePath,
                    ]
                    originalsArray << imagePath
                    imagesArray << image
                }
                mapOfProperties['associatedMedia'] = originalsArray
                mapOfProperties['images'] = imagesArray
            } else {
                def imagePath = mapOfProperties["associatedMedia"].replaceAll(grailsApplication.config.app.file.upload.path,
                        grailsApplication.config.app.uploads.url)
                def extension = FilenameUtils.getExtension(imagePath)
                def pathWithoutExt = imagePath.substring(0, imagePath.length() - extension.length() - 1 )
                def image = [
                        thumb : pathWithoutExt + "__thumb."+extension,
                        small : pathWithoutExt + "__small."+extension,
                        large : pathWithoutExt + "__large."+extension,
                        raw : imagePath,
                ]
                mapOfProperties['associatedMedia'] = [imagePath]
                mapOfProperties['images'] = [image]
            }
        }
    }

    def setupMediaUrlsForAssociatedMedia(mapOfProperties){
        if (mapOfProperties["associatedMedia"] != null){

            if (isCollectionOrArray(mapOfProperties["associatedMedia"])){
                def originalsArray = []
                mapOfProperties["associatedMedia"].each {
                    def imagePath = it.replaceAll(grailsApplication.config.app.file.upload.path,
                            grailsApplication.config.app.uploads.url)
                    originalsArray << imagePath
                }
                mapOfProperties['associatedMedia'] = originalsArray
            } else {
                def imagePath = mapOfProperties["associatedMedia"].replaceAll(grailsApplication.config.app.file.upload.path,
                        grailsApplication.config.app.uploads.url)
                mapOfProperties['associatedMedia'] = [imagePath]
            }
        }
    }

    def File copyBytesToImageDir(recordId, fileNameWithExtension, theBytes){
        File directory = new File(grailsApplication.config.fielddata.mediaDir + recordId)
        if(!directory.exists()){
            FileUtils.forceMkdir(directory)
        }
        File destFile = new File(grailsApplication.config.fielddata.mediaDir + recordId + File.separator + fileNameWithExtension.replaceAll(" ", "_"))
        try {
            FileUtils.writeByteArrayToFile(destFile, theBytes)
            //generate thumbnail, small, large
            generateAllSizes(destFile)
            destFile
        } catch (Exception e){
            log.info "Unable to create file: "  + fileNameWithExtension + " for record " + recordId
            //clean up afterwards
            if(directory.listFiles().length == 0){
                FileUtils.forceDelete(directory)
            }
            null
        }
    }
    def File copyToImageDir(recordId,currentFilePath){

        File directory = new File(grailsApplication.config.fielddata.mediaDir + recordId)
        if(!directory.exists()){
            FileUtils.forceMkdir(directory)
        }
        File destFile = new File(grailsApplication.config.fielddata.mediaDir + recordId + File.separator + (new File(currentFilePath)).getName().replaceAll(" ", "_"))
        try {
            FileUtils.copyFile(new File(currentFilePath),destFile)
            //generate thumbnail, small, large
            generateAllSizes(destFile)
            destFile
        } catch (Exception e){
            log.info "Unable to copy across file: "  + currentFilePath + " for record " + recordId
            //clean up afterwards
            if(directory.listFiles().length == 0){
                FileUtils.forceDelete(directory)
            }
            null
        }
    }

    def download(recordId, idx, address){
        File mediaDir = new File(grailsApplication.config.fielddata.mediaDir + File.separator + recordId + File.separator)
        if (!mediaDir.exists()){
            FileUtils.forceMkdir(mediaDir)
        }
        def destFile = new File(grailsApplication.config.app.file.upload.path + File.separator  + recordId + File.separator + idx + "_" +address.tokenize("/")[-1])
        def out = new BufferedOutputStream(new FileOutputStream(destFile))
        log.debug("Trying to download..." + address)
        String decodedAddress = StringEscapeUtils.unescapeXml(address);
        log.debug("Decoded address " + decodedAddress)
        out << new URL(decodedAddress).openStream()
        out.close()
        generateAllSizes(destFile)
        destFile
    }


    /** Generate thumbnails of all sizes */
    def generateAllSizes(File source){
        def fileName = source.getName()
        if(!fileName.contains(THUMB.suffix) && !fileName.contains(SMALL.suffix) && !fileName.contains(LARGE.suffix)){
            generateThumbnail(source, THUMB)
            generateThumbnail(source, SMALL)
            generateThumbnail(source, LARGE)
        }
    }

    File getFileForFormat(filePath, imageSize){
        def source = new File(filePath)
        def extension = FilenameUtils.getExtension(filePath)
        def targetFilePath = source.getPath().replace("." + extension, imageSize.suffix + "." + extension)
        new File(targetFilePath)
    }

    /** Generate an image of the specified size.*/
    def generateThumbnail(source, imageSize){
        def extension = FilenameUtils.getExtension(source.getPath())
        def sourcePath = source.getPath()
        def targetFilePath = sourcePath.substring(0, sourcePath.length() - (extension.length() + 1)) + imageSize.suffix + "." + extension
        def target = new File(targetFilePath)
        generateThumbnail(source, target, imageSize.size)
    }

    /** Generate a thumbnail to the specified file */
    def generateThumbnail(source, target, thumbnailSize){
        def t = new ThumbnailableImage(source)
        t.writeThumbnailToFile(target, thumbnailSize)
    }

    boolean isCollectionOrArray(object) {
        [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
    }
}