'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);
  await page.evaluate(()=>{
   const now=new Date(),days={};for(let i=6;i>=0;i--){const d=new Date(now);d.setDate(now.getDate()-i);days[rf240DayKey(d)]={averageHeartRate:70+i,restingHeartRate:54,maxHeartRate:140,minHeartRate:48,steps:5000};}
   const ledger={version:1,days,lastSyncAt:now.toISOString()};rf240Ledger=()=>ledger;db.set('weights',[{kg:65,date:new Date(+now-86400000).toISOString()},{kg:66,date:now.toISOString()}]);
   window.__healthFrames=[];window.__healthRecording=false;window.__healthPhase='';
   const audit=kind=>{
    const main=document.querySelector('#app main.rf263-health');if(!__healthRecording||!main)return;
    const pulse=main.querySelector('.tp169-pulse-journal');
    const bare=[...main.querySelectorAll('.tp168-chevron,.tp169-pulse-summary>b,.tp168-weight-summary>b')].filter(e=>!e.classList.contains('tp-global-chevron'));
    const valid=main.classList.contains('tp168-health')&&!!pulse?.querySelector('.tp5-pulse-trend-section')&&!!pulse?.querySelector('.tp5-pulse-today')&&bare.length===0;
    __healthFrames.push({kind,phase:__healthPhase,valid,bare:bare.length,trend:!!pulse?.querySelector('.tp5-pulse-trend-section')});
   };
   window.__healthAudit=audit;
   // Every completed timer task can be followed by a paint on a busy WebView.
   // Inspect those boundaries, rather than waiting for a final settled snapshot.
   const timeout=window.setTimeout;window.setTimeout=function(callback,delay,...args){if(typeof callback!=='function')return timeout(callback,delay,...args);return timeout(function(...values){try{return callback(...values)}finally{audit('timer');}},delay,...args);};
   new MutationObserver(()=>audit('mutation')).observe(document.getElementById('app'),{childList:true,subtree:true,attributes:true});
  });
  for(const [width,height] of [[320,740],[360,800],[393,873],[412,915]])for(const lang of ['hu','en','de','ro'])for(const theme of ['classicBlue','blue']){
   await page.setViewportSize({width,height});await page.evaluate(({lang,theme})=>{__healthRecording=false;rf240Ledger().days[rf240DayKey(new Date())].averageHeartRate=76;tp155R4ClosePanel(false);state.session=null;db.set('draft',null);db.set('language',lang);rf200SetTheme(theme);TrainPilot168Health.pulseOpen=false;TrainPilot168Health.weightOpen=false;go('home');},{lang,theme});await page.waitForTimeout(40);
   await page.evaluate(()=>{__healthFrames=[];__healthPhase='first-open';__healthRecording=true;});
   await page.locator(".top .tp151-nav-item[onclick=\"go('health')\"]").click();
   await page.evaluate(()=>__healthAudit('synchronous'));
   await page.evaluate(()=>new Promise(resolve=>{let count=0;const frame=()=>{__healthAudit('paint');if(++count===10)resolve();else requestAnimationFrame(frame);};requestAnimationFrame(frame);}));
   for(const action of ['repeat-open','refresh','async-refresh','weight-range']){
    await page.evaluate(action=>{__healthPhase=action;if(action==='repeat-open')[...document.querySelectorAll('.top .tp151-nav-item')].find(e=>e.getAttribute('onclick')==="go('health')").click();else if(action==='refresh')render();else if(action==='async-refresh')setTimeout(()=>{rf240Ledger().days[rf240DayKey(new Date())].averageHeartRate=83;render();},0);else tp168SetWeightRange('30');__healthAudit('synchronous');},action);
    await page.waitForTimeout(80);
    if(action==='async-refresh')assert.equal(await page.locator('.tp5-pulse-day:last-child strong').textContent(),'83','async data refresh reaches the final seven-day trend');
   }
   const frames=await page.evaluate(()=>{__healthRecording=false;return __healthFrames;});
   assert.ok(frames.some(f=>f.kind==='paint')&&frames.some(f=>f.kind==='timer'),'observe real render frames and deferred task boundaries');
   assert.deepEqual(frames.filter(f=>!f.valid),[],'Health must be complete at every potential paint '+width+'/'+lang+'/'+theme);
   assert.equal(await page.locator('.tp169-pulse-journal').count(),1);assert.equal(await page.locator('.tp5-pulse-day').count(),7);
   await page.locator('.tp169-pulse-summary').click();assert.equal(await page.locator('.tp169-pulse-journal.tp169-open').count(),1,'pulse disclosure still works');
   await page.evaluate(()=>{go('home');go('health');});await page.waitForTimeout(40);assert.equal(await page.locator('.tp5-pulse-trend-section').count(),1);
   if(width===393&&lang==='hu'&&theme==='classicBlue'){fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/health-first-frame.png'});}
  }
  assert.deepEqual(errors,[]);console.log('PASS Health first frame: 32 phone/language/theme cases, first/repeated navigation, render/async data refresh, weight range, timer/mutation/actual animation-frame boundaries, final pulse/chevrons only and retained disclosure/day semantics');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
