const fs=require('fs'),assert=require('node:assert/strict'); const s=fs.readFileSync('www/v228a.js','utf8'); assert.ok(s.includes('TrainPilot')); console.log('2.2.10 smoke test passed');
