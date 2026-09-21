const fs=require('fs');const s=fs.readFileSync('www/v224.js','utf8');
for(const x of ['RF224_VERSION','calendarScreen=function','toggleCalendarDay=rf224DayChoice','rf224DayChoice','rf224PlannerStrip','rf224SaveQuick','makeScheduleItem','addScheduleBatch']){if(!s.includes(x))throw new Error('v224 missing '+x)}
if(/generatePlanner\(\)/.test(s))throw new Error('v224 calendar must not depend on old planner generator UI');
console.log('v2.2.4 calendar-first planning tests OK');
