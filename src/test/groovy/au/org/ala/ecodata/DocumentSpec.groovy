package au.org.ala.ecodata

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.web.context.request.RequestAttributes
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

class DocumentSpec extends Specification implements DomainUnitTest<Document>, ControllerUnitTest<DocumentController> {
    Closure doWithConfig(){{ config->
        config.app.uploads.url = '/dir1/'
        config.imagesService.baseURL = 'https://ala.org.au/abc/'
    }}

    def "document should create url with host name"() {
        def url
        given:
        def document = new Document(filepath: "2021-04", filename: "1.jpeg")
        GrailsWebRequest.lookup().setAttribute(DocumentHostInterceptor.DOCUMENT_HOST_NAME, 'https://xyz.com', RequestAttributes.SCOPE_REQUEST)

        when:
        url = document.getUrl()

        then:
        url == "https://xyz.com/dir1/2021-04/1.jpeg"
    }

    def "document should check if it is available on public server" () {
        when:
        Document doc = new Document(identifier: "https://ala.org.au/abc/xyz.png", type : Document.DOCUMENT_TYPE_IMAGE, filename: 'abc', filepath: 'xyz')

        then:
        doc.isImageHostedOnPublicServer() == true
        doc.getUrl() == doc.identifier

        when:
        doc = new Document(identifier: "https://example.org",type : Document.DOCUMENT_TYPE_IMAGE, filename: 'abc', filepath: 'xyz')

        then:
        doc.isImageHostedOnPublicServer() == false
        doc.getUrl() != doc.identifier
    }
}
