<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>Index names | Admin | Data capture | Atlas of Living Australia</title>
</head>

<body>
<content tag="pageTitle">Index names</content>

<div class="row-fluid">
    <div class="span-12">
        <table class="table">
            <thead>
            <tr>
                <th>Index name</th>
                <th>Data type</th>
                <th>Model name</th>
            </tr>
            </thead>
            <tbody>
            <g:each var="entry" in="${indexNames}">
                <g:each in="${entry.value}" var="detail" status="index">
                    <tr>

                        <g:if test="${index == 0}">
                            <td rowspan="${entry.value?.size()}">
                                ${entry.key}
                            </td>
                        </g:if>
                        <td>
                            ${detail.dataType}
                        </td>
                        <td>
                            ${detail.modelName}
                        </td>
                    </tr>
                </g:each>
            </g:each>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>