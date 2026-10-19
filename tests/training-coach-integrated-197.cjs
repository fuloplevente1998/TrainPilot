const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm');
const path=fs.existsSync('www/training-coach-197.js')?'www/training-coach-197.js':'www/app.js';
let code=fs.readFileSync(path,'utf8');
if(path==='www/app.js'){
 const match=code.match(/^\/\/ @section training-coach-197\.js\n([\s\S]*?)^\/\/ @endsection training-coach-197\.js$/m);
 assert.ok(match,'Canonical Coach section exists');code=match[1];
}
let saves=0,paints=0;
const ctx={state:{session:null,current:0},window:{TrainPilotWearSync:{}},
 document:{createElement:()=>({}),head:{appendChild(){}}},
 rf212Lang:()=>'hu',tp197UiText:()=>({normal:'Normál',linear:'Lineáris'}),
 rf233ExerciseAdvice:()=>({action:'hold',label:'legacy'}),renderWorkout:()=>{paints++},
 persistDraft:()=>{saves++},alert:()=>{},esc:x=>String(x),activeProgramId:()=>null,
 tp197TrainingModes:()=>({schemaVersion:1,programId:'home-basic',explicitChoice:false,source:'legacy',trainingStyle:'normal',progressionMode:'double'}),
 tp197ExercisePreview:()=>({handled:true,action:'increase',reasonCode:'linear-stable-performance',historyCount:2,baseline:{weight:10,sets:3},proposed:{weight:12,sets:3}})
};
vm.createContext(ctx);vm.runInContext(code,ctx);
assert.equal(ctx.tp197CoachInfo('goblet-squat','home-basic'),null,'Legacy must stay separate');
ctx.tp197TrainingModes=()=>({schemaVersion:1,programId:'home-basic',explicitChoice:true,source:'program',trainingStyle:'normal',progressionMode:'linear'});
ctx.state.session={programId:'home-basic',started:'2026-10-20T10:00:00Z',timerOwner:'phone',exercises:[{id:'goblet-squat',sets:[{set:1,weight:10,reps:'',done:false},{set:2,weight:10,reps:'',done:false}]}]};
const se=ctx.state.session.exercises[0],info=ctx.tp197CoachInfo(se.id,'home-basic');
assert.equal(info.action,'increase');
assert.equal(ctx.tp197CoachAllowed(ctx.state.session,se,info),true);
const first=JSON.stringify(se.sets);
assert.equal(ctx.tp197CoachRequest(),true);
assert.equal(JSON.stringify(se.sets),first,'Preview cannot modify sets');
se.sets[0].reps='1';
assert.equal(ctx.tp197CoachApply(),false,'Changing set invalidates approval');
assert.equal(JSON.stringify(se.sets).includes('"weight":12'),false);
assert.equal(saves,0);
se.sets[0].reps='';
ctx.state.session.timerOwner='watch';
assert.equal(ctx.tp197CoachAllowed(ctx.state.session,se,info),false,'Watch owns session');
ctx.state.session.timerOwner='phone';se.sets[1].done=true;
assert.equal(ctx.tp197CoachAllowed(ctx.state.session,se,info),false,'No edits to completed sets');
se.sets[1].done=false;ctx.state.session.workoutPaused=true;
assert.equal(ctx.tp197CoachAllowed(ctx.state.session,se,info),false,'No paused workout edits');
ctx.state.session.workoutPaused=false;
assert.equal(ctx.tp197CoachRequest(),true);
assert.equal(ctx.tp197CoachApply(),true,'Explicit approval applies load');
assert.equal(se.sets[0].weight,12);
assert.equal(se.sets[1].weight,12);
assert.equal(saves,1,'Exactly one draft persistence');
assert.ok(paints>=2);
console.log('PASS #197 legacy isolation, explainable proposal and explicit owner-safe load approval');
