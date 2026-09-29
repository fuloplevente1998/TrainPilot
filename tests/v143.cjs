const fs=require('fs');
const src=fs.readFileSync('www/v143.js','utf8');
for(const s of ["const RF143_VERSION='1.4.3'","RF143_NEW_EXERCISES=[","'incline-pushup'","'db-front-squat'","'single-arm-db-press'","'close-grip-bench'","function discardDraft143()","generatePersonalProgram=function(input)","DEMOS[e.id]=[null,'search'"])if(!src.includes(s))throw Error('Missing '+s);
const m=src.match(/\{id:'/g)||[];if(m.length!==20)throw Error('Expected exactly 20 new exercise definitions, got '+m.length);
if(src.includes('const RF143_DEMOS='))throw Error('Unverified old video IDs must not ship');
console.log('PASS: RepForge 1.4.3 draft deletion, 20 exercise additions, generator integration and online demo mappings.');
