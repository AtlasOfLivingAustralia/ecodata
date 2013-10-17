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

    bootstrap_combo {
        dependsOn 'app_bootstrap'
        resource url: '/js/bootstrap-combobox.js'
        resource url: '/css/bootstrap-combobox.css'
    }

    bootbox {
        dependsOn 'app_bootstrap'
        resource url: 'js/bootbox.min.js'
    }

    app_bootstrap_responsive {
        dependsOn 'app_bootstrap'
        resource url: '/bootstrap/css/bootstrap-responsive.min.css'
    }

    vkbeautify {
        dependsOn 'jquery'
        resource url: 'js/vkbeautify.0.99.00.beta.js'
    }
}