<table class="table table-bordered">
    <thead>
        <tr>
            <th>Project Name</th>
            <th>Organisation Name</th>
            <th></th>
        </tr>
    </thead>
    <g:each in="${projectList}" var="project">
        <tr projectId="${project.externalProjectId}">
            <td>
                ${project.name}
                <div class="muted">
                    <small>${project.projectId}</small>
                </div>
            </td>
            <td>${project.organisationName}</td>
            <td>
                <a href="${createLink(controller: 'audit', action:'auditProject', params:[projectId: project.projectId])}" class="btn btn-small btnAudit">Audit</a>
            </td>
        </tr>
    </g:each>
</table>
