package au.org.ala.ecodata


import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

/*
 * Copyright (C) 2022 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * Created by Temi on 15/2/22.
 */

class DocumentUrlBuilderSpec extends Specification implements GrailsUnitTest {
    def source
    Closure doWithConfig() {{ config->
        config.biocollect.hostname = 'https://biocollect.ala.org.au'
        config.app.uploads.url = '/document/download/'
        config.app.path.document.url = ['a.b.c', 'xyz', 'abc', 'abc.dfg', 'f.g', 'documents', 'documents.image']
    }}

    def setup() {
        source = [
                'xyz': '/document/download/1.jpeg',
                'abc': 'https://biocollect.ala.org.au/xyz.jpg',
                'def': null,
                'a'  : [
                        'b': [
                                'c': '/document/download/1.jpeg'
                        ]
                ],
                'f'  : [['g': '/document/download/1.jpeg'], ['g': '/document/download/1.jpeg']]
        ]
    }

    def "updateDocumentURL should add hostname to all listed properties"() {
        def result

        when:
        result = DocumentUrlBuilder.updateDocumentURL(source, grailsApplication.config.biocollect.hostname)

        then:
        result == [
                'xyz': 'https://biocollect.ala.org.au/document/download/1.jpeg',
                'abc': 'https://biocollect.ala.org.au/xyz.jpg',
                'def': null,
                'a'  : [
                        'b': [
                                'c': 'https://biocollect.ala.org.au/document/download/1.jpeg'
                        ]
                ],
                'f'  : [['g': 'https://biocollect.ala.org.au/document/download/1.jpeg'], ['g': 'https://biocollect.ala.org.au/document/download/1.jpeg']]
        ]
    }
}
