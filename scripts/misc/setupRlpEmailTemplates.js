var rlpProgram = db.program.find({name: /Regional Landcare/});
if (rlpProgram.hasNext()) {
    var rlp2 = rlpProgram.next();
    rlp2.config.emailTemplates = {
        "reportSubmittedEmailTemplate":'RLP_REPORT_SUBMITTED_EMAIL_TEMPLATE',
        'reportApprovedEmailTemplate':'RLP_REPORT_APPROVED_EMAIL_TEMPLATE',
        'reportReturnedEmailTemplate':'RLP_REPORT_RETURNED_EMAIL_TEMPLATE',
        'planSubmittedEmailTemplate':'RLP_PLAN_SUBMITTED_EMAIL_TEMPLATE',
        'planApprovedEmailTemplate':'RLP_PLAN_APPROVED_EMAIL_TEMPLATE',
        'planReturnedEmailTemplate':'RLP_PLAN_RETURNED_EMAIL_TEMPLATE'
    }
}
db.program.save(rlp2);