const fs=require('fs');
const s=fs.readFileSync('www/v207.js','utf8');
for(const x of ["RF207_VERSION='2.0.7'","rf207ReadTodayHealth","readWorkout","activeCalories","averageHeartRate","maxHeartRate","heartRateSamples","Mai aktivitás • Health Connect","Alvás és HRV frissítése"])if(!s.includes(x))throw Error('Missing '+x);
if(!s.includes("sourceLabels")||!s.includes("dailySource"))throw Error('Health source selection missing');
console.log('PASS: RepForge 2.0.7 daily Health Connect dashboard.');
