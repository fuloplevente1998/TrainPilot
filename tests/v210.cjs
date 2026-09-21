const fs=require('fs');
const s=fs.readFileSync('www/v210.js','utf8');
if(!s.includes("RF210_VERSION='2.1.0'"))throw Error('Missing 2.1.0 version marker');
const nav=fs.readFileSync('www/v208.js','utf8');
for(const x of ['grid-template-columns:repeat(3','rf208-settings-btn','Egészség','Gyors elérés'])if(!nav.includes(x))throw Error('Missing 2.1.0 design dependency '+x);
const cal=fs.readFileSync('www/v209.js','utf8');
for(const x of ['rf209DeleteSchedule','rf209NearestPlan'])if(!cal.includes(x))throw Error('Missing 2.0.9 calendar fix '+x);
console.log('PASS: 2.1.0 compact design + calendar fixes');
