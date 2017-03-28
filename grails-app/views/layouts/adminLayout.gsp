<!DOCTYPE html>
<html lang="en">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="app.version" content="${g.meta(name: 'app.version')}"/>
    <meta name="app.build" content="${g.meta(name: 'app.build')}"/>
    <meta name="description" content="Atlas of Living Australia"/>
    <meta name="author" content="Atlas of Living Australia">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title><g:layoutTitle/></title>
    <style type="text/css">

    .icon-chevron-right {
        float: right;
        margin-top: 2px;
        margin-right: -6px;
        opacity: .25;
    }

    /* Pagination fix */
    .pagination .disabled, .pagination .currentStep, .pagination .step {
        float: left;
        padding: 0 14px;
        border-right: 1px solid;
        line-height: 34px;
        border-right-color: rgba(0, 0, 0, 0.15);
    }

    .pagination .prevLink {
        border-right: 1px solid #DDD !important;
        line-height: 34px;
        vertical-align: middle;
        padding: 0 14px;
        float: left;
    }

    .pagination .nextLink {
        vertical-align: middle;
        line-height: 34px;
        padding: 0 14px;
    }

    </style>
    <asset:stylesheet src="ecodata.css"/>
    <g:layoutHead/>

</head>

<body>
<div id="fixed-footer-wrapper">
    <div class="navbar navbar-fixed-top">
        <div class="navbar-inner">

            <div class="container-fluid">
                <a class="brand">Ecodata</a>

                <div class="nav-collapse collapse">
                    <div class="navbar-text pull-right">
                        <span id="buttonBar">
                            <ec:currentUserDisplayName/>&nbsp;<hf:loginLogout cssClass="btn btn-small"
                                                                              logoutUrl="${createLink(controller: 'logout', action: 'logout')}"/>
                            %{--<button class="btn btn-small btn-info" id="btnProfile"><i class="icon-user icon-white"></i>&nbsp;My Profile</button>--}%
                            <button class="btn btn-warning btn-small" id="btnAdministration"><i
                                    class="icon-cog icon-white"></i>&nbsp;Administration</button>
                            <g:pageProperty name="page.buttonBar"/>
                        </span>
                    </div>
                </div><!--/.nav-collapse -->
            </div>
        </div>
    </div>

    <div class="container-fluid">
        <legend>
            <table style="width: 100%">
                <tr>
                    <td><g:link class="discreet" controller="home" action="index">Home</g:link><fc:navSeparator/><g:link
                            class="discreet" action="index">Administration</g:link><fc:navSeparator/><g:pageProperty
                            name="page.pageTitle"/></td>

                </tr>
            </table>
        </legend>

        <div class="row-fluid">
            <div class="span3">
                <ul class="nav nav-list nav-stacked nav-tabs">
                    %{--<ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'users')}" title="Users" />--}%
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'tools')}" title="Tools"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'settings')}" title="Settings"/>
                    %{--<ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'metadata')}" title="Metadata" />--}%
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'audit')}" title="Audit"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'metadata')}"
                                       title="Raw activity model"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'activityModel')}"
                                       title="Activity model"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'rawOutputModels')}"
                                       title="Raw output models"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'programsModel')}"
                                       title="Programs model"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'searchScores')}"
                                       title="Scores list"/>

                </ul>

                <div style="text-align: center; margin-top: 30px;"><g:pageProperty name="page.adminButtonBar"/></div>
            </div>

            <div class="span9">
                <g:if test="${flash.errorMessage}">
                    <div class="container-fluid">
                        <div class="alert alert-error">
                            ${flash.errorMessage}
                        </div>
                    </div>
                </g:if>

                <g:if test="${flash.message}">
                    <div class="container-fluid">
                        <div class="alert alert-info">
                            ${flash.message}
                        </div>
                    </div>
                </g:if>

                <g:layoutBody/>

            </div>
        </div>
    </div>
</div>

<asset:javascript src="asset.js"/>

<script type="text/javascript">
    var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
    document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
<asset:script>
    var pageTracker = _gat._getTracker("UA-4355440-1");
    pageTracker._initData();
    pageTracker._trackPageview();

    // show warning if using IE6
    if ($.browser.msie && $.browser.version.slice(0,1) == '6') {
        $('#header').prepend($('<div style="text-align:center;color:red;">WARNING: This page is not compatible with IE6.' +
' Many functions will still work but layout and image transparency will be disrupted.</div>'));
    }
</asset:script>
<asset:script type="text/javascript">

    $(document).ready(function (e) {

        $.ajaxSetup({ cache: false });

        $("#btnLogout").click(function (e) {
            window.location = "${createLink(controller: 'logout', action:'index')}";
        });

        $("#btnAdministration").click(function (e) {
            window.location = "${createLink(controller: 'admin')}";
        });

        $("#btnProfile").click(function (e) {
            window.location = "${createLink(controller: 'userProfile')}";
        });

    });

</asset:script>

<!-- JS resources-->
<asset:javascript src="admin.js"/>
<asset:deferredScripts/>

</body>
</html>
