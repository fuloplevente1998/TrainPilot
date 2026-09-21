const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
const values=new Map();let html='';const root={get innerHTML(){return html},set innerHTML(v){html=v}};
const node=()=>({innerHTML:'',style:{setProperty(){}},classList:{add(){},remove(){}},querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){}});
const doc={querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node,head:node(),body:node(),documentElement:node()};
const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:{status:async()=>({profile:null})}}},addEventListener(){},scrollTo(){},open(){}},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{if(!ms)fn();return 1},clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert(){},confirm:()=>true,prompt:()=>null,console});
const index=fs.readFileSync('www/index.html','utf8');
for(const [,attrs,inline] of index.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)){const src=attrs.match(/src="([^"]+)"/);vm.runInContext(src?fs.readFileSync('www/'+src[1],'utf8'):inline,ctx,{filename:src?.[1]||'inline'});}
const run=s=>vm.runInContext(s,ctx);
async function tests(){
 assert.equal(run('makeBackup().appVersion'),'1.4.6');
 // Exact five-step effort weights restored; workout timestamps must be in the past.
 const now=new Date(Date.now()-3600000).toISOString(),later=new Date(Date.now()-1000).toISOString();
 for(const [effort,expected] of Object.entries({easy:100,light:100,good:100,challenging:96,hard:92,pain:80})){
  run(`db.set('history',[{started:${JSON.stringify(now)},finished:${JSON.stringify(later)},exercises:[{effort:${JSON.stringify(effort)}}]}])`);
  assert.equal(run('rf220EffortLoad().score'),expected,effort);
 }
 run(`db.set('history',[{started:${JSON.stringify(now)},finished:${JSON.stringify(later)},exercises:[{effort:'hard'},{effort:'challenging'},{effort:'light'},{effort:'easy'}]}])`);
 assert.equal(run('rf220EffortLoad().score'),92,'-8 -4 +1 +3 must equal 92');

 // Sleep stages exclude awake time instead of counting the whole session.
 run(`const d250=rf240DayKey(new Date());const s0=new Date(Date.now()-7200000).toISOString(),s1=new Date(Date.now()-3600000).toISOString(),s2=new Date().toISOString();rf245Recovery({version:1,days:{[d250]:{sleepSessions:[{id:'sleep250',source:'com.sec.android.app.shealth',start:s0,end:s2,stages:[{start:s0,end:s1,type:4,isAsleep:true},{start:s1,end:s2,type:1,isAsleep:false}]}]}}});`);
 assert.ok(run('state.health.recovery.sleepMinutes')>=59&&run('state.health.recovery.sleepMinutes')<=61);
 assert.equal(run('state.health.recovery.sleepStageBased'),true);

 // Stable workout ID survives timestamp edits.
 run(`db.set('history',[{id:'stable250',started:${JSON.stringify(now)},finished:${JSON.stringify(later)},exercises:[]}]);rf250EnsureStableWorkoutIds();`);
 const stable=run('history()[0].healthStableId');assert.ok(stable.startsWith('tpw_'));
 run(`let z=history();z[0].started=new Date(Date.now()-5000000).toISOString();z[0].finished=new Date(Date.now()-4000000).toISOString();db.set('history',z);rf250EnsureStableWorkoutIds();`);
 assert.equal(run('history()[0].healthStableId'),stable);

 // Extended fields are retained in the common wellness model.
 run(`const day250=rf240DayKey(new Date());db.set('healthLedgerV1',{version:1,days:{[day250]:{bloodPressureSystolic:122,bloodPressureDiastolic:78,bloodPressureTime:new Date().toISOString(),bloodGlucoseMmolL:5.1,bloodGlucoseTime:new Date().toISOString(),respiratoryRate:14.5,respiratoryRateTime:new Date().toISOString()}},lastSyncAt:new Date().toISOString()});rf240ApplyLedger();`);
 assert.equal(run('state.health.wellness.bloodPressureSystolic'),122);assert.equal(run('state.health.wellness.bloodGlucoseMmolL'),5.1);assert.equal(run('state.health.wellness.respiratoryRate'),14.5);

 // Per-record-type change tokens: two granted types => two independent tokens and polls.
 run(`db.set('history',[]);db.set('healthSyncMetaV1',{});db.set('healthLedgerV1',{version:1,days:{},warnings:[]});rf245Paint=()=>{};let tokenTypes=[],polls=0,reads250=0;const perms250={READ_HEART_RATE:true,READ_SLEEP:true,WRITE_EXERCISE:false};const plugin250={getStatus:async()=>({permissions:perms250}),requestRead:async()=>({}),createChangeToken:async a=>{tokenTypes.push(a.recordType);return {token:'tok-'+a.recordType}},pollChanges:async a=>{polls++;return {nextToken:a.token+'-n',changedDays:[],deletedCount:0}},readHealthDay:async()=>{reads250++;return {warnings:[],sleepSessions:[]}},readTrainingWindow:async()=>({warnings:[],exerciseSessions:[]})};`);
 await run('rf250Sync({plugin:plugin250,manual:true})');
 assert.deepEqual([...run('tokenTypes')].sort(),['HeartRateRecord','SleepSessionRecord']);
 assert.equal(run('Object.keys(rf240Meta().tokensByType).length'),2);assert.equal(run('reads250'),30);
 await run('rf250Sync({plugin:plugin250,manual:true})');assert.equal(run('polls'),2);

 // One user-visible sync action: Health only; History has none and Coach points to Health.
 run("db.set('language','hu')");
 run("go('health')");
 assert.equal((html.match(/>Szinkronizálás<\/button>/g)||[]).length,1);
 run("go('history')");assert.equal((html.match(/>Szinkronizálás<\/button>/g)||[]).length,0);
 assert.ok(run("!!window.TrainPilot155PanelNavigation?.coachPanel"),'Coach must use the Round 3 panel navigation surface');
 assert.ok(!run("rf233CoachScreen.toString().includes(\"readRecoveryHealth().then\")"),'Coach must not run an independent recovery sync');

 const java=fs.readFileSync('android/app/src/main/java/com/repforge/app/HealthBridgePlugin.java','utf8');
 assert.ok(java.includes('SleepSessionRecord.Stage'));assert.ok(java.includes('isAsleep'));assert.ok(java.includes('clientRecordId'));
 assert.ok(java.includes('READ_BLOOD_PRESSURE')&&java.includes('READ_BLOOD_GLUCOSE')&&java.includes('READ_RESPIRATORY_RATE'));
 assert.ok(java.includes('addNamedType'));
 console.log('PASS 2.5.0: effort weights, sleep stages, stable export IDs, extended Health data, per-type tokens and single sync UI.');
}
tests().catch(e=>{console.error(e);process.exitCode=1});

