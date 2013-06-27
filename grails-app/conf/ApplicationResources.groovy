modules = {
    application {
        resource url:'js/application.js'
    }

    app_bootstrap {
        dependsOn 'jquery'
        resource url: '/bootstrap/js/bootstrap.min.js'
        resource url: '/bootstrap/css/bootstrap.min.css'
        resource url: '/bootstrap/img/glyphicons-halflings-white.png'
        resource url: '/bootstrap/img/glyphicons-halflings.png'
    }

    app_bootstrap_responsive {
        dependsOn 'app_bootstrap'
        resource url: '/bootstrap/css/bootstrap-responsive.min.css'
    }
}