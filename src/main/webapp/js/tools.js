import $ from 'jquery';

$(document).ready(function() {
    $("#btnReloadConfig").click(function(e) {
        e.preventDefault();
        console.log(ecodataConfig);
        $.ajax(ecodataConfig.reloadConfigUrl).done(function(result) {
            document.location.reload();
        });
    });

    $("#btnClearMetadataCache").click(function(e) {
        e.preventDefault();
        $.ajax(ecodataConfig.clearMetaDataCacheUrl).done(function(result) {
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

    $("#btnReIndexAll").click(function(e) {
        e.preventDefault();
        $.ajax("${createLink(controller: 'search', action:'indexAll')}").done(function(result) {
            document.location.reload();
        }).fail(function (result) {
            alert(result);
        });
    });

    $("#btnRegenerateRecords").click(function(e) {
        e.preventDefault();
        var outputId = $('#outputId').val();
        if (outputId) {
            $.ajax("${createLink(controller: 'admin', action:'regenerateRecordsForOutput')}?outputId="+outputId).done(function(result) {
                document.location.reload();
            }).fail(function (result) {
                alert(result);
            });
        }
        else {
            alert("Please enter an output ID");
        }
    });

    $("#btnUpdateCollectoryForBiocollectProjects").click(function(e) {
        e.preventDefault();
        $.ajax("${createLink(controller: 'admin', action:'updateCollectoryEntryForBiocollectProjects')}").done(function(result) {
            document.location.reload();
        }).fail(function (result) {
            alert(result);
        });
    });

    $("#btnBuildGeoServerComponents").click(function(e) {
        e.preventDefault();
        $.ajax("${createLink(controller: 'admin', action:'buildGeoServerDependencies')}").done(function(result) {
            document.location.reload();
        }).fail(function (resp) {
            var result = JSON.parse(resp.responseText)
            alert(result.message);
        });
    });
});