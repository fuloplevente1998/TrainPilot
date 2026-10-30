const fs=require('fs');
const s=fs.readFileSync('www/v206.js','utf8');
for(const x of ["RF206_VERSION='2.0.6'","rf206HealthHub","rf206EstimatedKcal","Gyors választás","Elvégzett edzések","Legutóbbi testsúly","Health Connect","Egészség"])if(!s.includes(x))throw Error('Missing '+x);
if(!s.includes("go('history')")&&!s.includes("go(\\'history\\')"))throw Error('History navigation missing');
console.log('PASS: RepForge 2.0.6 home cleanup and health dashboard.');
