<%@ page import="grails.converters.JSON" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="api"/>
    <title>MERIT API - POST /ws/external/draft/getProjectDetails</title>
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
        <h3>Get project details</h3>
        <h4>Description</h4>
        <p>
            Returns the sites and activities that have been defined for a Project.
        </p>

        <h4>Authentication</h4>
        <p>
            In the short term requests will only be accepted from clients with an IP address appearing on a white list.
        </p>
        <h4>Method / URL</h4>
        GET /ws/external/draft/projectDetails

        <h4>Parameters</h4>
        <table class="table table-striped table-bordered">
            <thead>
            <tr>
                <th class="parameter-name">Name</th>
                <th class="parameter-mandatory">Mandatory</th>
                <th class="parameter-default">Default Value</th>
                <th class="parameter-description">Description</th>
            </tr>
            </thead>
            <tbody>
                <tr>
                    <td class="parameter-name">id</td>
                    <td class="parameter-mandatory">Yes</td>
                    <th class="parameter-default"></th>
                    <td class="parameter-description">The project's grant id, external id or guid, depending on the value supplied for the <i>type</i> parameter.</td>
                </tr>

                <tr>
                    <td class="parameter-name">type</td>
                    <td class="parameter-mandatory">No</td>
                    <th class="parameter-default">guid</th>
                    <td class="parameter-description">Specifies the type of the id field.  Must be one of 'grantId', 'externalId', 'guid'.</td>
                </tr>

            </tbody>
        </table>


        <h4>Response</h4>

        %{--<g:render template="objectDocumentation" model="${[object:schema, name:'request', overview:overview, omitDescription:true]}"></g:render>--}%

        <h4>Example</h4>
    </div>
</div>



</body>
</html>