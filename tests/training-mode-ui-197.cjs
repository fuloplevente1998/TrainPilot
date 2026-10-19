'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(__dirname,'..');
const isolated=path.join(root,'www','training-modes-ui-197.js'),app=path.join(root,'www','app.js');
const source=fs.readFileSync(fs.existsSync(isolated)?isolated:app,'utf8');
const section=fs.existsSync(isolated)?source:source.match(/^\/\/ @section training-modes-ui-197\.js\n([\s\S]*?)^\/\/ @endsection training-modes-ui-197\.js$/m)?.[1];
assert.ok(section,'Canonical #197 UI section missing');
const programs=[{id:'home-basic',days:[{id:'A',exercises:['pushup','running']}]}];
const stored={rest:90,profile:{goal:'muscle'},progressionSteps:{total:2.5}},history=[{id:'old',done:true}],draft={session:{id:'ongoing'}};
const original=JSON.stringify([stored,programs,history,draft]);
let reads=0,saves=0,lang='hu',calls=[],styleInjected=0;
const context={
 state:{session:draft.session},settings:()=>stored,activeProgram:()=>programs[0],
 programById:id=>programs.find(x=>x.id===id),
 byId:id=>id==='pushup'?{id,hu:'Fekvőtámasz',en:'Pushup',loadType:'bodyweight',repUnit:'ism.'}:{id,measurementType:'distance'},
 tp106IsDistance:e=>e.measurementType==='distance',
 rf212Lang:()=>lang,tp197TrainingModes:id=>({source:'legacy',explicitChoice:false,programId:id,trainingStyle:'normal',progressionMode:'double'}),
 tp197ExercisePreview:(id,pid,override)=>{calls.push({id,pid,override});return {action:'hold',reasonCode:'insufficient-history',baseline:{sets:3,weight:10},proposed:{sets:override.trainingStyle==='hit'?2:3,weight:10}};},
 settingsScreen:()=>'<main class="settings">Settings</main>',planScreen:()=>'<main class="plan">Program</main>',
 document:{getElementById:()=>null,createElement:()=>({id:'',textContent:''}),head:{appendChild:()=>styleInjected++}},
 esc:s=>String(s??'').replace(/[&<>"']/g,x=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[x])),
 db:{set:()=>saves++}
};
vm.createContext(context);vm.runInContext(section,context,{timeout:1200});
assert.equal(styleInjected,1);
assert.match(context.settingsScreen(),/id="tp197-mode-global"/);
assert.match(context.planScreen(),/id="tp197-mode-program"/);
assert.match(context.settingsScreen(),/Előnézet \(nem alkalmazott\)/);
assert.match(context.planScreen(),/Fekvőtámasz/);
assert.match(context.settingsScreen(),/option value="normal" selected/);
assert.match(context.settingsScreen(),/option value="double" selected/);
assert.equal(context.tp197UiSet('global',null,'trainingStyle','hit'),true);
assert.equal(context.tp197UiSet('global',null,'progressionMode','linear'),true);
assert.match(context.settingsScreen(),/option value="hit" selected/);
assert.match(context.settingsScreen(),/option value="linear" selected/);
assert.match(context.planScreen(),/option value="normal" selected/,'global pending selection must not replace program choice');
assert.equal(context.tp197UiSet('program','home-basic','trainingStyle','hit'),true);
assert.match(context.planScreen(),/Munkasorozat: 2/,'HIT preview should lower proposed volume without saving');
assert.equal(context.tp197UiSet('program','home-basic','progressionMode','rir'),true);
assert.match(context.planScreen(),/option value="rir" selected/);
assert.equal(context.tp197UiSet('program','home-basic','progressionMode','not-a-mode'),false);
assert.equal(context.tp197UiSet('program','missing','trainingStyle','hit'),false);
assert.equal(context.tp197UiSet('global',null,'other','hit'),false);
for(const [l,expected] of [['en','Training style and progression'],['de','Trainingsstil und Progression'],['ro','Stil de antrenament și progresie'],['hu','Edzésstílus és progresszió']]){
 lang=l;
 const screen=context.settingsScreen(),plan=context.planScreen();
 assert.ok(screen.includes(expected),'missing translation '+l);
 assert.ok(plan.includes(expected),'missing plan translation '+l);
 assert.match(screen,/id="tp197-mode-global-style"/);
 assert.match(plan,/id="tp197-mode-program-progress"/);
}
context.tp197UiReset('global',null);
assert.match(context.settingsScreen(),/option value="normal" selected/);
assert.match(context.settingsScreen(),/option value="double" selected/);
assert.match(context.planScreen(),/option value="hit" selected/,'reset of one scope must not reset another');
assert.equal(saves,0,'Preview UI may never persist');
assert.equal(JSON.stringify([stored,programs,history,draft]),original,'Preview UI must not mutate existing state');
assert.ok(calls.length>=4);
assert.ok(calls.every(c=>c.pid==='home-basic'&&c.override?.autoApply!==true));
console.log('PASS #197 UI: independent global/program choices, four languages, preview, reset and zero persistence');
