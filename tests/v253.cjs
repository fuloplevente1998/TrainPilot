const fs=require('fs'),assert=require('node:assert/strict');
const s=fs.readFileSync('www/v253.js','utf8');
for(const marker of ["RF253_VERSION='2.5.3'",'RF253_PRIMARY_ACTIONS','RF253_EMPHASIS_ACTIONS','RF253_CONTROL_ACTIONS','RF253_DANGER_ACTIONS','rf253SemanticRole','rf252ButtonRole=rf253SemanticRole','dataset.tpRole','rf253RenderCore','rf253AfterRender','rf253GoCore','RF253_CORE_ROUTES','TrainPilotNavigate',"makeBackup=function(){return {...rf253Backup(),appVersion:RF253_VERSION}}"]){assert.ok(s.includes(marker),'Missing '+marker);}
assert.ok(!/buttonText|Minden támogatott Health-adat szinkronizálása|Testsúly napló és grafikon/.test(s),'role classification must not depend on visible labels');
assert.ok(s.includes("if(name==='rf151SaveHistory')"));
assert.ok(s.includes("if(name==='saveHistoryWorkoutTime')"));
assert.ok(s.includes("typeof rf251RenderBase==='function'?rf251RenderBase"),'2.5.3 must bypass the nested 2.5.1/2.5.2 render wrappers through the verified pre-2.5.1 renderer');
assert.ok(s.includes('render=function(custom){const r=rf253RenderCore(custom);rf253AfterRender(custom);return r}'),'2.5.3 must have one final render wrapper');
assert.ok(s.includes("typeof rf225Go==='function'?rf225Go"),'2.5.3 must use the verified pre-Health navigation core');
assert.ok(s.includes("if(typeof rf245CoachVisible!=='undefined')rf245CoachVisible=false"),'Coach visibility reset must survive router consolidation');
assert.ok(s.includes("if(target==='health'&&typeof rf225HealthPage!=='undefined')rf225HealthPage='hub'"),'Health hub reset must survive router consolidation');
const prepPath='../scripts/prepare-build-253.py';
const prep=fs.existsSync(prepPath)?fs.readFileSync(prepPath,'utf8'):'';
if(prep)assert.ok(prep.includes('versionCode 2503')&&prep.includes('versionName "2.5.3"'));
console.log('PASS 2.5.3 semantic actions, consolidated routes and render hooks.');
