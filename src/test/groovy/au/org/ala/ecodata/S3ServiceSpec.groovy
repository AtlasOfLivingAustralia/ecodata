package au.org.ala.ecodata

import spock.lang.Specification

class S3ServiceSpec extends Specification {
    def "nextUniqueFileName returns original filename if not exists"() {
        given:
        def service = Spy(S3Service)
        service.fileExists(_, _) >> false

        expect:
        service.nextUniqueFileName("some/path", "file.txt") == "file.txt"
    }

    def "nextUniqueFileName returns incremented filename if file exists"() {
        given:
        def service = Spy(S3Service)
        service.fileExists(_, _) >>> [true, false]

        expect:
        service.nextUniqueFileName("some/path", "file.txt") == "0_file.txt"
    }

    def "nextUniqueFileName increments until unique"() {
        given:
        def service = Spy(S3Service)
        service.fileExists(_, _) >>> [true, true, false]

        expect:
        service.nextUniqueFileName("some/path", "file.txt") == "1_file.txt"
    }
}

