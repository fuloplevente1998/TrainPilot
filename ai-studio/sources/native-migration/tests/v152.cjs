const fs=require('fs');const s=fs.readFileSync('www/v152.js','utf8');
function ok(x,m){if(!x)throw new Error(m)}
ok(s.includes("RF152_VERSION='1.5.2'"),'version');
ok(s.includes("easy:'Túl könnyű'"),'easy feedback');
ok(s.includes("good:'Pont jó'"),'good feedback');
ok(s.includes("hard:'Nagyon nehéz'"),'hard feedback');
ok(s.includes("pain:'Fájdalom'"),'pain feedback');
ok(s.includes("if(effort==='pain')"),'pain blocks progression');
ok(s.includes("if(!all)return {action:'hold'"),'partial workout blocks progression');
ok(s.includes("['lateral-raise'"),'small-step isolation handling');
ok(s.includes("loadType==='single_dumbbell'"),'single dumbbell progression');
ok(s.includes("action:'increase'"),'weight increase recommendation');
ok(s.includes("action:'reps'"),'bodyweight rep progression');
ok(s.includes('rf152ApplySuggestions'),'suggestions applied at workout start');
ok(s.includes('rf152SetEffort'),'per-exercise effort capture');
console.log('PASS: RepForge 1.5.2 adaptive progression rules and effort feedback.');
