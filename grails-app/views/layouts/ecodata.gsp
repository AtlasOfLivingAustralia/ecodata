<!DOCTYPE html>
<html lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="app.version" content="${g.meta(name:'app.version')}"/>
    <meta name="app.build" content="${g.meta(name:'app.build')}"/>
    <meta name="description" content="Atlas of Living Australia"/>
    <meta name="author" content="Atlas of Living Australia">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title><g:layoutTitle /></title>

    <asset:stylesheet src="ecodata.css"/>
    <g:layoutHead />
</head>
<body class="${pageProperty(name:'body.class')}" id="${pageProperty(name:'body.id')}" onload="${pageProperty(name:'body.onload')}">

%{--<hf:banner logoutUrl="${grailsApplication.config.grails.serverURL}/logout/logout"/>--}%
  <div id="fixed-footer-wrapper">
    <div class="navbar navbar-fixed-top">
        <div class="navbar-inner">

            <div class="container-fluid">
                <a class="brand">Ecodata</a>
                <div class="nav-collapse collapse">
                    <div class="navbar-text pull-right">
                        <span id="buttonBar">
                            <ec:currentUserDisplayName />&nbsp;<hf:loginLogout cssClass="btn btn-small" logoutUrl="${createLink(controller:'logout', action:'logout')}"/>
                            %{--<button class="btn btn-small btn-info" id="btnProfile"><i class="icon-user icon-white"></i>&nbsp;My Profile</button>--}%
                            <button class="btn btn-warning btn-small" id="btnAdministration"><i class="icon-cog icon-white"></i>&nbsp;Administration</button>
                            <g:pageProperty name="page.buttonBar"/>
                        </span>
                    </div>
                </div><!--/.nav-collapse -->
            </div>
        </div>
    </div>
    <g:layoutBody />
    <div class="push"></div>
  </div>
  %{--<hf:footer/>--}%
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
<asset:javascript src="application.js"/>
<asset:deferredScripts/>

</body>
</html>