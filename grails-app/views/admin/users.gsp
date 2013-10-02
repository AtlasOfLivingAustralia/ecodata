<%@ page import="au.org.ala.ecodata.AccessLevel; au.org.ala.ecodata.Project; org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
<head>
    <meta name="layout" content="adminLayout"/>
    <title>Users - Admin - Data capture - Atlas of Living Australia</title>
</head>

<body>
    <r:require modules="bootstrap_combo,bootbox"/>
    <content tag="pageTitle">Users</content>
    <div class="hero-unit">
        Logged in user is <ec:currentUserDisplayName />
        <div class="pull-right">
            <g:if test="${ec.userInRole(role: grailsApplication.config.security.cas.adminRole)}">
                <button class="btn btn-danger clearPermissions" id="clearAllPermissions">Clear Permissions for ALL USERS</button>
            </g:if>
            <br/>
            <button class="btn btn-info clearPermissions" id="clearMyPermissions">Clear Permissions for <ec:currentUserDisplayName /></button>
        </div>
    </div>
    <div>
        <h4>Add Permissions</h4>
        <form class="form-horizontal">
            <div class="control-group">
                <label class="control-label" for="userId">User</label>
                <div class="controls">
                    <g:select name="user" id="userId" class="input-xlarge combobox" from="${userNamesList}" optionValue="${{it.displayName + " <" + it.userName +">"}}" optionKey="userId" noSelection="['':'start typing a user name']"/>
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="role">Permission level</label>
                <div class="controls">
                    <select name="role" id="role" >
                        <g:each var="l" in="${AccessLevel}">
                            <g:if test="${l != AccessLevel.starred}">
                                <option value="${l}">${l}</option>
                            </g:if>
                        </g:each>
                    </select>
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="projectId">Project</label>
                <div class="controls">
                    <g:select name="project" id="projectId" class="input-xlarge combobox" from="${Project.list()}" optionValue="name" optionKey="projectId" noSelection="['':'start typing a project name']" />
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

    <r:script>
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

            $(".combobox").combobox();

            $('.tooltips').tooltip();

            $('.clearPermissions').click(function(e) {
                var $this = this;
                var url, all_or_this;
                if ($($this).attr('id') == 'clearAllPermissions') {
                    url = "${createLink(controller: 'permissions', action: 'clearAllPermissionsForAllUsers')}"
                    all_or_this = "ALL users?";
                } else {
                     url = "${createLink(controller: 'permissions', action: 'clearAllPermissionsForUserId')}/${ec.currentUserId()}"
                    all_or_this = "this user?";
                }
                bootbox.confirm("Are you sure you want to clear all permission settings for "+ all_or_this, function(result) {
                    if (result) {
                        $.ajax({ url: url })
                        .done(function(data) {
                            alert(data);
                        })
                        .fail(function(jqXHR, textStatus, errorThrown) { alert(jqXHR.responseText); })
                        .always(function() { $("#spinnerRow").hide(); });
                    }
                });
            });
        });
    </r:script>
</body>
</html>