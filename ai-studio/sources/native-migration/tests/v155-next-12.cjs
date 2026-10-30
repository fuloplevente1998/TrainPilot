const assert=require('node:assert/strict');
const fs=require('node:fs');
const app=fs.readdirSync('www').filter(name=>name.endsWith('.js')).map(name=>fs.readFileSync('www/'+name,'utf8')).join('\n');

for(const marker of [
 'tp155MarkScheduleCompleted','data-tp-move-id','tp155-journal-tabs',
 "data-panel=\"quick\"",'tp155QuickPanelHtml','tp155ExercisePanelHtml',
 'tp155-attached-select','tp155-side-manual','tp155OrderHomeCards'
])assert.ok(app.includes(marker),'missing 1.5.5 next-12 marker: '+marker);

assert.match(app,/rf260OpenScheduleMove=function\(id,button\)[\s\S]*?rf260OpenTemporal\(input,button\|\|document\.activeElement\)/,'Calendar move must open the existing temporal picker');
assert.match(app,/tp155MarkScheduleCompleted=function\(id\)[\s\S]*?status:'completed'[\s\S]*?db\.set\('scheduled',rows\)/,'manual Calendar completion must persist scheduled status');
assert.doesNotMatch(app.match(/tp155MarkScheduleCompleted=function\(id\)[\s\S]*?\n \};/)?.[0]||'',/unshift\(|push\(/,'manual completion must not create a Journal entry');
console.log('PASS: TrainPilot 1.5.5 next-12 structural regression guards.');
