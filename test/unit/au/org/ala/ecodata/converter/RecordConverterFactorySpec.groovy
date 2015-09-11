package au.org.ala.ecodata.converter

import spock.lang.Specification

class RecordConverterFactorySpec extends Specification {

    def "factory should return a SingleSightingConverter if given a data type of 'singleSighting'"() {
        when:
        RecordConverter converter = RecordConverterFactory.getConverter("singleSighting")

        then:
        converter instanceof SingleSightingConverter
    }

    def "factory should return a ListConverter if given a data type of 'list'"() {
        when:
        RecordConverter converter = RecordConverterFactory.getConverter("list")

        then:
        converter instanceof ListConverter
    }

    def "factory should return a GenericConverter if given an unrecognised data type"() {
        when:
        RecordConverter converter = RecordConverterFactory.getConverter("something")

        then:
        converter instanceof GenericConverter
    }
}
