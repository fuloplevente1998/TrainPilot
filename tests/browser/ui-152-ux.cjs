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

  const theme=await page.evaluate(()=>{localStorage.removeItem('repforge:themeAccent');rf200ApplyTheme();return {key:rf200ThemeKey(),family:document.documentElement.dataset.tpThemeFamily,accent:getComputedStyle(document.documentElement).getPropertyValue('--accent').trim()}});
  assert.equal(theme.key,'classicBlue');assert.equal(theme.family,'basic');assert.ok(theme.accent,'default basic blue accent missing');
  assert.notEqual(theme.accent.toLowerCase(),'#58b8f7','matte Basic theme must not use the old neon blue');
  assert.ok(await page.evaluate(()=>TP1511_BASIC.length)>=10,'Basic palette should expose more matte color choices');

  await page.evaluate(()=>{state.session=null;state.workout=null;go('home')});await page.waitForSelector('.top .tp151-nav-dock');
  const stickyHome=await page.evaluate(async()=>{
   const top=document.querySelector('.top'),main=document.querySelector('main'),spacer=document.createElement('div');spacer.id='tp152StickyProbe';spacer.style.height='1600px';main.appendChild(spacer);
   window.scrollTo(0,600);await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));
   const result={top:top.getBoundingClientRect().top,position:getComputedStyle(top).position,actions:top.querySelectorAll('.tp151-top-actions .tp151-top-icon').length,tabs:top.querySelectorAll('.tp151-nav-dock .tp151-nav-item').length,brandInStrip:!!document.querySelector('.tp-brand-strip .brand')};
   window.scrollTo(0,0);spacer.remove();return result;
  });
  assert.equal(stickyHome.position,'sticky','Home header must use sticky positioning');
  assert.ok(Math.abs(stickyHome.top)<=1,'Home sticky header must remain pinned while scrolling');
  assert.equal(stickyHome.actions,2,'Coach and Settings must remain inside the sticky header');
  assert.equal(stickyHome.tabs,6,'all six main navigation tabs must remain inside the sticky header');
  assert.equal(stickyHome.brandInStrip,false,'Home brand/title must be physically removed instead of reserving layout space');
  const compactHeader=await page.evaluate(async()=>{const main=document.querySelector('main'),spacer=document.createElement('div');spacer.id='tp152CompactProbe';spacer.style.height='1600px';main.appendChild(spacer);window.scrollTo(0,600);await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));const nav=document.querySelector('.top .tp151-nav-dock'),actions=document.querySelector('.top .tp-sticky-actions-row'),brandActions=document.querySelector('.tp-brand-strip .tp152-brand-actions'),nr=nav?.getBoundingClientRect(),ar=actions?.getBoundingClientRect();const out={compact:document.documentElement.classList.contains('tp-nav-compact'),navTop:nr?.top,actionsTop:ar?.top,brandActionsDisplay:brandActions?getComputedStyle(brandActions).display:null};window.scrollTo(0,0);spacer.remove();await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));return out});
  assert.equal(compactHeader.compact,true,'real scroll must enter compact navigation mode');
  assert.ok(Math.abs(compactHeader.navTop-compactHeader.actionsTop)<12,'compact navigation and Coach/Settings actions must share one row: '+JSON.stringify(compactHeader));
  assert.equal(compactHeader.brandActionsDisplay,null,'removed brand strip must not retain duplicate Coach/Settings actions');

  const homeActive=page.locator('main.rf221-home > .hero.tp155-home-active-card');
  assert.equal(await homeActive.count(),1,'Home must expose one highlighted active-program card');assert.equal(await homeActive.evaluate(e=>e.classList.contains('tp155-r4-accent-surface')),true,'Home active program must use the shared highlighted surface');
  const homeLayout=await homeActive.evaluate(e=>{const g=e.querySelector('.tp155-home-active-grid'),l=e.querySelector('.tp155-home-program-left'),r=e.querySelector('.tp155-home-program-right'),h=e.querySelector('h1'),cs=getComputedStyle(e),gr=getComputedStyle(g),lr=l.getBoundingClientRect(),rr=r.getBoundingClientRect();return {border:parseFloat(cs.borderTopWidth),grid:gr.display,leftX:lr.x,rightX:rr.x,titleFont:parseFloat(getComputedStyle(h).fontSize),height:e.getBoundingClientRect().height,next:String(r.textContent||'').trim()}});
  assert.ok(homeLayout.border>=1.5,'Basic themes keep a visible matte selection frame on the Home active program: '+JSON.stringify(homeLayout));
  assert.equal(homeLayout.grid,'grid','Home active program content must use a two-column grid');
  assert.ok(homeLayout.rightX>homeLayout.leftX,'next workout/program information must sit to the right of the active program');
  assert.ok(homeLayout.titleFont>=21,'active program title must be more prominent');
  assert.ok(homeLayout.next.length>0,'right column must not stay empty');
  assert.ok(homeLayout.height<190,'Home active-program block should remain compact: '+JSON.stringify(homeLayout));
  const basicVisual=await homeActive.evaluate(e=>({shadow:getComputedStyle(e).boxShadow,borderColor:getComputedStyle(e).borderTopColor,family:document.documentElement.dataset.tpThemeFamily}));
  assert.equal(basicVisual.family,'basic');assert.equal(basicVisual.shadow,'none','Basic themes must not use glow on the active-program card');
  const homeFlow=await page.locator('main.rf221-home').evaluate(e=>({minHeight:getComputedStyle(e).minHeight,scrollHeight:document.documentElement.scrollHeight,viewport:innerHeight}));
  assert.equal(homeFlow.minHeight,'0px','Home must not force a full-viewport minimum height that creates needless scrolling');

  // Calendar is the source of truth for today's planned workout for the whole local day.
  const homeCalendarScenario=await page.evaluate(()=>{
   const p=activeProgram(),days=p?.days||[];if(days.length<2)return null;
   const first=days[0],second=days[1],now=new Date(),todayStart=new Date(now),futureStart=new Date(now);
   todayStart.setHours(0,0,0,0);futureStart.setDate(futureStart.getDate()+2);futureStart.setHours(18,0,0,0);
   window.__tp155HomeCalendarBackup={scheduled:structuredClone(scheduled()),history:structuredClone(history()),draft:db.get('draft',null)};
   const todayId='tp155-home-calendar-today',futureId='tp155-home-calendar-future';
   db.set('history',[{id:'tp155-home-calendar-history',programId:p.id,dayId:second.id,workout:second.id,started:new Date(now.getTime()-86400000).toISOString(),date:new Date(now.getTime()-86400000).toISOString(),exercises:[]}]);
   db.set('scheduled',[
    {id:todayId,programId:p.id,dayId:second.id,workout:second.id,start:todayStart.toISOString(),end:new Date(todayStart.getTime()+3600000).toISOString(),status:'planned',updatedAt:Date.now(),cancelled:false},
    {id:futureId,programId:p.id,dayId:first.id,workout:first.id,start:futureStart.toISOString(),end:new Date(futureStart.getTime()+3600000).toISOString(),status:'planned',updatedAt:Date.now(),cancelled:false}
   ]);
   db.set('draft',null);state.session=null;state.workout=null;state.tab='home';render();
   return {fallback:nextWorkout(),planned:nextPlanned()?.dayId,todayId,first:first.id,second:second.id};
  });
  assert.ok(homeCalendarScenario,'Home/Calendar priority scenario requires a two-day program');
  assert.equal(homeCalendarScenario.fallback,homeCalendarScenario.first,'test setup must reproduce the old cycle fallback mismatch');
  assert.equal(homeCalendarScenario.planned,homeCalendarScenario.second,'today\'s planned Calendar day must win even after its clock time');
  assert.match(await page.locator('main.rf221-home .tp155-home-program-right').innerText(),new RegExp(homeCalendarScenario.second),'Home next-workout block must show today\'s Calendar day');
  const plannedStart=page.locator('main.rf221-home .hero > .btn').first();
  assert.match(await plannedStart.getAttribute('onclick'),/startScheduledById/,'Home start action must use the scheduled item rather than the cycle fallback');
  await plannedStart.click();
  assert.deepEqual(await page.evaluate(()=>({day:state.session?.dayId||state.session?.workout||state.workout,scheduleId:state.session?.scheduleId||null})),{day:homeCalendarScenario.second,scheduleId:homeCalendarScenario.todayId},'Home must start the same day that Calendar scheduled for today');
  await page.evaluate(()=>{const b=window.__tp155HomeCalendarBackup;state.session=null;state.workout=null;db.set('scheduled',b.scheduled);db.set('history',b.history);db.set('draft',b.draft);delete window.__tp155HomeCalendarBackup;state.tab='home';render()});
  await page.waitForSelector('main.rf221-home');
  const navSizing=await page.evaluate(()=>{const cell=document.querySelector('.top.tp154-nav-grid .tp154-nav-cell'),icon=document.querySelector('.top.tp154-nav-grid .tp151-nav-icon,.top.tp154-nav-grid .tp154-nav-action-icon'),label=document.querySelector('.top.tp154-nav-grid .tp151-nav-label'),active=document.querySelector('.top.tp154-nav-grid .tp151-nav-item.active');return {cellH:cell?.getBoundingClientRect().height||0,icon:parseFloat(getComputedStyle(icon).fontSize)||0,label:parseFloat(getComputedStyle(label).fontSize)||0,shadow:active?getComputedStyle(active).boxShadow:'',border:active?parseFloat(getComputedStyle(active).borderTopWidth):0}});
  assert.ok(navSizing.cellH<=57,'2x4 navigation cell height must not increase: '+JSON.stringify(navSizing));assert.ok(navSizing.icon>=21,'navigation icons should be larger without taller cells');assert.ok(navSizing.label>=12.5,'Hungarian navigation labels should be more readable');assert.equal(navSizing.shadow,'none','Basic active navigation must stay matte without glow');assert.ok(navSizing.border>=1,'Basic active navigation keeps a matte selection frame');
  await page.evaluate(()=>rf200SetTheme('blue'));await page.waitForSelector('main.rf221-home > .hero.tp155-home-active-card');
  const neonVisual=await page.locator('main.rf221-home > .hero.tp155-home-active-card').evaluate(e=>({border:parseFloat(getComputedStyle(e).borderTopWidth),shadow:getComputedStyle(e).boxShadow,family:document.documentElement.dataset.tpThemeFamily}));
  assert.equal(neonVisual.family,'vivid');assert.ok(neonVisual.border>=1.5,'Neon themes may use the stronger colored selection frame');assert.notEqual(neonVisual.shadow,'none','Neon themes may use a subtle glow');
  await page.evaluate(()=>rf200SetTheme('classicBlue'));await page.waitForSelector('main.rf221-home');

  await page.evaluate(()=>{state.session=null;state.workout=null;state.tab='plan';render()});
  await page.waitForSelector('.tp152-active-program');
  const trainingSettings=page.locator('.tp1511-training');assert.equal(await trainingSettings.count(),1,'Workout settings must remain on the Workout page');
  const trainingSurface=await trainingSettings.evaluate(e=>({border:parseFloat(getComputedStyle(e).borderTopWidth),background:getComputedStyle(e).backgroundImage,height:e.getBoundingClientRect().height,stylePresent:!!document.getElementById('tp155WorkoutSettingsCompactCss')}));assert.ok(trainingSurface.border>=1&&trainingSurface.background!=='none','Workout settings must use the compact colored outline treatment: '+JSON.stringify(trainingSurface));assert.ok(trainingSurface.height<90,'collapsed Workout settings must stay compact');
  await trainingSettings.locator(':scope > summary').click();assert.ok(await trainingSettings.getAttribute('open')!==null);
  const trainingFieldHeight=await trainingSettings.locator('.field').first().evaluate(e=>e.getBoundingClientRect().height);assert.ok(trainingFieldHeight<=38,'Workout settings fields must stay compact');
  const trainingSave=trainingSettings.locator('.btn[onclick*="tp1511SaveTraining"]');const saveNormal=await trainingSave.evaluate(e=>getComputedStyle(e).backgroundColor);assert.match(saveNormal,/rgb\((?:3[0-9]|4[0-9]),\s*(?:7[0-9]|8[0-9]),\s*(?:5[0-9]|6[0-9])\)/,'Workout settings Save must use the dark green resting state');
  await trainingSettings.locator(':scope > summary').click();
  assert.equal(await page.locator('.tp152-active-program').count(),1);
  assert.ok(await page.locator('.tp152-day').count()>=2,'active program must expose workout days hierarchically');
  assert.equal(await page.locator('.tp152-day[open]').count(),1,'only the first workout day should start expanded');
  const firstDay=page.locator('.tp152-day').first(),firstExercise=firstDay.locator('.tp152-exercise').first();
  assert.equal(await firstExercise.getAttribute('open'),null,'exercise editor starts collapsed');
  await firstExercise.locator('.tp146-exercise-copy').click();assert.ok(await firstExercise.getAttribute('open')!==null,'exercise must expand inline');
  assert.equal(await firstExercise.locator('.tp152-edit-grid').isVisible(),true,'inline exercise fields must be visible');
  const loadAudit=await page.evaluate(()=>{
   const p=activeProgram(),d=p.days[0],id=(d.exercises||[]).find(x=>byId(x)?.loadType!=='bodyweight'),e=id?byId(id):null,row=id?document.querySelector('[data-tp146-exercise="'+CSS.escape(id)+'"]'):null;
   return {unit:e?tp149LoadLabel(e.loadType):'',labels:row?[...row.querySelectorAll('.tp152-edit-grid label')].map(x=>x.textContent):[]};
  });
  assert.ok(loadAudit.unit&&loadAudit.labels.some(x=>x.includes(loadAudit.unit)),'load editor must preserve loadType unit such as kg/kar or kg összesen');

  await page.locator('.tp150-quick-entry button').click();await page.waitForSelector('#tp150QuickResults');
  const quick=page.locator('#tp150QuickResults .tp152-quick-row').first();await quick.locator('.tp150-quick-copy').click();
  assert.equal(await quick.locator('.tp152-quick-body').isVisible(),true,'Quick Workout must expand inline');
  assert.equal(await page.locator('.tp151-quick-config').count(),0,'Quick Workout must not open the old separate config page');
  assert.ok(await page.locator('#tp150QuickResults .tp-play-btn').count()>0,'Quick Workout video control must be restored');
  const quickUnit=await page.evaluate(()=>{const e=[...exercises()].find(x=>x.loadType!=='bodyweight'&&demoInfo(x.id));if(!e)return null;const row=document.querySelector('[data-exercise-id="'+CSS.escape(e.id)+'"]');return {unit:tp149LoadLabel(e.loadType),text:row?.textContent||''}});
  assert.ok(quickUnit&&quickUnit.text.includes(quickUnit.unit),'Quick Workout load input must show the exercise-specific load unit');
  const flatVideo=await page.locator('#tp150QuickResults .tp-play-btn').first().evaluate(e=>({w:e.getBoundingClientRect().width,h:e.getBoundingClientRect().height}));
  assert.ok(flatVideo.w>flatVideo.h,'video controls must use the flatter global style');

  await page.evaluate(()=>{state.session=null;state.workout=null;go('programs')});await page.waitForSelector('.tp152-program-card');
  assert.equal(await page.locator('.tp152-program-card.active-program.tp155-r4-accent-surface').count(),1,'Programs active program must use the shared highlighted surface');
  assert.ok(await page.locator('.tp152-program-card').count()>=1);const pc=page.locator('.tp152-program-card').first();await pc.locator(':scope > summary').click();
  assert.ok(await pc.getAttribute('open')!==null,'program card must expand inline');assert.ok(await pc.locator('.tp152-program-day').count()>=1);
  const programDay=pc.locator('.tp152-program-day').first();await programDay.locator(':scope > summary').click();
  assert.ok(await programDay.locator('.tp152-program-exercise').count()>=1,'Programs must show detailed collapsible exercise rows');
  assert.ok(await programDay.locator('.tp152-program-exercise .meta').first().innerText(),'program exercise summary must include prescription details');
  assert.ok(await programDay.locator('.tp152-program-exercise .tp-play-btn').count()>0,'program exercise rows must retain video controls');

  await page.evaluate(()=>profileScreen());await page.waitForSelector('.tp152-profile');
  assert.equal(await page.locator('.tp152-profile-group').count(),5,'personal planner must be grouped into five hierarchical sections');
  const goalGroup=page.locator('.tp152-profile-group').nth(1);
  assert.equal(await goalGroup.getAttribute('open'),null,'goal and experience group should start collapsed');
  await goalGroup.locator(':scope > summary').click();
  assert.ok(await goalGroup.getAttribute('open')!==null,'goal and experience group must expand before interacting with its controls');
  const profileSelect=goalGroup.locator('.tp-select').first();await profileSelect.locator('.tp-select-trigger').click();
  assert.equal(await profileSelect.evaluate(e=>e.classList.contains('open')),true,'custom dropdown must open');
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true);assert.equal(await profileSelect.evaluate(e=>e.classList.contains('open')),false,'Android back must close an open dropdown before leaving the page');
  assert.equal(await page.evaluate(()=>state.tab),'profile','closing a dropdown must keep the current page');

  await page.evaluate(()=>go('settings'));await page.waitForSelector('#tp155R4PanelHost[data-panel="settings"] .tp152-settings');
  const settingsHost=page.locator('#tp155R4PanelHost[data-panel="settings"]');
  const themePanel=settingsHost.locator('.tp155-theme-setting');assert.equal(await themePanel.count(),1,'theme palette must use the aligned Settings card');
  assert.match(await themePanel.evaluate(e=>e.textContent||''),/Neon színek/,'vivid theme group must be renamed to Neon színek');
  const languagePanel=settingsHost.locator('.tp155-language-direct');assert.equal(await languagePanel.count(),1,'Language chooser must be directly available without an outer accordion');
  assert.equal(await languagePanel.locator('.tp-select').count(),1,'Language chooser must reuse the custom dropdown list');
  assert.doesNotMatch(await languagePanel.innerText(),/A telefon nyelve az alapértelmezett/,'redundant system-language help must be removed');
  await languagePanel.locator('.tp-select-trigger').click();assert.equal(await languagePanel.locator('.tp-select').evaluate(e=>e.classList.contains('open')),true,'Language list must open on the first tap');
  const languageOpenStyle=await languagePanel.locator('.tp-select-trigger').evaluate(e=>({shadow:getComputedStyle(e).boxShadow,border:parseFloat(getComputedStyle(e).borderTopWidth)}));assert.equal(languageOpenStyle.shadow,'none','Basic language dropdown must not glow');assert.ok(languageOpenStyle.border>=1,'Basic language dropdown may keep a matte selection border');
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true);assert.equal(await languagePanel.locator('.tp-select').evaluate(e=>e.classList.contains('open')),false,'Android back must close the open language list before the Settings sheet');
  assert.equal(await settingsHost.locator('.tp152-settings > details.tp152-accordion').count(),0,'Settings must not keep a nested accordion after Theme becomes a list dialog');
  assert.equal(await settingsHost.locator('.tp152-settings > .tp152-settings-backup').count(),1,'Backup must remain directly visible');
  assert.equal(await settingsHost.locator('.tp152-settings > .tp152-settings-profile').count(),1,'Training profile must remain directly visible');
  const dangerStyle=await settingsHost.locator('.tp152-settings-profile .btn.danger').evaluate(e=>getComputedStyle(e).backgroundColor);
  const safeStyle=await settingsHost.locator('.tp152-settings-profile .btn.secondary').evaluate(e=>getComputedStyle(e).backgroundColor);
  assert.notEqual(dangerStyle,safeStyle,'destructive actions must keep a distinct red danger treatment');
  assert.equal(await page.evaluate(()=>tp152T('historyEdit')),'Naplózott edzés módosítása');
  const settingsActive=page.locator('.rf208-settings-btn.active');assert.equal(await settingsActive.count(),1,'Settings grid cell must stay visibly active while its sheet is open');
  await themePanel.locator('.tp155-theme-open').click();assert.equal(await page.locator('#tp155ThemePicker').count(),1,'Theme color must open as a list dialog');
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true);assert.equal(await page.locator('#tp155ThemePicker').count(),0,'Android back must close the Theme list first');
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true);assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'next Android back must close the Settings sheet');
  assert.equal(await page.evaluate(()=>state.tab),'profile','closing Settings sheet must preserve the underlying full page');
  await page.evaluate(()=>go('home'));await page.waitForSelector('main.rf221-home');
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),false,'Android back at Home root should be released to Android');
  await page.evaluate(()=>rf233CoachScreen());await page.waitForSelector('#tp155R4PanelHost[data-panel="coach"] .tp151-coach');
  const coachHost=page.locator('#tp155R4PanelHost[data-panel="coach"]');
  assert.equal(await coachHost.locator('.tp151-coach-recommendation .tp151-advice').count(),1,'Coach panel must reuse the highlighted Health recommendation style');
  assert.equal(await coachHost.locator('.tp151-coach-recommendation.tp155-r4-accent-surface').count(),1,'Coach recommendation is the shared highlighted-surface reference');
  assert.equal(await coachHost.locator('.tp153-coach-next').count(),1,'Coach next workout must be an actionable navigation surface');
  const coachShort=coachHost.locator('.tp155-coach-short');assert.ok(await coachShort.count()>=1,'Coach must show concise per-exercise recommendations');
  const coachLengths=await coachShort.evaluateAll(es=>es.map(e=>String(e.textContent||'').trim().length));assert.ok(coachLengths.every(n=>n>0&&n<=90),'Coach exercise recommendation must stay concise: '+JSON.stringify(coachLengths));
  await page.evaluate(()=>go('health'));await page.waitForSelector('main.rf263-health');
  assert.equal(await page.locator('#tp155R4PanelHost').count(),0,'opening a full page must close the Coach panel');
  assert.equal(await page.locator('main.rf263-health.tp155-r4-page-reveal').count(),0,'Health full page must not restore the legacy heavy reveal motion');
  assert.ok(await page.locator('main.rf263-health').evaluate(e=>parseFloat(getComputedStyle(e).animationDuration)*1000)<=110.5,'Health full-page reveal must remain lightweight');
  assert.ok(await page.locator('main.rf263-health .stat[data-tp153-link="weight"]').count()>=1,'Health weight stat must link to the weight journal');
  assert.ok(await page.locator('main.rf263-health .stat[data-tp153-link="recovery"]').count()>=2,'Health Sleep and HRV stats must link to recovery history');
  assert.ok(await page.locator('main.rf263-health .tp151-health-card.tp155-r4-accent-surface').count()>=1,'Health daily summary must use the shared highlighted surface');
  await page.evaluate(()=>go('home'));

  await page.evaluate(()=>go('history'));await page.waitForSelector('main.tp150-journal-compact');
  const journalFilter=page.locator('.tp155-journal-filter');assert.equal(await journalFilter.count(),1,'Journal must expose one inline date filter');
  assert.match(await journalFilter.locator('summary').innerText(),/Szűrő dátum szerint/);
  assert.equal(await journalFilter.getAttribute('open'),null,'Journal date calendar starts collapsed');
  await journalFilter.locator('summary').click();assert.ok(await journalFilter.getAttribute('open')!==null,'Journal calendar must expand inline');
  assert.equal(await page.locator('.rf263-date-dialog').count(),0,'Journal date filtering must not open the old modal calendar');
  await page.evaluate(()=>tp155JournalSetMonth('2026-09-01',true));
  await page.locator('.tp155-journal-filter[open] [data-date="2026-09-10"]').click();
  assert.deepEqual(await page.evaluate(()=>({filter:rf263HistoryFilter,start:state.tp155JournalRangeStart})),{filter:{from:'',to:''},start:'2026-09-10'},'first Journal tap must establish only the Ettől boundary');
  assert.ok(await page.locator('.tp155-journal-filter').getAttribute('open')!==null,'Journal calendar stays open while waiting for Eddig');
  await page.locator('.tp155-journal-filter[open] [data-date="2026-09-15"]').click();
  assert.deepEqual(await page.evaluate(()=>rf263HistoryFilter),{from:'2026-09-10',to:'2026-09-15'},'second Journal tap must commit the inclusive Ettől–Eddig range');
  assert.equal(await page.locator('.tp155-journal-filter.tp155-range-active.tp155-r4-accent-surface').count(),1,'active Journal range must use the shared highlighted surface');
  assert.equal(await page.locator('.tp155-journal-filter [data-date="2026-09-12"].is-range').count(),1,'days between the endpoints must be visibly marked as the selected range');
  const clearJournal=page.locator('.tp155-journal-actions .btn').first();assert.equal(await clearJournal.isEnabled(),true);await clearJournal.click();
  assert.deepEqual(await page.evaluate(()=>rf263HistoryFilter),{from:'',to:''});assert.equal(await page.locator('.tp155-journal-filter').getAttribute('open'),null,'Clear filter must clear both boundaries and collapse the inline calendar');

  await page.evaluate(()=>{state.rf2211CalendarDate='';go('calendar')});await page.waitForSelector('#tp155R4PanelHost[data-panel="calendar"] .tp155-r4-calendar-panel-main');
  const calendarHost=page.locator('#tp155R4PanelHost[data-panel="calendar"]');
  assert.equal(await calendarHost.locator('.tp151-page-head').count(),0,'Calendar dropdown must remove the old Edzésnaptár heading and helper text');
  assert.equal(await calendarHost.locator('.tp151-calendar-frame').count(),1,'Calendar month grid must be the first primary panel content');
  assert.equal(await calendarHost.locator('.rf2211-planner').count(),1,'Planning settings remain at the bottom of Calendar');
  assert.equal(await calendarHost.locator('.rf2211-planner').evaluate(e=>e.tagName),'SECTION','Planning settings must always stay open');
  assert.equal(await calendarHost.locator('.rf2211-planner > summary').count(),0,'Planning settings must no longer be collapsible');
  const firstDayWrap=calendarHost.locator('#rf2211First').locator('xpath=..');
  await firstDayWrap.locator('.tp-select-trigger').scrollIntoViewIfNeeded();
  await firstDayWrap.locator('.tp-select-trigger').click();
  const anchoredSelect=await firstDayWrap.evaluate(w=>{const t=w.querySelector('.tp-select-trigger'),m=w.querySelector('.tp-select-menu'),p=w.closest('.tp155-r4-panel'),tr=t?.getBoundingClientRect(),mr=m?.getBoundingClientRect(),vv=window.visualViewport;return {placement:m?.dataset?.placement||'',trigger:{left:tr?.left||0,right:tr?.right||0,top:tr?.top||0,bottom:tr?.bottom||0},menu:{left:mr?.left||0,right:mr?.right||0,top:mr?.top||0,bottom:mr?.bottom||0},viewport:{left:vv?.offsetLeft||0,top:vv?.offsetTop||0,right:(vv?.offsetLeft||0)+(vv?.width||innerWidth),bottom:(vv?.offsetTop||0)+(vv?.height||innerHeight)},panelTransform:p?getComputedStyle(p).transform:''}});
  assert.equal(anchoredSelect.panelTransform,'none','settled Calendar panel must release its reveal transform before positioning fixed dropdowns');
  assert.ok(Math.abs(anchoredSelect.menu.left-anchoredSelect.trigger.left)<=3,'Calendar custom select must stay horizontally anchored to its trigger: '+JSON.stringify(anchoredSelect));
  if(anchoredSelect.placement==='top')assert.ok(anchoredSelect.menu.bottom<=anchoredSelect.trigger.top+8,'top-placed Calendar menu must remain above its trigger');
  else assert.ok(anchoredSelect.menu.top>=anchoredSelect.trigger.bottom-2,'bottom-placed Calendar menu must remain below its trigger');
  assert.ok(anchoredSelect.menu.left>=anchoredSelect.viewport.left-2&&anchoredSelect.menu.right<=anchoredSelect.viewport.right+2&&anchoredSelect.menu.top>=anchoredSelect.viewport.top-2&&anchoredSelect.menu.bottom<=anchoredSelect.viewport.bottom+2,'Calendar custom select must stay inside the visual viewport');
  await page.evaluate(()=>rf260CloseSelects());
  await page.evaluate(()=>{state.rf2211CalendarDate='2026-09-28';render()});await page.waitForSelector('#tp155R4PanelHost .tp151-day-panel');
  assert.equal(await calendarHost.locator('.tp151-day-panel.tp155-r4-accent-surface').count(),1,'selected-day Calendar details must use the shared highlighted surface');
  const before=await page.evaluate(()=>scheduled().filter(x=>!x.cancelled).length);
  const dayId=await page.evaluate(()=>activeProgram().days[0].id);
  await page.evaluate(dayId=>{const a=document.createElement('button');a.id='tp152-anchor';a.textContent='anchor';document.querySelector('#tp155R4PanelHost main').appendChild(a);rf2211AddDay(dayId,a)},dayId);
  await page.waitForSelector('#tpTemporalPicker');
  assert.equal(await page.evaluate(()=>scheduled().filter(x=>!x.cancelled).length),before,'selecting a workout day must ask for time before saving');
  assert.equal(await page.locator('#tpTemporalPicker select').count(),0,'time picker must not use native select controls');
  assert.equal(await page.locator('#tpTemporalPicker .tp155-time-option').count(),84,'time picker must expose scrollable hour and minute lists');
  const timePickerStyle=await page.evaluate(()=>{const modal=document.querySelector('#tpTemporalPicker'),card=modal?.querySelector('.tp155-time-card'),anchor=document.querySelector('#tp152-anchor'),cs=modal&&getComputedStyle(modal);return {blur:cs?.backdropFilter||cs?.webkitBackdropFilter||'',cardTop:card?.getBoundingClientRect().top||0,anchorTop:anchor?.getBoundingClientRect().top||0}});assert.match(timePickerStyle.blur,/blur\(/,'time picker must blur content behind the upward list');assert.ok(timePickerStyle.cardTop<timePickerStyle.anchorTop,'time picker should open upward from its trigger: '+JSON.stringify(timePickerStyle));
  await page.locator('[data-tp-hour="19"]').click();await page.locator('[data-tp-minute="20"]').click();await page.locator('#tp155R3TimeSave').click();
  assert.equal(await page.evaluate(()=>scheduled().filter(x=>!x.cancelled).length),before+1,'calendar item must save only after time confirmation');
  const addedId=await page.evaluate(()=>scheduled().filter(x=>!x.cancelled).at(-1)?.id);
  await page.evaluate(id=>tp151EditCalendarItem(id),addedId);await page.waitForSelector('#tp155R4PanelHost .tp151-calendar-item .tp151-form-grid');
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true);assert.equal(await page.evaluate(()=>state.tp151EditingScheduleId),null,'Android back must leave Calendar inline edit first');
  assert.equal(await page.locator('#tp155R4PanelHost[data-panel="calendar"]').count(),1,'closing Calendar inline edit must keep the Calendar panel open');
  await page.evaluate(()=>{tp155R4ClosePanel(false);state.tab='history';state.rf151HistoryEdit={key:'tp152-back-probe',workout:{}};render()});
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true);assert.equal(await page.evaluate(()=>state.rf151HistoryEdit),null,'Android back must leave Journal inline edit first');
  assert.equal(await page.evaluate(()=>state.tab),'history','closing Journal inline edit must stay in Log');

  await page.evaluate(()=>{state.session=null;state.workout=null;const p=activeProgram(),d=p.days[0];startWorkout(d.id,null,p.id)});await page.waitForSelector('main.tp153-workout');
  assert.equal(await page.locator('main.tp153-workout h1').count(),1,'active workout must show the exercise title only once as a heading');
  assert.equal(await page.locator('main.tp153-workout .tp153-workout-head').count(),1,'active workout must use one compact header card');
  assert.equal(await page.locator('main.tp153-workout .tp153-workout-head.tp155-r4-accent-surface').count(),1,'current exercise card must use the shared highlighted surface');
  assert.equal(await page.locator('main.tp153-workout .tp153-set-row.tp155-r4-accent-surface-lite').count(),1,'one current set row must use the lighter highlighted surface');
  const workoutCoach=page.locator('main.tp153-workout .tp155-workout-coach');assert.equal(await workoutCoach.count(),1,'active workout must show the current exercise Coach suggestion');
  assert.ok((await workoutCoach.innerText()).trim().length<=140,'active workout Coach suggestion must stay compact');
  assert.equal(await page.locator('main.tp153-workout .video-card').count(),0,'active workout must not keep the old large video card');
  assert.ok(await page.locator('main.tp153-workout .tp153-video-launch').count()<=1,'active workout uses only the compact demo launch');
  assert.equal(await page.locator('main.tp153-workout > .tp153-workout-guide').count(),0,'workout guidance must no longer sit at the bottom of the workout');
  const demoId=await page.evaluate(()=>state.session.exercises.find(x=>demoInfo(x.id))?.id||null);
  if(demoId){
   await page.evaluate(id=>{const i=state.session.exercises.findIndex(x=>x.id===id);if(i>=0){state.current=i;renderWorkout()}},demoId);
   const topGuide=page.locator('.tp153-workout-head .tp154-workout-guide');assert.equal(await topGuide.count(),1,'exercise demo and guidance must be discoverable in the top workout card');
   assert.equal(await topGuide.locator('.tp154-workout-guide-body').isVisible(),false,'top demo guidance starts compact');
   await topGuide.locator('summary').click();assert.equal(await topGuide.locator('.tp154-workout-guide-body').isVisible(),true,'top demo guidance must expand inline');
   assert.equal(await topGuide.locator('.tp153-video-launch').count(),1,'expanded top guidance must contain the demo launch');
   await page.evaluate(id=>openDemo(id),demoId);await page.waitForSelector('#videoModal');assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true,'Android back must consume the open video modal');assert.equal(await page.locator('#videoModal').count(),0,'Android back must close video before navigating')
  }
  await page.evaluate(()=>{state.session=null;state.workout=null;go('home')});

  await page.evaluate(()=>rf203ExerciseLibrary());await page.waitForSelector('details.tp-library-card');
  const lib=page.locator('details.tp-library-card').first();assert.equal(await lib.getAttribute('open'),null);await lib.locator('summary').click();assert.equal(await lib.locator('.tp-library-body').isVisible(),true,'exercise library must expand inline');

  assert.deepEqual(errors,[]);
  console.log('PASS: TrainPilot 1.5.2 hierarchical UX, inline editors, anchored time picker and Android back contract.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
