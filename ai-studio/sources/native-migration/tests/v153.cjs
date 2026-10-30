const fs=require('fs');
const s=fs.readFileSync('www/v153.js','utf8');
function ok(x,m){if(!x)throw new Error(m)}
ok(s.includes("id==='dead-bug'" )&&s.includes("return 'crunch'"),'dead bug must migrate to crunch');
ok(s.includes('rf153Move'),'program reorder missing');
ok(s.includes('rf153Remove'),'program remove missing');
ok(s.includes('rf153Add'),'program add missing');
ok(s.includes('changePlan'),'program replace missing');
ok(s.includes('changeExerciseSets'),'set editing missing');
ok(s.includes("RF153_VERSION='1.5.3'"),'version missing');
console.log('v1.5.3 program editor checks passed');
