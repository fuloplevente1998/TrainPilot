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
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);
  const personal=await page.evaluate(()=>{
   tp128BuildPreview({age:30,height:175,weight:70,goal:'muscle',experience:'beginner',activity:'mixed',location:'home',cadence:'alternate',split:'auto',focus:'balanced',minutes:45,gear:['dumbbells'],excluded:[],avoidAreas:[]},'home-basic');
   acceptPersonalProgram();const p=activeProgram();
   // A saved personalized prescription must reach a calendar-started workout.
   p.prescriptions[p.days[1].exercises[0]].sets=5;
   db.set('programs',programs().map(x=>x.id===p.id?p:x));
   return p;
  });
  assert.equal(personal.sourceTemplateId,'home-basic');assert.notEqual(personal.id,'home-basic');
  const setup=async(lang,theme)=>page.evaluate(({lang,theme,personal})=>{
   tp155R4ClosePanel(false);state.session=null;db.set('draft',null);db.set('language',lang);rf200SetTheme(theme);db.set('activeProgramId',personal.id);
   const old=programById('home-basic'),row=(id,date,status='planned')=>({...makeScheduleItem(old,date,'18:00',45,'A'),id,status,cancelled:status==='cancelled',updatedAt:1});
   const rows=[row('keep-date','2026-10-10'),row('move-date','2026-10-11'),row('remove-date','2026-10-13'),{...row('other-program','2026-10-20'),programId:'home-level2'},row('done','2026-10-12','completed'),row('logged','2026-10-14'),row('active','2026-10-16'),row('draft','2026-10-18'),row('deleted','2026-10-22','cancelled'),row('before','2026-10-09'),row('after','2026-10-24')];
   db.set('scheduled',rows);db.set('history',[{id:'log',scheduleId:'logged',programId:old.id,started:rows[5].start,finished:rows[5].end,exercises:[{id:'pushup',sets:[{done:true,reps:10}]}]}]);
   state.session={scheduleId:'active'};db.set('draft',{session:{scheduleId:'draft'}});
   db.set('plannerSettings',{...plannerSettings(),mode:'alternate',time:'18:00',minutes:45,weekdays:[1,3,5]});
   state.calendarMonth='2026-08';go('calendar');return rows;
  },{lang,theme,personal});
  const configure=async()=>{
   await page.locator('#rf2211Start').evaluate(e=>e.value='2026-10-10');await page.locator('#rf2211Weeks').fill('2');
   await page.locator('#rf2211First').locator('..').locator('.tp-select-trigger').click();
   await page.locator('#rf2211First').locator('..').locator('[data-value="1"]').click();
   // Assign temporal values without onchange: planning saves settings and rows together.
   await page.locator('#rf2211Time').evaluate(e=>e.value='19:20');await page.locator('#rf2211Minutes').evaluate(e=>e.value='60');
  };
  const planButton=page.locator('.tp155-auto-plan-only[onclick="rf2211PlanWeeks()"]');
  for(const [width,height] of [[320,740],[360,800],[393,873],[412,915]])for(const lang of ['hu','en','de','ro'])for(const theme of ['classicBlue','blue']){
   await page.setViewportSize({width,height});const before=await setup(lang,theme);
   const owner=page.locator('.tp155-planner-title .tp-calendar-program');
   assert.equal(await owner.getAttribute('data-program-id'),personal.id);
   assert.equal(await owner.textContent(),await page.evaluate(()=>tp149ProgramMeta(activeProgram(),'name')));
   for(const mode of ['weekly','daily','custom','alternate']){
    await page.locator('#rf230Mode').locator('..').locator('.tp-select-trigger').click();
    await page.locator('#rf230Mode').locator('..').locator('[data-value="'+mode+'"]').click();
   const fit=await page.evaluate(()=>{const panel=document.querySelector('#tp155R4PanelHost .tp155-r4-panel'),planner=panel.querySelector('.rf2211-planner'),owner=planner.querySelector('.tp-calendar-program');return {overflow:panel.scrollHeight-panel.clientHeight,bottom:planner.getBoundingClientRect().bottom,ownerOverflow:owner.scrollHeight-owner.clientHeight,width:document.documentElement.scrollWidth-innerWidth};});
   assert.ok(fit.overflow<=1&&fit.bottom<=height&&fit.ownerOverflow<=1&&fit.width<=1,'personal program remains fully visible '+width+'/'+lang+'/'+theme+' '+JSON.stringify(fit));
   }
   await configure();await planButton.click();
   const after=await page.evaluate(()=>scheduled());
   for(let i=4;i<before.length;i++)assert.deepEqual(after[i],before[i],'protected/previously deleted/outside rows remain unchanged');
   const pending=after.filter(x=>!x.cancelled&&!['done','logged','active','draft','before','after'].includes(x.id));
   assert.deepEqual(pending.map(x=>[x.id,x.programId,x.dayId]),[['keep-date',personal.id,'B'],['move-date',personal.id,'B'],['other-program',personal.id,'A']]);
   assert.equal(after[2].cancelled,true,'surplus old plan becomes a sync tombstone');
   for(const x of pending){assert.equal(new Date(x.start).getUTCHours(),17,'19:20 Budapest is 17:20 UTC');assert.equal(new Date(x.start).getUTCMinutes(),20);assert.equal(Date.parse(x.end)-Date.parse(x.start),3600000);}
   assert.equal(await page.locator('#tp153Toast.show').textContent(),await page.evaluate(()=>TrainPilot110.t('replanned')));
   assert.equal(await page.locator('#rf2211Start').inputValue(),'2026-10-10');assert.equal(await page.locator('#rf2211Weeks').inputValue(),'2');assert.equal(await page.locator('#rf2211First').inputValue(),'1');
   await planButton.click();assert.deepEqual(await page.evaluate(()=>scheduled()),after,'repeat planning is idempotent');
   if(width===393&&lang==='hu'&&theme==='classicBlue'){fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/calendar-personal-planner.png'});}
   await page.evaluate(()=>{state.session=null;db.set('draft',null);tp155R4ClosePanel(false);go('home');state.calendarMonth='2026-10';go('calendar');});
   await page.locator('.cal-cell[onclick*="2026-10-25"]').click();
   const dayOwner=page.locator('#tp108CalendarDay .tp-calendar-program');
   assert.equal(await dayOwner.getAttribute('data-program-id'),personal.id);
   assert.ok((await dayOwner.textContent()).includes(await page.evaluate(()=>tp149ProgramMeta(activeProgram(),'name'))));
   await page.locator('#tp108CalendarDay button[onclick*="rf2211AddDay"]').nth(1).click();
   await page.locator('#tpTemporalPicker').waitFor();await page.locator('[data-tp-hour="20"]').click();await page.locator('[data-tp-minute="20"]').click();
   await page.locator('#tp155R3TimeSave').click();
   const single=await page.evaluate(()=>scheduled().find(x=>localDateKey(new Date(x.start))==='2026-10-25'));
   assert.equal(single.programId,personal.id);assert.equal(single.dayId,'B');
   await page.evaluate(id=>startScheduledById(id),single.id);await page.waitForFunction(()=>!!state.session?.exercises);
   assert.equal(await page.evaluate(()=>state.session.programId),personal.id);
   assert.deepEqual(await page.evaluate(()=>state.session.exercises.map(x=>x.id)),personal.days[1].exercises);
   assert.equal(await page.evaluate(()=>state.session.exercises[0].sets.length),5,'actual personalized prescription reaches the workout');
  }
  // The second write may fail after calendar storage succeeded: rollback both keys.
  await page.setViewportSize({width:393,height:873});await setup('hu','classicBlue');await configure();
  const snapshot=await page.evaluate(()=>['scheduled','plannerSettings'].map(k=>localStorage.getItem('repforge:'+k)));
  await page.evaluate(()=>{document.querySelector('#tp153Toast')?.remove();window.__weekSet=db.set;db.set=(key,value)=>{if(key==='plannerSettings')throw Error('quota');return __weekSet(key,value);};});
  await planButton.click();await page.locator('#tp2628Dialog').waitFor();
  assert.equal(await page.locator('#tp153Toast.show').count(),0,'failed save does not claim success');
  assert.deepEqual(await page.evaluate(()=>['scheduled','plannerSettings'].map(k=>localStorage.getItem('repforge:'+k))),snapshot,'failed planner settings write rolls back the calendar');
  await page.evaluate(()=>db.set=__weekSet);await page.locator('[data-tp2628-ok]').click();
  assert.deepEqual(errors,[]);
  console.log('PASS calendar planner: 32 phone/language/theme cases, accepted personalized program owner/day/prescriptions, six-row layout, all-program replacement, stable IDs/tombstones, protected log/active/draft/outside, repeat saves, actual one-day picker/start and atomic failed save');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
