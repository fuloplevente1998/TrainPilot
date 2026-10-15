'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.existsSync('www/workout-undo-188.js')?fs.readFileSync('www/workout-undo-188.js','utf8'):
 fs.readFileSync('www/app.js','utf8').split('// @section workout-undo-188.js\n')[1].split('// @endsection workout-undo-188.js')[0];
const window={};vm.runInNewContext(source,{window});const core=window.TrainPilotUndo188,copy=v=>JSON.parse(JSON.stringify(v));
const before={timerOwner:'watch',exercises:[{id:'press',sets:[{setId:'a',reps:'9',weight:5.5,done:false},{setId:'b',reps:'',weight:5.5,done:false}]}],healthWear129:{heartRate:93,totalCalories:7},notes:'Keep notes',automaticTime162:{workMs:9000,restMs:1000}};
const complete={commandId:'record',workoutId:'w',exerciseId:'press',setId:'a'};
function recorded(){const s=copy(before),values=core.values(s.exercises[0].sets[0]);s.exercises[0].sets[0].done=true;core.capture(s,complete,values);return s;}
function command(s){return {...copy(s.lastRecord188),commandId:'undo',action:'undoSet',automation:{enabled:true,owner:'watch',phase:'ready',paused:true,key:'undo188:undo'}};}
const s=recorded(),undo=command(s);assert.equal(core.inverse(s,undo),true);
assert.equal(s.exercises[0].sets[0].done,false);assert.equal(s.exercises[0].sets[0].reps,'9');assert.equal(s.watchAutomation162.paused,true);
assert.deepEqual(s.healthWear129,before.healthWear129);assert.deepEqual(s.automaticTime162,before.automaticTime162);assert.equal(s.notes,before.notes);
assert.equal(core.inverse(s,undo),true,'exact replay is idempotent');assert.equal(core.inverse(s,{...undo,recordId:'collision'}),false);
for(const edit of [set=>set.reps='1',set=>set.done=true,set=>set.setId='other']){
 const changed=recorded(),old=command(changed);edit(changed.exercises[0].sets[1]);const saved=JSON.stringify(changed);
 assert.equal(core.inverse(changed,old),false);assert.equal(JSON.stringify(changed),saved,'conflict cannot replace the newer draft');
}
const next=recorded(),old=command(next);next.exercises[0].sets[1].done=true;core.capture(next,{...complete,commandId:'later',setId:'b'},core.values(before.exercises[0].sets[1]));
assert.equal(core.inverse(next,old),false,'a newer recording supersedes the former candidate');
const formatted=recorded();formatted.exercises[0].sets[0].weight='5,500';assert.equal(core.same(formatted.lastRecord188.expected,core.guard(formatted.exercises)),true);
console.log('PASS #188 inverse: stable identities, one last event, newer edit/record guard, replay, numeric formatting, health/time preservation');
