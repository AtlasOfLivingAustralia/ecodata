package au.org.ala.ecodata.config

import au.org.ala.ecodata.FileSystemService
import au.org.ala.ecodata.S3Service
import au.org.ala.ecodata.StorageService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StorageConfig {

    @Bean("storageService")
    @ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "S3")
    StorageService s3Service() {
        return new S3Service()
    }

    @Bean("storageService")
    @ConditionalOnProperty(prefix = "storage", name = "type", havingValue = "filesystem")
    StorageService fileSystemService() {
        return new FileSystemService()
    }

}
