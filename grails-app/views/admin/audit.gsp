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
        <a class="btn" href="${createLink(controller:'admin', action:'auditMessagesByEntity')}">Messages by Entity</a>
        <a class="btn" href="${createLink(controller:'admin', action:'auditMessagesByProject')}">Messages by Project</a>
    </body>
</html>
