<%@ page import="grails.converters.JSON" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="api"/>
    <title>MERIT API - POST /POST /external/draft/projectActivities</title>
    <r:require modules="application, app_bootstrap, vkbeautify"/>
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
            <h3>Update project activites</h3>
            <h4>Description</h4>
            <p></p>
            <ul>
                <li>Existing activities will be deleted and replaced with the supplied activities.</li>
                <li>Existing output targets will be deleted and replaced with the supplied output targets.</li>
                <li>Project implementation stages will be derived from the supplied activities. (if not specified)</li>
            </ul>

            <h4>Pre-requisites</h4>
            <ul>
                <li>The project must have been created in the MERIT system.</li>
                <li>Any sites referenced by the planned activities must have been created in the MERIT system</li>
                <li>The project must have no activities?  Or do we delete existing activities? Or add new ones?  Or allow updates by referencing an id?</li>
            </ul>
            <h4>Authentication</h4>
            <p>
                In the short term requests will only be accepted from clients with an IP address appearing on a white list.
            </p>
            <h4>Method / URL</h4>
            POST /external/draft/projectActivities

            <h4>Parameters</h4>
            <p>None</p>

            <h4>POST body</h4>
            <p>The POST body must contain a JSON formatted description of the project activities of the form:</p>
            <g:render template="objectDocumentation" model="${[object:schema, name:'request', overview:overview, omitDescription:true]}"></g:render>


            <h4>Response</h4>

        </div>
    </div>



</body>
</html>