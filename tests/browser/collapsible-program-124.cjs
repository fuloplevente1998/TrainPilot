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
  page.on('pageerror',e=>errors.push(String(e)));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);
  fs.mkdirSync('ui-evidence',{recursive:true});
  const original=await page.evaluate(()=>{
   const template=tp128Template('home-basic'),advanced=tp128Template('home-level2');
   const profile={age:30,height:175,weight:70,goal:'fitness',experience:'beginner',activity:'mixed',minutes:45,location:'home',gear:['dumbbells'],cadence:'alternate',split:'full',excluded:[],focus:'balanced',avoidAreas:[]};
   const p=generatePersonalProgram({...profile,templateId:'home-basic'});p.id='personal-124';
   const own={...JSON.parse(JSON.stringify(p)),id:'own-124',name:'My A/B – Evening routine'};
   db.set('programs',[template,advanced,p,own]);db.set('activeProgramId',p.id);
   db.set('history',[{id:'history-124',programId:p.id,dayId:'B',workout:'B',started:'2026-10-05T16:00:00Z',finished:'2026-10-05T16:45:00Z',exercises:[{...byId('db-floor-press'),sets:[{done:true,reps:10,weight:5}]}]}]);
   db.set('scheduled',[{...makeScheduleItem(p,'2026-10-08','18:00',45,'B'),id:'schedule-124'}]);
   return {programs:programs(),history:history(),scheduled:scheduled(),ownName:own.name};
  });
  const data=()=>page.evaluate(()=>({programs:programs(),history:history(),scheduled:scheduled()}));
  const assertJoined=async(locator,bodySelector)=>{
   const result=await locator.evaluate((d,bodySelector)=>{
    const summary=d.querySelector(':scope>summary'),body=d.querySelector(bodySelector);
    const s=getComputedStyle(summary),b=body&&getComputedStyle(body),outer=getComputedStyle(d);
    return {summary:[s.borderTopWidth,s.borderBottomWidth,s.borderRadius,s.backgroundColor],body:b&&[b.borderLeftWidth,b.borderRightWidth,b.borderBottomWidth,b.borderRadius,b.backgroundColor],outerBorder:outer.borderTopWidth};
   },bodySelector);
   assert.deepEqual(result.summary,['0px','0px','0px','rgba(0, 0, 0, 0)']);
   assert.ok(result.body,'visible body exists');assert.deepEqual(result.body,['0px','0px','0px','0px','rgba(0, 0, 0, 0)']);
   assert.notEqual(result.outerBorder,'0px','enclosing outline retained');
  };
  const orderCheck=async(day)=>{
   const before=await day.evaluate(e=>e.open),button=day.locator(':scope>summary .tp128-order-open');
   assert.equal(await button.count(),1,'order is in heading');await button.click();
   assert.equal(await day.evaluate(e=>e.open),before,'order click leaves day expansion unchanged');
   assert.equal(await page.locator('.tp128-order-list').count(),1);await page.locator('[data-tp2628-cancel]').click();
  };
  for(const [width,height] of [[320,568],[393,873],[873,393]])for(const lang of ['hu','en','de','ro']){
   await page.setViewportSize({width,height});
   await page.evaluate(lang=>{tp155R4ClosePanel(false);db.set('language',lang);rf200SetTheme(lang==='hu'?'yellow':'classicBlue');go('programs');},lang);
   const builtin=page.locator('[data-tp7-program-id="home-basic"]'),personal=page.locator('[data-tp7-program-id="personal-124"]'),own=page.locator('[data-tp7-program-id="own-124"]');
   const builtinName=await builtin.locator('h3').innerText(),personalName=await personal.locator('h3').innerText();
   assert.notEqual(personalName,builtinName);assert.ok(personalName.includes('('),'personalized designation localized');assert.equal(await own.locator('h3').innerText(),original.ownName);
   assert.equal(await personal.locator('.tp152-program-card-body').getAttribute('data-tp7-hydrated'),'false','library stays lazy');
   await personal.locator(':scope>summary').click();await assertJoined(personal,':scope>.tp152-program-card-body');
   const libraryDay=personal.locator('.tp152-program-day').first();
   assert.match(await libraryDay.locator(':scope>summary>strong').innerText(),/ A$/,'full day label without gold badge');
   await orderCheck(libraryDay);await libraryDay.locator(':scope>summary>strong').click();await assertJoined(libraryDay,':scope>.tp152-program-day-body');
   const exercise=libraryDay.locator('.tp107-program-editor').first();await exercise.locator(':scope>summary').click();
   await page.waitForFunction(()=>!!document.querySelector('[data-tp7-program-id="personal-124"] .tp107-program-editor [data-tp107-rx]'));
   await assertJoined(exercise,':scope>.tp107-editor-body');
   await page.evaluate(()=>go('plan'));
   const days=page.locator('.tp152-day');assert.equal(await days.count(),2);
   for(let index=0;index<2;index++){
    const day=days.nth(index),letter=index?'B':'A',title=day.locator(':scope>summary .tp146-day-title');
    assert.equal(await title.locator('.tp146-day-letter').innerText(),letter);
    const plain=await title.evaluate(e=>[...e.childNodes].filter(n=>n.nodeType===Node.TEXT_NODE).map(n=>n.textContent).join('').trim());
    assert.ok(plain.length>1,'meaningful title');assert.doesNotMatch(plain,/(?:^|\s)[AB](?:\s|$)/,'no plain duplicate day letter');assert.doesNotMatch(plain,/[–—-]\s*$/,'no trailing separator');
    await orderCheck(day);
    if(!await day.evaluate(e=>e.open))await day.locator(':scope>summary .tp146-day-title').click();
    await assertJoined(day,':scope>.tp152-day-body');
   }
   assert.equal(await page.locator('.tp152-day-body>.tp128-order-open').count(),0,'no separate order-button row');
   const active=page.locator('.tp152-active-program');await assertJoined(active,':scope>.tp152-program-body');
   const trainingExercise=days.first().locator('.tp107-program-editor').first();await trainingExercise.locator(':scope>summary').click();await assertJoined(trainingExercise,':scope>.tp107-editor-body');
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);
   if(width===393&&lang==='hu')await page.screenshot({path:'ui-evidence/program-days-124.png'});
   await page.evaluate(()=>go('calendar'));await page.locator('.tp110-replan-link').click();
   const names=await page.locator('#tp110ReplanProgram option').evaluateAll(nodes=>Object.fromEntries(nodes.map(n=>[n.value,n.textContent])));
   assert.equal(names['personal-124'],personalName);assert.equal(names['home-basic'],builtinName);assert.equal(names['own-124'],original.ownName);
   assert.equal(await page.locator('[data-tp110-apply]').isEnabled(),true,'accepted #123 replanner remains usable');await page.evaluate(()=>TrainPilotAndroidBack());
   await page.evaluate(()=>{state.tp3HistoryOpenKey=null;go('history');});
   const journal=page.locator('.tp3-combined-history');await journal.locator(':scope>summary').click();await assertJoined(journal,':scope>.tp3-combined-history-body');
   const frames=await journal.evaluate(d=>{const head=getComputedStyle(d.querySelector('.history-head')),summary=getComputedStyle(d.querySelector('.tp3-workout-summary')),metric=getComputedStyle(d.querySelector('.tp3-summary-metrics>div'));return {head:[head.borderWidth,head.borderRadius],summary:[summary.borderWidth,summary.borderRadius,summary.backgroundColor],metric:metric.borderWidth}});
   assert.deepEqual(frames.head,['0px','0px']);assert.deepEqual(frames.summary,['0px','0px','rgba(0, 0, 0, 0)']);assert.notEqual(frames.metric,'0px','independent metric cards kept');
   const journalExercise=journal.locator('.tp3-history-ex-item').first();await journalExercise.locator(':scope>summary').click();await assertJoined(journalExercise,':scope>.tp3-exercise-inline-editor');
   await journalExercise.locator('button[onclick*="tp3CancelExerciseEdit"]').click();
   const health=journal.locator('.rf-history-health-panel'),toggle=health.locator('.rf-history-health-toggle');await toggle.click();assert.equal(await toggle.getAttribute('aria-expanded'),'false');await toggle.click();assert.equal(await toggle.getAttribute('aria-expanded'),'true');
   if(width===393&&lang==='hu')await page.screenshot({path:'ui-evidence/journal-joined-124.png'});
   await journal.locator(':scope>summary').click();assert.equal(await journal.evaluate(d=>d.open),false);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);
  }
  assert.deepEqual(await data(),{programs:original.programs,history:original.history,scheduled:original.scheduled},'UI operations preserve stored data');
  // Existing custom program/day names survive; an empty day adds no accidental A.
  const labels=await page.evaluate(()=>{
   const p=programById('own-124'),custom={id:'B',name:'Evening mobility',exercises:[]};
   const before=JSON.stringify(p),title=tp146ProgramDayTitle(p,{id:'',name:''});
   return {name:tp149ProgramMeta(p,'name'),day:tp149ProgramDayName(p,custom),title,unchanged:before===JSON.stringify(p),safe:tp124DayWithoutBadge('Morning A / recovery','B')};
  });
  assert.equal(labels.name,original.ownName);assert.equal(labels.title,original.ownName);assert.equal(labels.day,'Evening mobility');assert.equal(labels.safe,'Morning A / recovery');assert.equal(labels.unchanged,true);
  // Theme/backup/profile accordions share the same frame and Health remains the reference.
  await page.evaluate(()=>{tp155R4ClosePanel(false);tp155R4OpenPanel('settings');});
  for(const accordion of await page.locator('#tp155R4PanelHost .tp152-accordion').all()){
   await accordion.locator(':scope>summary').click();await assertJoined(accordion,':scope>.tp152-accordion-body');
  }
  await page.evaluate(()=>{tp155R4ClosePanel(false);go('health');});
  const more=page.locator('.tp168-more-panel');await more.locator(':scope>summary').click();
  assert.equal(await more.evaluate(d=>d.open),true);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);
  await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);assert.deepEqual(await data(),{programs:original.programs,history:original.history,scheduled:original.scheduled});
  assert.deepEqual(errors,[]);
  console.log('PASS #124: 12 phone/language cases; joined Journal/history editor/Program/training/exercise/settings frames; metric cards retained; lazy library; personalized names and own names; correct single gold A/B; heading Order action and cancel without toggling; replanner, immutable data and restart');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exitCode=1;});
