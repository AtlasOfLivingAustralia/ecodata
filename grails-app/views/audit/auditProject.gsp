<!doctype html>
<html>
	<head>
		<meta name="layout" content="adminLayout"/>
		<title>Admin - Data capture - Atlas of Living Australia</title>
		<style type="text/css" media="screen">
		</style>
	</head>
	<body>
        <content tag="pageTitle">Audit</content>
        <div style="margin-bottom: 10px">
            <a href="${createLink(controller: 'admin', action:'audit')}" class="btn">
                <i class="icon-chevron-left"></i>
                Back to Audit Home
            </a>
        </div>
        <h4>Audit Trail for ${projectInstance.name}</h4>
        <g:render template="auditMessageTable" />
    </body>
</html>

<r:script>
</r:script>
