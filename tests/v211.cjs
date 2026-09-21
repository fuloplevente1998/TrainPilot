const fs=require('fs');
const s=fs.readFileSync('www/v211.js','utf8');
for(const x of ["RF211_VERSION='2.1.1'","TrainPilot","rf211BrandHtml","document.title='TrainPilot'","rf208-settings-btn","color-mix"])if(!s.includes(x))throw Error('Missing '+x);
console.log('PASS: TrainPilot 2.1.1 branding and settings icon visibility.');
