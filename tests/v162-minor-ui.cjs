const assert=require('node:assert/strict'),fs=require('node:fs');
const baseFile=fs.existsSync('www/trainpilot-162-minor-ui.js')?'www/trainpilot-162-minor-ui.js':'www/app.js';
const followFile=fs.existsSync('www/trainpilot-162-followup.js')?'www/trainpilot-162-followup.js':null;
const base=fs.readFileSync(baseFile,'utf8'),followSource=followFile?fs.readFileSync(followFile,'utf8'):'';
const app=base+(followSource?'\n'+followSource:'');
const pkg=require('../package.json'),src=require('../SOURCE_VERSION.json'),gradle=fs.readFileSync('android/app/build.gradle','utf8');
assert.equal(pkg.version,'1.7.7');assert.equal(src.version,'1.7.7');assert.equal(src.versionCode,2669);
assert.match(gradle,/versionCode\s+2669/);assert.match(gradle,/versionName\s+"1\.7\.7"/);

// Keep the 1.6.3 stale-session guard unchanged.
for(const marker of ["window.tp162VisibleHealthSessions","String(s?.source||'')!==ownPackage","clientRecordId.indexOf('trainpilot:tpw_')===0","stableIds.has(clientRecordId)","exerciseSessionCount:visible.length","exerciseMinutes:minutes"])assert.ok(app.includes(marker),'missing Health-count guard: '+marker);

// Calendar geometry is a protected 1.6.3 baseline.
for(const marker of [
 '[data-panel="calendar"] .tp155-r4-calendar-frame{padding:7px!important}',
 '[data-panel="calendar"] .calendar-head{min-height:40px!important;margin-bottom:6px!important;padding-right:50px!important;grid-template-columns:46px 1fr 46px!important;gap:6px!important}',
 '[data-panel="calendar"] .cal-weekdays{gap:2px!important}',
 '[data-panel="calendar"] .calendar-grid{gap:2px!important}',
 '[data-panel="calendar"] .cal-cell{min-height:42px!important;padding:3px!important;border-radius:10px!important}',
 '[data-panel="calendar"] .tp155-r4-planner-bottom{margin-top:7px!important}',
 '[data-panel="calendar"] .tp155-planner-always-open{padding:7px!important}'
])assert.ok(app.includes(marker),'calendar regression marker changed: '+marker);

const follow=followSource||app.slice(app.indexOf('// @section trainpilot-162-followup.js'),app.indexOf('// @endsection trainpilot-162-followup.js'));
assert.ok(follow.length>500,'1.6.3 follow-up section missing');
assert.ok(app.includes("host?.dataset?.panel==='exercises'"),'Programs parent-state guard must remain in the 1.6.3 base patch');
for(const marker of [
 'position:sticky!important;top:8px!important',
 '[data-panel="coach"] .tp151-coach-recommendation{margin-top:10px!important}',
 "host?.dataset?.panel==='quick'",
 'Workout landing card keeps a single Quick Workout title',
 'body.tp-home-view main.rf221-home{min-height:0!important;height:auto!important;max-height:none!important',
 "document.querySelectorAll('.tp-brand-strip').forEach(function(el){el.remove()})",
 "main.rf263-health",
 "RepForge → Google Naptár','TrainPilot → Google Naptár",
 'tp162-theme-columns',
 'grid-template-columns:repeat(2,minmax(0,1fr))!important',
 'position:absolute!important',
 'overflow-y:auto!important',
 'tp162-theme-dropdown',
 'ontoggle="tp162ThemeToggle(this)"',
 'tp162CloseThemeDropdown',
 'inlineTwoColumnThemePicker:true',
 'floatingScrollableThemePicker:true',
 "lang==='hu'?'Élénk színek'",
 'window.TrainPilot162Followup='
])assert.ok(follow.includes(marker),'missing 1.6.3 follow-up marker: '+marker);
assert.doesNotMatch(follow,/\.tp155-r4-panel-close\{[^}]*position:absolute!important/,'follow-up close control must not use absolute positioning');
assert.ok(follow.includes('calendarGeometryUntouched:true'),'calendar geometry protection flag missing');
console.log('PASS TrainPilot 1.7.7 release metadata + 1.6.3 regression guards');
