<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="ecodata"/>
    <title>Outputs</title>
</head>


<body>
<div class="container-fluid">
<h1>Request</h1>
    <h2>HTTP request</h2>

        PUT http://ecodata.ala.org.au/activity/v1

    <h2>Parameters</h2>


    <h2>Request body</h2>


    <g:each in="${outputs}" var="output">
        <g:render template="outputDocumentation" model="[name:output.key, output:output.value]"></g:render>
    </g:each>


    <h1>Response</h1>
</div>

</body>
</html>
