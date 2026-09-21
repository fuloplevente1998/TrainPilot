const assert=require('node:assert/strict'),fs=require('node:fs');
const section='www/trainpilot-150-compact-ui-performance.js';
const app=fs.existsSync(section)?fs.readFileSync(section,'utf8'):fs.readFileSync('www/app.js','utf8');
for(const marker of [
 'tp150-programs-frame','tp150-program-tools','tp150-active-program','tp150-journal-compact',
 "title:'Gyors edzés'","cardHelp:'Choose an exercise and start right away.'","title:'Schnelltraining'","title:'Antrenament rapid'",
 'tp150PreparedTranslate','TP150_I18N_SORTED_KEYS','body.tp-home-view main.rf221-home',
 'tp149HomeCoachRepaintRenderBase'
])assert.ok(app.includes(marker),'missing compact UI/performance marker: '+marker);
const historyFinal=app.slice(app.lastIndexOf('historyScreen=function()'));
assert.ok(!/<div class="hero"><h1>/.test(historyFinal),'final compact Journal must not render the old explanatory hero');
console.log('PASS TrainPilot 1.5.0 compact Programs/Workout/Journal UI, Quick Workout naming, prepared i18n and Home Coach scroll guards');
