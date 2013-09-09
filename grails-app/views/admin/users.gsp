<%@ page import="au.org.ala.ecodata.AccessLevel; au.org.ala.ecodata.Project; org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
    <head>
        <meta name="layout" content="adminLayout"/>
        <title>Users - Admin - Data capture - Atlas of Living Australia</title>
    </head>

    <body>
    <r:require module="bootstrap_combo"/>
    <script type="text/javascript">

        $(document).ready(function() {

            $("#addPermissionsButton").click(function(e) {
                e.preventDefault();
                if ($('#userId').val() && $('#role').val() && $('#projectId').val()) {
                    $("#spinner").show();
                    $.ajax( {
                        url: "${createLink(controller: 'permissions', action: 'addUserAsRoleToProject')}",
                        data: {userId: $("#userId").val(), role: $("#role").val(), projectId: $("#projectId").val() }
                    }).done(function(result) { alert(result); })
                    .fail(function(jqXHR, textStatus, errorThrown) { alert(jqXHR.responseText); })
                    .always(function(result) { $("#spinner").hide(); });
                } else {
                    alert("All fields are required - please check and try again.");
                }

            });

            var namesArray = [];
            <g:each var="it" in="${userNamesList}" status="s">namesArray[${s}] = "${it.userId} -- ${it.displayName?.toLowerCase()} -- ${it.userName?.toLowerCase()}";</g:each>

//            $('#userName').typeahead({
//                source: namesArray,
//                minLength: 2,
//                items: 15,
//                updater: function(item) {
//                    //console.log("email", item.split(" -- ")[1]);
//                    var userId = item.split(" -- ")[0];
//                    $("#userId").val(userId);
//                    return item.split(" -- ")[1];
//                }
//            });

            $(".combobox").combobox();

            $('.tooltips').tooltip();
        });
    </script>
        <content tag="pageTitle">Users</content>
        <div class="hero-unit">Logged in user is <ec:currentUserDisplayName />.</div>
        <div class="hide">
            Search for user: <input type="text" data-provide="typeahead" autocomplete="off" id="userName" placeholder="type a name or email address">
        </div>
        <div>
            <h4>Add Permissions</h4>
            <form class="form-horizontal">
                <div class="control-group">
                    <label class="control-label" for="userId">User</label>
                    <div class="controls">
                        <g:select name="user" id="userId" class="input-xlarge combobox" from="${userNamesList}" optionValue="${{it.displayName + " <" + it.userName +">"}}" optionKey="userId" noSelection="['':'']"/>
                    </div>
                </div>
                <div class="control-group">
                    <label class="control-label" for="role">Permission level</label>
                    <div class="controls">
                        <g:select name="role" id="role" from="${AccessLevel}" value="editor" />
                    </div>
                </div>
                <div class="control-group">
                    <label class="control-label" for="projectId">Project</label>
                    <div class="controls">
                        <g:select name="project" id="projectId" class="input-xlarge combobox" from="${Project.list()}" optionValue="name" optionKey="projectId" noSelection="['':'']" />
                    </div>
                </div>
                <div class="control-group">
                    <div class="controls">
                        <button id="addPermissionsButton" class="btn btn-primary">Submit</button>
                        <g:img dir="images" file="spinner.gif" id="spinner" class="hide"/>
                    </div>
                </div>
            </form>

        </div>
        <div>
            <g:each var="it" in="${userNamesMap}">
                key = ${it.key} || val = ${it.value}<br/>
            </g:each>
        </div>
    </body>
</html>