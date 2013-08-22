<table class="table table-striped table-bordered">
    <thead>
        <tr>
            <th>Date</th>
            <th>Entity Type</th>
            <th>Entity ID</th>
            <th>User ID</th>
            <th>Event Type</th>
            <th>Project ID</th>
            <th></th>
        </tr>
    </thead>
    <tbody>
        <g:each in="${auditMessages}" var="message">
            <tr auditMessageId="${message.id}">
                <td><g:formatDate date="${message.date}" format="yyyy-MM-dd HH:mm:ss"/> </td>
                <td>${message.entityType?.substring(message.entityType?.lastIndexOf('.')+1)}</td>
                <td>${message.entityId}</td>
                <td>${message.userId}</td>
                <td>${message.eventType}</td>
                <td>${message.projectId}</td>
                <td>
                    <button class="btn btn-small btnViewEntity"><i class="icon-zoom-in"></i>&nbsp;Entity</button>
                </td>
            </tr>
        </g:each>
    </tbody>
</table>

<script type="text/javascript">

    $(".btnViewEntity").click(function(e) {
        e.preventDefault();
        var messageId = $(this).parents("tr[auditMessageId]").attr("auditMessageId");
        showModal({
            url: "${createLink(controller:'audit', action:'messageEntityDetails')}/" + messageId
        });
    });

</script>
