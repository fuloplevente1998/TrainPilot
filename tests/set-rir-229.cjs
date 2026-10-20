'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
function section(name){const split='www/'+name;if(fs.existsSync(split))return fs.readFileSync(split,'utf8');const app=fs.readFileSync('www/app.js','utf8');return app.split('// @section '+name+'\n')[1].split('// @endsection '+name)[0];}
let fail=false,saved;
const first={setId:'s1',done:true,reps:'10',weight:10},second={setId:'s2',done:true,reps:'10',weight:10},pending={setId:'s3',done:false,reps:'',weight:10};
const exercise={id:'press',sets:[first,second,pending],actualRir:8},session={syncId:'w',timerOwner:'phone',programId:'p',exercises:[exercise]};
const box={state:{session,current:0},window:{},document:{createElement:()=>({}),head:{appendChild(){}}},esc:String,
 rf152FeedbackHtml:()=>'<div></div>',tp197TrainingModes:()=>({explicitChoice:true,progressionMode:'rir'}),byId:()=>({}),rf212Lang:()=> 'hu',
 persistDraft:()=>{if(fail)throw Error('disk full');saved=JSON.parse(JSON.stringify(session));},renderWorkout(){}};
vm.createContext(box);vm.runInContext(section('training-actual-rir-197.js'),box);vm.runInContext(section('workout-undo-188.js'),box);
const api=box.window.TrainPilotSetRir229,save=box.tp197RirSave;
assert.equal(api.observation(exercise),8,'legacy remains readable, no fabricated per-set values');
assert.equal(save('0',false,'s1'),true);assert.equal(first.actualRir,0);assert.equal(exercise.actualRir,8);
assert.equal(save('6',false,'s2'),true);assert.equal(second.actualRir,6);assert.equal(api.observation(exercise),0,'lowest completed actual RIR is conservative');
assert.equal(save('4',false,'s3'),false);assert.equal(save('4',false,'removed'),false);
assert.equal(save('2.5',false,'s1'),false);assert.equal(first.actualRir,0);assert.equal(save('11',false,'s1'),false);
fail=true;assert.equal(save('5',false,'s1'),false);assert.equal(first.actualRir,0);fail=false;
assert.equal(save('',false,'s1'),true);assert.equal(api.observation(exercise),6);
assert.equal(save('',false,'s2'),true);assert.equal(api.observation(exercise),null,'explicit clear must not reactivate legacy RIR');
assert.equal(saved.exercises[0].setRir229,true);assert.equal(saved.exercises[0].actualRir,8);
assert.equal(api.observation(JSON.parse(JSON.stringify(exercise))),null,'backup/reload preserve explicit missing state');
session.timerOwner='watch';assert.equal(save('3',false,'s1'),false);session.timerOwner='phone';
session.workoutPaused=true;assert.equal(save('3',false,'s1'),false);session.workoutPaused=false;
assert.equal(save('3',false,'s1'),true);assert.match(box.tp197RirHistory(exercise),/1\.: <strong>3<\/strong>/);
assert.equal(Object.hasOwn(pending,'actualRir'),false);
const undo=box.window.TrainPilotUndo188;const before=undo.values({...second,done:false});
undo.capture(session,{commandId:'record',workoutId:'w',exerciseId:'press',setId:'s2'},before);
save('0',false,'s2');assert.equal(undo.available(session),true,'optional RIR does not change the existing cross-platform completion guard');
const command={commandId:'undo',workoutId:'w',exerciseId:'press',setId:'s2',recordId:'record',expected:session.lastRecord188.expected};
assert.equal(undo.inverse(session,command),true);assert.equal(second.done,false);assert.equal(Object.hasOwn(second,'actualRir'),false,'undo clears the observation attached to the undone completion');
assert.equal(api.observation(exercise),3);
for(const invalid of [null,undefined,'',-1,11,2.5,'bad'])assert.equal(api.valid(invalid),false);
assert.equal(api.valid(0),true);assert.equal(api.observation({actualRir:7,sets:[{done:true,actualRir:null}]}),7);
console.log('PASS #229 distinct per-set RIR, zero/clear, conservative Coach, legacy compatibility, storage rollback, owner and Undo');
