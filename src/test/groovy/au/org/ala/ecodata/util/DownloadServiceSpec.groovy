package au.org.ala.ecodata.util

import au.org.ala.ecodata.DownloadService
import org.apache.commons.io.FilenameUtils
import spock.lang.Specification

class DownloadServiceSpec extends Specification {

    DownloadService service = new DownloadService()

    def "sanitiseZipComponent removes unsafe characters"() {
        expect:
        service.sanitiseZipComponent('Unsafe:/\\Char*?', '', 50) == 'Unsafe___Char__'
    }

    def "sanitiseZipComponent normalises whitespace"() {
        expect:
        service.sanitiseZipComponent('My     Project    Name', '', 50) == 'My Project Name'
    }

    def "sanitiseZipComponent truncates long names"() {
        given:
        def input = 'S' * 100

        expect:
        service.sanitiseZipComponent(input, '', 32).size() == 32
    }

    def "sanitiseZipComponent preserves file extension when requested"() {
        given:
        def input = 'VeryLongFileNameThatShouldBeTrimmed.jpeg'

        when:
        def result = service.sanitiseZipComponent(input, '', 20, true)

        then:
        FilenameUtils.getExtension(result) == 'jpeg'
        result.size() <= 20
    }

    def "sanitiseZipComponent uses fallback when value is null"() {
        when:
        def result = service.sanitiseZipComponent(null, 'fallback', 50)

        then:
        result  == 'fallback'
    }

    def "sanitiseZipComponent safely handles null value and fallback"() {
        when:
        def result = service.sanitiseZipComponent(null, null, 50)

        then:
        result == ''
    }

    def "zipSafeProjectFolderName truncates name and appends id"() {
        given:
        def project = [name: 'Very Long Project Name That Should Be Trimmed']

        when:
        def result = service.zipSafeProjectFolderName(project, 'fb9b9ca3-79d0-4bf8-b58d', 10, 6)

        then:
        result.startsWith('Very Long')
        result.contains('_')
    }

    def "zipSafeFileName preserves extension"() {
        when:
        def result = service.zipSafeFileName('Bad:/File Name.jpeg', 'fallback', 50)

        then:
        FilenameUtils.getExtension(result) == 'jpeg'
        !result.contains(':')
        !result.contains('/')
    }
}
