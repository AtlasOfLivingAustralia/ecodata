<g:if test="${!omitDescription}">
<strong>Description</strong>
<p>
<g:message code="${'api.'+name+'.description'}" default="${g.message([code:'api.description.missing'])}"/>
</p>
</g:if>
<ul id="propertiesTabs" class="nav nav-tabs big-tabs">
    <li class="nav-item"><a data-bs-target="#overview" class="active nav-link" id="overview-tab" data-bs-toggle="tab">Overview</a></li>
    <li class="nav-item"><a data-bs-target="#schema" class="nav-link" id="schema-tab" data-bs-toggle="tab">Schema</a></li>
    <li class="nav-item"><a data-bs-target="#example" class="nav-link" id="example-tab" data-bs-toggle="tab">Example</a></li>

</ul>
<div class="tab-content">
    <div class="tab-pane active" id="overview">
        <p><g:message code="api.overview.caption" args="${[name]}"/></p>
        <div class="row-fluid">
            <div class="span4">
                <pre>
                    ${raw((overview as grails.converters.JSON).toString(true))}
                </pre>
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
            ${raw((object as grails.converters.JSON).toString(true))}
        </pre>
    </div>
    <div class="tab-pane" id="example">
        <pre>
        <g:if test="${example}">
            ${raw((example as grails.converters.JSON).toString(true))}
        </g:if>
        <g:else>
            to be supplied....
        </g:else>
        </pre>

    </div>
</div>

