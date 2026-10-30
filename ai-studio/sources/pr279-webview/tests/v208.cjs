const fs=require('fs');
const s=fs.readFileSync('www/v208.js','utf8');
for(const x of ["RF208_VERSION='2.0.8'","Gyors elérés","rf206HealthHub()","Egészség","rf208-tabs","min-height:40px","Napló"])if(!s.includes(x))throw Error('Missing '+x);
console.log('PASS: RepForge 2.0.8 navigation and quick access cleanup.');
