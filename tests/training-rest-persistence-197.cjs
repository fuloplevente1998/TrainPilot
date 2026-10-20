const assert=require('node:assert/strict');
const fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync('www/app.js','utf8');
function piece(start,end){
 const a=source.indexOf(start),b=source.indexOf(end,a);
 assert.ok(a>=0&&b>a,'Canonical source range: '+start);
 return source.slice(a,b);
}
const model=piece('var tp152Rx=function','var tp152ReplaceExercise=function');
const panel=piece('var tp1511Prog=function','const tp1511Plan=planScreen');
const disk={
 settings:{rest:90,progressionSteps:{isolation:.5,per_hand:1,single_dumbbell:1,total:2.5}},
 programs:[{id:'home-basic',days:[{id:'A',exercises:['inherited','custom','same-default']}],
  prescriptions:{inherited:{sets:3,rest:90},custom:{sets:3,rest:150},'same-default':{sets:3,rest:90}}}]
};
const cloned=x=>x===undefined?undefined:JSON.parse(JSON.stringify(x));
const inputs={tp1511Rest:'65',tp1511Iso:'.5',tp1511Per:'1',tp1511Single:'1',tp1511Total:'2.5'};
const writes=[];
function boot(){
 const context={
  db:{get:(k,d)=>cloned(disk[k]??d),set:(k,v)=>{writes.push(k);disk[k]=cloned(v)}},
  settings:()=>cloned(disk.settings),programs:()=>cloned(disk.programs),
  activeProgram:()=>cloned(disk.programs[0]),activeProgramId:()=>disk.programs[0].id,
  rf155DayState:(dayId)=>{const ps=cloned(disk.programs),program=ps[0];return {ps,program,day:program.days.find(d=>d.id===dayId)};},
  byId:id=>({id,sets:3,reps:'8',weight:5}),
  document:{getElementById:id=>({value:inputs[id]})},
  window:{TrainPilotFeedback:{saved(){}}},
  render(){},cloudChanged(){},tp149T:()=>'',tp1511T:()=>'',tp149ProgramMeta:()=>'',
  esc:v=>String(v),startRest(){}
 };
 vm.createContext(context);vm.runInContext(model+'\n'+panel,context);
 return context;
}
let app=boot();
app.tp1511SaveTraining();
assert.equal(disk.settings.programRest['home-basic'],65,'Saved default survives settings write');
assert.equal(disk.programs[0].prescriptions.inherited.rest,65,'Inherited rest follows program setting');
assert.equal(disk.programs[0].prescriptions.custom.rest,150,'Previously distinct exercise rest is kept');
app.tp152SaveRx('A','same-default','rest','65');
assert.equal(disk.programs[0].prescriptions['same-default'].restCustomized,true,'Explicit exercise rest has provenance');
inputs.tp1511Rest='80';
app.tp1511SaveTraining();
assert.equal(disk.programs[0].prescriptions.inherited.rest,80);
assert.equal(disk.programs[0].prescriptions['same-default'].rest,65,'Explicit exercise rest stays even when matching prior default');
assert.equal(disk.programs[0].prescriptions.custom.rest,150);
app=boot(); // App runtime reload uses same persistent settings and programs.
assert.equal(app.tp1511Rest(),80,'Program rest survives fresh application runtime');
assert.equal(app.tp152Rx(disk.programs[0],'same-default',{id:'same-default'}).rest,65,'Custom rest survives application runtime');
assert.equal(app.tp152Rx(disk.programs[0],'inherited',{id:'inherited'}).rest,80);
assert.ok(writes.includes('settings')&&writes.includes('programs'));
console.log('PASS #197 rest settings persist across runtime reload, inherited defaults and explicit exercise overrides');
