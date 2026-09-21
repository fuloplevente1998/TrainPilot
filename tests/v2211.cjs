const fs=require('fs'),assert=require('node:assert/strict');
const js=fs.readFileSync('www/v228b.js','utf8');
assert.ok(js.includes('toggleCalendarDay=rf2211SelectDate'),'calendar tap opens chooser');
assert.ok(js.includes('rf2211-daybuttons'),'day buttons rendered');
assert.ok(js.includes('rf2211PlanWeeks'),'bulk planner present');
assert.ok(js.includes('activeProgram()'),'planner follows active program');
assert.ok(js.includes('cloudChanged'),'calendar changes trigger sync');
assert.ok(js.includes('Tervezési beállítások'),'collapsible planner restored');
console.log('TrainPilot 2.2.11 hybrid calendar planner checks passed');
