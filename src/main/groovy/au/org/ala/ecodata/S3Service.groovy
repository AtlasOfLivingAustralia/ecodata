package au.org.ala.ecodata

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.model.*

@Slf4j
class S3Service implements StorageService {
    @Autowired
    GrailsApplication grailsApplication
    S3Client client
    String bucketName
    String baseUrl

    S3Client getS3Client() {
        if (!client) {
            def config = grailsApplication.config
            String region = config.getProperty('aws.region', String)
            boolean containerCredentials = config.getProperty('aws.credentials.container', Boolean, false)
            String profileName = config.getProperty('aws.credentials.profile', String)

            bucketName = config.getProperty('aws.s3.bucket', String)
            baseUrl = config.getProperty('aws.s3.baseUrl', String)
            S3ClientBuilder clientBuilder = S3Client.builder()
                    .region(Region.of(region))
            if (containerCredentials) {
                log.info("Using container credentials for S3 client")
                clientBuilder.credentialsProvider(DefaultCredentialsProvider.create())
            } else if (profileName) {
                // when developing locally, use the profile credentials provider
                clientBuilder.credentialsProvider(ProfileCredentialsProvider.create('test-ala-developer'))
            }
            else {
                throw new RuntimeException("Profile name must be provided for S3 client")
            }

            client = clientBuilder.build()
        }

        client
    }

    /**
     * Upload a file to S3
     */
    String uploadFile(String key, InputStream inputStream, long contentLength) {
        // save to temp file to determine content length
        File tempFile
        if (contentLength in [0L, -1L]) {
            tempFile = File.createTempFile("s3upload", null)
            tempFile.withOutputStream { tempStream ->
                inputStream.transferTo(tempStream)
            }

            contentLength = tempFile.length()
            inputStream = tempFile.newInputStream()
        }


        try {
            String contentType = 'application/octet-stream'
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build()

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength))
            return key
        } catch (Exception e) {
            log.error("Error uploading file to S3: ${e.message}", e)
            throw new Exception("Failed to upload file to S3", e)
        }
        finally {
            if (tempFile) {
                inputStream.close()
                tempFile.delete()
            }
        }
    }

    /**
     * Download a file from S3
     */
    InputStream downloadFile(String key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()

            return s3Client.getObject(getRequest)
        } catch (NoSuchKeyException e) {
            log.error("File not found: ${key}", e)
            throw new FileNotFoundException("File not found: ${key}")
        } catch (Exception e) {
            log.error("Error downloading file from S3: ${e.message}", e)
            throw new Exception("Failed to download file from S3", e)
        }
    }

    /**
     * Delete a file from S3
     */
    boolean deleteFile(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()

            s3Client.deleteObject(deleteRequest)
            return true
        } catch (Exception e) {
            log.error("Error deleting file from S3: ${e.message}", e)
            return false
        }
    }

    /**
     * Check if file exists
     */
    boolean fileExists(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()

            s3Client.headObject(headRequest)
            return true
        } catch (NoSuchKeyException e) {
            return false
        } catch (Exception e) {
            log.error("Error checking file existence: ${e.message}", e)
            return false
        }
    }

    /**
     * List files in a directory (prefix)
     */
    List<String> listFiles(String prefix = "") {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build()

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest)

            return response.contents().collect { it.key() }
        } catch (S3Exception e) {
            log.error("Error listing files: ${e.message}", e)
            return []
        }
    }

    /**
     * Copy a file within S3
     */
    boolean copyFile(String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build()

            s3Client.copyObject(copyRequest)
            return true
        } catch (S3Exception e) {
            log.error("Error copying file: ${e.message}", e)
            return false
        }
    }

    /**
     * Get file metadata
     */
    Map getFileMetadata(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()

            HeadObjectResponse response = s3Client.headObject(headRequest)

            return [
                    contentType: response.contentType(),
                    contentLength: response.contentLength(),
                    lastModified: response.lastModified(),
                    etag: response.eTag(),
                    metadata: response.metadata()
            ]
        } catch (S3Exception e) {
            log.error("Error getting file metadata: ${e.message}", e)
            return null
        }
    }

    @Override
    String saveFile(String path, String filename, InputStream inputStream, long contentLength) {
        uploadFile(getKey(path, filename), inputStream, contentLength)
    }

    @Override
    InputStream getFile(String path, String filename) {
        return downloadFile(getKey(path, filename))
    }

    @Override
    boolean deleteFile(String path, String filename) {
        return deleteFile(getKey(path, filename))
    }

    @Override
    boolean fileExists(String path, String filename) {
        return fileExists(getKey(path, filename))
    }

    @Override
    void archiveFile(String path, String filename) {
        String key = getKey(path, filename)
        String archiveKey = getKey("archive/${path}", filename)
        copyFile(key, archiveKey)
        deleteFile(key)
    }

    @Override
    String nextUniqueFileName(String path, String filename) {
        int counter = 0
        String newFilename = filename
        List files = listFiles(path)
        while (fileExistsInPrefix(files, newFilename)) {
            newFilename = "${counter}_${filename}"
            counter++
        }

        return newFilename
    }

    @Override
    boolean validateDocumentFilePath(String path, String filename) {
        return true
    }


    boolean fileExistsInPrefix(List files = [], String filename) {
            return files.any { it.endsWith("/${filename}") }
    }

    private static String getKey(String path, String filename) {
        return "${path}/${filename}"
    }
}