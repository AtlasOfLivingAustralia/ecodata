package au.org.ala.ecodata

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Directory
import groovy.util.logging.Slf4j
import org.apache.commons.io.FilenameUtils
import org.imgscalr.Scalr
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Slf4j
class ImageUtils {

    /**
     * Creates a thumbnail of an image file.
     * @param original the image file to create a thumbnail of
     * @param destination where to write the thumbnail
     * @param targetSize the size of the thumbnail (this will be the size of the largest out of width and height)
     * @return the thumbnail file if successful, null otherwise.
     */
    static File makeThumbnail(File original, File destination, int targetSize) {
        def ext = FilenameUtils.getExtension(original.name)
        BufferedImage img = ImageIO.read(original)
        BufferedImage tn = Scalr.resize(img, targetSize, Scalr.OP_ANTIALIAS)
        try {
            ImageIO.write(tn, ext, destination)
            return destination
        } catch(IOException e) {
            log.error("Write error for " + destination.getPath() + ": " + e.getMessage(), e)
            return null
        }
    }

    /**
     * Reads the exif data from the supplied file, and if it's available, checks the image orientation.
     * If necessary, a processed version of the image will be created and written to the supplied file.
     *
     * @param original the file to check and rotate if necessary
     * @param rotated a placeholder for the output, must not exist already.
     * @return true if processing was performed.
     */
    static boolean reorientImage(File original, File output) {
        int orientation = getOrientation(original)

        List<Scalr.Rotation> transforms = []
        switch (orientation) {

            case 1:
                break // Image is already oriented correctly
            case 2:
                transforms << Scalr.Rotation.FLIP_HORZ
                break
            case 3:
                transforms << Scalr.Rotation.CW_180
                break
            case 4:
                transforms << Scalr.Rotation.FLIP_VERT
                break
            case 5:
                transforms << Scalr.Rotation.CW_180
                transforms << Scalr.Rotation.FLIP_HORZ
            case 6:
                transforms << Scalr.Rotation.CW_90
                break
            case 7:
                transforms << Scalr.Rotation.CW_90
                transforms << Scalr.Rotation.FLIP_VERT
                break
            case 8:
                transforms << Scalr.Rotation.CW_270
                break
        }

        boolean processed = false

        if (transforms) {
            BufferedImage result = ImageIO.read(original)

            transforms.each { transform ->
                result = Scalr.rotate(result, transform, Scalr.OP_ANTIALIAS)
            }
            processed = ImageIO.write(result, FilenameUtils.getExtension(original.name), output)
        }
        processed
    }

    /**
     * Returns the orientation tag from the image EXIF data, if available.  If no EXIF data exists,
     * this method returns 0.
     * @param file the image file to check.
     * @return the value of the EXIF orientation tag, or 0 if no EXIF data was found.
     */
    private static int getOrientation(File file) {
        int orientation = 0

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file)
            Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class)

            if (dir && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                orientation = dir.getInt(ExifIFD0Directory.TAG_ORIENTATION)
            }
        }
        catch (ImageProcessingException e) {
            log.info("Unsupported file type encountered when attempting to read image metadata: ${file.name}")
        }

        return orientation
    }
}
