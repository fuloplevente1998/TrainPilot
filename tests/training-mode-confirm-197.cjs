'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const isolated=path.resolve(__dirname,'../www/training-mode-confirm-197.js'),app=path.resolve(__dirname,'../www/app.js');
const full=fs.readFileSync(fs.existsSync(isolated)?isolated:app,'utf8');
const code=fs.existsSync(isolated)?full:full.match(/^\/\/ @section training-mode-confirm-197\.js\n([\s\S]*?)^\/\/ @endsection training-mode-confirm-197\.js$/m)?.[1];
assert.ok(code,'Canonical #197 confirmation section missing');
const originals={
 programs:[{id:'p-1',prescriptions:{pushup:{sets:3,reps:10,weight:20}}}],
 history:[{id:'logged',done:true,weight:20}],
 session:{id:'finished'},
 draft:{current:{sets:[{weight:20}]}}
};
let stored={rest:90,profile:{goal:'muscle'}},writes=0,rendered='',lang='hu';
const pending=Object.create(null);
let cancelFocused=false;
const cancelPanel={open:true,querySelector:()=>({focus:()=>{cancelFocused=true;}})};
const context={
 settings:()=>stored,
 db:{set:(key,value)=>{assert.equal(key,'settings');writes++;stored=value;}},
 state:{session:null},
 activeProgram:()=>originals.programs[0],
 programById:id=>originals.programs.find(x=>x.id===id),
 rf212Lang:()=>lang,esc:s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])),
 tp197UiKey:(scope,pid)=>scope==='program'?'program:'+String(pid):'global',
 tp197UiPending:pending,
 tp197UiChoice:(scope,pid)=>{
  const key=scope==='program'?'program:'+pid:'global';
  if(pending[key])return {...pending[key]};
  const mode=stored.trainingModes197,entry=scope==='program'?mode?.byProgram?.[pid]:null;
  return {...(entry?.explicitChoice===true?entry:mode?.explicitChoice?mode.defaults:{trainingStyle:'normal',progressionMode:'double'})};
 },
 tp197UiPanel:()=>'<details><p class="small muted tp197-ui-notice">Old notice</p><button type="button" class="btn secondary tp197-ui-reset" onclick="tp197UiReset()">Reset</button></details>',
 tp197UiSet:(scope,pid,field,value)=>{pending[scope==='program'?'program:'+pid:'global']={...context.tp197UiChoice(scope,pid),[field]:value};return true;},
 tp197UiRefresh:()=>{rendered=context.tp197UiPanel('global',null,true);},
 document:{getElementById:()=>cancelPanel,createElement:()=>({id:'',textContent:''}),head:{appendChild:()=>{}}}
};
vm.createContext(context);
vm.runInContext(code,context,{timeout:1500});
const snapshot=()=>JSON.stringify([originals,stored]);
const before=snapshot();
assert.match(context.tp197UiPanel('global',null,true),/Mentés/);
assert.match(context.tp197UiPanel('global',null,true),/Mégse/);
context.tp197UiSet('global',null,'trainingStyle','hit');
context.tp197UiSet('global',null,'progressionMode','rir');
assert.equal(writes,0);
assert.equal(context.tp197UiRequestSave('global',null),true);
assert.match(rendered,/Jóváhagyom a módváltást/);
assert.equal(writes,0,'request is preview only');
assert.equal(context.tp197UiCancel('global',null),true);
assert.equal(writes,0,'cancel must never persist');
assert.equal(cancelPanel.open,false,'Cancel collapses the mode editor');
assert.equal(cancelFocused,true,'Cancel returns focus to its visible summary');
assert.equal(snapshot(),before,'cancel must not alter settings, history, draft or program');

context.tp197UiSet('global',null,'trainingStyle','hit');
context.tp197UiRequestSave('global',null);
context.tp197UiSet('global',null,'progressionMode','linear');
assert.equal(context.tp197UiApprove('global',null),false,'changing selection invalidates approval');
assert.equal(writes,0);
context.tp197UiRequestSave('global',null);
assert.equal(context.tp197UiApprove('global',null),true);
assert.equal(writes,1,'one atomic settings write per confirmation');
assert.equal(stored.trainingModes197.schemaVersion,1);
assert.equal(stored.trainingModes197.revision,1);
assert.deepEqual(JSON.parse(JSON.stringify(stored.trainingModes197.defaults)),{trainingStyle:'hit',progressionMode:'linear'});
assert.equal(stored.trainingModes197.explicitChoice,true);
assert.equal(stored.rest,90);
assert.ok(!('trainingModes197' in originals.programs[0]));
assert.equal(originals.history[0].weight,20);
assert.match(context.tp197UiPanel('global',null,true),/visszavonása/);

context.state.session={id:'live',paused:true};
context.tp197UiSet('program','p-1','progressionMode','double');
assert.equal(context.tp197UiRequestSave('program','p-1'),false,'active workout blocks new confirmation');
assert.equal(writes,1);
assert.equal(context.tp197UiUndo('global',null),false,'active workout blocks undo');
context.state.session=null;

context.tp197UiSet('program','p-1','trainingStyle','normal');
assert.equal(context.tp197UiRequestSave('program','p-1'),true);
stored={...stored,profile:{goal:'changed elsewhere'}};
assert.equal(context.tp197UiApprove('program','p-1'),true,'unrelated setting changes are preserved');
assert.equal(stored.profile.goal,'changed elsewhere');
assert.equal(stored.trainingModes197.byProgram['p-1'].trainingStyle,'normal');
assert.equal(stored.trainingModes197.byProgram['p-1'].explicitChoice,true);
assert.equal(stored.trainingModes197.revision,2);
assert.equal(context.tp197UiUndo('global',null),false,'different scope must not undo last program save');
assert.equal(context.tp197UiUndo('program','p-1'),true);
assert.equal(stored.trainingModes197.byProgram['p-1'],undefined);
assert.equal(stored.trainingModes197.defaults.trainingStyle,'hit');
assert.equal(stored.profile.goal,'changed elsewhere');
assert.equal(context.tp197UiUndo('global',null),false,'single level undo must not recursively roll back');

context.tp197UiSet('program','p-1','trainingStyle','hit');
context.tp197UiRequestSave('program','p-1');
stored.trainingModes197={...stored.trainingModes197,revision:999};
assert.equal(context.tp197UiApprove('program','p-1'),false,'stale approval cannot overwrite other settings changes');
assert.equal(stored.trainingModes197.revision,999);

context.tp197UiSet('global',null,'trainingStyle','normal');
context.tp197UiRequestSave('global',null);
assert.equal(context.tp197UiApprove('global',null),true);
const portable=JSON.parse(JSON.stringify(stored));
assert.equal(portable.trainingModes197.schemaVersion,1);
assert.equal(portable.trainingModes197.defaults.trainingStyle,'normal');
assert.equal(originals.draft.current.sets[0].weight,20);

for(const [l,label] of [['hu','Mentés'],['en','Save'],['de','Speichern'],['ro','Salvează']]){
 lang=l;assert.ok(context.tp197UiPanel('global',null).includes(label),'missing locale '+l);
}
console.log('PASS #197 5/7: two-step save, cancel, stale guard, session protection, single-write persistence, undo, 4 languages and backup JSON');
