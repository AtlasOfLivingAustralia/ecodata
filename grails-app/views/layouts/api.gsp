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
    <link href="${grailsApplication.config.headerAndFooter.baseURL}/css/bootstrap.min.css" rel="stylesheet" media="screen,print"/>
    <link href="${grailsApplication.config.headerAndFooter.baseURL}/css/bootstrap-theme.min.css" rel="stylesheet" media="screen,print"/>
    <asset:stylesheet src="ecodata.css"/>
    <g:layoutHead />
</head>
<body class="${pageProperty(name:'body.class')}" id="${pageProperty(name:'body.id')}" onload="${pageProperty(name:'body.onload')}">

%{--<hf:banner logoutUrl="${grailsApplication.config.grails.serverURL}/logout/logout"/>--}%
  <div id="fixed-footer-wrapper">
    <div class="navbar navbar-default navbar-fixed-top">
        %{--<div class="navbar-inner">--}%

            <div class="container-fluid">
                <p class="navbar-text brand">Draft MERIT API documentation</p>


            </div>
        %{--</div>--}%
    </div>

    <g:layoutBody />
    <div class="push"></div>
  </div>
  %{--<hf:footer/>--}%

<asset:javascript src="application"/>
<asset:deferredScripts/>


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