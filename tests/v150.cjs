const fs=require('fs');const s=fs.readFileSync('www/v150.js','utf8');
function ok(x,m){if(!x)throw new Error(m)}
ok(s.includes("RF150_VERSION='1.5.0'"),'version');
ok(s.includes("'db-curl'"),'A biceps');ok(s.includes("'oh-triceps'"),'B triceps');
ok(s.includes("'home-basic','home-level2'"),'home migration');ok(s.includes('rf146Order'),'equipment ordering');
ok(s.includes('7 gyakorlatos'),'7 exercise rationale');
console.log('PASS: RepForge 1.5 A/B full-body foundation, arm split and equipment ordering.');
