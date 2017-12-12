<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>Edit Score</title>
</head>
<body>

<h3>Scores</h3>
    <a href="${createLink(action:'createScore')}" class="btn btn-success">New Score</a>

<h4>Existing Scores</h4>
    <table class="table table-striped">
        <thead>
        <tr>
            <th></th>
            <th>Category</th>
            <th>Label</th>
            <th>Output target?</th>
        </tr>
        </thead>
        <tbody>
            <g:each in="${scores}" var="score">
                <tr>
                    <td><a href="${createLink(action:'editScore', id:score.scoreId)}">Edit</a></td>
                    <td>${score.category}</td>
                    <td>${score.label}</td>
                    <td>${score.isOutputTarget ? 'Yes' : 'No'}</td>
                </tr>
            </g:each>

        </tbody>
    </table>

</body>
<script type="text/javascript">

</script>
</html>