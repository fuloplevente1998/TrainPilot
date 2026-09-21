const fs=require('fs');
const s=fs.readFileSync('www/v233.js','utf8');
function ok(x,m){if(!x)throw new Error(m)}
ok(s.includes("RF233_VERSION='2.3.3'"),'version');
ok(s.includes('function rf233CoachPlan'),'coach plan');
ok(s.includes('function rf233TargetWorkout'),'target workout');
ok(s.includes('function rf233ExerciseAdvice'),'exercise advice');
ok(s.includes('rf152Recommendation(id)'),'adaptive progression input');
ok(s.includes('rf220Readiness()'),'readiness input');
ok(s.includes("mode=r.score<55?'light':r.score<75?'reduced':'normal'"),'three load modes');
ok(s.includes("effort==='pain'"),'pain blocks progression');
ok(s.includes('startScheduledById'),'today planned workout start');
ok(s.includes("go('calendar')"),'future workout calendar action');
ok(s.includes('rf220CoachScreen=rf233CoachScreen'),'coach override');
ok(s.includes('rf220BindHome=rf233BindHome'),'home coach override');
ok(s.includes('appVersion:RF233_VERSION'),'backup version');
console.log('PASS: TrainPilot 2.3.3 actionable Coach checks.');
