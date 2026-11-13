package au.org.ala.ecodata

import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest

/**
 * Grails 8 keeps the outermost servlet request (security / application filter wrappers) exposed to controllers
 * instead of substituting the resolved MultipartHttpServletRequest, so
 * {@code request instanceof MultipartHttpServletRequest} is no longer a reliable test.
 * Check the request content type instead, then use the request upload accessors
 * ({@code request.getFile}, {@code getFileNames}, {@code getFileMap}) which Grails resolves behind the wrappers.
 */
@CompileStatic
class MultipartRequestUtils {

    static boolean isMultipart(HttpServletRequest request) {
        String contentType = request?.contentType
        contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith('multipart/')
    }
}

