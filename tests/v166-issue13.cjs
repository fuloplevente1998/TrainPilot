const assert=require('node:assert/strict'),fs=require('node:fs');
const section='www/trainpilot-166-issue13-home-coach-card.js';
const app=fs.readFileSync(fs.existsSync(section)?section:'www/app.js','utf8');
for(const marker of [
 'tp166HomeCoachMarkup',
 'tp166DecorateHomeCoach',
 'fullCardTap:true',
 'noHomeButtons:true',
 'coachDesign:true',
 'scrollGestureGuard:true'
])assert.ok(app.includes(marker),'missing #13 marker: '+marker);
assert.match(app,/className='card tp151-coach-recommendation tp166-home-coach'/);
assert.match(app,/card\.setAttribute\('role','button'\)/);
assert.match(app,/go\('coach'\)/);
assert.ok(!/tp166HomeCoachMarkup[\s\S]{0,5000}home\.coachDetails/.test(app),'#13 Home Coach must not restore Details button');
console.log('PASS TrainPilot #13 Home Coach card guards.');
