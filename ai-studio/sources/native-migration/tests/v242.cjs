const fs=require('fs'),assert=require('assert');
const s=fs.readFileSync('www/v242.js','utf8');
for(const x of ["RF242_VERSION='2.4.2'","RF242_TOLERANCE_MS=3*60*1000","rf242BestSession","matchMode:'exercise-session'","matchMode:rf242HasTrainingMetric(padded)?'tolerance':'empty'","sourceWindowStart","sourceWindowEnd","rf240SyncWorkout=async function"])assert.ok(s.includes(x),'missing '+x);
assert.ok(s.includes('p.readTrainingWindow({start:h.started,end:h.finished})'),'exact-first query missing');
assert.ok(s.includes('Date.parse(h.started)-RF242_TOLERANCE_MS'),'padded fallback missing');
assert.ok(s.includes('p.readTrainingWindow({start:session.start,end:session.end})'),'session re-read missing');
console.log('PASS: TrainPilot 2.4.2 tolerant workout Health matching checks.');
