<!DOCTYPE html>
<html lang="en">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="app.version" content="${g.meta(name: 'info.app.version')}"/>
    <meta name="app.commit.id" content="${ec.gitProperty(name: 'git.commit.id')}"/>
    <meta name="app.build.time" content="${ec.buildProperty(name: 'build.time')}"/>
    <meta name="description" content="Atlas of Living Australia"/>
    <meta name="author" content="Atlas of Living Australia">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title><g:layoutTitle/></title>

    <asset:stylesheet src="ecodata.css"/>
    <g:layoutHead/>

</head>

<body>
<div id="fixed-footer-wrapper">
    <div class="navbar navbar-expand-lg navbar-light bg-light">
            <div class="container-fluid">
                <p class="nav-item navbar-brand" style="margin-top: 20px">Ecodata</p>
                <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#collapsibleHeaderContent" aria-controls="collapsibleHeaderContent" aria-expanded="false" aria-label="Toggle navigation">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="collapsibleHeaderContent">
                    <div class="d-flex flex-grow-1">
                        <span id="buttonBar" class="ms-auto">
                            <ec:currentUserDisplayName/>&nbsp;<button id="btnLogout" class='nav-item btn btn-info'>Logout</button>
                            <button class="btn btn-warning nav-item" id="btnAdministration"><i class="fa fa-cog"></i>&nbsp;Administration</button>
                            <g:pageProperty name="page.buttonBar"/>
                        </span>
                    </div>
                </div><!--/.nav-collapse -->
            </div>
       %{-- </div>--}%
    </div>

    <div class="container-fluid">
        <div class="row">
            <div class="col-md-3">
                <ul class="nav flex-column nav-stacked nav-pills">

                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'tools')}" title="Tools"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'settings')}" title="Settings"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'audit')}" title="Audit"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'metadata')}"
                                       title="Raw activity model"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'editActivityFormDefinitions')}"
                                       title="Edit activity form definitions"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'editActivityFormTemplates')}"
                                       title="Edit activity form templates"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'programsModel')}"
                                       title="Programs model"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'searchScores')}"
                                       title="Scores list"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'getIndexNames')}"
                                       title="List custom indices"/>
                    <ec:breadcrumbItem href="${createLink(controller: 'graphql', action: 'browser')}"
                                       title="Graphql API"/>

                </ul>

                <div style="text-align: center; margin-top: 30px;"><g:pageProperty name="page.adminButtonBar"/></div>
            </div>

            <div class="col-md-9">
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

<asset:javascript src="bundle-vendor"/>
<asset:javascript src="bundle-runtime"/>
<asset:javascript src="admin"/>
<asset:javascript src="bundle-score"/>
<asset:deferredScripts/>

<asset:script type="text/javascript">

    $(document).ready(function (e) {

        $.ajaxSetup({ cache: false });

        $("#btnLogout").click(function (e) {
            window.location = "${createLink(controller: 'admin', action:'logout')}";
        });

        $("#btnAdministration").click(function (e) {
            window.location = "${createLink(controller: 'admin')}";
        });

        $("#btnProfile").click(function (e) {
            window.location = "${createLink(controller: 'userProfile')}";
        });

    });

</asset:script>

<!-- Google Analytics -->
<script>
    (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
            m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
    })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

    ga('create', 'UA-4355440-1', 'auto');
    ga('send', 'pageview');
</script>
<!-- End Google Analytics -->

</body>
</html>
