const fs=require('node:fs'),assert=require('node:assert/strict');
const index=fs.readFileSync('www/index.html','utf8');
const js=fs.readFileSync('www/health-dashboard-168.js','utf8');
const css=fs.readFileSync('www/health-dashboard-168.css','utf8');
assert.match(index,/health-dashboard-168\.css/,'Health 1.6.8 stylesheet must be loaded');
assert.match(index,/health-dashboard-168\.js/,'Health 1.6.8 runtime must be loaded');
for(const x of ["version:'1.6.8'",'tp168-health','tp168-today-card','tp168-pulse-panel','tp168-connect-panel','TrainPilot168Health'])assert.ok(js.includes(x),'missing '+x);
for(const x of ['tp168-metrics','tp168-status-grid','tp168-health-note','tp168-health-view'])assert.ok(css.includes(x),'missing CSS '+x);
console.log('PASS TrainPilot 1.6.8 Health dashboard assets');
