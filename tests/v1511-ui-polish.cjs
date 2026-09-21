const assert=require('node:assert/strict'),fs=require('node:fs');
const section=fs.existsSync('www/trainpilot-1511-ui-polish.js')?'www/trainpilot-1511-ui-polish.js':'www/app.js';
const app=fs.readFileSync(section,'utf8');
const pkg=require('../package.json'),src=require('../SOURCE_VERSION.json');
const gradle=fs.readFileSync('android/app/build.gradle','utf8');

for(const marker of [
 "const TP1511_VERSION='1.5.1'",
 "daily:'Napi aktív kalória'",
 "tp1511-training",
 "programRest",
 "classicBlue",
 "graphite",
 "TP1511_FLAGS",
 "tp1511-time-pair",
 "rf260TemporalPaintTime=function",
 ".tp-library-card .tp-play-btn",
 ".tp146-exercise .tp-play-btn",
 ".tp-library-card>summary::after",
 ".tp146-exercise-chevron",
 ".tp151-nav-dock .tp151-nav-item.active"
])assert.ok(app.includes(marker),'missing 1.5.1 UI marker: '+marker);

assert.ok(app.includes("TP1511_BASIC=['classicBlue','classicGreen','classicRed','classicOrange','classicPurple','graphite']"),'basic color palette missing');
assert.ok(app.includes("TP1511_VIVID=['yellow','blue','green','purple','orange','cyan','teal','pink','indigo','lime','red','magenta','lavender','sky','mint','coral']"),'vivid color palette missing');
assert.ok(app.includes("rf212LanguagePanel=function(){const cur=rf212LangSetting()"),'compact language panel missing');
assert.ok(app.includes("settingsScreen=function(){const backup="),'final compact settings screen missing');
assert.ok(!app.slice(app.indexOf("settingsScreen=function(){const backup="),app.indexOf("var tp1511TimeChange")).includes('plannerPanel()'),'Settings must not contain workout calendar shortcut');
assert.ok(!app.slice(app.indexOf("settingsScreen=function(){const backup="),app.indexOf("var tp1511TimeChange")).includes('id="rest"'),'Settings must not contain rest-time field');
assert.ok(app.includes("tp1511Plan=planScreen;planScreen=function()"),'Workout settings must be injected on Workout page');
assert.ok(app.includes("color:var(--text)!important"),'selected navigation text contrast guard missing');
assert.ok(app.includes("font-size:21px!important"),'larger navigation icons missing');
assert.ok(app.includes("font-size:11px!important"),'larger navigation labels missing');
assert.ok(app.includes("<select class=\"field\" onchange=\"tp1511TimeChange"),'compact hour/minute selects missing');

assert.equal(pkg.version,'1.6.2');
assert.equal(src.version,'1.6.2');
assert.equal(src.versionCode,2651);
assert.match(gradle,/versionCode\s+2651/);
assert.match(gradle,/versionName\s+"1\.6\.2"/);

console.log('PASS TrainPilot 1.5.1 unified UI polish guards');
