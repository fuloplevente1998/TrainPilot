const assert=require('node:assert/strict'),fs=require('node:fs');
const s=fs.readFileSync('www/trainpilot-151-unified-design.js','utf8');
for(const x of [
 'tp151-nav-dock','tp151-top-actions','rf233CoachScreen()',
 'tp151QuickConfig','tp151QuickTarget','targetReps',
 'tp151CalendarDayPanel','tp151ScheduleCard','tp151EditCalendarItem',
 'tp151HeartLine','minHeartRate','maxHeartRate','heartRateSamples',
 'tp151-health-coach','tp151-coach-recommendation','tp151-weight'
])assert.ok(s.includes(x),'Missing unified-design marker: '+x);
const sec=s;
assert.ok(sec.includes("healthSettings=async function()"));
assert.ok(sec.includes("p.requestRead()"));
assert.ok(sec.includes("p.openSettings()"));
const cal=sec.slice(sec.indexOf('calendarScreen=function'),sec.indexOf('healthSettings=async function'));
assert.ok(!cal.includes('scheduleListHtml()'),'Calendar must not render a separate planned-workout list');
assert.ok(cal.includes('tp151CalendarDayPanel()'),'Calendar selected-day panel missing');
console.log('PASS: TrainPilot unified compact design structure.');
