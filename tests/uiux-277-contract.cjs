'use strict';
// Low-cost source contract, independent of Chromium availability.
const assert=require('node:assert/strict'),fs=require('node:fs');
const html=fs.readFileSync('www/index.html','utf8'),
      css=fs.readFileSync('www/uiux-277.css','utf8'),
      settings=fs.readFileSync('www/uiux-277-settings.js','utf8');
assert.ok(html.includes('<script defer src="app.js"></script><script defer src="uiux-277-settings.js"></script>'),
  'Settings enhancement must run after canonical app.js');
assert.ok(html.includes('tp277ProgramIcon(tp277ProgramKind(p,name))'),'Program icon derived from program metadata');
assert.ok(html.includes('tp277DayOverview(p,d)'),'Workout preview derived from existing day renderer');
for(const token of ['tp277-settings-heading','tp152-settings-backup','tp152-settings-profile','rf212-language',
  'tp197-mode-panel','tp105-data','cloudStatus']){
 assert.ok(settings.includes(token),token+' must remain available for grouping');
}
assert.ok(settings.includes('const previous=settingsScreen;'),'Preserve the canonical Settings renderer');
assert.ok(settings.includes('previous.apply(this,arguments)'),'Do not replace functional Settings content');
assert.ok(settings.includes('note.append(label,text)'),'Native Wear info is only explanatory');
assert.doesNotMatch(settings,/\b(?:render|go|startWorkout|syncNow|syncHomeNow)\s*=\s*function/,'No navigation, workout or sync ownership override');
assert.doesNotMatch(settings,/connected\s*[:=]\s*true|paired\s*[:=]\s*true/,'No fabricated watch status');
assert.ok(css.includes('main.tp277-settings'),'Settings styling limited to existing screen');
assert.ok(css.includes('min-height:44px!important;'),'Control touch targets remain usable');
assert.ok(!css.includes('main.tp277-settings .tp154-nav-cell'),'Keep the nav untouched');
new Function(settings);
console.log('PASS PR277 Program, Workout and Settings UI contracts');
