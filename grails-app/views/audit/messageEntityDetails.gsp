<div>
    <table class="table table-condensed table-striped">
        <g:each in="${auditMessage?.entity}" var="kvp">
            <tr>
                <td>${kvp.key}</td>
                <td>${kvp.value}</td>
            </tr>
        </g:each>
    </table>
</div>

<script type="text/javascript">
    <g:set var="entityType" value="${auditMessage?.entityType?.substring(auditMessage?.entityType?.lastIndexOf('.') + 1)}" />
    setModalTitle("${entityType} Details");
</script>