package au.org.ala.ecodata

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import org.apache.commons.io.FileUtils
import org.imgscalr.Scalr
import org.apache.commons.io.FilenameUtils

class ThumbnailableImage {

    ThumbnailableImage(imageFile){ this.imageFile = imageFile }

    def imageFile
    BufferedImage img = ImageIO.read(imageFile)
    /**
     * Write a thumbnail to file
     */
    def writeThumbnailToFile(File newThumbnailFile, Float edgeLength) {
        def width = img.getWidth()
        def filename = this.imageFile.getName()
        def ext = FilenameUtils.getExtension(filename)
        if (width > edgeLength) {
            BufferedImage tn = Scalr.resize(img, edgeLength as Integer, Scalr.OP_ANTIALIAS)
            try {
                ImageIO.write(tn, ext, newThumbnailFile)  // accept other file formats
            } catch(IOException e) {
                log.error "Write error for " + newThumbnailFile.getPath() + ": " + e.getMessage()
            }
        } else {
            FileUtils.copyFile(imageFile, newThumbnailFile)
        }
    }
}