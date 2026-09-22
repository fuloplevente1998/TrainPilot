const fs=require('node:fs'),assert=require('node:assert/strict');
const files=['www/trainpilot-167-issues22-24-gui-journal.js','www/trainpilot-166-issue13-home-coach-card.js','www/trainpilot-165-journal-data-recovery.js'];
const app=files.some(fs.existsSync)?files.filter(fs.existsSync).map(x=>fs.readFileSync(x,'utf8')).join('\n'):fs.readFileSync('www/app.js','utf8');
for(const marker of [
 'TrainPilot167Issues2224',
 'duplicateProgramRemoved:true',
 'healthCoachRemoved:true',
 'epochFallbackRemoved:true',
 'recoverRealDate:true',
 'missingDateLabel:true',
 "rf235HealthCoachCard=function(){return ''}",
 'tp167RecoverHistoryDate',
 'Dátum nem elérhető'
])assert.ok(app.includes(marker),'missing 1.6.7 marker: '+marker);
const section=app.slice(app.indexOf('trainpilot-167-issues22-24-gui-journal.js'));
assert.doesNotMatch(section,/new Date\(0\)/,'1.6.7 must not introduce epoch fallback');
assert.match(section,/tp167-home-coach-readiness/);
assert.doesNotMatch(section,/targetSub|targetHtml/,'#22 compact Home Coach must not render duplicated target/program metadata');
console.log('PASS TrainPilot 1.6.7 issues #22 #23 #24 static guards.');
