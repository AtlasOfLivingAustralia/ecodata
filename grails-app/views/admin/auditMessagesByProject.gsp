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
            <a href="${createLink(controller: 'admin', action:'audit')}" class="btn btn-outline-dark">
                <i class="fa fa-chevron-left"></i>
                Back to Audit Home
            </a>
        </div>
        <form class="well well-small">
            <div>
                <label class="control-label" for="projectName">Find a project:</label>
                <div class="form-row">
                    <g:textField class="form-control" name="projectName" id="projectName"/>
                    <button class="btn btn-primary" id="btnFindProject">Find</button>
                </div>
            </div>
        </form>
        <div id="projectList">
        </div>
    </body>
</html>
<asset:script>

    $(document).ready(function() {
        $("#btnFindProject").click(function(e) {
            e.preventDefault();
            doSearch();
        });

        $("#projectName").keydown(function(e) {
            if (e.keyCode == 13) {
                doSearch();
            }
        });
    });

    function doSearch() {
        var q = $("#projectName").val();
        if (q) {
            $("#projectList").html("");
            $.ajax("${createLink(controller:'audit', action:'findProjectResultsTableFragment')}?q=" + q).done(function(body) {
                $("#projectList").html(body);
            });
        }
    }

</asset:script>
