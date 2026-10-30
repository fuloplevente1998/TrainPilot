'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');

// The compatibility runner extracts app.js sections into individual files.
// Run the exact shipped section in both that environment and the source tree.
const root=path.resolve(__dirname,'..');
const isolated=path.join(root,'www','training-modes-197.js');
const app=path.join(root,'www','app.js');
const input=fs.readFileSync(fs.existsSync(isolated)?isolated:app,'utf8');
const marker=/^\/\/ @section training-modes-197\.js\n([\s\S]*?)^\/\/ @endsection training-modes-197\.js$/m;
const source=fs.existsSync(isolated)?input:input.match(marker)?.[1];
assert.ok(source,'The shipped app must contain the #197 resolver section');

const legacy={schemaVersion:1,programId:'home-basic',trainingStyle:'normal',progressionMode:'double',explicitChoice:false,source:'legacy'};
const modes=(defaults={trainingStyle:'hit',progressionMode:'linear'},extra={})=>({
 schemaVersion:1,defaults,explicitChoice:true,byProgram:{},...extra
});
function harness(saved,sessionProgramId=null){
 const originalSettings={rest:90,progressionSteps:{total:2.5},
  trainingModes197:saved,profile:{goal:'muscle'}};
 const preserved={
  programs:[{id:'home-basic',prescriptions:{pushup:{sets:2,reps:'8–12'}}}],
  history:[{id:'finished',exercises:[{id:'pushup',sets:[{reps:12,done:true}]}]}],
  draft:{session:{started:'2026-10-19T09:00:00Z',exercises:[]}}
 };
 const before=JSON.stringify([originalSettings,preserved]);
 const counters={reads:0,active:0};
 const context={
  settings:()=>{counters.reads++;return originalSettings;},
  activeProgramId:()=>{counters.active++;return 'home-basic';},
  state:{session:sessionProgramId?{programId:sessionProgramId}:null}
 };
 vm.createContext(context);
 vm.runInContext(source,context,{timeout:1000});
 assert.equal(typeof context.tp197TrainingModes,'function');
 return {
  get:(id)=>JSON.parse(JSON.stringify(context.tp197TrainingModes(id))),
  clean:()=>assert.equal(JSON.stringify([originalSettings,preserved]),before,'resolver must not write saved data'),
  counters
 };
}

// Legacy backups must retain the current progression engine unchanged.
const old=harness(undefined);
assert.deepEqual(old.get(),legacy);
assert.deepEqual(old.get('home-basic'),legacy);
old.clean();

// Global explicit selection, without mutating its stored config.
const global=harness(modes());
assert.deepEqual(global.get(),{...legacy,trainingStyle:'hit',progressionMode:'linear',explicitChoice:true,source:'global'});
assert.deepEqual(global.get('another-program'),{...legacy,programId:'another-program',trainingStyle:'hit',progressionMode:'linear',explicitChoice:true,source:'global'});
global.clean();

// Program choices beat global ones; explicit disabled override falls through.
const program=harness(modes({trainingStyle:'normal',progressionMode:'double'},{
 byProgram:{'home-basic':{trainingStyle:'hit',progressionMode:'rir',explicitChoice:true},
            'gym-ab':{trainingStyle:'normal',progressionMode:'linear',explicitChoice:false}}
}));
assert.deepEqual(program.get(),{...legacy,trainingStyle:'hit',progressionMode:'rir',explicitChoice:true,source:'program'});
assert.deepEqual(program.get('gym-ab'),{...legacy,programId:'gym-ab',explicitChoice:true,source:'global'});
program.clean();

// Session ownership determines the implicit program ID, not activeProgramId().
const session=harness(modes(undefined,{byProgram:{
 'session-program':{trainingStyle:'hit',progressionMode:'double',explicitChoice:true}
}}),'session-program');
assert.equal(session.get().programId,'session-program');
assert.equal(session.get().source,'program');
assert.equal(session.counters.active,0);
session.clean();

// Even a valid-looking label must not enable the new policy without explicit approval.
for(const saved of [
 modes(undefined,{explicitChoice:false}),
 modes({trainingStyle:'hit',progressionMode:'rir'},{explicitChoice:false}),
 modes(undefined,{schemaVersion:2}),
 modes({trainingStyle:'unknown',progressionMode:'linear'}),
 modes({trainingStyle:'hit',progressionMode:'something'}),
 modes(undefined,{explicitChoice:'true'}),
 modes(undefined,{byProgram:{'home-basic':{trainingStyle:'hit',progressionMode:'unknown',explicitChoice:true}}}),
 modes(undefined,{byProgram:{'home-basic':{trainingStyle:'hit',progressionMode:'rir'}}}),
 modes(undefined,{byProgram:{'home-basic':null}}),
 {schemaVersion:1,explicitChoice:true},
 null,
 []
]){
 const h=harness(saved);
 assert.deepEqual(h.get(),legacy,'Invalid/implicit selection must be legacy: '+JSON.stringify(saved));
 h.clean();
}

// JSON round-trip models the existing backup/storage path; no migration or writes.
const serialized=JSON.parse(JSON.stringify(modes(undefined,{byProgram:{
 'home-basic':{trainingStyle:'normal',progressionMode:'rir',explicitChoice:true,updatedAt:'2026-10-19T19:00:00Z'}
}})));
const restored=harness(serialized);
const a=restored.get(),b=restored.get();
assert.deepEqual(a,b);
assert.notStrictEqual(a,b,'every read should return a fresh value object');
assert.equal(a.source,'program');
assert.equal(a.progressionMode,'rir');
restored.clean();
console.log('PASS #197: read-only global/program resolver, legacy and invalid fallbacks, stable backup round-trip, session ownership');
