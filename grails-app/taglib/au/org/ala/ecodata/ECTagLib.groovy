package au.org.ala.ecodata

import groovy.xml.MarkupBuilder

class ECTagLib {

    static namespace = "ec"

    def userService

    /**
     * @attr active
     * @attr title
     * @attr href
     */
    def breadcrumbItem = { attrs, body ->
        def active = attrs.active
        if (!active) {
            active = attrs.title
        }
        def current = pageProperty(name:'page.pageTitle')?.toString()

        def mb = new MarkupBuilder(out)
        mb.li(class: active == current ? 'active' : '') {
            a(href:attrs.href) {
                i(class:'icon-chevron-right') { mkp.yieldUnescaped('&nbsp;')}
                mkp.yield(attrs.title)
            }
        }
    }

    def currentUserDisplayName = { attrs, body ->
        def mb = new MarkupBuilder(out)
        mb.span(class:'username') {
            UserDetails userDetails = request.getAttribute(UserDetails.REQUEST_USER_DETAILS_KEY)
            if (userDetails) {
                mkp.yield(userDetails.displayName)
            } else {
                mkp.yield(userService.currentUserDisplayName)
            }
        }
    }

}
