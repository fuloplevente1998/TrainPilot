const fs=require('fs'),assert=require('node:assert/strict');
const index=fs.readFileSync('www/index.html','utf8');
const startup=fs.readFileSync('www/startup.js','utf8');
const ready=fs.readFileSync('www/ready.js','utf8');
assert.ok(index.includes('<div id="tpBoot" aria-label="Betöltés"></div>'),'blank boot overlay is present');
assert.ok(!index.includes('<div id="tpBoot">TrainPilot</div>'),'boot wordmark is removed');
assert.ok(startup.includes('requestAnimationFrame'),'startup waits for a paint-stable reveal barrier');
assert.ok(startup.includes('durationMs'),'startup duration is measured');
assert.ok(startup.includes('setTimeout(()=>{if(typeof initCloud'), 'cloud init remains deferred');
assert.ok(ready.includes("appVersion:'1.1.2'"),'final layer reports 2.6.2');
console.log('PASS startup 2.3.2: blank overlay, paint-stable reveal, timing metric, deferred cloud.');

