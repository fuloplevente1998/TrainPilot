'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));fs.mkdirSync('ui-evidence',{recursive:true});
  await page.clock.install({time:new Date('2026-10-04T20:30:00+02:00')});
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);
  const host=page.locator('#tp155R4PanelHost'),button=page.locator('.tp110-replan-link'),apply=page.locator('[data-tp110-apply]');
  const fixture=await page.evaluate(()=>{
   const p=activeProgram(),at=offset=>{const d=new Date();d.setDate(d.getDate()+offset);return localDateKey(d);};
   const rows=Array.from({length:6},(_,i)=>({...makeScheduleItem(p,at(i-3),'08:00',45,p.days[i%p.days.length].id),id:'calendar123-'+i}));
   // All missed rows are still planned; history, completed rows and draft stay protected.
   rows[0].status='completed';db.set('scheduled',rows);
   const history=[{id:'history123',scheduleId:rows[1].id,started:rows[1].start,finished:rows[1].end,exercises:[{...byId('db-floor-press'),sets:[{done:true,reps:10}]}]}];
   db.set('history',history);db.set('draft',{session:{scheduleId:rows[5].id}});state.session=null;
   db.set('plannerSettings',{...plannerSettings(),mode:'alternate'});go('home');return {rows,history};
  });
  for(const [width,height] of [[320,568],[393,873],[873,393]])for(const lang of ['hu','en','de','ro']){
   await page.setViewportSize({width,height});
   await page.evaluate(lang=>{tp155R4ClosePanel(false);db.set('language',lang);state.calendarMonth='2026-08';state.rf2211CalendarDate='2026-08-18';go('calendar');},lang);
   const bounds=await button.boundingBox();assert.ok(bounds.height>=44&&bounds.width>=44,'visible touch target '+width+'/'+lang+' '+JSON.stringify(bounds));
   assert.equal(await button.isEnabled(),true);assert.equal(await button.evaluate(e=>e.classList.contains('secondary')),false);
   await button.scrollIntoViewIfNeeded();const calendarScroll=await page.locator('#tp155R4PanelHost .tp155-r4-panel').evaluate(e=>e.scrollTop);
   await button.click();assert.equal(await host.getAttribute('data-panel'),'replan');
   assert.equal(await page.locator('.tp110-replan-row').count(),3,'multiple overdue planned rows, today elapsed, and future tail need no manual skip');
   assert.equal(await apply.isEnabled(),true);assert.deepEqual(await page.evaluate(()=>scheduled()),fixture.rows,'opening preview does not save');
   const title=await page.locator('.tp106-builder-header').boundingBox(),close=await page.locator('.tp106-builder-header button').boundingBox();
   assert.ok(title.height<65,'compact title '+width+'/'+lang+' '+JSON.stringify(title));assert.ok(close.y>=0&&close.y+close.height<=height,JSON.stringify({width,height,lang,title,close}));
   await apply.scrollIntoViewIfNeeded();const action=await apply.boundingBox();assert.ok(action.y+action.height<=height+1,'apply reachable');
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);
   if(width===393&&lang==='hu')await page.screenshot({path:'ui-evidence/calendar-replan-123.png'});
   await page.locator('.tp106-builder-header button').click();assert.equal(await host.getAttribute('data-panel'),'calendar');
   assert.deepEqual(await page.evaluate(()=>[state.calendarMonth,state.rf2211CalendarDate]),['2026-08','2026-08-18']);
   assert.equal(await button.evaluate(e=>e===document.activeElement),true,'focus returns to current Calendar button');
   assert.ok(Math.abs(await page.locator('#tp155R4PanelHost .tp155-r4-panel').evaluate(e=>e.scrollTop)-calendarScroll)<=1,'calendar scroll is restored');
   await button.click();await page.keyboard.press('Escape');assert.equal(await host.getAttribute('data-panel'),'calendar');
   await button.click();assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true);assert.equal(await host.getAttribute('data-panel'),'calendar');
   // Clicking the backdrop is a one-step return too.
   await button.click();await host.dispatchEvent('click');assert.equal(await host.getAttribute('data-panel'),'calendar');
   await page.locator('#tp155R4PanelHost .tp155-r4-panel-close').click();assert.equal(await host.count(),0);
   assert.equal(await page.evaluate(()=>document.body.style.overflow),'','closing Calendar releases scroll lock');
  }
  await page.setViewportSize({width:393,height:873});
  await page.evaluate(()=>{db.set('language','hu');go('calendar');});await button.click();
  await page.locator('#tp110ReplanProgram').locator('..').locator('.tp-select-trigger').click();
  await page.keyboard.press('Escape');assert.equal(await page.locator('.tp-select.open').count(),0);assert.equal(await host.getAttribute('data-panel'),'replan','Escape dismisses nested picker first');
  await page.locator('#tp110ReplanDate').locator('..').locator('.tp-temporal-trigger').click();
  await page.evaluate(()=>TrainPilotAndroidBack());assert.equal(await page.locator('#tpTemporalPicker').count(),0);assert.equal(await host.getAttribute('data-panel'),'replan');
  await page.evaluate(()=>render());await page.locator('.tp106-builder-header button').click();assert.equal(await host.getAttribute('data-panel'),'calendar','redraw preserves return context');
  await button.click();await apply.click();assert.equal(await page.locator('[data-tp110-replan-status]').getAttribute('data-kind'),'success');
  const saved=await page.evaluate(()=>scheduled());
  for(const i of [0,1,5])assert.deepEqual(saved[i],fixture.rows[i],'protected schedule '+i);
  for(const i of [2,3,4]){assert.equal(saved[i].id,fixture.rows[i].id);assert.equal(saved[i].dayId,fixture.rows[i].dayId);assert.ok(Date.parse(saved[i].start)>Date.parse('2026-10-04T20:30:00+02:00'));}
  assert.deepEqual(await page.evaluate(()=>history()),fixture.history);await page.evaluate(()=>TrainPilotAndroidBack());assert.equal(await host.getAttribute('data-panel'),'calendar','save then back returns to refreshed Calendar');
  await button.click();assert.equal(await apply.isDisabled(),true);assert.match(await page.locator('[data-tp110-preview]').innerText(),/Nincs újratervezendő/);
  await page.evaluate(()=>go('health'));assert.equal(await host.count(),0,'explicit full-page navigation discards return context');
  await page.evaluate(()=>tp155R4OpenPanel('replan'));await page.locator('.tp106-builder-header button').click();assert.equal(await host.count(),0,'standalone replanner does not invent Calendar origin');
  await page.evaluate(()=>go('calendar'));await button.click();await page.evaluate(()=>tp155R4OpenPanel('settings'));await page.locator('#tp155R4PanelHost .tp155-r4-panel-close').click();assert.equal(await host.count(),0,'panel navigation discards old return context');
  // Entry is always usable with no schedule, and enlarged text still permits closing/applying.
  await page.evaluate(()=>{db.set('scheduled',[]);go('calendar');});assert.equal(await button.isEnabled(),true);await button.click();assert.equal(await apply.isDisabled(),true);await page.evaluate(()=>TrainPilotAndroidBack());
  await page.addStyleTag({content:'html.tp110-large-text #tp155R4PanelHost :is(p,label,.field,.btn){font-size:20px!important;line-height:1.4!important}'});
  await page.evaluate(()=>{document.documentElement.classList.add('tp110-large-text');db.set('scheduled',Array.from({length:30},(_,i)=>{const p=activeProgram();return {...makeScheduleItem(p,'2026-10-01','08:00',45,p.days[i%p.days.length].id),id:'large123-'+i};}));tp155R4RefreshPanel();});
  for(const width of [320,393]){
   await page.setViewportSize({width,height:568});await button.click();await apply.scrollIntoViewIfNeeded();
   assert.ok((await apply.boundingBox()).y+(await apply.boundingBox()).height<=569);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);
   await page.locator('.tp106-builder-header button').click();assert.equal(await host.getAttribute('data-panel'),'calendar');
  }
  assert.deepEqual(errors,[]);console.log('PASS #123: 12 viewport/language cases; prominent always-available entry, automatic overdue preview, compact reachable controls, X/Escape/Back/backdrop return with month/date/focus, nested controls, redraw/save/protected data, explicit navigation, empty schedule and enlarged text');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
