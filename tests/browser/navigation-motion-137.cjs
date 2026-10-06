'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((q,r)=>{const url=new URL(q.url,'http://local').pathname,file=path.join(root,url==='/'?'index.html':url);fs.readFile(file,(e,b)=>{if(e){r.writeHead(404);return r.end()}r.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');r.end(b)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));const browser=await chromium.launch({args:['--no-sandbox']});try{
 const p=await browser.newPage({viewport:{width:393,height:873},hasTouch:true,isMobile:true}),errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished&&window.TrainPilot137Motion);
 const initial=await p.evaluate(()=>JSON.stringify({history:history(),programs:programs(),scheduled:scheduled()}));
 const audit=async()=>p.evaluate(()=>[...document.querySelectorAll('.tp137-motion-group')].map(g=>{const m=g.querySelector(':scope > .tp137-selection'),b=g.querySelector('.tp137-selected'),r=m.getBoundingClientRect(),t=b.getBoundingClientRect();return {name:g.className,count:g.querySelectorAll(':scope > .tp137-selection').length,delta:Math.max(Math.abs(r.x-t.x),Math.abs(r.y-t.y),Math.abs(r.width-t.width),Math.abs(r.height-t.height)),label:b.innerText,aria:m.getAttribute('aria-hidden'),viewport:innerWidth,overflow:document.documentElement.scrollWidth-innerWidth}}));
 const settled=async()=>{await p.waitForFunction(()=>[...document.querySelectorAll('.tp137-selection')].every(e=>!e.getAnimations().some(a=>a.playState==='running')),{},{timeout:1500});for(const a of await audit()){assert.equal(a.count,1);assert.equal(a.aria,'true');assert.ok(a.delta<1,JSON.stringify(a));assert.ok(a.overflow<=1,JSON.stringify(a));assert.ok(a.label.length>0)}};
 await p.evaluate(()=>go('history'));
 const tabs=p.locator('.tp177-journal-tabs button'),tabBox=await p.locator('.tp177-journal-tabs').boundingBox();
 // Observe a real intermediate frame, then interrupt it. The new animation must
 // start at its current position, not at the last destination or the first tab.
 await tabs.nth(1).evaluate(b=>b.click());
 await p.locator('.tp177-journal-tabs .tp137-selection').evaluate(m=>{const a=m.getAnimations()[0];a.pause();a.currentTime=65});
 const midway=await p.locator('.tp177-journal-tabs .tp137-selection').boundingBox();assert.ok(midway.x>tabBox.x+10&&midway.x<tabBox.x+115,JSON.stringify(midway));
 const continuity=await p.evaluate(()=>{const before=document.querySelector('.tp177-journal-tabs .tp137-selection').getBoundingClientRect().x;document.querySelectorAll('.tp177-journal-tabs button')[2].click();return {before,after:document.querySelector('.tp177-journal-tabs .tp137-selection').getBoundingClientRect().x}});assert.ok(Math.abs(continuity.before-continuity.after)<2,JSON.stringify(continuity));await settled();
 for(let i=0;i<24;i++){await tabs.nth(i%3).evaluate(b=>b.click());await p.waitForTimeout(12)}await settled();assert.equal(await tabs.nth(2).getAttribute('aria-selected'),'true');
 assert.deepEqual(await p.locator('.tp177-journal-tabs').boundingBox(),tabBox,'tab bar remains stationary');
 await p.locator('.tp107-statistics-tabs button').nth(1).click();await settled();assert.equal(await p.locator('.tp107-cardio').count(),1);
 await p.locator('.tp107-statistics-tabs button').nth(0).click();await settled();assert.equal(await p.locator('.tp107-personal-records').count(),1);
 for(const [width,height] of [[320,740],[393,873],[873,393]])for(const lang of ['hu','en','de','ro'])for(const theme of ['yellow','classicBlue','green']){
  await p.setViewportSize({width,height});await p.evaluate(({lang,theme})=>{db.set('language',lang);rf200SetTheme(theme);go('history')},{lang,theme});await tabs.nth(1).click();await p.locator('.tp177-periods button[data-period="7d"]').click();await settled();assert.equal(await p.locator('.tp177-chart-column').count(),7);await tabs.nth(2).click();await p.locator('.tp107-statistics-tabs button').nth(1).click();await settled();
 }
 // Main navigation also moves between rows, and overlay toggles retain their
 // existing behavior instead of being replaced with fake tabs.
 await p.evaluate(()=>go('home'));await settled();await p.locator('.tp154-coach-action').click();await settled();assert.equal(await p.locator('#tp155R4PanelHost[data-panel="coach"]').count(),1);await p.locator('.tp154-coach-action').click();await settled();assert.equal(await p.locator('#tp155R4PanelHost').count(),0);
 await p.emulateMedia({reducedMotion:'reduce'});await p.evaluate(()=>go('history'));await tabs.nth(1).evaluate(b=>b.click());assert.equal(await p.locator('.tp137-selection').evaluateAll(es=>es.flatMap(e=>e.getAnimations()).length),0);for(const a of await audit())assert.ok(a.delta<1,JSON.stringify(a));
 await p.emulateMedia({reducedMotion:'no-preference'});await tabs.nth(2).evaluate(b=>b.click());await p.setViewportSize({width:412,height:915});for(const a of await audit())assert.ok(a.delta<1,'resize settles immediately '+JSON.stringify(a));
 // A long Journal must not create one animation/compositor surface per row.
 // This reproduces the phone lag structurally, without timing thresholds that
 // depend on the CI runner or claiming desktop measurements are Android FPS.
 await p.evaluate(()=>{
  window.tp137SavedHistory=history();
  db.set('history',Array.from({length:240},(_,i)=>{const id='motion-large-'+i,t=Date.now()-i*86400000;return {id,healthStableId:'tpw_'+rf250Hash(id),programId:activeProgramId(),dayId:'A',programName:'Performance fixture',started:new Date(t-1800000).toISOString(),finished:new Date(t).toISOString(),exercises:[]}}));go('home');
 });await settled();await p.evaluate(()=>document.fonts.ready);
 const budget=await p.evaluate(()=>{
  const base=Element.prototype.animate,result={markers:0,surfaces:0,layoutFrames:0};
  Element.prototype.animate=function(frames,options){
   if(this.matches('.tp137-selection')){result.markers++;if(frames.some(f=>Object.keys(f).some(k=>!['transform','offset','easing','composite'].includes(k))))result.layoutFrames++;}
   else result.surfaces++;
   return base.call(this,frames,options);
  };
  try{go('history');return {...result,entries:document.querySelectorAll('details.rf263-history').length}}finally{Element.prototype.animate=base;}
 });
 assert.equal(budget.entries,240,'exercise a genuinely large rendered Journal');assert.equal(budget.markers,1,'one selection animation per route transaction');assert.equal(budget.surfaces,1,'one content animation regardless of history length');assert.equal(budget.layoutFrames,0,'selection changes only transform, never per-frame width/height');await settled();
 await p.evaluate(()=>{db.set('history',tp137SavedHistory);delete window.tp137SavedHistory;go('home')});
 assert.equal(await p.evaluate(()=>JSON.stringify({history:history(),programs:programs(),scheduled:scheduled()})),initial,'navigation never changes workout data');assert.deepEqual(errors,[]);
 console.log('PASS #137: real intermediate motion, continuous interruption, 24 rapid switches, stationary tabs, records/cardio/period content, portrait/landscape × four languages × three themes, main navigation and overlay toggles, reduced motion, resize, unchanged workout data, bounded surfaces with 240 Journal entries');
}finally{await browser.close();server.close()}})().catch(e=>{console.error(e);process.exitCode=1;server.close()});
