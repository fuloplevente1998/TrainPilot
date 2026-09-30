const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const values=new Map();let html='';const root={get innerHTML(){return html},set innerHTML(v){html=v}};
const node=()=>({innerHTML:'',style:{setProperty(){}},classList:{add(){},remove(){}},querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){}});
const document={querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node,head:node(),body:node(),documentElement:node()};
const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:{status:async()=>({profile:null})}}},addEventListener(){},scrollTo(){},open(){}},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{if(!ms)fn();return 1},clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert(){},confirm:()=>true,prompt:()=>null,console});
for(const [,attrs,inline] of fs.readFileSync('www/index.html','utf8').matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)){
 const src=attrs.match(/src="([^"]+)"/);vm.runInContext(src?fs.readFileSync('www/'+src[1],'utf8'):inline,ctx,{filename:src?.[1]||'inline'});
}
const run=s=>vm.runInContext(s,ctx);
// DOMContentLoaded is deliberately not fired by this VM. Load the shipped inline
// Journal callback verbatim so the test exercises the same final button handler.
const index=fs.readFileSync('www/index.html','utf8');
vm.runInContext(index.slice(index.indexOf(' const tp6HistoryHealthFlights='),index.indexOf(' window.healthFromHistory=healthFromHistory;')+' window.healthFromHistory=healthFromHistory;'.length),ctx);
(async()=>{
 const finish=Date.now()-30*60000,start=finish-26*3600000;
 const ws=[{start:new Date(start).toISOString(),end:new Date(start+15*60000).toISOString()},{start:new Date(finish-(85*60+33)*1000).toISOString(),end:new Date(finish).toISOString()}];
 const row={id:'overnight-routing',started:ws[0].start,finished:ws[1].end,activeIntervals:ws,exercises:[]};
 const calls=[],writes=[];
 const read=async w=>{
  calls.push(w);assert.ok(Date.parse(w.end)-Date.parse(w.start)<=24*3600000,'native bridge rejects a window longer than 24 hours');
  assert.ok(ws.some(s=>s.start===w.start&&s.end===w.end),'query must exclude the pause and tolerance padding');
  return {averageHeartRate:120,minHeartRate:90,maxHeartRate:150,heartRateSamples:10,activeCalories:40,totalCalories:null,distanceMeters:null,exerciseSessions:[],exerciseSessionCount:0,warnings:[]};
 };
 ctx.plugin79={readWorkout:read,readTrainingWindow:read,writeWorkout:async w=>writes.push(w),getStatus:async()=>({permissions:{READ_HEART_RATE:true,READ_EXERCISE:true,WRITE_EXERCISE:true}}),requestRead:async()=>({}),readHealthDay:async()=>({warnings:[],exerciseSessions:[],sleepSessions:[]}),createChangeToken:async()=>({token:'79-token'}),pollChanges:async()=>({nextToken:'79-token',changedDays:[]})};
 run(`db.set('history',[${JSON.stringify(row)}]);healthPlugin=()=>plugin79;rfHistoryHealthCache.clear();rf245Paint=()=>{};`);
 // Actual inline Journal refresh must share the interval implementation.
 await run('healthFromHistory(0)');
 assert.deepEqual(calls.map(w=>[w.start,w.end]),ws.map(w=>[w.start,w.end]));
 assert.equal(run('rfHistoryHealthCache.get(history()[0].started).summary.activeCalories'),80);
 assert.equal(run('rfHistoryHealthCache.get(history()[0].started).summary.totalCalories'),null);
 assert.equal(run('rfHistoryHealthNumber(null)'),null,'unknown Health metrics must not become measured zero');
 assert.equal(run('rfHistoryHealthCache.get(history()[0].started).summary.heartRateSamples'),20);
 // Unified Health sync uses rf245ReadWorkout, which previously captured the old function.
 calls.length=0;
 const report=await run('rf250Sync({plugin:plugin79,manual:true})');
 assert.deepEqual([...report.errors],[]);
 assert.equal(run('history()[0].health240.activeCalories'),80);
 assert.equal(run('history()[0].health240.activeDurationMs'),(100*60+33)*1000);
 assert.equal(writes.length,2,'manual Health export records each active segment');
 assert.deepEqual(writes.map(w=>[w.start,w.end]),ws.map(w=>[w.start,w.end]));
 assert.notEqual(writes[0].clientRecordId,writes[1].clientRecordId);
 await run('rf250Sync({plugin:plugin79,manual:true})');
 assert.equal(writes.length,2,'unchanged segments must not be exported twice');
 console.log('PASS #79 shipped Journal refresh + unified sync + per-segment export across a >24h pause');
})().catch(e=>{console.error(e);process.exitCode=1});
