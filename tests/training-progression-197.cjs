'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(__dirname,'..'),isolated=path.join(root,'www','progression-modes-197.js');
const text=fs.readFileSync(fs.existsSync(isolated)?isolated:path.join(root,'www','app.js'),'utf8');
const section=text.match(/^\/\/ @section progression-modes-197\.js\n([\s\S]*?)^\/\/ @endsection progression-modes-197\.js$/m);
const code=fs.existsSync(isolated)?text:section?.[1];
assert.ok(code,'Canonical progression section must exist');
const saved={modes:{source:'program',trainingStyle:'normal',progressionMode:'double',explicitChoice:true},
 target:{kind:'reps',sets:3,min:8,max:12,rest:90,rirMin:1,rirMax:3},
 exercise:{loadType:'per_hand',repUnit:'ism'},step:1,
 recent:[{weight:10,allDone:true,minRep:10,reps:[10,10,11],hitTop:false,belowMin:false,effort:'good'}]};
const baseline=JSON.stringify(saved),context={};
vm.createContext(context);vm.runInContext(code,context,{timeout:1000});
const parserText=fs.readFileSync(fs.existsSync(path.join(root,'www','trainpilot-140-progression-2.js'))?path.join(root,'www','trainpilot-140-progression-2.js'):path.join(root,'www','app.js'),'utf8');
vm.runInContext(parserText.match(/var tp140ParseTarget=[\s\S]*?\n};/)[0],context);
for(const absent of [null,undefined,''])assert.equal(context.tp140ParseTarget('8–12',absent).rirMin,null,'Missing target RIR must not become zero');
assert.equal(context.tp140ParseTarget('8–12',0).rirMin,0,'Explicit target zero remains valid');
const preview=input=>JSON.parse(JSON.stringify(context.tp197ComputePreview(input)));
const check=(x,action,reason)=>{assert.equal(x.action,action);assert.equal(x.reasonCode,reason);assert.equal(x.autoApply,false);assert.equal(x.previewOnly,true)};
check(preview({...saved,modes:{source:'legacy',explicitChoice:false}}),'legacy','legacy');
check(preview(saved),'reps','double-next-repetition');
assert.deepEqual(preview(saved).proposed.repTargets,[11,10,11]);
const top={...saved.recent[0],hitTop:true,minRep:12,reps:[12,12,12]};
check(preview({...saved,recent:[top,top]}),'hold','double-await-stability');
check(preview({...saved,recent:[top,top,top]}),'increase','double-stable-top');
assert.equal(preview({...saved,recent:[top,top,top]}).proposed.weight,11);
check(preview({...saved,recent:[{...top,pain:true},top,top]}),'hold','pain');
check(preview({...saved,recent:[top,top,top],readiness:{parts:2,score:48}}),'hold','low-readiness');
check(preview({...saved,recent:[{...top,allDone:false},top,top]}),'hold','incomplete');
check(preview({...saved,recent:[{...top,effort:'hard'},top,top]}),'hold','high-effort');
check(preview({...saved,recent:[{...top,minRep:6,belowMin:true},top,top]}),'hold','below-target');
check(preview({...saved,recent:[top,{...top,weight:11},top]}),'hold','double-await-stability');
check(preview({...saved,step:0,recent:[top,top,top]}),'hold','missing-load-step');
const linear={...saved,modes:{...saved.modes,progressionMode:'linear'}};
check(preview({...linear,recent:[saved.recent[0]]}),'hold','linear-await-stability');
check(preview({...linear,recent:[saved.recent[0],saved.recent[0]]}),'increase','linear-stable-performance');
const rir={...saved,modes:{...saved.modes,progressionMode:'rir'}};
check(preview({...rir,recent:[top,top]}),'hold','rir-feedback-missing');
check(preview({...rir,recent:[{...top,actualRir:2}]}),'hold','rir-on-target');
check(preview({...rir,recent:[{...top,actualRir:0},top]}),'hold','rir-overexertion');
check(preview({...rir,recent:[{...top,actualRir:6},{...top,actualRir:6}]}),'increase','rir-stable-reserve');
check(preview({...rir,target:{...saved.target,rirMin:null,rirMax:null},recent:[top,top]}),'hold','rir-target-missing');
const hit={...saved,modes:{...saved.modes,trainingStyle:'hit'}};
assert.equal(preview(hit).proposed.sets,2);
assert.equal(preview({...hit,isolation:true}).proposed.sets,1);
check(preview({...linear,exercise:{loadType:'bodyweight',repUnit:'ism'},recent:[top,top]}),'variation','linear-stable-performance');
check(preview({...saved,exercise:{measurementType:'distance',loadType:'bodyweight'},recent:[top,top,top]}),'hold','non-strength-activity');
check(preview({...saved,exercise:{loadType:'bodyweight',repUnit:'mp'},recent:[top,top,top]}),'hold','non-strength-activity');
assert.equal(JSON.stringify(saved),baseline,'the pure engine must not mutate its input');
// Exercise adapter must respect selected program, real owned history, no writes.
let reads=0;const local={
 state:{session:null},activeProgramId:()=> 'home-basic',
 tp197TrainingModes:()=>saved.modes,
 byId:()=>({id:'db-floor-press',loadType:'per_hand',repUnit:'ism'}),
 tp140Prescription:()=>saved.target,
 tp128Rows:()=>{reads++;return [{workout:{feedback:{rating:'good'}},exercise:{effort:'good'}},{workout:{feedback:{rating:'good'}},exercise:{effort:'good'}}];},
 tp140Occurrence:()=>({...top,reps:[10,10,11],minRep:10,hitTop:false}),
 rf220Readiness:()=>({parts:2,score:80}),
 programById:()=>({prescriptions:{}}),TP140_ISOLATION:new Set(),tp140Step:()=>1
};
const serial=JSON.stringify(local.state);
vm.createContext(local);vm.runInContext(code,local,{timeout:1000});
const out=local.tp197ExercisePreview('db-floor-press');
assert.equal(out.action,'reps');assert.equal(out.autoApply,false);
assert.equal(reads,1);assert.equal(JSON.stringify(local.state),serial);
console.log('PASS #197: safe read-only double / linear / RIR / HIT recommendations, pain, readiness, ownership and no mutation');
