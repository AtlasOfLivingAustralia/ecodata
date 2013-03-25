<%--
  Created by IntelliJ IDEA.
  User: Mark
  Date: 14/03/13
  Time: 5:01 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Sites</title>
</head>
<body>
    <h3>${sites.size()} sites</h3>
    <ul>
    <g:each in="${sites}" var="s">
        <li>${s.name}: ${s.location?.decimalLatitude}</li>
    </g:each>
    </ul>
</body>
</html>