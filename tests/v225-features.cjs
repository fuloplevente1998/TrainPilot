const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
const values=new Map(),root={innerHTML:''};
const node=()=>({innerHTML:'',style:{setProperty(){}},classList:{add(){},remove(){}},querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){}});
const doc={querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node,head:node(),body:node(),documentElement:node()};
const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>false},scrollTo(){},open(){}},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:()=>1,clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert(){},confirm:()=>true,prompt:()=>null,console});
for(const [,f] of fs.readFileSync('www/index.html','utf8').matchAll(/<script\b[^>]*\bsrc="([^"]+)"[^>]*>/g))vm.runInContext(fs.readFileSync('www/'+f,'utf8'),ctx,{filename:f});
const run=s=>vm.runInContext(s,ctx);


(async()=>{
 const builtins=run('JSON.stringify(programs().filter(p=>p.builtin))');
 for(const gear of [[],['dumbbell'],['dumbbells'],['dumbbells','support'],['machines'],['barbell','bench'],Object.keys(run('GEAR132'))]){
  for(const minutes of [20,30,45,60])for(const experience of ['beginner','intermediate']){
   ctx.profile={age:28,height:178,weight:80,goal:'fitness',experience,activity:'physical',minutes,location:'home',cadence:'alternate',split:'full',excluded:[],gear};
   const p=run('generatePersonalProgram(profile)');
   assert.ok(p.days.every(d=>d.exercises.length>=3&&d.exercises.length<=7));
   for(const day of p.days){assert.equal(day.exercises.length,new Set(day.exercises).size);for(const id of day.exercises){
    ctx.id=id;assert.ok(run('available132(id,profile)'));assert.ok(p.prescriptions[id]);
    assert.ok(p.prescriptions[id].sets>=2);if(experience==='beginner')assert.ok(run('byId(id).beginnerSafe'));
   }}
   ctx.preview=p;run('state.programPreview=preview;showPersonalPreview()');
  }
 }
 assert.equal(run('JSON.stringify(programs().filter(p=>p.builtin))'),builtins,'saved 7-exercise A/B must not change');
 assert.equal(run("programById('home-basic').days[0].exercises.length"),7);
 ctx.profile={age:28,height:178,weight:80,goal:'muscle',experience:'beginner',activity:'mixed',minutes:45,cadence:'custom',split:'full',excluded:['pushup'],gear:[]};
 assert.ok(run('generatePersonalProgram(profile).days.every(d=>!d.exercises.includes("pushup"))'));
 run('state.health.recovery={sleepMinutes:null,hrvRmssdMs:null,hrvBaselineMs:null}');
 assert.equal(run('rf141RecoverySignals(state.health.recovery).length'),0);
 assert.ok(!run('rf141RecoveryText()').includes('alvás rövid'));
 run('rf206HealthHub();healthPlugin=()=>bridge');
 ctx.bridge={getStatus:async()=>({permissions:{READ_HEART_RATE:true}}),requestRead:async()=>({}),createChangeToken:async()=>({token:'test'}),readHealthDay:async()=>({sources:['watch'],averageHeartRate:90,activeCalories:null,warnings:['READ_ACTIVE_CALORIES_BURNED: calories unavailable']})};
 await run('rf225ConnectHealth()');
 assert.equal(run('state.health.daily.averageHeartRate'),90);
 assert.equal(run('state.health.daily.activeCalories'),null);
 assert.equal(run('db.get(RF245_REPORT).status'),'partial');
 assert.ok(run('db.get(RF245_REPORT).errors.join(" ")').includes('calories unavailable'));
 assert.equal(run('state.health.busy'),false);
 ctx.bridge={getStatus:async()=>({permissions:{READ_HEART_RATE:true}}),requestRead:async()=>({}),createChangeToken:async()=>({token:'new'}),readHealthDay:async()=>({sources:['new'],averageHeartRate:88,activeCalories:null,warnings:[]})};
 await run('rf207ReadTodayHealth()');
 assert.equal(run('state.health.daily.averageHeartRate'),88);
 assert.equal(run('state.health.daily.sources[0]'),'new','fresh ledger must replace stale source metadata');
 const manifest=fs.readFileSync('android/app/src/main/AndroidManifest.xml','utf8');
 for(const name of Object.keys(run('rf225PermissionNames')))assert.ok(manifest.includes('android.permission.health.'+name),name);
 console.log('PASS 2.2.5 profile combinations, preview prescriptions, unchanged A/B, partial Health reads, stale source fallback and manifest');
})().catch(e=>{console.error(e);process.exitCode=1});
