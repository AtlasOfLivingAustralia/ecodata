<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="ecodata"/>
		<title>Ecodata | Atlas of Living Australia </title>
	</head>
	<body>
		<div class="container-fluid" style="margin-top:50px;">
			<h1>Ecodata webservices</h1>
            <g:if test="${flash.message || error}">
                <g:set var="error" value="${flash.message?:user?.error}"/>
                <div class="row-fluid">
                    <div class="alert alert-error large-space-before">
                        <button type="button" class="close" data-dismiss="alert">&times;</button>
                        <span>Error: ${error}</span>
                    </div>
                </div>
            </g:if>
            <p class="lead">Webservices supporting fieldcapture applications</p>
			<ul>
                <li><g:link controller="documentation">Draft MERIT API documentation</g:link></li>
			</ul>
		</div>
	</body>
</html>
