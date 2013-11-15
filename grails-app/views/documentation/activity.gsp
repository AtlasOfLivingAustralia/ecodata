
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="ecodata"/>
    <title><g:message code="api.output.page.title" args="${[name]}"/></title>
</head>

<body>

    <div class="container-fluid" style="position: relative; top:40px">

        <h4><g:message code="api.activity.heading" args="${[name]}"/></h4>
        <g:render template="objectProperties" model="${[object:activity, name:name,  descriptionKeyPrefix:"api.property"]}"/>
    </div>
</body>
</html>