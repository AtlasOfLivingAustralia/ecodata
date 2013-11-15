<div class="row-fluid">
    <h4><g:message code="api.output.heading" args="${[name]}"/></h4>

    <g:render template="objectProperties" model="${[object:output.properties.data, name:name]}"/>

    <g:each in="${output.definitions}" var="definition">
        <h5 id="${definition.key}"><g:message code="api.output.definition.heading" args="${[definition.key]}"/></h5>
        <g:render template="objectProperties" model="${[object:definition.value, name:name+'.'+definition.key, descriptionKeyPrefix:"api.property.${name}"]}"/>
    </g:each>
</div>