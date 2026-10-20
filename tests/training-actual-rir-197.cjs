'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const split='www/training-actual-rir-197.js',src=fs.readFileSync(fs.existsSync(split)?split:'www/app.js','utf8');
const match=src.match(/^\/\/ @section training-actual-rir-197\.js\n([\s\S]*?)^\/\/ @endsection training-actual-rir-197\.js$/m);
const source=fs.existsSync(split)?src:match?.[1];
assert.ok(source,'Actual RIR runtime section exists');
let writes=0,paints=0,saved;
const exercise={id:'goblet-squat',repUnit:'ism',sets:[{done:true,reps:'10',weight:10},{done:false,reps:'',weight:10}]};
const ctx={
 state:{session:{programId:'home-basic',timerOwner:'phone',exercises:[exercise]},current:0},
 window:{TrainPilotWearSync:{}},
 document:{createElement:()=>({}),head:{appendChild(){}}},
 rf152FeedbackHtml:()=>'<div class="card" id="rf152Feedback"><strong>Effort</strong></div>',
 tp197TrainingModes:()=>({explicitChoice:true,progressionMode:'rir'}),
 tp106IsDistance:e=>e?.id==='run',
 byId:id=>({id,repUnit:id==='plank'?'mp':'ism'}),
 rf212Lang:()=> 'hu',esc:x=>String(x),
 persistDraft:()=>{writes++;saved=JSON.parse(JSON.stringify(ctx.state.session));},
 renderWorkout:()=>{paints++}
};
vm.createContext(ctx);vm.runInContext(source,ctx,{timeout:1000});
assert.match(ctx.rf152FeedbackHtml(exercise),/Tényleges RIR/);
assert.match(ctx.rf152FeedbackHtml(exercise),/value="0"/);
assert.equal(ctx.tp197RirSave('0'),true,'RIR zero is a valid actual report');
assert.equal(exercise.actualRir,0);assert.equal(saved.exercises[0].actualRir,0,'saved draft retains actual RIR');
assert.equal(ctx.tp197RirSave('6'),true);assert.equal(exercise.actualRir,6);
assert.equal(ctx.tp197RirSave('10'),true);assert.equal(exercise.actualRir,10);
const w=writes;assert.equal(ctx.tp197RirSave('11'),false);assert.equal(ctx.tp197RirSave('-1'),false);
assert.equal(ctx.tp197RirSave('2.5'),false);assert.equal(writes,w);assert.equal(exercise.actualRir,10);
assert.equal(ctx.tp197RirSave(''),true);assert.equal(Object.hasOwn(exercise,'actualRir'),false,'clearing must remove actual report');
assert.equal(Object.hasOwn(saved.exercises[0],'actualRir'),false);
exercise.sets[0].done=false;assert.equal(ctx.tp197RirSave('2'),false);
assert.doesNotMatch(ctx.rf152FeedbackHtml(exercise),/Tényleges RIR/);
exercise.sets[0].done=true;
ctx.state.session.timerOwner='watch';assert.equal(ctx.tp197RirSave('2'),false);
ctx.state.session.timerOwner='phone';exercise.id='run';assert.equal(ctx.tp197RirSave('2'),false);
exercise.id='plank';exercise.repUnit='mp';assert.equal(ctx.tp197RirSave('2'),false);
exercise.id='goblet-squat';exercise.repUnit='ism';
ctx.tp197TrainingModes=()=>({explicitChoice:false,progressionMode:'rir'});assert.equal(ctx.tp197RirSave('2'),false);
ctx.tp197TrainingModes=()=>({explicitChoice:true,progressionMode:'double'});assert.equal(ctx.tp197RirSave('2'),false);
ctx.tp197TrainingModes=()=>({explicitChoice:true,progressionMode:'rir'});
assert.equal(ctx.tp197RirSave('3'),true);assert.equal(exercise.actualRir,3);
assert.match(ctx.tp197RirHistory({actualRir:0}),/>0<\/strong>/,'Journal displays actual zero');
for(const actualRir of [null,undefined,'',-1,11,2.5])assert.equal(ctx.tp197RirHistory({actualRir}),'');
assert.equal(ctx.tp197RirHistory({prescription:{rir:2}}),'','Target RIR is not a journal measurement');
const oldDraft=JSON.stringify(saved),originalPersist=ctx.persistDraft;
ctx.persistDraft=()=>{throw Error('storage full')};
assert.equal(ctx.tp197RirSave('8'),false);assert.equal(exercise.actualRir,3);assert.equal(JSON.stringify(saved),oldDraft);
ctx.persistDraft=originalPersist;
assert.ok(paints>=5);
console.log('PASS #197 actual RIR: 0..10 optional self-report, draft persistence, clear, active strength/phone ownership guard');
