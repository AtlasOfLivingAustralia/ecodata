package au.org.ala.ecodata

import groovy.xml.MarkupBuilder

class ECTagLib {

    static namespace = "ec"

    def userService, authService

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
                mkp.yield(attrs.title)
                span(class:'glyphicon glyphicon-chevron-right pull-right') { mkp.yieldUnescaped('&nbsp;')}
            }
        }
    }

    def currentUserDisplayName = { attrs, body ->
        def mb = new MarkupBuilder(out)

        mb.span(class:'username') {
            def displayName = authService.displayName
            if (displayName) {
                mkp.yield(displayName)
            } else if (request.userPrincipal) {
                mkp.yield(request.userPrincipal)
            } else {
                mkp.yield(userService.currentUserDisplayName)
            }
        }
    }

    /**
     * Check if the logged in user has the requested role
     *
     * @attr role REQUIRED
     */
    def userInRole = { attrs ->
        if (authService.userInRole(attrs.role)) {
            out << true
        }
    }

    def currentUserId = { attrs, body ->
        out << authService.userDetails()?.userId
    }

    /**
     * Renders a textual description of the constraints applied to the value of a property (as described in the
     * supplied schema).
     * @attr property the property description from the schema.
     */
    def outputPropertyConstraints = { attrs, body ->
        def property = attrs.property
        if (property.format == 'date-time') {
            out << g.message(code: 'api.constraints.date.time')
        }
        else if (property.format == 'base64') {
            out << g.message(code: 'api.constraints.base64')
        }
        else if (property.enum) {
            out << g.message(code: 'api.constraints.enum')
            writeConstraints(property.enum)
        }
        else if (property.type == 'array') {
            if (property.items?.enum) {
                out << g.message(code: 'api.constraints.array.enum')
                writeConstraints(property.items.enum)
            }
            else if (property.items.type == 'object') {
                def ref
                if (property.items.oneOf) {
                    ref = property.items.oneOf[0].$ref
                    def anchor = buildAnchor(ref)
                    out << g.message(code: 'api.constraints.array.object', args:[anchor.href, anchor.name])
                }
                else if (property.items.anyOf) {
                    out << g.message(code: 'api.constraints.array.objects')
                    def constraints = []
                    property.items.anyOf.each{
                        def anchor = buildAnchor(it.$ref)
                        constraints << g.message(code:'api.constraints.object.ref', args:[anchor.href, anchor.name])
                    }
                    writeConstraints(constraints, false, false)
                }


            }
        }
        else if ((property.minimum != null) || (property.maximum != null)) {
            if ((property.minimum != null) & (property.maximum != null)) {
                out << g.message(code: 'api.constraints.property.between', args:[property.minimum, property.maximum])
            }
            else if (property.minimum != null) {
                out << g.message(code: 'api.constraints.property.greaterThan', args:[property.minimum])
            }
            else {
                out << g.message(code: 'api.constraints.property.lessThan', args:[property.maximum])
            }
        }
    }

    private def buildAnchor(ref) {

        def name = ref.substring(ref.lastIndexOf('/')+1)
        name = name.replaceAll('%20', ' ')
        def href = ref.startsWith('http')?ref+'.html':'#'+name
        [href:href, name:name]

    }

    private void writeConstraints(constraints, quote = true, escape = true) {
        def mb = new MarkupBuilder(out)
        mb.setEscapeAttributes(escape)
        mb.ul {
            constraints.each { value ->
                if (quote) {
                    value = "'${value}'"
                }
                mb.li {
                    if (escape) {
                        mkp.yield(value)
                    }
                    else {
                        mkp.yieldUnescaped(value)
                    }
                }
            }
        }
    }

    /**
     * @attr name
     * @attr targetField
     * @attr printable
     * @attr size optionally overrides the bootstrap size class for the input
     */
    def datePicker = { attrs ->
        /**
         <input data-bind="datepicker:startDate.date" name="startDate" id="startDate" type="text" size="16"
         data-validation-engine="validate[required]" class="input-xlarge"/>
         <span class="add-on open-datepicker"><i class="icon-th"></i></span>
         */

        def mb = new MarkupBuilder(out)

        if (!attrs.printable) {
            def inputAttrs = [
                    "data-bind":"datepicker:${attrs.targetField}",
                    name:"${attrs.name}",
                    id:"${attrs.id ?: attrs.name}",
                    type:'text',
                    size:'16',
                    class: attrs.size ?: 'input-xlarge'
            ]

            def ignoreList = ['name', 'id']
            attrs.each {
                if (!ignoreList.contains(it.key)) {
                    inputAttrs[it.key] = it.value
                }
            }

            if (attrs.required) {
                inputAttrs["data-validation-engine"] = "validate[required]"
            }

            mb.input(inputAttrs) {
            }

            mb.span(class:'add-on open-datepicker') {
                mb.i(class:'icon-th') {
                    mkp.yieldUnescaped("&nbsp;")
                }
            }
        } else {
            def inputAttrs = [
                    name:"${attrs.name}",
                    id:"${attrs.id ?: attrs.name}",
                    class: (attrs.size ?: 'span6') + ' printed-form-field'
            ]

            def ignoreList = ['name', 'id']
            attrs.each {
                if (!ignoreList.contains(it.key)) {
                    inputAttrs[it.key] = it.value
                }
            }
            mb.span(inputAttrs) {
                mkp.yieldUnescaped("&nbsp;")
            }
        }
    }

}
