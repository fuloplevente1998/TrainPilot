const fs=require('node:fs'),assert=require('node:assert/strict');
const issuePath='www/trainpilot-167-issues22-24-gui-journal.js';
const full=fs.existsSync('www/app.js')?fs.readFileSync('www/app.js','utf8'):'';
const issue=fs.existsSync(issuePath)?fs.readFileSync(issuePath,'utf8'):(()=>{const a=full.indexOf('// @section trainpilot-167-issues22-24-gui-journal.js'),b=full.indexOf('// @endsection trainpilot-167-issues22-24-gui-journal.js',a);return a>=0&&b>a?full.slice(a,b):''})();
const app=issue;
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
assert.ok(issue.length>100,'1.6.7 compatibility section missing');
assert.doesNotMatch(issue,/new Date\(0\)/,'1.6.7 must not introduce epoch fallback');
assert.match(issue,/tp167-home-coach-readiness/);
assert.doesNotMatch(issue,/targetSub|targetHtml/,'#22 compact Home Coach must not render duplicated target/program metadata');
console.log('PASS TrainPilot 1.6.7 issues #22 #23 #24 static guards.');
