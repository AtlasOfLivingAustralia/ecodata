<%@ page import="au.org.ala.ecodata.AccessLevel; au.org.ala.ecodata.Project; org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
    <head>
        <meta name="layout" content="adminLayout"/>
        <title>Users - Admin - Data capture - Atlas of Living Australia</title>
    </head>

    <body>
    <script type="text/javascript">

        $(document).ready(function() {

            $("#addPermissionsButton").click(function(e) {
                e.preventDefault();
                $("#spinner").show();
                $.ajax( {
                    url: "${createLink(controller: 'permissions', action: 'addUserAsRoleToProject')}",
                    data: {userId: $("#userId").val(), role: $("#role").val(), projectId: $("#projectId").val() }
                })
                .done(function(result) { alert(result); })
                .fail(function(jqXHR, textStatus, errorThrown) { alert(jqXHR.responseText); })
                .always(function(result) { $("#spinner").hide(); });
            });

            var namesArray = [];
            <g:each var="it" in="${userNamesList}" status="s">namesArray[${s}] = "${it.userId} -- ${it.displayName?.toLowerCase()} -- ${it.userName?.toLowerCase()}";</g:each>

            $('#userName').typeahead({
                source: namesArray,
                minLength: 2,
                updater: function(item) {
                    //console.log("email", item.split(" -- ")[1]);
                    var userId = item.split(" -- ")[0];
                    $("#userId").val(userId);
                    return item.split(" -- ")[1];
                }
            });

            $('.tooltips').tooltip();
        });
    </script>
        <content tag="pageTitle">Users</content>
        <div class="hero-unit">Logged in user is <ec:currentUserDisplayName />.</div>
        <div>
            Search for user: <input type="text" data-provide="typeahead" autocomplete="off" id="userName" placeholder="type a name or email address">

        </div>
        <div>
            Add permissions for userId
            <input type="text" disabled="disabled" title="use search to set this" name="user" id="userId" value="" class="input-mini"/>
            %{--<g:select name="user" id="userId" from="${userNamesList}" optionValue="${{it.displayName + " <" + it.userName +">"}}" optionKey="userId" noSelection="['':'-- choose a user --']"/>--}%
            as
            <g:select name="role" id="role" from="${AccessLevel}" value="editor" />
            to project
            <g:select name="project" id="projectId" from="${Project.list()}" optionValue="name" optionKey="projectId" noSelection="['':'-- choose a project --']" />
            <br>
            <button id="addPermissionsButton" class="btn btn-primary">Submit</button>
            <g:img dir="images" file="spinner.gif" id="spinner" class="hide"/>
        </div>
        <div>
            <g:each var="it" in="${userNamesMap}">
                key = ${it.key} || val = ${it.value}<br/>
            </g:each>
        </div>
    </body>
</html>