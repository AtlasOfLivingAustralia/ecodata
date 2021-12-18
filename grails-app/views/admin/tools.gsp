<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<!doctype html>
<html>
    <head>
        <meta name="layout" content="adminLayout"/>
        <title>Tools - Admin - Data capture - Atlas of Living Australia</title>
        <script>
            window.ecodataConfig = {
                reloadConfigUrl:"${createLink(controller: 'admin', action:'reloadConfig')}",
                clearMetaDataCacheUrl:"${createLink(controller: 'admin', action:'clearMetadataCache')}"
            }
        </script>
    </head>

    <body>

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
                        <button id="btnReloadConfig" class="btn btn-small btn-info" title="Reloads external config">Reload&nbsp;External&nbsp;Config</button>
                    </td>
                    <td>
                        Reads any defined config files and merges new config with old. Usually used after a change is
                        made to external config files. Note that this cannot remove a config item as the result is a
                        union of the old and new config.
                    </td>
                </tr>
                <tr>
                    <td>
                        <button id="btnReIndexAll" class="btn btn-small btn-info" title="External config not set up yet">Reindex&nbsp;All&nbsp;(ElasticSearch)</button>
                    </td>
                    <td>
                        Re-index all data. This action re-creates the ElasticSearch index.
                    </td>
                </tr>
            <tr>
                <td>
                   <button id="btnRegenerateRecords" class="btn btn-small btn-info" title="Regenerates records for a specified output">Regenerate&nbsp;Records</button>
                </td>
                <td>

                    Invokes the OutputService.createOrUpdateRecordsForOutput method for the supplied output
                    <p>
                    Output id: <input type="text" id="outputId" name="outputId" class="input-xxxlarge">
                    </p>
                </td>
            </tr>
            <tr>
                <td>
                    <button id="btnUpdateCollectoryForBiocollectProjects" class="btn btn-small btn-info" title="Forcefully update information in Collectory of internal Biocollect projects.">Update Collectory</button>
                </td>
                <td>
                    Forcefully update information in Collectory of internal Biocollect projects. Note: This does not create a new entry since it assumes an entry exists in Collectory.
                </td>
            </tr>
            <tr>
                <td>
                    <button id="btnBuildGeoServerComponents" class="btn btn-small btn-info" title="Clear GeoServer components.">Built GeoServer</button>
                </td>
                <td>
                    Delete existing layers, store and workspace associates with Ecodata and create new ones.
                </td>
            </tr>
            <tr>
                <td>
                    <button id="btnMigrateUserDetailsToEcodata" class="btn btn-small btn-info" title="Migrate UserDetails to Ecodata.">Migrate UserDetails</button>
                </td>
                <td>
                    Migrate the existing MERIT users from UserDetails into the Eccodata Database
                </td>
            </tr>
            </tbody>
        </table>
    <script defer="defer" src="/assets/bundle-runtime.js"></script>
    <script defer="defer" src="/assets/bundle-vendor.js"></script>
    <script defer="defer" src="/assets/bundle-tools.js"></script>
    </body>
</html>