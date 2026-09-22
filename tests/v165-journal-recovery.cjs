const assert=require('node:assert/strict'),fs=require('node:fs');
const section='www/trainpilot-165-journal-data-recovery.js';
const app=fs.readFileSync(fs.existsSync(section)?section:'www/app.js','utf8');
for(const marker of [
 'tp165SafeHistoryRecord',
 'tp165DeleteBrokenHistory',
 'legacyRows:true',
 'partialRows:true',
 'rowIsolation:true'
])assert.ok(app.includes(marker),'missing 1.6.5 Journal recovery marker: '+marker);
assert.match(app,/Array\.isArray\(source\.exercises\)/);
assert.match(app,/Array\.isArray\(e\.sets\)/);
assert.match(app,/try\{return itemBase\.call/);
console.log('PASS TrainPilot 1.6.5 legacy/partial Journal row isolation guards.');
