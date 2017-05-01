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
        <div class="form-horizontal well well-small">
            <div class="control-group">
                <label class="control-label" for="entityId">Enter an Entity ID:</label>
                <div class="controls">
                    <g:textField class="input-xlarge" name="entityId" id="entityId"/>
                    <button class="btn btn-primary" id="btnViewAuditMessages">View</button>
                </div>
            </div>
        </div>
        <div id="auditMessageContainer">
        </div>
    </body>
</html>
<asset:script>

    $(document).ready(function() {
        $("#btnViewAuditMessages").click(function(e) {
            e.preventDefault();
            doSearch();
        });

        $("#entityId").keydown(function(e) {
            if (e.keyCode == 13) {
                doSearch();
            }
        });

    });

    function doSearch() {
        var entityId = $("#entityId").val();
        if (entityId) {
            $("#auditMessageContainer").html("");
            $.ajax("${createLink(controller:'audit', action:'entityAuditMessageTableFragment')}?entityId=" + entityId).done(function(body) {
                $("#auditMessageContainer").html(body);
            });
        }
    }

</asset:script>
