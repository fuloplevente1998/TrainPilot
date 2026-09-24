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
  // Phase 5 #41 replaced the 1.5.5 triangle with a 30x36 theme-aware chevron.
  // The original navigation, expansion and responsive coverage remains below.
  const assertSharedChevron=async(loc,name)=>{
   await loc.waitFor({state:'attached'});
   await page.evaluate(()=>tpGlobalApplyChevrons(document));
   const style=await arrowStyle(loc);
   assert.ok(style.w>=25&&style.h>=30,name+' must have the shared touch target');
   assert.ok(style.glyph?.includes('›'),name+' must render the shared chevron');
   assert.equal(style.clip,'none',name+' must not render a clipped triangle');
   const c=await loc.evaluate(el=>getComputedStyle(el).color);
   const theme=await page.evaluate(()=>{const e=document.createElement('i');e.style.color='var(--accent2)';document.body.appendChild(e);const c=getComputedStyle(e).color;e.remove();return c});
   assert.equal(c,theme,name+' must use the selected theme color');
   return style;
  };
  await assertSharedChevron(pArrow,'Program disclosure');
  await program.locator(':scope > summary').click();
  await page.waitForTimeout(80);
  await assertSharedChevron(pArrow,'Expanded program disclosure');

  const day=program.locator('.tp152-program-day').first();
  const dArrow=day.locator(':scope > summary .tp152-chevron');
  await assertSharedChevron(dArrow,'Program day disclosure');
  await day.locator(':scope > summary').click();await page.waitForTimeout(80);
  const exercise=day.locator('.tp146-exercise').first();
  const eArrow=exercise.locator(':scope > summary .tp146-exercise-chevron').first();
  await assertSharedChevron(eArrow,'Program exercise disclosure');
  await exercise.locator(':scope > summary').click();await page.waitForTimeout(80);
  await assertSharedChevron(eArrow,'Expanded program exercise disclosure');

  await page.evaluate(()=>go('plan'));await page.waitForSelector('.tp152-active-program');
  const activeArrow=page.locator('.tp152-active-program>summary>.tp152-chevron').first();
  await assertSharedChevron(activeArrow,'Active program disclosure');

  const workoutDay=page.locator('.tp152-plan .tp152-day').first();
  const workoutDayArrow=workoutDay.locator(':scope > summary .tp152-day-title-row .tp152-chevron').first();
  await assertSharedChevron(workoutDayArrow,'Workout day disclosure');
  await workoutDay.locator(':scope > summary').click();await page.waitForTimeout(80);
  await assertSharedChevron(workoutDayArrow,'Expanded Workout day disclosure');
  await workoutDay.locator(':scope > summary').click();await page.waitForTimeout(80);

  const workoutExercise=workoutDay.locator('.tp152-exercise').first();
  const workoutExerciseArrow=workoutExercise.locator(':scope > summary .tp146-exercise-chevron').first();
  await assertSharedChevron(workoutExerciseArrow,'Workout exercise disclosure');
  await workoutExercise.locator(':scope > summary').click();await page.waitForTimeout(80);
  await assertSharedChevron(workoutExerciseArrow,'Expanded Workout exercise disclosure');

  await page.evaluate(()=>{state.session=null;state.workout=null;tp150QuickPicker()});
  await page.waitForSelector('.tp150-quick-picker .tp152-quick-row');
  const quickRow=page.locator('.tp150-quick-picker .tp152-quick-row').first();
  const quickArrow=quickRow.locator(':scope > summary .tp-exercise-actions .tp152-chevron').first();
  await assertSharedChevron(quickArrow,'Quick Workout disclosure');
  await quickRow.locator(':scope > summary').click();await page.waitForTimeout(80);
  await assertSharedChevron(quickArrow,'Expanded Quick Workout disclosure');

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
