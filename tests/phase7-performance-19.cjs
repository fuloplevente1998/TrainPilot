const assert=require('node:assert/strict'),fs=require('node:fs');
const index=fs.readFileSync('www/index.html','utf8'),pkg=require('../package.json'),src=require('../SOURCE_VERSION.json'),gradle=fs.readFileSync('android/app/build.gradle','utf8');
for(const marker of ["phase7-performance-19-r4","historySnapshot:'operation-scoped'","persistentCache:false","lazyPrograms:true","batchedProgressStats:true","deferredCoachProgress:true","tp3EnsureHistoryBody","tp7EnsureProgramBody",'data-tp7-coach-progress="pending"'])assert.ok(index.includes(marker),'missing Phase 7 marker '+marker);
assert.equal(pkg.version,'1.7.7');assert.equal(src.version,'1.7.7');assert.equal(src.versionCode,2666);assert.equal(src.baselineCommit,'d0a3881ed9d8ec9d1238a914b6e6cab5da1e7c3e');
assert.match(gradle,/versionCode\s+2666/);assert.match(gradle,/versionName\s+"1\.7\.7"/);
console.log('PASS Phase 7 #19 performance architecture + 1.7.7 metadata guards');
