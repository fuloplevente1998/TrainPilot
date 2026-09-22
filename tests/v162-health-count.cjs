const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
const values=new Map();let html='';const root={get innerHTML(){return html},set innerHTML(v){html=v}};
const node=()=>({innerHTML:'',style:{setProperty(){}},classList:{add(){},remove(){}},querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){}});
const doc={querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node,head:node(),body:node(),documentElement:node()};
const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:{status:async()=>({profile:null})}}},addEventListener(){},scrollTo(){},open(){}},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{if(!ms)fn();return 1},clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert(){},confirm:()=>true,prompt:()=>null,console});
const index=fs.readFileSync('www/index.html','utf8');
for(const [,attrs,inline] of index.matchAll(/<script([^>]*)>([\s\S]*?)<\/script>/g)){const src=attrs.match(/src="([^"]+)"/);vm.runInContext(src?fs.readFileSync('www/'+src[1],'utf8'):inline,ctx,{filename:src?.[1]||'inline'});}
const run=s=>vm.runInContext(s,ctx);

run(`db.set('history',[
 {started:'2026-09-22T08:00:00.000Z',finished:'2026-09-22T08:10:00.000Z',healthStableId:'tpw_kept'},
 {started:'2026-09-22T09:00:00.000Z',finished:'2026-09-22T09:10:00.000Z'}
])`);
run(`state.health.daily={
 exerciseSessions:[
  {source:'com.repforge.app',start:'2026-09-22T08:00:00.000Z',end:'2026-09-22T08:10:00.000Z',durationMinutes:10,clientRecordId:'trainpilot:tpw_kept'},
  {source:'com.repforge.app',start:'2026-09-22T08:01:00.000Z',end:'2026-09-22T08:11:00.000Z',durationMinutes:10,clientRecordId:'trainpilot:tpw_deleted'},
  {source:'com.repforge.app',start:'2026-09-22T09:00:00.500Z',end:'2026-09-22T09:10:00.500Z',durationMinutes:10},
  {source:'com.repforge.app',start:'2026-09-22T09:02:00.000Z',end:'2026-09-22T09:12:00.000Z',durationMinutes:10},
  {source:'com.samsung.android.wearable',start:'2026-09-22T10:00:00.000Z',end:'2026-09-22T10:20:00.000Z',durationMinutes:20}
 ],exerciseSessionCount:5,exerciseMinutes:60
}`);
const result=run('rf223Daily()');
assert.equal(result.exerciseSessionCount,3,'deleted and nearby stale TrainPilot sessions must be hidden');
assert.equal(result.exerciseMinutes,40,'minutes must be recomputed from visible sessions');
assert.deepEqual(Array.from(result.exerciseSessions,s=>s.clientRecordId||s.source),['trainpilot:tpw_kept','com.repforge.app','com.samsung.android.wearable']);
const java=fs.readFileSync('android/app/src/main/java/com/repforge/app/HealthBridgePlugin.java','utf8');
assert.ok(java.includes('x.put("clientRecordId",r.getMetadata().getClientRecordId())'));
console.log('PASS 1.6.2 Health count: stable IDs, legacy exact match, external sessions and visible minutes');
