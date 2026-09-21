const fs=require('fs');
const s=fs.readFileSync('www/v235.js','utf8');
function ok(x,m){if(!x)throw new Error(m)}
ok(s.includes("RF235_VERSION='2.3.5'"),'version');
ok(s.includes('rf233CoachPlan()'),'Coach plan used');
ok(s.includes('rf233CoachScreen()'),'Coach opens from Health');
ok(s.includes('rf206HealthHub=function'),'Health hub wrapper');
ok(s.includes('Coach használja'),'Coach usage label');
ok(s.includes('Aktivitási kontextus'),'activity-context label');
ok(s.includes('Tájékoztató'),'informational label');
ok(s.includes('Trendadat'),'trend label');
ok(s.includes('rf235DecorateRecovery'),'recovery metric labels');
ok(s.includes('rf235DecorateStats'),'Health stat labels');
ok(!s.includes('rf220Readiness=function'),'2.3.5 must not rewrite readiness scoring');
ok(s.includes('appVersion:RF235_VERSION'),'backup version');
console.log('PASS: TrainPilot 2.3.5 Health Coach access and metric-role labels.');
