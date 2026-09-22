const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
  const errors=[];page.on('pageerror',e=>errors.push(e?.stack||e?.message||String(e)));page.on('dialog',d=>d.accept());
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{state.session=null;state.workout=null;go('plan')});await page.waitForSelector('.top.tp154-nav-grid');

  const snap=async()=>page.evaluate(()=>{
   const cells=[...document.querySelectorAll('.top.tp154-nav-grid .tp154-nav-cell')].map((el,i)=>{const r=el.getBoundingClientRect(),s=getComputedStyle(el),label=el.querySelector('.tp151-nav-label');return {i,left:r.left,top:r.top,width:r.width,height:r.height,text:(el.textContent||'').trim(),settings:el.classList.contains('tp154-settings-action'),coach:el.classList.contains('tp154-coach-action'),font:label?parseFloat(getComputedStyle(label).fontSize):null,labelH:label?label.getBoundingClientRect().height:null,cellMin:s.minHeight}});
   const minTop=Math.min(...cells.map(x=>x.top));for(const x of cells)x.relTop=x.top-minTop;
   return {cells,compact:document.documentElement.classList.contains('tp-nav-compact')};
  });
  const before=await snap();
  assert.equal(before.cells.length,8,'8 primary controls');
  const rowBuckets=[];for(const x of before.cells){let r=rowBuckets.find(y=>Math.abs(y.top-x.top)<4);if(r)r.count++;else rowBuckets.push({top:x.top,count:1})}
  assert.deepEqual(rowBuckets.map(x=>x.count),[4,4],'2x4 navigation before scroll');
  assert.ok(before.cells.filter(x=>x.font!=null).every(x=>x.font>=11.4),'navigation labels must be larger');
  assert.ok(before.cells.filter(x=>x.font!=null).every(x=>x.labelH<=30),'translated navigation labels must fit in max two lines');

  await page.evaluate(()=>window.scrollTo(0,700));await page.waitForTimeout(180);
  const after=await snap();
  assert.equal(after.compact,true,'scroll enters sticky compact state');
  assert.equal(after.cells.length,8,'all 8 controls remain in sticky header');
  for(let i=0;i<8;i++){
   assert.ok(Math.abs(before.cells[i].left-after.cells[i].left)<2,'nav cell column must not move on scroll '+i);
   assert.ok(Math.abs(before.cells[i].width-after.cells[i].width)<2,'nav cell width must remain stable '+i);
   assert.ok(Math.abs(before.cells[i].height-after.cells[i].height)<2.5,'nav cell height must remain stable '+i);
   assert.ok(Math.abs(before.cells[i].relTop-after.cells[i].relTop)<2.5,'nav row position must remain stable '+i);
  }
  const coach=after.cells.find(x=>x.coach),settings=after.cells.find(x=>x.settings);
  assert.ok(coach&&settings,'Coach and Settings remain present');
  assert.ok(coach.width>70&&settings.width>70,'Coach and Settings must keep full grid cell width');

  await page.evaluate(()=>{window.scrollTo(0,0);go('programs')});await page.waitForSelector('.tp152-program-card');
  const program=page.locator('.tp152-program-card').first();
  const pArrow=program.locator(':scope > summary .tp152-program-chevron');
  const arrowStyle=async loc=>loc.evaluate(el=>{const s=getComputedStyle(el),b=getComputedStyle(el,'::before');return {w:parseFloat(s.width),h:parseFloat(s.height),font:parseFloat(s.fontSize),border:s.borderTopWidth,bg:s.backgroundImage,glyph:b.content,clip:b.clipPath,fill:b.backgroundColor,hostTransform:s.transform,transform:b.transform}});
  const pClosed=await arrowStyle(pArrow);
  assert.ok(pClosed.w<=18.6&&pClosed.h<=18.6,'program disclosure is a compact triangle target');
  assert.equal(pClosed.border,'0px');assert.equal(pClosed.bg,'none');assert.equal(pClosed.font,0,'literal chevron text must be hidden');
  assert.match(pClosed.clip,/polygon/i,'disclosure must use one CSS triangle');
  assert.notEqual(pClosed.fill,'rgba(0, 0, 0, 0)','triangle must be visibly filled');

  await program.locator(':scope > summary').click();
  await page.waitForTimeout(180);
  const pOpen=await arrowStyle(pArrow);
  assert.notEqual(pOpen.transform,pClosed.transform,'open disclosure triangle rotates from right to down');

  const day=program.locator('.tp152-program-day').first();const dArrow=day.locator(':scope > summary .tp152-chevron');
  const dStyle=await arrowStyle(dArrow);
  assert.deepEqual({w:dStyle.w,h:dStyle.h,font:dStyle.font,border:dStyle.border,bg:dStyle.bg,glyph:dStyle.glyph,clip:dStyle.clip,fill:dStyle.fill},{w:pOpen.w,h:pOpen.h,font:pOpen.font,border:pOpen.border,bg:pOpen.bg,glyph:pOpen.glyph,clip:pOpen.clip,fill:pOpen.fill},'program and day disclosures share one simple triangle component');
  await day.locator(':scope > summary').click();await page.waitForTimeout(180);
  const exercise=day.locator('.tp146-exercise').first();const eArrow=exercise.locator(':scope > summary .tp146-exercise-chevron').first();
  await eArrow.waitFor({state:'attached'});
  const eClosed=await arrowStyle(eArrow);
  assert.equal(eClosed.font,0,'exercise card must hide the legacy chevron glyph');
  assert.equal(eClosed.bg,'none','exercise card must use the plain triangle treatment');
  assert.match(eClosed.clip,/polygon/i,'exercise card disclosure must be a CSS triangle');
  await exercise.locator(':scope > summary').click();await page.waitForTimeout(180);
  const eOpen=await arrowStyle(eArrow);
  assert.equal(eClosed.hostTransform,'none','closed exercise host must not rotate');
  assert.equal(eOpen.hostTransform,'none','open exercise host must not inherit legacy 180-degree rotation');
  assert.notEqual(eOpen.transform,eClosed.transform,'exercise disclosure rotates from right to down');

  await page.evaluate(()=>go('plan'));await page.waitForSelector('.tp152-active-program');
  const activeArrow=page.locator('.tp152-active-program>summary>.tp152-chevron').first();
  const aStyle=await arrowStyle(activeArrow);
  assert.equal(aStyle.font,0,'active-program literal chevron text must stay hidden');
  assert.ok(aStyle.w<=18.6&&aStyle.h<=18.6,'active-program disclosure must not regain the legacy 28/32px chevron box');
  assert.equal(aStyle.bg,'none','active-program disclosure must not regain the legacy gradient background');
  assert.equal(aStyle.hostTransform,'none','active-program host must never rotate; only the triangle rotates');
  assert.match(aStyle.clip,/polygon/i,'active-program uses the same CSS triangle');

  const workoutDay=page.locator('.tp152-plan .tp152-day').first();
  const workoutDayArrow=workoutDay.locator(':scope > summary .tp152-day-title-row .tp152-chevron').first();
  const wdOpen=await arrowStyle(workoutDayArrow);
  assert.equal(wdOpen.font,0,'Workout day must hide the legacy cyan chevron glyph');
  assert.ok(wdOpen.w<=18.6&&wdOpen.h<=18.6,'Workout day must use the compact 1.5.5 triangle host');
  assert.equal(wdOpen.bg,'none','Workout day must not keep the legacy gradient chevron box');
  assert.equal(wdOpen.hostTransform,'none','Workout day host must never rotate');
  assert.match(wdOpen.clip,/polygon/i,'Workout day disclosure must be the same CSS triangle');
  await workoutDay.locator(':scope > summary').click();await page.waitForTimeout(180);
  const wdClosed=await arrowStyle(workoutDayArrow);
  assert.equal(wdClosed.hostTransform,'none','Workout day host stays unrotated when closed');
  assert.notEqual(wdClosed.transform,wdOpen.transform,'Workout day triangle changes between down/open and right/closed');
  await workoutDay.locator(':scope > summary').click();await page.waitForTimeout(180);

  const workoutExercise=workoutDay.locator('.tp152-exercise').first();
  const workoutExerciseArrow=workoutExercise.locator(':scope > summary .tp146-exercise-chevron').first();
  const weClosed=await arrowStyle(workoutExerciseArrow);
  assert.equal(weClosed.hostTransform,'none','closed Workout exercise host must not rotate');
  assert.equal(weClosed.font,0,'Workout exercise must hide the legacy chevron glyph');
  assert.equal(weClosed.bg,'none','Workout exercise must use the plain triangle treatment');
  assert.match(weClosed.clip,/polygon/i,'Workout exercise disclosure must be a CSS triangle');
  await workoutExercise.locator(':scope > summary').click();await page.waitForTimeout(180);
  const weOpen=await arrowStyle(workoutExerciseArrow);
  assert.equal(weOpen.hostTransform,'none','open Workout exercise host must not inherit the legacy 180-degree rotation');
  assert.notEqual(weOpen.transform,weClosed.transform,'Workout exercise triangle changes from right/closed to down/open');

  await page.evaluate(()=>{state.session=null;state.workout=null;tp150QuickPicker()});await page.waitForSelector('.tp150-quick-picker .tp152-quick-row');
  const quickRow=page.locator('.tp150-quick-picker .tp152-quick-row').first();
  const quickArrow=quickRow.locator(':scope > summary .tp-exercise-actions .tp152-chevron').first();
  const qClosed=await arrowStyle(quickArrow);
  assert.equal(qClosed.font,0,'Quick Workout must hide the legacy chevron glyph');
  assert.ok(qClosed.w<=18.6&&qClosed.h<=18.6,'Quick Workout uses the compact 1.5.5 triangle host');
  assert.equal(qClosed.bg,'none','Quick Workout must not keep the legacy rounded/gradient chevron box');
  assert.equal(qClosed.hostTransform,'none','closed Quick Workout chevron host must not rotate');
  assert.match(qClosed.clip,/polygon/i,'Quick Workout disclosure must be a CSS triangle');
  await quickRow.locator(':scope > summary').click();await page.waitForTimeout(180);
  const qOpen=await arrowStyle(quickArrow);
  assert.equal(qOpen.hostTransform,'none','open Quick Workout chevron host must not inherit the legacy 180-degree rotation');
  assert.notEqual(qOpen.transform,qClosed.transform,'Quick Workout triangle changes from right/closed to down/open');

  await page.evaluate(()=>go('calendar'));await page.waitForSelector('#tp155R4PanelHost[data-panel="calendar"] .tp155-r4-calendar-panel-main');
  assert.equal(await page.locator('#tp155R4PanelHost .tp151-page-head').count(),0,'Calendar panel must remove the old standalone page heading and hint');
  assert.equal(await page.locator('#tp155R4PanelHost .tp151-calendar-frame').count(),1,'Calendar panel must expose the month grid directly');
  assert.equal(await page.locator('#tp155R4PanelHost .rf2211-planner').count(),1,'Planning settings must remain available at the bottom of Calendar');
  const calendarPanelLayout=await page.evaluate(()=>{const h=document.querySelector('#tp155R4PanelHost'),f=h?.querySelector('.tp151-calendar-frame'),p=h?.querySelector('.rf2211-planner'),panel=h?.querySelector('.tp155-r4-panel');return {tab:state.tab,frameTop:f?.getBoundingClientRect().top||0,plannerTop:p?.getBoundingClientRect().top||0,animation:panel?getComputedStyle(panel).animationName:''}});
  assert.equal(calendarPanelLayout.tab,'plan','opening Calendar panel must keep the underlying full-page route');
  assert.ok(calendarPanelLayout.plannerTop>calendarPanelLayout.frameTop,'Planning settings must render below the Calendar grid');
  assert.notEqual(calendarPanelLayout.animation,'none','Calendar panel must use the shared reveal motion');
  await page.evaluate(()=>{state.rf2211CalendarDate='2026-09-28';render()});await page.waitForSelector('#tp155R4PanelHost .tp151-day-panel');
  assert.equal(await page.locator('#tp155R4PanelHost .tp151-day-panel.tp155-r4-accent-surface').count(),1,'selected Calendar day details must use the shared highlighted surface');
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true,'Android Back must close Calendar panel');
  assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'Calendar panel must close before changing route');
  assert.equal(await page.evaluate(()=>state.tab),'plan','closing Calendar panel must preserve the underlying route');

  await page.evaluate(()=>go('health'));await page.waitForSelector('.tp151-health>.rf263-sync-card');
  assert.equal(await page.locator('.tp151-health>.tp151-page-head,.tp151-health>.hero').count(),0,'Health page must not restore the removed large title/explanation block');
  assert.equal(await page.locator('.tp151-health>.rf263-sync-card').count(),1,'Health functional content must begin directly with the sync card');

  await page.evaluate(()=>profileScreen());await page.waitForSelector('.rf148-profile-accordion');
  const exclusion=page.locator('.rf148-profile-accordion').first();
  const exclusionArrow=async()=>exclusion.locator('summary').evaluate(el=>{const s=getComputedStyle(el,'::after');return {border:s.borderTopWidth,bg:s.backgroundImage,fill:s.backgroundColor,w:parseFloat(s.width),h:parseFloat(s.height),clip:s.clipPath,transform:s.transform}});
  const exclusionClosed=await exclusionArrow();
  assert.equal(exclusionClosed.border,'0px','profile exclusion disclosure must be borderless');
  assert.equal(exclusionClosed.bg,'none','profile exclusion disclosure must not retain the legacy red glyph treatment');
  assert.ok(exclusionClosed.w<=9.6&&exclusionClosed.h<=12.6,'profile exclusion disclosure uses the compact triangle');
  assert.match(exclusionClosed.clip,/polygon/i,'profile exclusion disclosure must be a CSS triangle');
  assert.notEqual(exclusionClosed.fill,'rgba(0, 0, 0, 0)','profile exclusion triangle must be visible');
  await exclusion.locator('summary').click();await page.waitForTimeout(180);
  assert.notEqual((await exclusionArrow()).transform,exclusionClosed.transform,'profile exclusion triangle rotates from right to down');

  assert.deepEqual(errors,[],'page errors: '+errors.join('\n'));
  console.log('PASS TrainPilot 1.5.5 nav consistency');
 } finally{if(browser)await browser.close();server.close()}
})().catch(e=>{console.error(e);server.close();process.exit(1)});
