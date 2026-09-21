const fs=require('fs');
const s=fs.readFileSync('www/v234.js','utf8');
function ok(x,m){if(!x)throw new Error(m)}
ok(s.includes("RF234_VERSION='2.3.4'"),'version');
ok(s.includes('readWellness'),'native wellness read');
ok(s.includes('READ_STEPS'),'steps permission UI');
ok(s.includes('READ_WEIGHT'),'weight permission UI');
ok(s.includes('READ_BODY_FAT'),'body fat permission UI');
ok(s.includes('READ_OXYGEN_SATURATION'),'oxygen permission UI');
ok(s.includes('READ_VO2_MAX'),'vo2 permission UI');
ok(s.includes('wellnessLatest'),'local wellness cache');
ok(s.includes('rf233SignalHtml'),'Coach context');
ok(!s.includes('rf220Readiness=function'),'wellness must not rewrite readiness scoring');
ok(s.includes('appVersion:RF234_VERSION'),'backup version');
const j=fs.readFileSync('android/app/src/main/java/com/repforge/app/HealthBridgePlugin.java','utf8');
for(const x of ['READ_STEPS','READ_WEIGHT','READ_BODY_FAT','READ_OXYGEN_SATURATION','READ_VO2_MAX','readWellness','StepsRecord.STEPS_COUNT_TOTAL','WeightRecord','BodyFatRecord','OxygenSaturationRecord','Vo2MaxRecord'])ok(j.includes(x),'missing '+x);
console.log('PASS: TrainPilot 2.3.4 Health Connect wellness checks.');
