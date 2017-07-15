var report = db.report.find({reportId:'0bb1908a-1ebf-402c-82d1-891ec3ecfc43'}).next();

if (report.data.evidenceFor1_1) {
    print(report.data.evidenceFor1_1.indexOf('\u00A0'));
    report.data.evidenceFor1_1 = report.data.evidenceFor1_1.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor1_2) {
    report.data.evidenceFor1_2 = report.data.evidenceFor1_2.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor1_3) {
    report.data.evidenceFor1_3 = report.data.evidenceFor1_3.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor1_4) {
    report.data.evidenceFor1_4 = report.data.evidenceFor1_4.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor1_5) {
    report.data.evidenceFor1_5 = report.data.evidenceFor1_5.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor1_6) {
    report.data.evidenceFor1_6 = report.data.evidenceFor1_6.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor2_1) {
    report.data.evidenceFor2_1 = report.data.evidenceFor2_1.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor2_2) {
    report.data.evidenceFor2_2 = report.data.evidenceFor2_2.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor2_3) {
    report.data.evidenceFor2_3 = report.data.evidenceFor2_3.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor2_4) {
    report.data.evidenceFor2_4 = report.data.evidenceFor2_4.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor3_1) {
    report.data.evidenceFor3_1 = report.data.evidenceFor3_1.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor3_2) {
    report.data.evidenceFor3_2 = report.data.evidenceFor3_2.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor3_3) {
    report.data.evidenceFor3_3 = report.data.evidenceFor3_3.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor3_4) {
    report.data.evidenceFor3_4 = report.data.evidenceFor3_4.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor3_5) {
    report.data.evidenceFor3_5 = report.data.evidenceFor3_5.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor3_6) {
    report.data.evidenceFor3_6 = report.data.evidenceFor3_6.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor4_1) {
    report.data.evidenceFor4_1 = report.data.evidenceFor4_1.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor4_2) {
    report.data.evidenceFor4_2 = report.data.evidenceFor4_2.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor4_3) {
    report.data.evidenceFor4_3 = report.data.evidenceFor4_3.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor4_4) {
    report.data.evidenceFor4_4 = report.data.evidenceFor4_4.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor4_5) {
    report.data.evidenceFor4_5 = report.data.evidenceFor4_5.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor5_1) {
    report.data.evidenceFor5_1 = report.data.evidenceFor5_1.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor5_2) {
    report.data.evidenceFor5_2 = report.data.evidenceFor5_2.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor5_3) {
    report.data.evidenceFor5_3 = report.data.evidenceFor5_3.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}
if (report.data.evidenceFor5_4) {
    report.data.evidenceFor5_4 = report.data.evidenceFor5_4.replace(/[\u00A0\u1680​\u180e\u2000-\u2009\u200a​\u200b​\u202f\u205f​\u3000]/g, ' ');
}

db.report.save(report);
