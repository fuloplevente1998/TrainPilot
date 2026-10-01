const fs=require('node:fs'),assert=require('node:assert/strict');
const app=fs.readFileSync('www/app.js','utf8');
assert.ok(app.includes("'progress.title':['Progresszív cél','Progressive goal','Progressives Ziel','Obiectiv progresiv']"),'progressive goal title translations missing');
assert.ok(app.includes("'progress.goalNote':['Az edzéselőzményeid alapján számolt következő fejlődési lépcső."),'progressive goal explanation missing');
assert.ok(app.includes("tp149T('progress.goalNote')"),'progressive goal explanation is not rendered in active workout');
assert.ok(app.includes("'settings.progression':['Progresszív cél • tényleges súlylépcsők'"),'progression settings wording not updated');
assert.ok(app.includes("rf233CoachPlan=function"),'Coach engine unexpectedly missing');
assert.ok(app.includes("rf220Readiness()"),'Coach readiness input unexpectedly missing');
console.log('PASS: progressive goal is distinct from the existing Coach recommendation.');
