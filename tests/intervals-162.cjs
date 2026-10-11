const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const extracted=fs.existsSync('www/intervals-162.js'),source=fs.readFileSync(extracted?'www/intervals-162.js':'www/app.js','utf8'),section=extracted?[null,source]:source.match(/\/\/ @section intervals-162.js\n([\s\S]*?)\/\/ @endsection intervals-162.js/);
const c=vm.createContext({window:{addEventListener(){}},console});vm.runInContext(section[1],c);const core=c.window.TrainPilotInterval162Core;
const steps=[{exerciseId:'running',setId:'a',work:2,rest:1,rounds:2},{exerciseId:'plank',setId:'b',work:2,rest:1,rounds:2}];
for(const [ms,step,round,phase,remaining,completed] of [[0,0,1,'work',2000,0],[1999,0,1,'work',1,0],[2000,0,1,'rest',1000,0],[3000,0,2,'work',2000,0],[5000,0,2,'rest',1000,0],[6000,1,1,'work',2000,1],[8000,1,1,'rest',1000,1],[9000,1,2,'work',2000,1],[11000,1,2,'complete',0,2],[999999,1,2,'complete',0,2]]){
 const p=core.phase(steps,true,ms);assert.equal(p.stepIndex,step);assert.equal(p.round,round);assert.equal(p.phase,phase);assert.equal(p.remainingMs,remaining);assert.equal(p.completed,completed);assert.equal(p.totalMs,11000);
}
assert.equal(core.phase([steps[0]],false,5000).phase,'complete','manual intervals have no trailing rest');assert.equal(core.phase([{...steps[0],rest:0}],true,2000).round,2,'zero rest does not invent a phase');assert.equal(core.phase(steps,true,-100).elapsedMs,0);
for(const s of [{...steps[0],work:0},{...steps[0],rounds:101},{...steps[0],rest:1.2}])assert.equal(core.config(s),null);
assert.equal(core.phase(Array.from({length:100},()=>({...steps[0],work:3600,rest:3600,rounds:2})),true,0),null);assert.equal(core.phase(steps,true,NaN),null);assert.equal(core.phase([],true,0),null);
const session={started:'one',exercises:[{id:'running',sets:[{setId:'wrong'}]},{id:'running',sets:[{setId:'a'}]},{id:'plank',sets:[{setId:'b'}]}]},run={started:'one',steps};assert.equal(core.locate(session,steps[0]).index,1,'duplicate exercise IDs still resolve the exact stable set');assert.equal(core.owned(session,run),true);assert.equal(core.owned({...session,started:'other'},run),false);session.exercises[2].sets=[];assert.equal(core.owned(session,run),false,'removing any queued owner invalidates the plan');
console.log('PASS #162 timeline: exact boundaries, zero rest, inter-set rest, late callback clamping, validation and stable ownership');
