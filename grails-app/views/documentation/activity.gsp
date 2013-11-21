
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="api"/>
    <title><g:message code="api.output.page.title" args="${[name]}"/></title>
</head>

<body>

    <div class="container-fluid api">

        <div class="row-fluid">

            <div class="bs-callout bs-callout-warning">
                <h4>This API documentation is in draft format</h4>
                <p>This API documentation is intended as a starting point for discussion and is subject to change.<p>
            </div>
        </div>
        <div class="row-fluid">
            <h4><g:message code="api.activity.heading" args="${[name]}"/></h4>
            <g:render template="objectDocumentation" model="${[object:activity, name:name, overview:overview, descriptionKeyPrefix:"api.property"]}"/>
        </div>
    </div>
</body>
</html>