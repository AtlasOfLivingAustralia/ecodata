<table class="table table-striped table-bordered">
    <thead>
    <tr>
        <th class="property-name"><g:message code="api.property.name.header"/></th>
        <th class="property-type"><g:message code="api.property.type.header"/></th>
        <th class="property-description"><g:message code="api.property.description.header"/></th>
        <th class="property-constraints"><g:message code="api.property.constraints.header"/></th>
    </tr>
    </thead>
    <tbody>

    <g:each in="${object.properties}" var="property">
        <tr>
            <td class="property-name">${property.key}</td>
            <td class="property-type">${property.value.type?:'string'}</td>
            <td class="property-description"><g:message code="${descriptionKeyPrefix+'.'+property.key+'.description'}" default="${property.value.description}"/></td>
            <td><ec:outputPropertyConstraints property="${property.value}"/></td>
        </tr>
    </g:each>

    </tbody>
</table>


