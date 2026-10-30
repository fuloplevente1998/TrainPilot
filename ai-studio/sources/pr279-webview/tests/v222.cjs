const fs=require('fs');
const p='www/v222.js';
const s=fs.readFileSync(p,'utf8');
for(const needle of ['exerciseSessions','exerciseSessionCount','exerciseMinutes','diagnostics','Health Connect diagnostics','rf222Timeline','rf207DailyCard=function']){
  if(!s.includes(needle)) throw new Error('Missing v2.2.2 feature: '+needle);
}
console.log('v2.2.2 Health Connect timeline checks passed');
