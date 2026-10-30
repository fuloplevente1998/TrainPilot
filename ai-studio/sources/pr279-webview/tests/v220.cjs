const fs=require('fs');
const s=fs.readFileSync('www/v220.js','utf8');
const required=['RF220_VERSION','rf220Readiness','rf220CoachScreen','rf220ExerciseStats','rf220ProgressHtml','TrainPilot Coach','rf220PainRecent','readRecoveryHealth'];
for(const item of required){if(!s.includes(item)){console.error('missing',item);process.exit(1)}}
console.log('TrainPilot 2.2 Coach/Progress checks OK');
