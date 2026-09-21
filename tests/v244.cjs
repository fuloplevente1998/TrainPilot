const fs=require('fs'),assert=require('assert');
const s=fs.readFileSync('www/v244.js','utf8');
for(const x of ["RF244_VERSION='2.4.4'",'rf244UnifiedSync','Összes Health adat frissítése','Alvásengedély','rf206HealthHub=rf244HealthHub','rf241HealthDetails=function'])assert.ok(s.includes(x),'missing '+x);
assert.equal((s.match(/onclick="rf244UnifiedSync\(\)"/g)||[]).length,1,'Health hub should have one primary sync action');
assert.ok(!s.includes('onclick="rf241RefreshWorkoutHealth') ,'details must not contain a second workout Health refresh button');
console.log('PASS: TrainPilot 2.4.4 simplified Health UI checks.');