<g:applyLayout name="ecodata">
    <head>
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
    </head>

    <body>
    <div class="container-fluid">
        <legend>
            <table style="width: 100%">
                <tr>
                    <td><g:link class="discreet" controller="home" action="index">Home</g:link><fc:navSeparator/><g:link class="discreet" action="index">Administration</g:link><fc:navSeparator/><g:pageProperty name="page.pageTitle"/></td>
                    <td style="text-align: right"><span><g:pageProperty name="page.adminButtonBar"/></span></td>
                </tr>
            </table>
        </legend>

        <div class="row-fluid">
            <div class="span3">
                <ul class="nav nav-list nav-stacked nav-tabs">
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'users')}" title="Users" />
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'tools')}" title="Tools" />
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'settings')}" title="Settings" />
                    <ec:breadcrumbItem href="${createLink(controller: 'admin', action: 'metadata')}" title="Metadata" />
                </ul>
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
    </body>
</g:applyLayout>