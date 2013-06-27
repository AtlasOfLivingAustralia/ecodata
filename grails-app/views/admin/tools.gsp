<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
    <head>
        <meta name="layout" content="adminLayout"/>
        <title>Tools - Admin - Data capture - Atlas of Living Australia</title>
    </head>

    <body>
        <script type="text/javascript">

            $(document).ready(function() {

                $("#btnReloadConfig").click(function(e) {
                    e.preventDefault();
                    $.ajax("${createLink(controller: 'admin', action:'reloadConfig')}").done(function(result) {
                        document.location.reload();
                    });
                });

                $("#btnClearMetadataCache").click(function(e) {
                    e.preventDefault();
                    $.ajax("${createLink(controller: 'admin', action:'clearMetadataCache')}").done(function(result) {
                        document.location.reload();
                    }).fail(function (result) {
                        alert(result);
                    });
                });

                $("#btnReloadDB").click(function(e) {
                    e.preventDefault();
                    $.ajax("${createLink(controller: 'admin', action:'load', params: [drop:true])}").done(function(result) {
                        document.location.reload();
                    }).fail(function (result) {
                        alert(result);
                    });
                });

                $("#btnDumpDB").click(function(e) {
                    e.preventDefault();
                    $.ajax("${createLink(controller: 'admin', action:'dump')}").done(function(result) {
                        document.location.reload();
                    }).fail(function (result) {
                        alert(result);
                    });
                });

            });

        </script>
        <content tag="pageTitle">Tools</content>
        <table class="table table-bordered table-striped">
            <thead>
                <tr>
                    <th>Tool</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>
                        <button id="btnReloadDB" class="btn btn-small btn-info">Reload&nbsp;Test Data</button>
                    </td>
                    <td>
                        Reloads test data into the database. Loads projects, sites, activities and outputs from the files
                        in the configured dump directory.
                    </td>
                </tr>
                <tr>
                    <td>
                        <button id="btnDumpDB" class="btn btn-small btn-info">Dump&nbsp;Database</button>
                    </td>
                    <td>
                        Dumps the projects, sites, activities and outputs collections as JSON to files
                        in the configured dump directory.
                    </td>
                </tr>
                <tr>
                    <td>
                        <button id="btnClearMetadataCache" class="btn btn-small btn-info">Clear&nbsp;Metadata&nbsp;Cache</button>
                    </td>
                    <td>
                        Removes all cached values for metadata requests and causes the metadata to be requested
                        from the source at the next attempt to use the metadata.
                    </td>
                </tr>
                <tr>
                    <td>
                        <button disabled id="btnReloadConfig" class="btn btn-small btn-info" title="External config not set up yet">Reload&nbsp;External&nbsp;Config</button>
                    </td>
                    <td>
                        Reads any defined config files and merges new config with old. Usually used after a change is
                        made to external config files. Note that this cannot remove a config item as the result is a
                        union of the old and new config.
                    </td>
                </tr>

            </tbody>
        </table>
    </body>
</html>