const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(data)});
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:720},locale:'hu-HU'});
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilot155Next12?.calendarMove&&window.TrainPilot155PhoneRound4?.calendarMoveInPlace);

  const initial=await page.evaluate(()=>{
   db.set('language','hu');db.set('activeProgramId','home-basic');db.set('history',[]);db.set('draft',null);
   const p=activeProgram(),d=p.days[0],start=new Date();start.setDate(start.getDate()+2);start.setHours(18,0,0,0);const end=new Date(start.getTime()+45*60000),id='next12-calendar';
   db.set('scheduled',[{id,programId:p.id,dayId:d.id,workout:d.id,start:start.toISOString(),end:end.toISOString(),status:'planned',cancelled:false}]);
   state.session=null;state.tab='home';render();go('calendar');state.rf2211CalendarDate=localDateKey(start);render();
   return {id,oldDate:localDateKey(start),newDate:localDateKey(new Date(start.getTime()+3*86400000))};
  });
  const calendar=page.locator('#tp155R4PanelHost[data-panel="calendar"]');await calendar.waitFor();
  const actions=await calendar.locator('.tp151-calendar-item .tp151-action-row button').allInnerTexts();assert.ok(actions.includes('Áthelyezés'));assert.ok(actions.includes('Elvégezve'));
  assert.equal(await calendar.locator('.tp155-r4-panel-close').isVisible(),true,'Calendar panel must keep a visible recovery close button');
  assert.equal(await page.locator('.top.tp154-nav-grid').isVisible(),true,'Calendar must keep the top navigation visible');
  const closeReference=await calendar.locator('.tp155-r4-panel-close').evaluate(e=>{const r=e.getBoundingClientRect(),p=e.closest('.tp155-r4-panel').getBoundingClientRect(),content=e.parentElement.querySelector('.tp155-r4-panel-content')?.getBoundingClientRect(),c=getComputedStyle(e);return {width:parseFloat(c.width),height:parseFloat(c.height),rightGap:p.right-r.right,background:c.backgroundColor,position:c.position,closeBottom:r.bottom,contentTop:content?.top}});
  assert.ok(Math.abs(closeReference.width-closeReference.height)<0.05,'panel close control must be square');assert.ok(closeReference.rightGap>=10&&closeReference.rightGap<=18,'1.6.2 panel close control must keep the left-shifted right inset: '+JSON.stringify(closeReference));assert.equal(closeReference.position,'sticky','1.6.2 panel close control must be sticky, not absolute');assert.ok(closeReference.contentTop<closeReference.closeBottom,'panel content should start beside the floating sticky close control instead of reserving its full height');assert.match(closeReference.background,/rgb\((?:6[5-9]|7[0-9]),\s*(?:3[0-9]|4[0-9]),\s*(?:3[0-9]|4[0-9])\)/,'panel close control must use the dark red treatment');
  const panelLayers=await page.evaluate(()=>{const n=document.querySelector('.top.tp154-nav-grid'),h=document.getElementById('tp155R4PanelHost'),nr=n.getBoundingClientRect(),hr=h.getBoundingClientRect();return {nav:parseInt(getComputedStyle(n).zIndex)||0,host:parseInt(getComputedStyle(h).zIndex)||0,navBottom:nr.bottom,hostTop:hr.top}});
  assert.ok(panelLayers.nav>panelLayers.host,'2x4 navigation must stay above main panels');assert.ok(panelLayers.hostTop>=panelLayers.navBottom-8&&panelLayers.hostTop<=panelLayers.navBottom+1,'1.6.1 main panels may lift slightly under the navigation while remaining behind it: '+JSON.stringify(panelLayers));
  await calendar.getByRole('button',{name:'Áthelyezés'}).click();await page.waitForSelector('.tp155-move-banner');
  assert.equal(await page.locator('#tpTemporalPicker').count(),0,'Calendar move must reuse the visible main calendar, not open a second calendar');
  await page.evaluate(date=>toggleCalendarDay(date),initial.newDate);await page.waitForTimeout(50);
  const moved=await page.evaluate(({id,oldDate,newDate})=>{const x=scheduled().find(row=>row.id===id);return {date:localDateKey(new Date(x.start)),oldCount:scheduleOnDate(oldDate).filter(row=>!row.cancelled).length,newCount:scheduleOnDate(newDate).filter(row=>row.id===id).length,programId:x.programId,dayId:x.dayId}},initial);
  assert.equal(moved.date,initial.newDate);assert.equal(moved.oldCount,0);assert.equal(moved.newCount,1);assert.ok(moved.programId&&moved.dayId);
  await page.evaluate(id=>tp155MarkScheduleCompleted(id),initial.id);
  assert.deepEqual(await page.evaluate(id=>({status:scheduled().find(x=>x.id===id)?.status,history:history().length}),initial.id),{status:'completed',history:0},'manual completion must not create a duplicate journal workout');

  await page.evaluate(()=>{tp155R4ClosePanel(false);go('history')});await page.waitForSelector('.tp155-journal-tabs');
  await page.locator('.tp155-journal-tabs button').nth(1).click();await page.waitForTimeout(40);assert.equal(await page.evaluate(()=>state.tab),'stats');assert.equal(await page.locator('.tp155-journal-tabs button.active').innerText(),'Statisztikák');
  const statsChrome=await page.evaluate(()=>{const main=document.querySelector('#app main');return {hero:!!main?.querySelector(':scope > .hero'),back:[...(main?.children||[])].some(el=>el.tagName==='BUTTON'&&String(el.getAttribute('onclick')||'').includes("go('home')"))}});
  assert.deepEqual(statsChrome,{hero:false,back:false},'Statistics must not repeat title/description or old Back button');

  await page.evaluate(()=>go('calendar'));await page.waitForSelector('#tp155R4PanelHost[data-panel="calendar"]');
  const planner=page.locator('.rf2211-planner');assert.equal(await planner.evaluate(e=>e.tagName),'SECTION','Calendar planning settings must be permanently open');assert.equal(await planner.locator('summary').count(),0,'Calendar planning settings must not be collapsible');assert.equal(await planner.locator('.rf2211-planbody').isVisible(),true);
  const plannerText=await planner.innerText();assert.doesNotMatch(plannerText,/több hét előre/i);assert.doesNotMatch(plannerText,/Válassz ritmust/i);
  assert.ok(await planner.evaluate(e=>e.scrollHeight<=innerHeight-24),'compact Calendar planner should fit inside one phone viewport');
  assert.equal(await page.locator('#rf230Mode option[value="custom"]').count(),1,'manual Calendar planning mode must be restored');
  await page.evaluate(()=>{const s=document.querySelector('#rf230Mode');s.value='custom';s.dispatchEvent(new Event('change',{bubbles:true}))});await page.waitForTimeout(20);
  assert.equal(await page.locator('.tp155-custom-plan-hint').isVisible(),true);
  assert.equal(await page.locator('.tp155-auto-plan-only').first().isVisible(),false);
  await page.evaluate(()=>rf233CoachScreen());await page.waitForSelector('#tp155R4PanelHost[data-panel="coach"]');
  assert.equal(await page.locator('.top.tp154-nav-grid').isVisible(),true,'Coach must keep the top navigation visible');
  assert.equal(await page.locator('#tp155R4PanelHost[data-panel="coach"] .tp155-r4-panel-close').isVisible(),true);
  const coachClose=await page.locator('#tp155R4PanelHost[data-panel="coach"] .tp155-r4-panel-close').evaluate(e=>{const r=e.getBoundingClientRect(),p=e.closest('.tp155-r4-panel').getBoundingClientRect(),c=getComputedStyle(e);return {width:parseFloat(c.width),height:parseFloat(c.height),rightGap:p.right-r.right}});assert.ok(Math.abs(coachClose.width-closeReference.width)<.05&&Math.abs(coachClose.height-closeReference.height)<.05,'Coach must use the shared X dimensions');assert.ok(coachClose.rightGap>=-.05&&coachClose.rightGap<=24,'Coach X must stay inside the same top-right panel rail');
  await page.evaluate(()=>rf233CoachScreen());
  assert.equal(await page.locator('#tp155R4PanelHost').getAttribute('data-panel'),'coach','Coach refresh must stay inside the panel');
  assert.equal(await page.locator('#tp155R4PanelHost').count(),1,'Coach refresh must not duplicate or dismiss the panel');

  await page.evaluate(()=>{cloudProfile={name:'Test',email:'test@example.com'}});
  await page.locator('.tp154-settings-action').click();await page.waitForSelector('#tp155R4PanelHost[data-panel="settings"]');
  assert.equal(await page.locator('#tp155R4PanelHost[data-panel="settings"] .tp155-r4-panel-close').isVisible(),true);
  const settingsClose=await page.locator('#tp155R4PanelHost[data-panel="settings"] .tp155-r4-panel-close').evaluate(e=>{const r=e.getBoundingClientRect(),p=e.closest('.tp155-r4-panel').getBoundingClientRect(),c=getComputedStyle(e);return {width:parseFloat(c.width),height:parseFloat(c.height),rightGap:p.right-r.right}});assert.ok(Math.abs(settingsClose.width-closeReference.width)<.05&&Math.abs(settingsClose.height-closeReference.height)<.05,'Settings must use the shared X dimensions');assert.ok(settingsClose.rightGap>=-.05&&settingsClose.rightGap<=24,'Settings X must stay inside the same top-right panel rail');
  assert.equal(await page.locator('.top.tp154-nav-grid').isVisible(),true,'Settings must keep the top navigation visible');
  assert.equal(await page.locator('.tp155-language-select').evaluate(el=>el.closest('.tp-select')?.classList.contains('tp155-language-attached')),true,'language selector must use attached dropdown positioning');
  const themeSetting=page.locator('.tp155-theme-setting');assert.equal(await themeSetting.count(),1,'Theme color must use the aligned Settings card');
  const themeAlignment=await themeSetting.evaluate(e=>{const r=e.getBoundingClientRect(),s=e.parentElement.querySelector('.tp155-language-direct')?.getBoundingClientRect();return {left:Math.abs(r.left-s.left),right:Math.abs(r.right-s.right)}});assert.ok(themeAlignment.left<=1&&themeAlignment.right<=1,'Theme color card must align with the other Settings cards');
  assert.equal(await page.locator('.tp155-sync-check').count(),2,'Drive and Calendar auto-sync must use compact themed checks');
  const settingsText=await page.locator('#tp155R4PanelHost[data-panel="settings"]').innerText();assert.match(settingsText,/TrainPilot → Google Naptár/,'visible Calendar sync direction must use TrainPilot branding');assert.doesNotMatch(settingsText,/RepForge → Google Naptár/,'legacy branding must not remain in the visible Calendar sync direction');
  const syncCheckSize=await page.locator('.tp155-sync-check input').first().evaluate(e=>{const r=e.getBoundingClientRect();return {w:r.width,h:r.height}});assert.ok(syncCheckSize.w<=24&&syncCheckSize.h<=24,'auto-sync checks must remain compact');
  const settingsLayoutBefore=await page.evaluate(()=>{const theme=document.querySelector('.tp155-theme-setting'),backup=document.querySelector('.tp152-settings-backup');return {themeHeight:theme?.getBoundingClientRect().height||0,backupOffset:backup?.offsetTop||0}});
  await themeSetting.locator('.tp155-theme-open').click();await page.waitForFunction(()=>document.querySelector('.tp162-theme-dropdown')?.open===true);
  const themeDropdown=page.locator('.tp162-theme-dropdown'),themeMenu=themeDropdown.locator('.tp162-theme-columns');
  assert.equal(await page.locator('#tp155ThemePicker').count(),0,'theme chooser must stay attached to Settings instead of opening the legacy modal');
  assert.ok(await themeDropdown.locator('.tp155-theme-option').count()>=10,'theme chooser must expose the complete theme set');
  assert.equal(await themeDropdown.locator('.tp162-theme-column').count(),2,'theme chooser must keep separate Basic and Vivid columns');
  const overlayStyle=await themeMenu.evaluate(e=>({position:getComputedStyle(e).position,overflowY:getComputedStyle(e).overflowY,maxHeight:parseFloat(getComputedStyle(e).maxHeight),clientHeight:e.clientHeight,scrollHeight:e.scrollHeight}));
  assert.equal(overlayStyle.position,'absolute','theme choices must float over Settings instead of expanding document flow');
  assert.equal(overlayStyle.overflowY,'auto','theme choices must scroll inside the floating dropdown');
  assert.ok(overlayStyle.maxHeight<=301&&overlayStyle.clientHeight<=301,'theme dropdown must keep a compact phone-height cap: '+JSON.stringify(overlayStyle));
  assert.ok(overlayStyle.scrollHeight>overlayStyle.clientHeight,'complete theme list must be internally scrollable');
  const settingsLayoutOpen=await page.evaluate(()=>{const theme=document.querySelector('.tp155-theme-setting'),backup=document.querySelector('.tp152-settings-backup');return {themeHeight:theme?.getBoundingClientRect().height||0,backupOffset:backup?.offsetTop||0}});
  assert.ok(Math.abs(settingsLayoutOpen.themeHeight-settingsLayoutBefore.themeHeight)<=1&&Math.abs(settingsLayoutOpen.backupOffset-settingsLayoutBefore.backupOffset)<=1,'opening Theme must not change Settings document layout: '+JSON.stringify({settingsLayoutBefore,settingsLayoutOpen}));
  assert.match(await themeDropdown.innerText(),/Alap színek/i);assert.match(await themeDropdown.innerText(),/Élénk színek/i);
  const activeTheme=themeDropdown.locator('.tp155-theme-option.active');assert.equal(await activeTheme.count(),1,'current theme must have one clear active state');assert.equal(await activeTheme.getAttribute('aria-pressed'),'true');
  await page.setViewportSize({width:320,height:720});await page.waitForTimeout(40);
  const narrowTheme=await themeMenu.evaluate(e=>{const r=e.getBoundingClientRect();return {left:r.left,right:r.right,width:r.width,viewport:innerWidth,client:e.clientWidth,scroll:e.scrollWidth}});
  assert.ok(narrowTheme.left>=0&&narrowTheme.right<=narrowTheme.viewport+0.5,'floating theme chooser must fit a 320px viewport: '+JSON.stringify(narrowTheme));assert.ok(narrowTheme.scroll<=narrowTheme.client+1,'theme columns must not create horizontal overflow at 320px');
  await page.setViewportSize({width:393,height:720});await page.waitForTimeout(30);
  const themeBefore=await page.evaluate(()=>rf200ThemeKey());await themeDropdown.locator('.tp155-theme-option:not(.active)').first().click();await page.waitForTimeout(30);
  assert.notEqual(await page.evaluate(()=>rf200ThemeKey()),themeBefore,'Theme must be selectable while Settings remains open');assert.equal(await page.locator('#tp155R4PanelHost[data-panel="settings"]').count(),1,'changing theme must keep Settings open');assert.equal(await page.locator('.tp162-theme-dropdown[open]').count(),0,'theme dropdown must close after choosing a color like the language dropdown');
  await page.locator('.tp162-theme-dropdown .tp155-theme-open').click();await page.waitForFunction(()=>document.querySelector('.tp162-theme-dropdown')?.open===true);
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true,'Android Back must close the open Theme dropdown first');assert.equal(await page.locator('.tp162-theme-dropdown[open]').count(),0);assert.equal(await page.locator('#tp155R4PanelHost[data-panel="settings"]').count(),1);
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true,'next Android Back must handle the open main panel');
  assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'Android Back must close the main panel');

  const scrollProbe=await page.evaluate(()=>{go('programs');const main=document.querySelector('#app main'),spacer=document.createElement('div');spacer.id='tp155ScrollProbe';spacer.style.height='1800px';main.appendChild(spacer);window.scrollTo(0,600);return window.scrollY});await page.waitForTimeout(30);await page.evaluate(()=>document.querySelector('.tp154-settings-action')?.click());await page.waitForSelector('#tp155R4PanelHost[data-panel="settings"]');const scrollWhileOpen=await page.evaluate(()=>{const n=document.querySelector('.top.tp154-nav-grid').getBoundingClientRect(),h=document.getElementById('tp155R4PanelHost').getBoundingClientRect();return {navVisible:n.bottom>0,hostTop:h.top,navBottom:n.bottom,hostAligned:h.top>=n.bottom-8&&h.top<=n.bottom+1}});assert.equal(scrollWhileOpen.navVisible,true);assert.equal(scrollWhileOpen.hostAligned,true,'1.6.1 main panel may lift slightly under the navigation on a scrolled page while remaining behind it: '+JSON.stringify(scrollWhileOpen));await page.evaluate(()=>document.querySelector('#tp155R4PanelHost[data-panel="settings"] .tp155-r4-panel-close')?.click());await page.waitForTimeout(30);const scrollAfterClose=await page.evaluate(()=>window.scrollY);assert.ok(Math.abs(scrollAfterClose-scrollProbe)<=5,'closing a main panel must preserve the background scroll position within browser pixel rounding: '+JSON.stringify({scrollProbe,scrollAfterClose}));await page.evaluate(()=>document.getElementById('tp155ScrollProbe')?.remove());

  const timerPosition=await page.evaluate(()=>{const el=document.createElement('div');el.className='timer';document.body.appendChild(el);const p=getComputedStyle(el).position;el.remove();return p});
  assert.equal(timerPosition,'static','rest timer must stay in normal document flow');

  await page.evaluate(()=>{const now=new Date(),h={id:'delete-me',started:now.toISOString(),finished:new Date(now.getTime()+1800000).toISOString(),workout:'A',dayId:'A',programId:'home-basic',programName:'Otthoni A/B – Alap',exercises:[]};db.set('history',[h]);go('history')});
  await page.locator('[data-tp152-history-key="delete-me"] > summary').click();
  await page.locator('[data-tp152-history-key="delete-me"] .tp3-summary-edit').click();
  assert.equal(await page.locator('.tp155-history-delete').count(),1,'logged workout inline editor must expose a delete action');assert.equal((await page.locator('.tp155-history-delete').first().textContent()).trim(),'Törlés');
  await page.evaluate(()=>{const rows=history();rows[0].photos=[{id:'photo-test',label:'after',createdAt:new Date().toISOString(),updatedAt:Date.now(),deletedAt:null}];db.set('history',rows);window.tp2628Confirm=async()=>true;window.rf130PhotoPlugin=()=>({delete:async()=>({})});render();const item=document.querySelector('[data-tp152-history-key="delete-me"]');if(item)item.open=true});
  await page.evaluate(()=>rf130DeletePhoto('delete-me','photo-test'));await page.waitForTimeout(30);
  assert.equal(await page.locator('[data-tp152-history-key="delete-me"]').getAttribute('open'),'','photo deletion must keep the journal entry open');
  assert.equal(await page.locator('[data-tp152-history-key="delete-me"] .rf130-photo-section').count(),1,'photo controls must remain after deleting the last photo');
  await page.evaluate(()=>{const modal=document.createElement('div');modal.id='rf130PhotoModal';modal.className='video-modal';document.body.appendChild(modal)});
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true,'Android Back must handle the journal photo modal');
  assert.equal(await page.locator('#rf130PhotoModal').count(),0,'Android Back must close the journal photo modal');

  await page.evaluate(()=>{go('plan');db.set('draft',null);state.session=null;render()});await page.locator('.tp150-quick-entry button').click();
  const quick=page.locator('#tp155R4PanelHost[data-panel="quick"]');await quick.waitFor();assert.equal(await page.evaluate(()=>state.session),null);
  const quickNav=await page.evaluate(()=>[...document.querySelectorAll('.top.tp154-nav-grid .tp151-nav-item')].find(b=>/go\(['"]plan['"]\)/.test(b.getAttribute('onclick')||''))?.classList.contains('active'));assert.equal(quickNav,true,'Workout navigation must remain active while Quick Workout is open');
  assert.equal(await quick.locator('.tp160-quick-context').count(),1,'Quick Workout title must appear exactly once');assert.equal(await quick.locator('.hero,.tp151-page-head').count(),0,'Quick Workout must not restore a duplicate explanatory header');
  assert.equal(await quick.locator('.tp155-r4-panel-close').isVisible(),true,'Quick Workout panel must restore the shared X close button');
  const quickClose=await quick.locator('.tp155-r4-panel-close').evaluate(e=>{const r=e.getBoundingClientRect(),p=e.closest('.tp155-r4-panel').getBoundingClientRect(),c=getComputedStyle(e);return {width:parseFloat(c.width),height:parseFloat(c.height),rightGap:p.right-r.right}});assert.ok(Math.abs(quickClose.width-closeReference.width)<0.05&&Math.abs(quickClose.height-closeReference.height)<0.05,'Quick Workout must use the same close-control dimensions');assert.ok(quickClose.rightGap>=-.05&&quickClose.rightGap<=24,'Quick Workout X must stay inside the same top-right panel rail');
  assert.equal(await quick.locator('.tp155-quick-panel > .hero').count(),0,'Quick Workout panel must not show the redundant Quick Workout header card');
  assert.equal(await quick.locator('#tp155QuickQuery,#tp155QuickMuscle,#tp155QuickGear').count(),3,'Quick Workout must keep search, muscle and equipment filtering');
  const quickFilter=quick.locator('.tp155-quick-filter');assert.equal(await quickFilter.getAttribute('open'),null,'Quick Workout filter should start collapsed');
  await quickFilter.locator(':scope > summary').click();
  const quickBefore=await quick.locator('.tp152-quick-row').count();
  const muscleSelect=quick.locator('#tp155QuickMuscle');const muscleValue=await muscleSelect.locator('option').nth(1).getAttribute('value');
  await muscleSelect.selectOption(muscleValue);await page.waitForTimeout(40);
  const quickAfter=await quick.locator('.tp152-quick-row').count();assert.ok(quickAfter>0&&quickAfter<quickBefore,'muscle filter must narrow Quick Workout results');
  await muscleSelect.selectOption('all');await page.waitForTimeout(30);
  const firstQuick=quick.locator('.tp152-quick-row').first();await firstQuick.locator('summary').click();await firstQuick.locator('.tp150-quick-start').click();await page.waitForFunction(()=>!!state.session);
  assert.equal(await page.locator('#tp155R4PanelHost[data-panel="quick"]').count(),0,'Quick panel must close before the full Workout screen opens');

  await page.evaluate(()=>{state.session=null;db.set('draft',null);go('programs');muscleLibrary()});
  const library=page.locator('#tp155R4PanelHost[data-panel="exercises"]');await library.waitFor();assert.equal(await library.locator('#libraryQuery,#libraryMuscle,#libraryGear').count(),3);assert.ok(await library.locator('#libraryResults details.tp-library-card').count()>50);const programsNav=await page.evaluate(()=>[...document.querySelectorAll('.top.tp154-nav-grid .tp151-nav-item')].find(b=>/go\(['"]programs['"]\)/.test(b.getAttribute('onclick')||''))?.classList.contains('active'));assert.equal(programsNav,true,'Programs navigation must remain active in the exercise-library subview');const libraryClose=await library.locator('.tp155-r4-panel-close').evaluate(e=>{const r=e.getBoundingClientRect(),p=e.closest('.tp155-r4-panel').getBoundingClientRect(),c=getComputedStyle(e);return {width:parseFloat(c.width),height:parseFloat(c.height),rightGap:p.right-r.right}});assert.ok(Math.abs(libraryClose.width-closeReference.width)<.05&&Math.abs(libraryClose.height-closeReference.height)<.05,'Exercise library must use the shared X dimensions');assert.ok(libraryClose.rightGap>=-.05&&libraryClose.rightGap<=24,'Exercise library X must stay inside the same top-right panel rail');
  assert.equal(await library.locator('.tp155-r4-panel-close').evaluate(e=>getComputedStyle(e).position),'sticky','Exercise-library X must use sticky positioning');
  const libraryWidth=await library.evaluate(root=>{const p=root.querySelector('.tp155-r4-panel');p.scrollLeft=999;return {client:p.clientWidth,scroll:p.scrollWidth,left:p.scrollLeft}});assert.ok(libraryWidth.scroll<=libraryWidth.client+1&&libraryWidth.left===0,'exercise library must not drift or retain horizontal scroll');
  const pinned=await library.evaluate(root=>{const filters=root.querySelector('.tp155-library-filters'),results=root.querySelector('#libraryResults');const before=filters.getBoundingClientRect().top;results.scrollTop=120;return {before,after:filters.getBoundingClientRect().top,overflow:getComputedStyle(results).overflowY,separate:filters.nextElementSibling===results}});assert.equal(pinned.separate,true);assert.match(pinned.overflow,/auto|scroll/);assert.ok(Math.abs(pinned.after-pinned.before)<1,'exercise filters must stay pinned while only the card list scrolls');
  const libraryMuscle=library.locator('#libraryMuscle').locator('xpath=..');await libraryMuscle.locator('.tp-select-trigger').click();
  const filterLayers=await libraryMuscle.evaluate(w=>{const menu=w.querySelector('.tp-select-menu'),results=document.querySelector('#libraryResults'),trigger=w.querySelector('.tp-select-trigger');const mr=menu.getBoundingClientRect(),tr=trigger.getBoundingClientRect();return {menu:parseInt(getComputedStyle(menu).zIndex)||0,results:parseInt(getComputedStyle(results).zIndex)||0,open:w.classList.contains('open'),position:getComputedStyle(menu).position,dx:Math.abs(mr.left-tr.left),gap:Math.min(Math.abs(mr.top-tr.bottom),Math.abs(tr.top-mr.bottom))}});assert.equal(filterLayers.open,true);assert.ok(filterLayers.menu>filterLayers.results,'exercise filter menus must layer above the result count and cards');assert.equal(filterLayers.position,'absolute','Program exercise-library filter menu must stay attached to its field');assert.ok(filterLayers.dx<=1&&filterLayers.gap<=6,'Program exercise-library filter menu must open directly beside its trigger instead of sliding to the bottom');
  console.log('PASS: TrainPilot 1.5.5 NEXT12 + phone round4 + Quick filter cleanup.');
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve))}
})().catch(error=>{console.error(error);process.exit(1)});
