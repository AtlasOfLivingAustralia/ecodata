modules = {
    application {
        dependsOn 'jquery'
        resource url:'js/application.js'
        resource url: 'css/ecodata.css'
    }

    app_bootstrap {
        dependsOn 'bootstrap'
        resource url: 'images/glyphicons-halflings-white.png'
        resource url: 'images/glyphicons-halflings.png'
    }

    app_bootstrap_responsive {
        dependsOn 'app_bootstrap'
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

    vkbeautify {
        dependsOn 'jquery'
        resource url: 'js/vkbeautify.0.99.00.beta.js'
    }
}