const fs=require('fs'),assert=require('assert');
const s=fs.readFileSync('www/v243.js','utf8');
for(const x of ["RF243_VERSION='2.4.3'","com.sec.android.app.shealth':'Samsung Health'","rf243FriendlySource","rf241SourceText=function","rf243EnergyState","totalCompact","activeMissing","totalMeaning","rf241CompactHealth=function","rf241HealthDetails=function"])assert.ok(s.includes(x),'missing '+x);
assert.ok(s.includes('e.active==null&&e.total!=null'),'total-energy compact fallback missing');
assert.ok(s.includes('TrainPilot nem becsüli át aktív kcal-ra'),'must not infer active kcal from total energy');
assert.ok(!s.includes('activeCalories=e.total'),'must not synthesize active calories');
console.log('PASS: TrainPilot 2.4.3 friendly sources and workout-energy semantics checks.');