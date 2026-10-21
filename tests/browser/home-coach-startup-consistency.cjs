const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
const snap=page=>page.evaluate(()=>{const c=document.querySelector('#rf220CoachCard');return c?{cls:c.className,text:(c.innerText||'').replace(/\s+/g,' ').trim(),kicker:!!c.querySelector('.tp151-kicker'),copy:!!c.querySelector('.tp166-home-coach-copy'),readiness:!!c.querySelector('.tp167-home-coach-readiness'),legacyHead:!!c.querySelector('.rf221-coach-head'),legacyLine:!!c.querySelector('.rf221-coach-line')}:null});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({headless:true,args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await p.addInitScript(()=>{
  localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
  localStorage.setItem('repforge:history',JSON.stringify(Array.from({length:200},(_,i)=>({id:'boot-'+i,programId:'home-basic',dayId:i%2?'A':'B',started:new Date(Date.now()-(i+1)*86400000).toISOString(),finished:new Date(Date.now()-(i+1)*86400000+3600000).toISOString(),exercises:[{id:'db-squat',loadType:'per_hand',repUnit:'ism.',sets:[{set:1,reps:'10',weight:8,done:true}]}]}))));
  window.__bootReads={history:0,writes:0,beforeDCL:null,first:null};
  const get=Storage.prototype.getItem,html=Object.getOwnPropertyDescriptor(Element.prototype,'innerHTML');
  Storage.prototype.getItem=function(key){if(key==='repforge:history')__bootReads.history++;return get.call(this,key);};
  Object.defineProperty(Element.prototype,'innerHTML',{...html,set(value){
   if(this.id==='app'){
    __bootReads.writes++;
    __bootReads.first??={scope:window.TrainPilotPhase7Performance?.historySnapshot,history:__bootReads.history};
   }
   return html.set.call(this,value);
  }});
  document.addEventListener('DOMContentLoaded',()=>{__bootReads.beforeDCL={history:__bootReads.history,writes:__bootReads.writes};},{once:true});
 });await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotHomeCoachStartup?.version==='1');await p.waitForTimeout(180);
 const assertStartup=async()=>{
  const reads=await p.evaluate(()=>__bootReads);
  assert.equal(reads.beforeDCL.writes,0,'first render waits for final modules instead of constructing an uncached legacy screen');
  assert.equal(reads.writes,1,'cold startup renders the app root once');
  assert.equal(reads.first.scope,'operation-scoped','first root write already uses the shared read scope');
  assert.ok(reads.first.history-reads.beforeDCL.history<=1,'first render reads the journal at most once: '+JSON.stringify(reads));
  assert.ok(reads.history-reads.beforeDCL.history<=1,'all startup modules share one parsed journal snapshot: '+JSON.stringify(reads));
  const before=await p.evaluate(()=>localStorage.getItem('repforge:history'));
  const repair=await p.evaluate(()=>{
   const card=document.querySelector('#rf220CoachCard'),html=card.innerHTML,reads=__bootReads.history;
   TrainPilotHomeCoachStartup.decorate();TrainPilotHomeCoachStartup.decorate();
   const unchanged=card.innerHTML===html&&__bootReads.history===reads;
   card.innerHTML='<div class="rf221-coach-head">Legacy</div>';TrainPilotHomeCoachStartup.decorate();
   return {unchanged,repaired:!!card.querySelector('.tp166-home-coach-copy')&&!card.querySelector('.rf221-coach-head')};
  });assert.equal(repair.unchanged,true,'repair callbacks leave an already final card and its journal alone');assert.equal(repair.repaired,true,'an actual late legacy repaint is still repaired');
  assert.equal(await p.evaluate(()=>localStorage.getItem('repforge:history')),before,'boot and legacy repair preserve all workouts');
 };
 await assertStartup();
 const boot=await snap(p);assert.ok(boot,'startup Home Coach missing');assert.match(boot.cls,/tp166-home-coach/);assert.equal(boot.kicker,true,'startup must already show accepted Mai javaslat card');assert.equal(boot.copy,true);assert.equal(boot.readiness,true);assert.equal(boot.legacyHead,false,'legacy compact Coach must not repaint after boot');assert.equal(boot.legacyLine,false);
 await p.evaluate(()=>go('health'));await p.waitForTimeout(60);await p.evaluate(()=>go('home'));await p.waitForTimeout(180);
 const back=await snap(p);assert.ok(back);assert.match(back.cls,/tp166-home-coach/);assert.equal(back.kicker,true);assert.equal(back.copy,true);assert.equal(back.readiness,true);assert.equal(back.legacyHead,false);assert.equal(back.legacyLine,false);
 assert.equal(back.text,boot.text,'Home Coach content/structure must be identical at startup and after route roundtrip');
 for(const width of [320,360,412]){
  await p.setViewportSize({width,height:873});await p.reload();await p.waitForFunction(()=>TrainPilotBoot?.finished);await p.waitForTimeout(180);await assertStartup();
 }
 console.log('PASS: cold startup uses final modules, one root render and one scoped journal read at 320/360/393/412px; final Coach repair is a no-op, real legacy repaint recovers, data and route roundtrip stay identical.');
}finally{if(b)await b.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
