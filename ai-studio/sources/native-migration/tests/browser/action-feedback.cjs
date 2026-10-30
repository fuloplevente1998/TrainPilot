'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});
});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));
  await page.clock.install({time:new Date('2026-10-09T12:00:00+02:00')});
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);
  const toast=page.locator('#tp153Toast.show'),inline=page.locator('[data-tp110-replan-status]');
  for(const width of [320,360,393,412])for(const lang of ['hu','en','de','ro'])for(const theme of ['classicBlue','blue']){
   await page.setViewportSize({width,height:873});
   await page.evaluate(({lang,theme})=>{
    tp155R4ClosePanel(false);state.session=null;db.set('draft',null);db.set('history',[]);db.set('language',lang);rf200SetTheme(theme);
    db.set('scheduled',[{...makeScheduleItem(activeProgram(),'2026-10-10','18:00',45,'A'),id:'feedback-plan'}]);go('calendar');
   },{lang,theme});
   await page.locator('.tp110-replan-link').click();await page.locator('[data-tp110-apply]').click();
   assert.equal(await toast.textContent(),await page.evaluate(()=>TrainPilot110.t('replanned')));
   assert.equal(await inline.textContent(),'','no duplicate green success text');
   assert.equal(await inline.getAttribute('data-kind'),'success');
   assert.equal(await toast.getAttribute('role'),'status');assert.equal(await toast.getAttribute('aria-live'),'polite');
   const box=await toast.boundingBox();assert.ok(box.x>=0&&box.x+box.width<=width+1&&box.y>=0&&box.y+box.height<=873);
   assert.equal(await toast.evaluate(e=>getComputedStyle(e).pointerEvents),'none');
   if(width===393&&lang==='hu'&&theme==='classicBlue'){fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/action-feedback-replan.png'});}
   await page.clock.runFor(5500);assert.equal(await page.locator('#tp153Toast').count(),0,'success expires');
   const before=await page.evaluate(()=>scheduled());
   await page.evaluate(()=>{window.__feedbackSet=db.set;db.set=(key,value)=>{if(key==='scheduled')throw Error('quota');return window.__feedbackSet(key,value);};});
   await page.locator('[data-tp110-apply]').click();assert.equal(await inline.getAttribute('data-kind'),'error');assert.ok((await inline.textContent()).length>0);
   assert.equal(await toast.count(),0,'failed storage must not show success');assert.deepEqual(await page.evaluate(()=>scheduled()),before);
   await page.evaluate(()=>db.set=window.__feedbackSet);
   await page.evaluate(()=>TrainPilotAndroidBack());
   await page.evaluate(()=>rf209DeleteSchedule('feedback-plan'));await page.locator('[data-tp2628-cancel]').click();
   assert.equal(await page.evaluate(()=>scheduled()[0].cancelled),false);assert.equal(await toast.count(),0,'cancel is not success');
   await page.evaluate(()=>rf209DeleteSchedule('feedback-plan'));await page.locator('[data-tp2628-confirm]').click();
   assert.equal(await page.evaluate(()=>scheduled()[0].cancelled),true);assert.equal(await toast.textContent(),({hu:'Törölve.',en:'Deleted.',de:'Gelöscht.',ro:'Șters.'})[lang]);
   await page.clock.runFor(5500);assert.equal(await toast.count(),0);
   const pure=await page.evaluate(()=>{
    const base=render;let calls=0;render=function(){calls++;return base.apply(this,arguments);};
    for(let i=0;i<20;i++)TrainPilotFeedback.saved();render=base;
    return {calls,hosts:document.querySelectorAll('#tp153Toast').length};
   });assert.deepEqual(pure,{calls:0,hosts:1},'one toast host and no full-page redraw');
   await page.evaluate(()=>{render();tp155R4RefreshPanel();});assert.equal(await toast.count(),1,'toast survives redraw');
   await page.clock.runFor(5500);assert.equal(await page.locator('#tp153Toast').count(),0);
   await page.evaluate(()=>alert(tp149T('planner.saveFailed')));await page.locator('#tp2628Dialog').waitFor();assert.equal(await toast.count(),0);
   await page.locator('[data-tp2628-ok]').click();
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);
  }
  await page.evaluate(()=>{state.session=null;db.set('draft',null);TrainPilotFeedback.afterReload('Törölve.');});
  await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);
  assert.equal(await toast.textContent(),'Törölve.','local erase feedback survives the required reload');
  assert.equal(await page.evaluate(()=>sessionStorage.getItem('trainpilot:actionFeedback')),null,'receipt is consumed once');
  await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);assert.equal(await toast.count(),0,'feedback is not repeated on another launch');
  assert.deepEqual(errors,[]);
  console.log('PASS action feedback: 32 width/language/theme cases; real replan save and confirmed/cancelled deletion, no duplicate inline success, storage failure retains data/errors, single live toast, expiry, redraw, no extra render and error dialog');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
