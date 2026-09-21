const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
const values=new Map(),root={innerHTML:''};let now=100000, callback,alerts=[];
const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k),setItem:(k,v)=>values.set(k,v)},document:{querySelector:()=>({...root,remove:()=>{}}),getElementById:()=>null,addEventListener:()=>{}},window:{},navigator:{},setInterval:fn=>{callback=fn;return 1},clearInterval:()=>{},Date:class extends Date{static now(){return now}},alert:t=>alerts.push(t),confirm:()=>true,console});
for(const f of ['backup.js','demos.js','cloud.js','app.js'])vm.runInContext(fs.readFileSync('www/'+f,'utf8'),ctx);
const run=s=>vm.runInContext(s,ctx);
assert.equal(run("byId('crunch').reps"),'15–20');assert.equal(run("byId('crunch').sets"),3);
assert.equal(run("plan().A.includes('plank')"),true);
run("changeExerciseSets('crunch',4); startWorkout('B')");
assert.equal(run("state.session.exercises[5].sets.length"),4);
assert.equal(run("state.session.exercises[1].loadType"),'per_hand');
assert.equal(run("formatSet(state.session.exercises[1],{weight:5,reps:10})"),'5 kg/kar × 10 ism.');
assert.equal(run("formatSet(state.session.exercises[2],{weight:10,reps:10})"),'10 kg összesen × 10 ism.');
assert.equal(run("formatSet(state.session.exercises[0],{weight:5,reps:10})"),'5 kg/kar × 10/láb');
assert.equal(run("formatSet({loadType:'bodyweight',repUnit:'mp'},{reps:45})"),'45 mp');
assert.match(run("formatSet({id:'db-ohp'},{weight:10,reps:8})"),/régi súlyjelölés/);
run('toggleSet(0,0)');assert.equal(run('state.session.exercises[0].sets[0].done'),false);
run("upd(0,0,'reps','10');toggleSet(0,0)");assert.equal(run('state.session.exercises[0].sets[0].done'),true);
now+=95000;callback();assert.equal(run('state.timer'),0);
run('finishWorkout()');assert.equal(run('history().length'),1);
run('resetStarter()');assert.equal(run("byId('crunch').sets"),2);assert.equal(run('plan().A.length'),5);
run('resetPlan()');assert.equal(run("byId('crunch').sets"),3);assert.equal(run('plan().A.length'),6);
// Changing one field must preserve other customized values.
run("changeExerciseValue('db-ohp','weight','6,5');changeExerciseSets('db-ohp',4)");
assert.equal(run("byId('db-ohp').weight"),6.5);
// Loading existing saved exercises adds metadata but preserves user prescriptions.
values.set('repforge:exercises',JSON.stringify([{id:'db-ohp',sets:4,reps:'6–8',weight:7}]));
assert.equal(run("byId('db-ohp').sets"),4);assert.equal(run("byId('db-ohp').loadType"),'per_hand');
console.log('PASS: defaults, starter/full profiles, saved customization, dynamic sets, units, legacy history, validation, timer, workout save.');
