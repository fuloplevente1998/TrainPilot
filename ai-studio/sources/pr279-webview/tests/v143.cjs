const fs=require('fs');
const src=fs.readFileSync('www/v143.js','utf8');
for(const s of ["const RF143_VERSION='1.4.3'","RF143_NEW_EXERCISES=[","'incline-pushup'","'db-front-squat'","'single-arm-db-press'","'close-grip-bench'","function discardDraft143()","generatePersonalProgram=function(input)","'NASM'"])if(!src.includes(s))throw Error('Missing '+s);
const m=src.match(/\{id:'/g)||[];if(m.length!==20)throw Error('Expected exactly 20 new exercise definitions, got '+m.length);
const vids=(src.match(/':'[A-Za-z0-9_-]{8,}'/g)||[]);if(vids.length<20)throw Error('Expected 20 video mappings');
console.log('PASS: RepForge 1.4.3 draft deletion, 20 exercise additions, generator integration and online demo mappings.');
