'use strict';
// PR #271 P3a: real Journal and Progress in the opt-in visual shell.
// No production data, mocked stat source or new chart engine. The existing
// history rendering, period calculations and muscle illustrations remain.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const u=new URL(req.url,'http://localhost'),file=path.resolve(root,'.'+(u.pathname==='/'?'/index.html':u.pathname));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(e,b)=>{if(e){res.writeHead(404);res.end();return}
 res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(b)});
});
(async()=>{
 await new Promise(ok=>server.listen(0,'127.0.0.1',ok));
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  for(const width of [320,393]){
   const page=await browser.newPage({viewport:{width,height:width===320?740:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
   const errors=[];page.on('pageerror',e=>errors.push(e.message||String(e)));
   page.on('dialog',dialog=>dialog.accept());
   await page.addInitScript(()=>{try{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))}catch(_){}});
   const url='http://127.0.0.1:'+server.address().port+'/';
   await page.goto(url);
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   await page.evaluate(()=>go('history'));
   assert.equal(await page.locator('#tp271Dock').count(),0,width+': default main unchanged');
   await page.goto(url+'?tp271=1');
   await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotMockup271?.enabled&&document.querySelector('#tp271Header'));
   const header=page.locator('#tp271Header');
   assert.equal(await header.count(),1,width+': one compact mockup header');
   assert.equal(await header.locator('.tp271-header-brand strong').textContent(),'TrainPilot',width+': original brand');
   assert.equal(await header.locator('.tp271-header-mark path').count(),1,width+': real launcher dumbbell silhouette');
   assert.equal(await header.locator('button[data-tp271-header]').count(),3,width+': Settings, Language, Theme only');
   assert.equal(await page.locator('#tp271Dock button[data-tp271-target]').count(),5,width+': exactly five permanent bottom tabs');
   assert.equal(await page.locator('.top.tp154-nav-grid .tp151-nav-item').first().isVisible(),false,width+': no surviving 2x4 top navigation');
   assert.equal(await page.locator('.top.tp154-nav-grid .tp154-settings-action').isVisible(),false,width+': original settings handler is accessible but not duplicated visually');
   const headerSize=await header.evaluate(el=>({width:el.getBoundingClientRect().width,height:el.getBoundingClientRect().height,overflow:document.documentElement.scrollWidth-innerWidth}));
   assert.ok(headerSize.height>=44&&headerSize.height<=70,width+': compact header geometry '+JSON.stringify(headerSize));
   assert.ok(headerSize.overflow<=1,width+': new header has no overflow');
   if(width===393){
     await header.locator('button[data-tp271-header="language"]').click();
     assert.equal(await header.locator('.tp271-header-picker[data-kind="language"]').count(),1,'language picker lives in the header');
     await header.locator('.tp271-header-picker button').filter({hasText:'English'}).click();
     assert.equal(await page.evaluate(()=>rf212LangSetting()),'en','main language store updated through header');
     await page.waitForFunction(()=>document.querySelector('#tp271Header button[data-tp271-header="settings"]')?.getAttribute('aria-label')==='Settings');
     await page.locator('#tp271Header button[data-tp271-header="theme"]').click();
     assert.equal(await page.locator('#tp271Header .tp271-header-picker[data-kind="theme"] .tp271-picker-option').count()>10,true,'all main theme choices preserved');
     await page.locator('#tp271Header .tp271-header-picker[data-kind="theme"] .tp271-picker-option[data-tp271-choice="classicBlue"]').click();
     assert.equal(await page.evaluate(()=>rf200ThemeKey()),'classicBlue','main theme store updated through header');
     await page.evaluate(()=>{rf212SetLang('system');rf200SetTheme('yellow')});
   }
   await page.evaluate(()=>{
     state.session=null;state.workout=null;db.set('draft',null);
     const at=(offset,hour)=>{
       const d=new Date();d.setHours(hour,0,0,0);d.setDate(d.getDate()+offset);
       return d.toISOString();
     };
     const lift=(weight,reps)=>({id:'db-floor-press',repUnit:'ism.',sets:[{set:1,weight,reps:String(reps),done:true}]});
     const row=(id,offset,weight)=>({id,workout:'A',dayId:'A',programId:'home-basic',
       programName:'Valódi tesztprogram',started:at(offset,17),finished:at(offset,18),
       exercises:[lift(weight,10)],photos:[]});
     db.set('history',[row('p271-journal-1',-1,12),row('p271-journal-2',-2,11),row('p271-journal-3',-5,10)]);
   });
   for(const theme of ['classicBlue','yellow']){
     await page.evaluate(t=>{rf200SetTheme(t);state.tp177JournalView='log';go('history')},theme);
     await page.waitForSelector('main.tp150-journal-compact .tp155-journal-filter');
     const journal=page.locator('main.tp150-journal-compact');
     if(width===393&&theme==='yellow'){
       fs.mkdirSync('ui-evidence',{recursive:true});
       await page.waitForFunction(()=>document.querySelector('#tp271Dock button[data-tp271-target="log"]')?.getAttribute('aria-current')==='page');
       await page.evaluate(()=>{document.scrollingElement.scrollTop=0;document.activeElement?.blur?.()});
       await page.screenshot({path:'ui-evidence/pr271-journal-393.png'});
     }
     assert.equal(await journal.locator('.tp177-journal-tabs button').count(),3,width+'/'+theme+': three real Journal destinations');
     assert.equal(await journal.locator('details.rf263-history').count(),3,width+'/'+theme+': actual saved workouts remain');
     const filtered=journal.locator('.tp155-journal-filter');
     assert.equal(await filtered.count(),1,width+'/'+theme+': canonical calendar survives');
     const first=journal.locator('details.rf263-history').first();
     assert.equal(await first.getAttribute('open'),null,width+'/'+theme+': real workouts start compact');
     await first.locator(':scope > summary').click();
     assert.equal(await first.locator('.history-body').isVisible(),true,width+'/'+theme+': workout sets/details still expand');
     assert.ok(await first.locator('.history-body').innerText().then(t=>t.length>10),width+'/'+theme+': real workout detail, not empty card');
     await first.locator(':scope > summary').click();
     await filtered.locator(':scope > summary').click();
     assert.equal(await journal.locator('.tp155-journal-calendar').isVisible(),true,width+'/'+theme+': date filter opens');
     assert.equal(await journal.locator('.tp155-journal-day').count()>=28,true,width+'/'+theme+': actual month dates');
     const date=await journal.locator('.tp155-journal-day[aria-current="date"]').getAttribute('data-date');
     assert.ok(date,width+'/'+theme+': real today is indicated');
     // The canonical Round-4 Journal picker selects a range in two taps:
     // first tap stores a pending start; second tap commits start/end.
     await journal.locator('.tp155-journal-day[aria-current="date"]').click();
     assert.equal(await page.evaluate(()=>!!state.tp155JournalRangeStart),true,width+'/'+theme+': first date is pending');
     await journal.locator('.tp155-journal-day[data-date="'+date+'"]').click();
     assert.equal(await page.evaluate(()=>rf263HistoryFilter.from===rf263HistoryFilter.to&&!!rf263HistoryFilter.from),true,width+'/'+theme+': second tap commits the original date-range filter');
     await page.evaluate(()=>{if(typeof window.rf263ClearHistoryFilter==='function')window.rf263ClearHistoryFilter();go('history')});
     await page.waitForSelector('main.tp150-journal-compact');
     const previewMetrics=await page.evaluate(()=>{
       const main=document.querySelector('#app main.tp150-journal-compact'),dock=document.getElementById('tp271Dock');
       document.scrollingElement.scrollTop=document.scrollingElement.scrollHeight;
       const last=main.querySelector('details.rf263-history:last-of-type');
       return {clearance:parseFloat(getComputedStyle(main).paddingBottom),
         lastBottom:last?.getBoundingClientRect().bottom,
         dockTop:dock.getBoundingClientRect().top,
         overflows:document.documentElement.scrollWidth>innerWidth+1};
     });
     assert.ok(previewMetrics.clearance>=80,width+'/'+theme+': Journal leaves bottom-dock scroll clearance');
     assert.equal(previewMetrics.overflows,false,width+'/'+theme+': no horizontal Journal overflow');

     await page.locator('#tp271Dock button[data-tp271-target="progress"]').click();
     await page.waitForSelector('main.tp177-progress .tp177-volume-panel');
     assert.equal(await page.evaluate(()=>state.tp177JournalView),'progress',width+'/'+theme+': canonical Journal Progress state');
     const progress=page.locator('main.tp177-progress');
     if(width===393&&theme==='yellow'){
       await page.waitForFunction(()=>document.querySelector('#tp271Dock button[data-tp271-target="progress"]')?.getAttribute('aria-current')==='page');
       await page.evaluate(()=>{document.scrollingElement.scrollTop=0;document.activeElement?.blur?.()});
       await page.screenshot({path:'ui-evidence/pr271-progress-393.png'});
     }
     assert.equal(await progress.locator('.tp177-journal-tabs button').count(),3,width+'/'+theme+': three original tabs retained');
     assert.equal(await progress.locator('.tp177-metric').count(),4,width+'/'+theme+': four actual metrics retained');
     assert.equal(await progress.locator('.tp177-chart-axis').count(),1,width+'/'+theme+': original chart axis preserved');
     assert.equal(await progress.locator('.tp177-anatomy').count(),1,width+'/'+theme+': original muscle anatomy retained');
     assert.equal(await progress.locator('.tp177-coach').count(),1,width+'/'+theme+': actual Coach entry retained');
     const card=progress.locator('.tp177-metric[data-metric="workouts"]');
     await card.click();
     assert.equal(await card.getAttribute('aria-expanded'),'true',width+'/'+theme+': original metric disclosure');
     assert.equal(await progress.locator('#tp177MetricDetails').isVisible(),true,width+'/'+theme+': real detailed view opens');
     await card.click();
     assert.equal(await card.getAttribute('aria-expanded'),'false',width+'/'+theme+': same detail can close');
     const chart=progress.locator('.tp177-volume-toggle');
     // The real module intentionally remembers its expanded state when a
     // theme changes. Normalize the test fixture before verifying expansion.
     if(await chart.getAttribute('aria-expanded')==='true')await chart.click();
     assert.equal(await chart.getAttribute('aria-expanded'),'false',width+'/'+theme+': volume starts compact for test');
     await chart.click();
     assert.equal(await chart.getAttribute('aria-expanded'),'true',width+'/'+theme+': volume expands original graph');
     const seven=progress.locator('.tp177-periods button[data-period="7d"]').first();
     await seven.click();
     assert.equal(await seven.getAttribute('aria-pressed'),'true',width+'/'+theme+': original seven-day period state');
     const boxes=await progress.evaluate(el=>{
       const dock=document.getElementById('tp271Dock'),metric=el.querySelector('.tp177-metric'),anatomy=el.querySelector('.tp177-anatomy');
       document.scrollingElement.scrollTop=document.scrollingElement.scrollHeight;
       return {columns:getComputedStyle(el.querySelector('.tp177-metric-grid')).gridTemplateColumns.split(' ').length,
         cardHeight:metric.getBoundingClientRect().height,
         lastPadding:parseFloat(getComputedStyle(el).paddingBottom),
         dockTop:dock.getBoundingClientRect().top,
         coachBottom:el.querySelector('.tp177-coach')?.getBoundingClientRect().bottom,
         anatomyWidth:anatomy.getBoundingClientRect().width,
         axisPosition:getComputedStyle(el.querySelector('.tp177-chart-axis')).position,
         overflow:document.documentElement.scrollWidth-innerWidth};
     });
     assert.equal(boxes.columns,2,width+'/'+theme+': Studio-like two-column real metric grid');
     assert.ok(boxes.cardHeight>=80,width+'/'+theme+': reachable cards '+JSON.stringify(boxes));
     assert.ok(boxes.coachBottom<=boxes.dockTop+5,width+'/'+theme+': final Coach action accessible above dock '+JSON.stringify(boxes));
     assert.ok(boxes.lastPadding>=80,width+'/'+theme+': scroll-safe footer clearance');
     assert.ok(boxes.anatomyWidth>0&&boxes.axisPosition==='sticky',width+'/'+theme+': original anatomy and sticky chart axis');
     assert.ok(boxes.overflow<=1,width+'/'+theme+': no horizontal Progress overflow '+JSON.stringify(boxes));
     if(width===393&&theme==='yellow'){
       // Keep interaction-state evidence separate from real first frames.
       await page.evaluate(()=>{document.scrollingElement.scrollTop=0;const m=document.querySelector('#app main.tp177-progress');if(m)m.scrollTop=0;document.activeElement?.blur?.()});
       await page.screenshot({path:'ui-evidence/pr271-progress-volume-expanded-393.png'});
       await page.evaluate(()=>{state.tp177JournalView='log';go('history');document.scrollingElement.scrollTop=0;document.activeElement?.blur?.()});
       await page.waitForFunction(()=>document.querySelector('#tp271Dock button[data-tp271-target="log"]')?.getAttribute('aria-current')==='page');
       assert.equal(await page.locator('#tp271Dock button[aria-current="page"]').count(),1,'only Journal selected when returning from Progress');
       await page.screenshot({path:'ui-evidence/pr271-journal-filter-expanded-393.png'});
     }
     // P4a: the six live Health tiles and the original Round-4 Settings
     // controls are still reachable in the trial shell at both phone widths.
     await page.evaluate(()=>go('health'));
     await page.waitForSelector('main.tp168-health .tp5-today-grid.tp5-today-full');
     const health=await page.evaluate(()=>{
       const main=document.querySelector('#app main.tp168-health'),grid=main.querySelector('.tp5-today-grid.tp5-today-full');
       return {tiles:grid.querySelectorAll(':scope > .tp5-today-metric').length,
         columns:getComputedStyle(grid).gridTemplateColumns.split(' ').length,
         footer:parseFloat(getComputedStyle(main).paddingBottom),
         overflow:document.documentElement.scrollWidth-innerWidth};
     });
     assert.equal(health.tiles,6,width+'/'+theme+': six actual Health metrics');
     assert.equal(health.columns,3,width+'/'+theme+': three-column Health layout');
     assert.ok(health.footer>=80,width+'/'+theme+': Health scroll clears dock');
     assert.ok(health.overflow<=1,width+'/'+theme+': no Health horizontal overflow');
     const healthDetails=page.locator('main.tp168-health details.tp168-more-panel');
     assert.equal(await healthDetails.count(),1,width+'/'+theme+': complete canonical Health detail disclosure');
     await healthDetails.locator(':scope > summary').click();
     assert.notEqual(await healthDetails.getAttribute('open'),null,width+'/'+theme+': original Health details open');
     await healthDetails.locator(':scope > summary').click();
     assert.equal(await healthDetails.getAttribute('open'),null,width+'/'+theme+': original Health details close');
     if(width===393&&theme==='yellow'){
       await page.evaluate(()=>{document.scrollingElement.scrollTop=0;const m=document.querySelector('#app main.tp168-health');if(m)m.scrollTop=0;document.activeElement?.blur?.()});
       await page.screenshot({path:'ui-evidence/pr271-health-393.png'});
     }
     await page.locator('#tp271Header button[data-tp271-header="settings"]').click();
     await page.waitForFunction(()=>document.querySelector('#tp155R4PanelHost')?.dataset.panel==='settings');
     const settings=await page.evaluate(()=>{
       const host=document.querySelector('#tp155R4PanelHost');
       return {panel:!!host?.querySelector('.tp155-r4-panel'),
         rows:host?.querySelectorAll('.setting').length||0,
         overflow:document.documentElement.scrollWidth-innerWidth};
     });
     assert.equal(settings.panel,true,width+'/'+theme+': original Settings panel');
     assert.ok(settings.rows>0,width+'/'+theme+': real Settings sections retained');
     assert.ok(settings.overflow<=1,width+'/'+theme+': no Settings horizontal overflow');
     const settingsRoot=page.locator('#tp155R4PanelHost[data-panel="settings"] main.tp1511-settings');
     assert.equal(await settingsRoot.count(),1,width+'/'+theme+': canonical Settings screen retained');
     const backup=settingsRoot.locator('.tp152-settings-backup');
     assert.equal(await backup.count(),1,width+'/'+theme+': local backup card retained');
     assert.equal(await backup.locator('button[onclick="exportData()"]').count(),1,width+'/'+theme+': original backup/export command');
     assert.equal(await backup.locator('button[onclick="chooseImport()"]').count(),1,width+'/'+theme+': original restore/import command');
     assert.equal(await settingsRoot.locator('.rf212-language').count(),1,width+'/'+theme+': persisted language control retained');
     const backupGrid=await backup.locator('.grid2').evaluate(el=>({
       columns:getComputedStyle(el).gridTemplateColumns.split(' ').length,
       buttons:[...el.querySelectorAll('button')].every(b=>getComputedStyle(b).display!=='none')
     }));
     assert.equal(backupGrid.columns,2,width+'/'+theme+': compact two-column backup actions');
     assert.equal(backupGrid.buttons,true,width+'/'+theme+': backup actions not hidden');
     if(width===393&&theme==='yellow'){
       await page.screenshot({path:'ui-evidence/pr271-settings-393.png'});
       await page.locator('#tp155R4PanelHost[data-panel="settings"] .tp155-r4-panel').evaluate(el=>{el.scrollTop=el.scrollHeight});
       await page.screenshot({path:'ui-evidence/pr271-settings-bottom-393.png'});
     }
     await page.evaluate(()=>go('settings'));
   }
   if(width===393){
     const localized={hu:'Egészség',en:'Health',de:'Gesundheit',ro:'Sănătate'};
     for(const [lang,label] of Object.entries(localized)){
       await page.evaluate(x=>rf212SetLang(x),lang);
       await page.waitForFunction(x=>document.documentElement.lang===x&&document.querySelector('#tp271Dock button[data-tp271-target="health"] .tp271-dock-label')?.textContent===({hu:'Egészség',en:'Health',de:'Gesundheit',ro:'Sănătate'}[x]),lang);
       assert.equal(await page.locator('#tp271Dock button[data-tp271-target="health"] .tp271-dock-label').textContent(),label,lang+': canonical saved language drives dock');
       assert.equal(await page.evaluate(()=>rf212LangSetting()),lang,lang+': main language persisted');
       assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),lang+': no language overflow');
     }
     await page.evaluate(()=>rf212SetLang('hu'));
   }
   assert.deepEqual(errors,[],width+': no runtime errors');
   await page.close();
  }
  console.log('PASS PR271 P1e/P4b: real compact gold header, three Settings shortcuts, five bottom tabs, localized data and screenshots');
 }finally{await browser?.close();await new Promise(ok=>server.close(ok))}
})().catch(e=>{console.error(e);process.exitCode=1});
