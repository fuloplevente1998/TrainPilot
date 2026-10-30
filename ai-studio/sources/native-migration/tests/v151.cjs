const fs=require('fs');const s=fs.readFileSync('www/v151.js','utf8');
function ok(x,m){if(!x)throw new Error(m)}
ok(s.includes("RF151_VERSION='1.5.1'"),'version');
ok(s.includes('rf151AddExercise'),'exercise add');
ok(s.includes('rf151RemoveExercise'),'exercise remove');
ok(s.includes('rf151AddSet'),'set add');
ok(s.includes('rf151RemoveSet'),'set remove');
ok(s.includes('rf151SetValue'),'set value editing');
ok(s.includes('rf151ToggleDone'),'set completion editing');
ok(s.includes('rf151SaveHistory'),'history save');
ok(s.includes('rf142ValidWindow'),'time validation preserved');
ok(s.includes('rf142ReadHealthForWorkout'),'Health Connect refresh preserved');
ok(s.includes("db.set('history',h)"),'history persistence');
console.log('PASS: RepForge 1.5.1 full workout history editor.');
