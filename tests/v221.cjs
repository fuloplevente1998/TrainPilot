const fs=require('fs');
const s=fs.readFileSync('www/v221.js','utf8');
for(const x of ['rf221CompactHome','rf221StatsScreen','rf221RemoveProgramStats','rf220CoachScreen','overflow:hidden','rf221-coach-actions'])if(!s.includes(x))throw new Error('missing '+x);
console.log('v2.2.1 dashboard checks OK');
