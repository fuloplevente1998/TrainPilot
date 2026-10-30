const fs=require('fs');
const s=fs.readFileSync('www/v209.js','utf8');
['RF209_VERSION','rf209DeleteSchedule','rf209NearestPlan','toggleCalendarDay=function','scheduleListHtml=function','Törlés'].forEach(x=>{if(!s.includes(x))throw Error('Missing '+x)});
console.log('PASS v2.0.9 schedule cleanup');
