const assert=require('node:assert/strict');
const fs=require('node:fs');
const ui=fs.readFileSync('www/trainpilot-110.js','utf8');
assert.match(ui,/const rf110PaintBase=rf245Paint/);
assert.match(ui,/if\(rf110AutoHealthFlight&&!rf250ActiveManual\)return/);
assert.match(ui,/return rf110PaintBase\(\)/);
assert.match(ui,/setTimeout\(\(\)=>rf110AutoHealthSync\(\{force:true\}\),1200\)/);
assert.match(ui,/setInterval\(\(\)=>rf110AutoHealthSync\(\),RF110_AUTO_HEALTH_MS\)/);
