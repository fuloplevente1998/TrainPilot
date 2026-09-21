const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
const values=new Map();let writes=0,html='';const root={get innerHTML(){return html},set innerHTML(v){writes++;html=v}};
const node=()=>({innerHTML:'',style:{setProperty(){}},classList:{add(){},remove(){}},querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){}});
const doc={querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node,head:node(),body:node(),documentElement:node()};
let tasks=[],cloudChecks=0;const errors=[];const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:{status:async()=>{cloudChecks++;return {profile:null}}}}},addEventListener:(name,fn)=>{if(name==='error')errors.push(fn)},scrollTo(){},open(){}},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{if(!ms)tasks.push(fn);return 1},clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert(){},confirm:()=>true,prompt:()=>null,console});
const index=fs.readFileSync('www/index.html','utf8');
const order=[];
for(const [,attrs,inline] of index.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)){
 const src=attrs.match(/src="([^"]+)"/);if(src){order.push(src[1]);if(src[1]!=='startup.js')assert.ok(attrs.includes('defer'),'feature scripts must defer');}
 vm.runInContext(src?fs.readFileSync('www/'+src[1],'utf8'):inline,ctx,{filename:src?.[1]||'inline'});
 if(src&&src[1]!=='ready.js')assert.equal(writes,0,'no partial screen before the final layer');
 assert.equal(cloudChecks,0,'cloud must not block initial UI');
}
const run=s=>vm.runInContext(s,ctx);
async function tests(){
 assert.equal(run('makeBackup().appVersion'),'1.4.6');
 run('state.health={recovery:{sleepMinutes:null,hrvRmssdMs:null}};db.set("history",[])');
 assert.equal(run('rf220Readiness().sleep'),null);
 assert.equal(run('rf220Readiness().score'),null);
 run('state.health.recovery={day:"2020-01-01",sleepMinutes:480,hrvRmssdMs:50,hrvBaselineMs:50}');
 assert.equal(run('rf220Readiness().score'),null);
 assert.ok(run('rf220WorkoutTime({started:"2026-01-01T10:00:00Z",finished:"2026-01-01T11:00:00Z"})')>0);
 run('state.health.recovery={sleepEnd:new Date().toISOString(),hrvTime:new Date().toISOString(),sleepMinutes:480,hrvRmssdMs:50,hrvBaselineMs:50}');
 assert.equal(run('rf220Readiness().score'),85);
 run('state.health.recovery.sleepEnd=new Date(Date.now()+86400000).toISOString();state.health.recovery.hrvTime=state.health.recovery.sleepEnd');
 assert.equal(run('rf220Readiness().score'),null);

 run(`const testDay=rf240DayKey(new Date());const testStart=new Date(Date.now()-3600000).toISOString();const testEnd=new Date(Date.now()-1000).toISOString();
 const testLedger={version:1,days:{[testDay]:{sleepSessions:[{id:'a',source:'com.sec.android.app.shealth',start:testStart,end:testEnd},{id:'b',source:'other',start:testStart,end:testEnd},{id:'c',source:'com.sec.android.app.shealth',start:testStart,end:testEnd}],hrvRmssdMs:50,hrvTime:testEnd}}};rf245Recovery(testLedger);`);
 assert.ok(run('state.health.recovery.sleepMinutes')<61,'duplicate providers and overlapping sessions must not multiply sleep');
 run('testLedger.days[testDay].sleepSessions=[];testLedger.days[testDay].hrvRmssdMs=null;rf245Recovery(testLedger)');
 assert.equal(run('state.health.recovery.sleepMinutes'),undefined,'deleted recovery must not survive');

 run(`rf245Paint=()=>{};let nativeReads=0,writes245=0,tokenCalls=0,failDay=false,watchSession=false;
 const perms245={READ_SLEEP:true,READ_HEART_RATE_VARIABILITY:true,READ_EXERCISE:true,WRITE_EXERCISE:true};
 const plugin245={getStatus:async()=>({permissions:perms245}),requestRead:async()=>({}),requestWrite:async()=>({}),
 createChangeToken:async()=>{tokenCalls++;return {token:'initial'}},pollChanges:async()=>({nextToken:'next',changedDays:[rf240RecentDays(5)[4]]}),
 readHealthDay:async()=>{nativeReads++;if(failDay)throw Error('test failure');return {warnings:[],sleepSessions:[],hrvRmssdMs:null}},
 readTrainingWindow:async()=>({warnings:[],exerciseSessionCount:1,exerciseSessions:watchSession?[{source:'watch',start:testStart,end:testEnd}]:[],averageHeartRate:90}),
 writeWorkout:async()=>{writes245++}};
 db.set('history',[{id:'test245',started:testStart,finished:testEnd,exercises:[],health240:{windowStart:testStart,windowEnd:testEnd,syncedAt:new Date().toISOString(),exerciseSessionCount:1}}]);
 db.set('healthSyncMetaV1',{});`);
 const a=run('rf245Sync({plugin:plugin245,manual:true})');const b=run('rf245Sync({plugin:plugin245,manual:true})');assert.equal(a,b,'double click shares one flight');await a;
 assert.equal(run('nativeReads'),30);assert.equal(run('writes245'),1);
 assert.equal(run('history()[0].health240.averageHeartRate'),90,'manual sync refreshes partial workout immediately');
 await run('rf245Sync({plugin:plugin245,manual:true})');assert.equal(run('writes245'),1,'repeat does not export again');
 run('failDay=true');const partial=await run('rf245Sync({plugin:plugin245,manual:true})');assert.equal(partial.status,'partial');assert.ok(run("Object.values(rf240Meta().tokensByType||{}).length>0&&Object.values(rf240Meta().tokensByType||{}).every(x=>x==='next')"),'per-record-type tokens remain on the last successful next token');
 run('failDay=false;watchSession=true;db.set("healthExports245",{})');await run('rf245Sync({plugin:plugin245,manual:true})');assert.equal(run('writes245'),1,'watch session blocks duplicate export');
 run('watchSession=false;delete perms245.READ_EXERCISE');await run('rf245Sync({plugin:plugin245,manual:true})');assert.equal(run('writes245'),1,'cannot export without checking existing sessions');
 run('perms245.READ_EXERCISE=true;db.set("healthExports245",{})');await run('rf245Sync({plugin:plugin245,manual:false})');assert.equal(run('writes245'),1,'background never starts a new export');
 assert.equal(run('state.health.busy'),false);
 // A click during a background run must still perform the manual forced/export pass.
 run('db.set("healthExports245",{});nativeReads=0;');
 const background=run('rf245Sync({plugin:plugin245,manual:false})');
 const manual=run('rf245Sync({plugin:plugin245,manual:true})');
 assert.notEqual(background,manual);await Promise.all([background,manual]);
 assert.equal(run('writes245'),2,'manual request is not swallowed by a background flight');
 // A brand-new empty workout must not query a padded window in the future.
 run(`let futureQueries=0;const emptyPlugin={readTrainingWindow:async args=>{if(Date.parse(args.end)>Date.now())futureQueries++;return {exerciseSessions:[],warnings:[]}}};`);
 await run('rf245ReadWorkout(emptyPlugin,history()[0],true)');
 assert.equal(run('futureQueries'),0);
 run("db.set('language','hu')");run('go("health")');assert.ok(/>Szinkronizálás<\/button>/.test(html));assert.ok(!html.includes('null/100'));
 console.log('PASS legacy 2.4.5 behavior on current runtime: actual runtime, missing/stale/future inputs, recovery dedup/deletion, shared flight, forced refresh, incremental retry, export idempotency and watch/permission guards.');
}
tests().catch(e=>{console.error(e);process.exitCode=1});

