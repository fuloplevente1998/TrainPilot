const fs=require('fs');const s=fs.readFileSync('www/v223.js','utf8');
for(const x of ['RF223_VERSION','Mai állapot','Today status','Heutiger Status','Starea de azi','rf223StatusHtml','rf223Apply','exerciseMinutes','activeCalories','exerciseSessionCount','rf215WeightScreen','go(\'health\')']){if(!s.includes(x))throw new Error('v223 missing '+x)}
if(!/grid-template-columns:repeat\(4/.test(s))throw new Error('Today status must be compact four-column grid');
console.log('v2.2.3 Today status tests OK');