const fs=require('fs');
const s=fs.readFileSync('www/v146.js','utf8');
function ok(x,m){if(!x)throw new Error(m)}
ok(s.includes("const RF146_VERSION='1.4.6'"),'version missing');
ok(s.includes("'db-curl'"),'A-day biceps curl missing');
ok(s.includes("'oh-triceps'"),'B-day overhead triceps missing');
ok(s.includes("ids.length<7"),'seven-exercise generator cap missing');
ok(s.includes("['home-basic','home-level2','home-varied']"),'all built-in home programs must be reordered');
ok(s.includes('rf146Order'),'equipment ordering helper missing');
ok(s.includes('homeEquipmentOrder146'),'home ordering migration flag missing');
console.log('v1.4.6 arm split and home equipment ordering checks passed');
