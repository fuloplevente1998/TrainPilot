'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
 page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.tp106CustomExercisePanelHtml);
 const opener=page.locator('button[onclick="customExerciseScreen()"]'),panel=page.locator('#tp155R4PanelHost[data-panel="custom-exercise"]');
 for(const language of ['hu','en','de','ro'])for(const width of [320,360,393,412]){
  await page.setViewportSize({width,height:873});
  await page.evaluate(lang=>{db.set('language',lang);state.session=null;db.set('draft',null);go('programs')},language);
  const count=await page.evaluate(()=>exercises().length);
  await opener.click();await panel.waitFor({state:'visible'});
  assert.equal(await page.evaluate(()=>state.tab),'programs','overlay preserves Programs route');
  const style=await panel.evaluate(host=>{
   const close=host.querySelector('.tp155-r4-panel-close'),r=close.getBoundingClientRect(),p=close.closest('.tp106-builder-header').getBoundingClientRect();
   return {color:getComputedStyle(close).backgroundColor,fit:host.scrollWidth<=host.clientWidth+1&&host.querySelector('main').scrollWidth<=host.querySelector('main').clientWidth+1,right:p.right-r.right,top:r.top-p.top,side:r.width,form:!!host.querySelector('#ceMeasure'),selects:host.querySelectorAll('.tp-select').length,rounded:parseFloat(getComputedStyle(host.querySelector('.tp155-r4-panel')).borderTopLeftRadius),header:!!close.closest('.tp106-builder-header')};
  });
  assert.equal(style.color,'rgb(71, 37, 41)','shared red X');assert.ok(style.form&&style.side>=36&&style.right>=0&&style.right<=24&&style.top>=0&&style.top<=38,JSON.stringify(style));
  assert.equal(style.fit,true,language+'/'+width+' panel must fit');assert.equal(style.selects,5,'current themed selects are ready on first open');
  assert.ok(style.rounded>=18&&style.header,'full frame and docked close-button header');
  await panel.locator('.tp155-r4-panel').evaluate(el=>el.scrollTop=el.scrollHeight);
  assert.equal(await panel.evaluate(host=>{const h=host.querySelector('.tp106-builder-header').getBoundingClientRect(),x=host.querySelector('.tp155-r4-panel-close').getBoundingClientRect();return x.top>=h.top&&x.bottom<=h.bottom&&x.right<=h.right}),true,'X stays inside the header when scrolled');
  await panel.locator('input[name="ceGear"]').first().evaluate(el=>el.closest('details').open=true);
  const checkbox=panel.locator('input[name="ceGear"]').first();await checkbox.check();
  const checked=await checkbox.evaluate(el=>{const ref=document.createElement('span');ref.style.color='var(--accent)';document.body.appendChild(ref);const cs=getComputedStyle(el),r=el.getBoundingClientRect(),out={appearance:cs.appearance,bg:cs.backgroundColor,accent:getComputedStyle(ref).color,w:r.width,h:r.height,label:el.closest('label').getBoundingClientRect().height};ref.remove();return out});
  assert.equal(checked.appearance,'none');assert.equal(checked.bg,checked.accent);assert.equal(checked.w,26);assert.equal(checked.h,26);assert.ok(checked.label>=44);
  await panel.locator('#ceHu').fill('Cancelled custom exercise');
  await panel.locator('.tp155-r4-panel-close').click();assert.equal(await panel.count(),0);assert.equal(await page.evaluate(()=>exercises().length),count,'cancel writes no library item');
  assert.equal(await opener.evaluate(el=>document.activeElement===el),true,'close restores opener focus');
  await opener.click();assert.equal(await panel.locator('#ceHu').inputValue(),'','reopen does not leak cancelled values');
  assert.equal(await page.evaluate(()=>TrainPilotAndroidBack()),true);await panel.waitFor({state:'detached'});
  assert.equal(await page.evaluate(()=>state.tab),'programs');
  const programCount=await page.evaluate(()=>programs().length);
  await page.locator('button[onclick="createCustomProgram()"]').click();
  const programPanel=page.locator('#tp155R4PanelHost[data-panel="custom-program"]');await programPanel.waitFor({state:'visible'});
  assert.equal(await programPanel.evaluate(host=>{const probe=document.createElement('div');probe.style.background='var(--card)';host.appendChild(probe);const expected=getComputedStyle(probe).backgroundColor,actual=getComputedStyle(host.querySelector('.tp155-r4-panel')).backgroundColor;probe.remove();return expected===actual}),true,'program panel has an opaque card background');
  assert.equal(await programPanel.locator('#tp106ProgramName').count(),1);
  assert.equal(await programPanel.evaluate(host=>host.scrollWidth<=host.clientWidth+1&&host.querySelector('.tp155-r4-panel-close').closest('.tp106-builder-header')!==null),true,'new program frame fits '+language+'/'+width);
  await programPanel.locator('.tp155-r4-panel-close').click();assert.equal(await page.evaluate(()=>programs().length),programCount,'cancel creates no program');
 }
 for(const theme of ['classicBlue','green']){
  await page.evaluate(t=>{rf200SetTheme(t);db.set('language','hu');go('programs')},theme);await opener.click();
  const input=panel.locator('input[name="ceGear"]').first();await input.evaluate(el=>el.closest('details').open=true);await input.check();
  assert.equal(await input.evaluate(el=>{const ref=document.createElement('span');ref.style.color='var(--accent)';document.body.appendChild(ref);const match=getComputedStyle(el).backgroundColor===getComputedStyle(ref).color;ref.remove();return match}),true,'checkbox follows '+theme);
  await panel.locator('.tp155-r4-panel-close').click();
 }
 await page.setViewportSize({width:393,height:873});await page.evaluate(()=>{rf200SetTheme('yellow');db.set('language','hu');go('programs')});
 await page.locator('button[onclick="createCustomProgram()"]').click();
 const programPanel=page.locator('#tp155R4PanelHost[data-panel="custom-program"]');
 await programPanel.locator('#tp106ProgramName').fill('Távolság program');await programPanel.locator('button[type="submit"]').click();
 const programId=await page.evaluate(()=>tp106ProgramId);assert.ok(programId);assert.equal(await page.evaluate(id=>programById(id).days.length,programId),2);
 const day=programPanel.locator('.tp152-custom-day').first(),select=day.locator('select').first();
 await select.locator('..').locator('.tp-select-trigger').click();await select.locator('..').locator('.tp-select-option[data-value="running"]').click();
 assert.equal(await page.evaluate(id=>programById(id).days[0].exercises[0],programId),'running');
 await day.locator('button[onclick^="addCustomExercise("]').click();assert.equal(await page.evaluate(id=>programById(id).days[0].exercises.length,programId),4);
 await day.locator('.tp152-custom-actions .danger').last().click();assert.equal(await page.evaluate(id=>programById(id).days[0].exercises.length,programId),3);
 fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/custom-program-106.png',animations:'disabled'});
 await programPanel.locator('.tp155-r4-panel-close').click();assert.equal(await page.evaluate(()=>state.tab),'programs');
 await page.evaluate(id=>editCustomProgram(id),programId);assert.equal(await programPanel.locator('select').first().inputValue(),'running');await programPanel.locator('.tp155-r4-panel-close').click();
 // All six built-in activities can be found and started without making a custom exercise.
 for(const id of ['running','jogging','inline-skating','cycling','walking','hiking']){
  await page.evaluate(()=>{state.session=null;db.set('draft',null);go('plan')});await page.locator('.tp150-quick-entry button').click();
  const name=await page.evaluate(id=>byId(id).hu,id);await page.locator('#tp155QuickQuery, #tp150QuickQuery').first().fill(name);
  const row=page.locator('#tp150QuickResults [data-exercise-id="'+id+'"]');await row.locator('summary').click();await row.locator('.tp150-quick-start').click();
  await page.waitForSelector('[data-tp106-distance]');assert.equal(await page.evaluate(()=>state.session.exercises[0].id),id);
  assert.equal(await page.evaluate(()=>state.session.exercises[0].measurementType),'distance');assert.equal(await page.locator('#tp106GpsButton').count(),1);
  assert.match(await page.locator('.tp155-workout-coach').innerText(),/Idő és távolság/);
 }
 await page.evaluate(()=>{state.session=null;db.set('draft',null);go('programs')});await opener.click();
 await panel.locator('#ceHu').fill('Futás / kocogás / görkori');
 await panel.locator('#ceMeasure').locator('..').locator('.tp-select-trigger').click();await panel.locator('#ceMeasure').locator('..').locator('.tp-select-option[data-value="distance"]').click();
 await panel.locator('#ceMeasure').locator('..').waitFor({state:'visible'});await page.waitForTimeout(180);
 fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/custom-exercise-106.png'});
 await panel.locator('button[onclick="saveCustomExercise14()"] ').click();await panel.waitFor({state:'detached'});await page.locator('#tp153Toast.show').waitFor({state:'visible'});assert.equal(await page.locator('#tp153Toast').innerText(),await page.evaluate(()=>tp149T('customExercise.saved')));assert.equal(await page.locator('#tp2628Dialog').count(),0,'successful exercise save uses non-blocking feedback');
 const exercise=await page.evaluate(()=>exercises().find(e=>e.custom&&e.hu==='Futás / kocogás / görkori'));
 assert.ok(exercise);assert.equal(exercise.measurementType,'distance');assert.equal(exercise.repUnit,'mp');assert.equal(exercise.loadType,'bodyweight');assert.equal(exercise.sets,1);
 // Use the real Quick picker and current workout renderer rather than constructing a fake form.
 await page.evaluate(()=>go('plan'));await page.locator('.tp150-quick-entry button').click();
 await page.locator('#tp155QuickQuery, #tp150QuickQuery').first().fill('Futás / kocogás / görkori');
 const quick=page.locator('#tp150QuickResults [data-exercise-id="'+exercise.id+'"]');await quick.locator('summary').click();await quick.locator('.tp150-quick-start').click();
 await page.waitForSelector('[data-tp106-distance]');assert.equal(await page.evaluate(()=>state.session.exercises[0].measurementType),'distance');
 await page.locator('[data-tp106-distance]').fill('5,25');await page.locator('.tp153-set-row input[inputmode="numeric"]').first().fill('1800');
 assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].distanceMeters),5250);
 await page.locator('[data-tp106-distance]').fill('wrong');await page.locator('.tp153-set-row .check').first().click();
 assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),false,'invalid input cannot mark an old distance as completed');
 await page.locator('[data-tp106-distance]').fill('5,25');
 for(const width of [320,360,393,412]){await page.setViewportSize({width,height:873});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'distance workout fits '+width)}
 await page.setViewportSize({width:393,height:873});await page.screenshot({path:'ui-evidence/distance-workout-106.png'});
 await page.locator('.tp153-set-row .check').first().click();await page.locator('[data-tp-workout-next]').click();await page.waitForFunction(()=>!state.session);
 const logged=await page.evaluate(()=>history()[0]);assert.equal(logged.exercises[0].sets[0].distanceMeters,5250);assert.equal(logged.exercises[0].sets[0].reps,'1800');
 await page.evaluate(()=>go('history'));await page.locator('details.rf263-history>summary').first().click();
 const item=page.locator('details.tp3-history-ex-item').first();assert.match(await item.locator('summary').innerText(),/5,25 km/);
 assert.equal(await page.locator('.tp3-summary-metrics>div').nth(2).locator('strong').innerText(),'0','distance is not counted as repetitions');assert.equal(await page.locator('.tp3-summary-metrics>div').nth(3).locator('strong').innerText(),'—','distance is not lifted volume');
 await item.locator('summary').click();await item.locator('[data-tp106-distance]').fill('6,125');await page.screenshot({path:'ui-evidence/distance-journal-106.png'});
 await item.locator('.tp3-ex-save').click();await page.waitForFunction(()=>history()[0].exercises[0].sets[0].distanceMeters===6125);
 assert.match(await page.locator('details.tp3-history-ex-item summary').first().innerText(),/6,125 km/);assert.equal(await page.evaluate(()=>history()[0].exercises[0].sets[0].distanceSource),'manual');
 // Native bridge emulation verifies real workout button/field locking and recovery, not GNSS hardware.
 await page.evaluate(()=>{
  db.set('draft',null);state.session=null;go('plan');window.gpsUI={active:false,distanceMeters:0,elapsedSeconds:0};window.gpsUIStarts=0;
  window.Capacitor={isNativePlatform:()=>true,Plugins:{DistanceTracker:{status:async()=>({...gpsUI}),start:async args=>{gpsUIStarts++;gpsUI={...gpsUI,key:args.key,active:true};return {...gpsUI}},stop:async()=>{gpsUI.active=false;return {...gpsUI}}}}};
 });
 await page.locator('.tp150-quick-entry button').click();await page.locator('#tp155QuickQuery, #tp150QuickQuery').first().fill('Futás / kocogás / görkori');await quick.locator('summary').click();await quick.locator('.tp150-quick-start').click();
 await page.waitForSelector('#tp106GpsButton');await page.locator('#tp106GpsButton').click();await page.waitForFunction(()=>tp106GpsState.active&&!tp106GpsBusy);
 assert.equal(await page.locator('[data-tp106-distance]').isDisabled(),true);assert.equal(await page.evaluate(()=>gpsUIStarts),1);
 await page.evaluate(async()=>{gpsUI.distanceMeters=1000;gpsUI.elapsedSeconds=300;gpsUI.hasFix=true;await tp106SyncGPS()});
 assert.equal(await page.locator('[data-tp106-distance]').inputValue(),'1');await page.locator('#tp106GpsButton').click();await page.waitForFunction(()=>!tp106GpsState.active&&!tp106GpsBusy);
 assert.equal(await page.locator('[data-tp106-distance]').isDisabled(),false);assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].reps),'300');
 await page.locator('#tp106GpsButton').click();await page.waitForFunction(()=>tp106GpsState.active&&!tp106GpsBusy);await page.locator('#tp150QuickAdd button').click();await quick.locator('summary').click();await quick.locator('.tp150-quick-start').click();await page.waitForSelector('#tp106GpsButton');assert.equal(await page.evaluate(()=>gpsUI.active),false,'reconfiguring an existing Quick exercise stops native GPS before replacing its sets');assert.equal(await page.evaluate(()=>db.get('gpsTrip106',null)),null);
 // Exercise-time steps use a narrow, read-only bridge and update only their own card.
 await page.evaluate(()=>{
  state.session.started=new Date(Date.now()-180000).toISOString();state.session.activeIntervals=[{start:state.session.started,end:null}];
  window.stepUI=84;window.stepUIReads=0;
  window.Capacitor.Plugins.HealthBridge={readStepsWindow:async()=>{stepUIReads++;return {steps:stepUI,permissions:{READ_STEPS:true}}},readTrainingWindow:async()=>({steps:stepUI,permissions:{READ_STEPS:true},sources:['all'],sourceLabels:{all:'Health Connect'}})};
 });
 await page.locator('#tp106Steps [data-tp106-steps-refresh]').click();await page.waitForFunction(()=>state.session.health240?.steps===84);
 const originalMain=await page.locator('main.tp153-workout').elementHandle();
 await page.evaluate(()=>stepUI=102);await page.locator('#tp106Steps [data-tp106-steps-refresh]').click();await page.waitForFunction(()=>state.session.health240?.steps===102);
 assert.equal(await page.evaluate(el=>el===document.querySelector('main.tp153-workout'),originalMain),true,'step refresh must not redraw the workout');
 assert.equal(await page.locator('#tp106Steps [data-tp106-steps-count]').innerText(),'102');
 for(const width of [320,360,393,412]){await page.setViewportSize({width,height:873});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'steps card fits '+width)}
 await page.evaluate(()=>{window.Capacitor.Plugins.HealthBridge.readStepsWindow=async()=>({steps:null,permissions:{READ_STEPS:false}})});
 await page.locator('#tp106Steps [data-tp106-steps-refresh]').click();await page.waitForFunction(()=>!tp106StepsView.busy);
 assert.match(await page.locator('[data-tp106-steps-status]').innerText(),/Engedélyezd/);assert.equal(await page.locator('[data-tp106-steps-count]').innerText(),'102');assert.equal(await page.locator('[data-tp106-steps-grant]').isVisible(),true,'missing READ_STEPS exposes a dedicated permission button');
 await page.setViewportSize({width:393,height:873});await page.screenshot({path:'ui-evidence/workout-health-steps-106.png',animations:'disabled'});
 // Indoors, record duration with an unknown distance instead of inventing kilometres.
 await page.locator('[data-tp106-distance]').fill('');await page.locator('.tp153-set-row input[inputmode="numeric"]').first().fill('180');
 await page.locator('.tp153-set-row .check').first().click();assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),true);
 await page.locator('[data-tp-workout-next]').click();await page.waitForFunction(()=>!state.session);
 assert.equal(await page.evaluate(()=>history()[0].exercises[0].sets[0].distanceMeters),undefined);assert.equal(await page.evaluate(()=>history()[0].exercises[0].sets[0].reps),'180');
 await page.evaluate(()=>go('history'));await page.locator('details.rf263-history>summary').first().click();
 await page.evaluate(()=>healthFromHistory(0));
 assert.equal(await page.evaluate(()=>history()[0].health240.steps),102,'Journal refresh persists the completed workout steps for export');
 assert.match(await page.locator('[data-rf-history-health="0"]').innerText(),/Lépések/);assert.match(await page.locator('[data-rf-history-health="0"]').innerText(),/102/);
 await page.screenshot({path:'ui-evidence/journal-health-steps-106.png',animations:'disabled'});
 assert.deepEqual(errors,[],'page errors');console.log('PASS 1.0.6 custom panel: four languages/mobile widths, cancel/reopen/Back, themed checkbox, real Quick workout km/time, invalid input, Journal editing, GPS controls and field locking.');
}finally{await browser?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
