%{--<%@page expressionCodec="none" %>--}%
<g:if test="${!omitDescription}">
<strong>Description</strong>
<p>
<g:message code="${'api.'+name+'.description'}" default="${g.message([code:'api.description.missing'])}"/>
</p>
</g:if>
<ul id="propertiesTabs" class="nav nav-tabs big-tabs">
    <li class="active"><a href="#overview" id="overview-tab" data-toggle="tab">Overview</a></li>
    <li><a href="#schema" id="schema-tab" data-toggle="tab">Schema</a></li>
    <li><a href="#example" id="example-tab" data-toggle="tab">Example</a></li>

</ul>
<div class="tab-content">
    <div class="tab-pane active" id="overview">
        <p><g:message code="api.overview.caption" args="${[name]}"/></p>
        <div class="row-fluid">
            <div class="span4">
                <pre>

                </pre>
                <g:if test="${overview}">

                    <asset:script>
                        $(function(){$('#overview pre').text(vkbeautify.json('${overview as grails.converters.JSON}'));});

                    </asset:script>
                </g:if>
            </div>
        </div>
        <div class="row-fluid">
            <p>
                <g:message code="api.overview.properties.table.caption" args="${[name]}"/>
            </p>

            <div class="span11">
                <g:set var="toDocument" value="${target?:object}"/>
                <g:render template="objectProperties" model="${[object:toDocument, required:toDocument.required?:[], name:name]}"/>

                <g:each in="${object.definitions}" var="definition">
                    <h5 id="${definition.key}"><g:message code="api.output.definition.heading" args="${[definition.key]}"/></h5>
                    <g:render template="objectProperties" model="${[object:definition.value, required:definition.value.required?:[], name:name+'.'+definition.key, descriptionKeyPrefix:"api.property.${name}"]}"/>
                </g:each>
            </div>
        </div>

    </div>
    <div class="tab-pane" id="schema">
        <pre>
            ${(object as grails.converters.JSON).toString(true)}
        </pre>

        <asset:script>

            $(function(){
                var schema = ${object as grails.converters.JSON};
                $('#schema pre').text(vkbeautify.json(schema));});

        </asset:script>
    </div>
    <div class="tab-pane" id="example">
        <pre>
            to be supplied....
        </pre>

        <g:if test="${example}">
            <asset:script>
                $(function(){
                    var example = ${example as grails.converters.JSON};
                    $('#example pre').text(vkbeautify.json(example));
                });
            </asset:script>
        </g:if>

    </div>
</div>

