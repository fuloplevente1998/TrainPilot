const fs=require('node:fs'),assert=require('node:assert/strict');
const index=fs.readFileSync('www/index.html','utf8');
const css=fs.readFileSync('www/health-dashboard-168.css','utf8');
assert.match(index,/health-dashboard-168\.css/,'Health stylesheet must be loaded');
for(const x of ["version:'1.6.9'",'tp168-health','tp168-today-card','tp168-pulse-panel','tp168-connect-panel','tp168-weight-journal','tp168ToggleWeight','positive(d.restingHeartRate)','TrainPilot168Health'])assert.ok(index.includes(x),'missing Health 1.6.9 runtime marker '+x);
assert.ok(index.includes('DOMContentLoaded'),'Health runtime must initialize after deferred app.js');
assert.ok(!/healthHeader\(main\)/.test(index),'legacy standalone Health heading must not be rendered');
for(const x of ['data-tp-theme-family="vivid"','tp168-weight-summary','tp168-weight-body','tp168-today-date','tp-library-card[open]>summary::after'])assert.ok(css.includes(x),'missing Health 1.6.9 CSS '+x);
console.log('PASS TrainPilot 1.6.9 themed Health + inline weight journal guards');
