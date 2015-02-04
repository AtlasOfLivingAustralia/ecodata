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

    <r:require modules="application, app_bootstrap, app_bootstrap_responsive"/>
    <r:layoutResources/>
    <g:layoutHead />
</head>
<body class="${pageProperty(name:'body.class')}" id="${pageProperty(name:'body.id')}" onload="${pageProperty(name:'body.onload')}">

%{--<hf:banner logoutUrl="${grailsApplication.config.grails.serverURL}/logout/logout"/>--}%
  <div id="fixed-footer-wrapper">
    <div class="navbar navbar-fixed-top">
        <div class="navbar-inner">

            <div class="container-fluid">
                <a class="brand">Draft MERIT API documentation</a>


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
<r:script>
    var pageTracker = _gat._getTracker("UA-4355440-1");
    pageTracker._initData();
    pageTracker._trackPageview();

    // show warning if using IE6
    if ($.browser.msie && $.browser.version.slice(0,1) == '6') {
        $('#header').prepend($('<div style="text-align:center;color:red;">WARNING: This page is not compatible with IE6.' +
                ' Many functions will still work but layout and image transparency will be disrupted.</div>'));
    }
</r:script>

<!-- JS resources-->
<r:layoutResources/>

</body>
</html>