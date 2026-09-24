const assert=require('node:assert/strict'),fs=require('node:fs');
const index=fs.readFileSync('www/index.html','utf8'),pkg=require('../package.json'),src=require('../SOURCE_VERSION.json'),gradle=fs.readFileSync('android/app/build.gradle','utf8');
for(const marker of [
 "TP5_VERSION='phase5-health-27-r1'",
 "pulseSource:'healthLedgerV1.averageHeartRate'",
 "calendarDays:7",
 "zeroPulseAsMissing:true",
 "persistentWeightEdit:true",
 "ownWeightDialog:true",
 "window.tp5PulseTrendRows=function()",
 "window.tp5OpenWeightEditor=function(date)",
 "data-tp5-weight-edit",
 "data-tp2629-input"
])assert.ok(index.includes(marker),'missing Phase 5 marker: '+marker);
assert.match(index,/healthLedgerV1/);
assert.match(index,/averageHeartRate/);
assert.equal(pkg.version,'1.7.4');
assert.equal(src.version,'1.7.4');assert.equal(src.versionCode,2663);assert.equal(src.baselineCommit,'f064298ddbe1e7e4cf1ecdfca18b3d93899dc74a');
assert.match(gradle,/versionCode\s+2663/);assert.match(gradle,/versionName\s+"1\.7\.4"/);
console.log('PASS Phase 5 static: #27 pulse source/calendar window + persistent TrainPilot weight editor.');
