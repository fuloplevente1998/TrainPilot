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
const listen=()=>new Promise(r=>server.listen(0,'127.0.0.1',r));
const base=()=>'http://127.0.0.1:'+server.address().port+'/';

(async()=>{
 await listen();let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  const pageErrors=[];
  page.on('pageerror',e=>pageErrors.push(e?.stack||e?.message||String(e)));
  page.on('dialog',d=>d.accept());
  await page.goto(base());await page.waitForFunction(()=>window.TrainPilotBoot?.finished);

  await page.evaluate(()=>{db.set('draft',null);state.session=null;state.workout=null;state.tab='plan';render()});
  await page.waitForSelector('.tp150-quick-entry');
  assert.equal(await page.locator('.tp150-quick-entry button').innerText(),'Gyors edzés indítása');
  assert.equal(await page.locator('.tp150-active-program').count(),1,'Active program must use the compact card');
  assert.equal(await page.locator('.tp150-plan-compact > .hero').count(),0,'Workout page must not restore the oversized active-program hero');

  await page.evaluate(()=>{state.tab='programs';render()});
  await page.waitForSelector('.tp150-programs-frame');
  assert.equal(await page.locator('.tp150-program-tools button').count(),3,'Programs actions must be grouped at the top');
  assert.ok(await page.locator('.tp150-programs-frame .program-card').count()>=1,'Workout programs must stay inside the dynamic framed list');
  assert.equal(await page.locator('.tp150-programs-compact > .hero').count(),0,'Programs page must not keep the oversized explanatory hero');

  const preparedCalls=await page.evaluate(()=>{
   const original=tp149UserValues;let calls=0;
   tp149UserValues=function(){calls++;return original();};
   db.set('language','en');document.documentElement.lang='en';state.tab='programs';render();
   tp149UserValues=original;return calls;
  });
  assert.ok(preparedCalls<=2,'Non-Hungarian DOM translation should prepare protected values once per render, got '+preparedCalls);
  assert.equal(await page.locator('.tp150-programs-frame-title').innerText(),'Workout programs');

  await page.evaluate(()=>{db.set('language','hu');document.documentElement.lang='hu';state.tab='history';render()});
  await page.waitForSelector('.tp150-journal-compact');
  assert.equal(await page.locator('.tp150-journal-compact > .hero').count(),0,'Journal title/description hero must be removed for every language');
  await page.evaluate(()=>{state.tab='plan';render()});
  await page.waitForSelector('.tp150-quick-entry');

  await page.locator('.tp150-quick-entry button').click();
  await page.waitForSelector('#tp150QuickResults');
  assert.ok(await page.locator('#tp150QuickResults .tp150-quick-row').count()>=100,'Quick picker must expose the full built-in exercise library');

  const libraryBefore=await page.evaluate(()=>{const e=byId('db-floor-press');return {sets:e.sets,reps:e.reps,weight:e.weight}});
  const floor=page.locator('#tp150QuickResults [data-exercise-id="db-floor-press"]');
  await floor.locator('summary').click();
  assert.equal(await floor.locator('[data-tp152-qsets]').inputValue(),String(libraryBefore.sets),'Inline Quick config should start from the exercise/default recommendation');
  await floor.locator('[data-tp152-qsets]').selectOption('4');
  await floor.locator('[data-tp152-qtarget]').fill('8');
  await floor.locator('[data-tp152-qweight]').fill('20');
  await floor.locator('.tp150-quick-start').click();
  await page.waitForFunction(()=>state.session?.type==='quick'&&state.session.exercises.length===1);
  let session=await page.evaluate(()=>({type:state.session.type,programId:state.session.programId,workout:state.session.workout,count:state.session.exercises.length,id:state.session.exercises[0].id,tab:state.tab,sets:state.session.exercises[0].sets.length,target:state.session.exercises[0].targetReps,weight:state.session.exercises[0].sets[0].weight}));
  assert.deepEqual(session,{type:'quick',programId:null,workout:'quick',count:1,id:'db-floor-press',tab:'plan',sets:4,target:'8',weight:20});
  assert.deepEqual(await page.evaluate(()=>{const e=byId('db-floor-press');return {sets:e.sets,reps:e.reps,weight:e.weight}}),libraryBefore,'Quick Workout configuration must not mutate global exercise defaults');
  assert.equal(await page.locator('#tp150QuickAdd').count(),1,'active Quick Workout must offer Add exercise');
  assert.match(await page.locator('main .tp153-workout-head .tp153-workout-topline span').innerText(),/Gyors edzés/i,'compact active Quick Workout header must keep the workout label');

  const weight=page.locator('.row input[inputmode="decimal"]').first();
  if(await weight.count())await weight.fill('20');
  const reps=page.locator('.row input[inputmode="numeric"]').first();
  await reps.fill('8');
  await page.locator('.row .check').first().click();
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),true,'normal set completion must work in Quick Workout');

  await page.locator('#tp150QuickAdd button').click();
  await page.waitForSelector('#tp150QuickQuery');
  await page.locator('#tp150QuickQuery').fill('plank');
  await page.waitForTimeout(50);
  const plankRow=page.locator('#tp150QuickResults .tp150-quick-row[data-exercise-id="plank"]');
  assert.equal(await plankRow.count(),1,'Plank must be searchable in Quick picker');
  await plankRow.locator('summary').click();
  await plankRow.locator('[data-tp152-qsets]').selectOption('2');
  await plankRow.locator('[data-tp152-qtarget]').fill('30');
  await plankRow.locator('.tp150-quick-start').click();
  await page.waitForFunction(()=>state.session?.exercises?.length===2&&state.session.exercises[state.current]?.id==='plank');
  session=await page.evaluate(()=>({count:state.session.exercises.length,current:state.current,ids:state.session.exercises.map(e=>e.id),plankSets:state.session.exercises[1].sets.length,plankTarget:state.session.exercises[1].targetReps}));
  assert.equal(session.count,2);assert.deepEqual(session.ids,['db-floor-press','plank']);assert.equal(session.plankSets,2);assert.equal(session.plankTarget,'30');

  await page.evaluate(()=>{
   for(const e of state.session.exercises)for(const s of e.sets){if(!s.reps)s.reps=e.id==='plank'?'30':'8';s.done=true}
   persistDraft();renderWorkout();
  });
  const historyBefore=await page.evaluate(()=>history().length);
  await page.locator('[data-tp-workout-next]').click();
  await page.waitForTimeout(250);
  const finishState=await page.evaluate(()=>({
   hasSession:!!state.session,tab:state.tab,current:state.current,historyAfter:history().length,
   dialog:document.querySelector('#tp2628Dialog')?.textContent||'',
   done:state.session?.exercises?.flatMap(e=>e.sets||[]).filter(s=>s.done).length??null,
   undone:state.session?.exercises?.flatMap(e=>e.sets||[]).filter(s=>!s.done).length??null
  }));
  finishState.historyBefore=historyBefore;finishState.pageErrors=pageErrors.slice();
  assert.equal(finishState.hasSession,false,'Quick Workout did not finish: '+JSON.stringify(finishState));
  const saved=await page.evaluate(()=>{const h=history()[0];return {type:h.type,quick:h.quickWorkout,count:h.exercises.length,ids:h.exercises.map(e=>e.id),draft:db.get('draft',null)}});
  assert.equal(saved.type,'quick');assert.equal(saved.quick,true);assert.equal(saved.count,2);assert.deepEqual(saved.ids,['db-floor-press','plank']);assert.equal(saved.draft,null);

  await page.evaluate(()=>{state.tab='history';render()});
  await page.waitForSelector('.history-title');
  assert.equal(await page.locator('.history-title').first().innerText(),'Gyors edzés','Journal must use the localized Quick Workout title');

  await page.evaluate(()=>rf203ExerciseLibrary());
  await page.waitForSelector('details.tp-library-card');
  assert.ok(await page.locator('.tp150-library-start').count()>=100,'Exercise Library rows must each have a start button');
  const libraryCard=page.locator('details.tp-library-card').first();
  assert.equal(await libraryCard.getAttribute('open'),null,'Exercise Library cards should start collapsed in the 1.5.2 hierarchy');
  await libraryCard.locator('summary').click();
  await libraryCard.locator('.tp150-library-start').waitFor({state:'visible'});
  assert.equal(await libraryCard.locator('.tp150-library-start').isVisible(),true,'Exercise Library start button must be visible after expanding its card');

  const audit=await page.evaluate(()=>window.TrainPilotQuickWorkout.audit());
  for(const [lang,missing] of Object.entries(audit))assert.deepEqual(missing,[],lang+' Quick Workout translation keys missing');

  console.log('PASS: TrainPilot 1.5.2 Quick Workout edits inline, adds exercises, reuses set engine, logs to Journal and exposes Library start buttons.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
