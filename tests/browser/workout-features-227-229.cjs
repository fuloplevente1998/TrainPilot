'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright'),root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const url=new URL(req.url,'http://local'),file=path.resolve(root,'.'+(url.pathname==='/'?'/index.html':url.pathname));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});
});
(async()=>{
 await new Promise(done=>server.listen(0,'127.0.0.1',done));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:360,height:800},locale:'hu-HU'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
  await page.addInitScript(()=>{
   localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
   window.testHaptics=[];Object.defineProperty(navigator,'vibrate',{configurable:true,value:n=>{window.testHaptics.push(n);return true;}});
  });
  const boot=()=>page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await boot();
  await page.evaluate(()=>{
   const p={id:'features-e2e',name:'Feature E2E',location:'home',level:'all',builtin:false,days:[{id:'A',name:'A',exercises:['goblet-squat']}],
    prescriptions:{'goblet-squat':{sets:3,reps:'8–12',rir:2,rest:30,weight:10,loadType:byId('goblet-squat').loadType}}};
   db.set('programs',[...programs().filter(x=>x.id!==p.id),p]);db.set('activeProgramId',p.id);db.set('history',[]);db.set('draft',null);
   db.set('settings',{...settings(),trainingModes197:{schemaVersion:1,defaults:{trainingStyle:'normal',progressionMode:'rir'},explicitChoice:true,byProgram:{}}});go('plan');
  });
  const toggle=page.locator('.tp227-haptic-setting input');assert.equal(await toggle.count(),1);
  await page.locator('.tp1511-training').evaluate(x=>x.open=true);
  const checkbox=await toggle.evaluate(el=>{const css=getComputedStyle(el);return {appearance:css.appearance,width:css.width,height:css.height,radius:css.borderRadius};});
  assert.deepEqual(checkbox,{appearance:'none',width:'22px',height:'22px',radius:'7px'},'Haptic uses the shared themed square/check style');
  await toggle.uncheck();assert.equal(await page.evaluate(()=>settings().setHaptic227),false);
  await page.reload();await boot();await page.evaluate(()=>go('plan'));await page.locator('.tp1511-training').evaluate(x=>x.open=true);
  assert.equal(await toggle.isChecked(),false,'Haptic preference survives reload');await toggle.check();
  await page.evaluate(()=>startWorkout('A',null,'features-e2e'));
  const rows=()=>page.locator('.tp153-set-row');
  await rows().nth(0).locator('input[oninput*="weight"]').fill('12.5');await rows().nth(0).locator('input[oninput*="reps"]').fill('10');
  await rows().nth(0).locator('.check').click();
  assert.deepEqual(await page.evaluate(()=>testHaptics),[40],'Accepted manual set has one short cue');
  assert.equal(await page.locator('.tp228-copy').count(),0,'No manual copy button');
  assert.equal(await rows().count(),3);
  await rows().nth(1).locator('input[oninput*="reps"]').fill('10');await rows().nth(1).locator('.check').click();
  await rows().nth(2).locator('input[oninput*="reps"]').fill('10');await rows().nth(2).locator('.check').click();
  await page.locator('#rf152Feedback button[onclick*="good"]').click();
  const inputs=page.locator('.tp229-rir input');assert.equal(await inputs.count(),3);
  assert.deepEqual(await inputs.evaluateAll(xs=>xs.map(x=>x.value)),['','',''],'No target or copied RIR observations');
  await inputs.nth(0).fill('0');await inputs.nth(1).fill('4');await inputs.nth(2).fill('2');
  assert.equal(await page.evaluate(()=>tp229RirObservation(state.session.exercises[0])),0);
  assert.deepEqual(await page.evaluate(()=>TrainPilotWearSync.makeSnapshot().exercises[0].sets.map(s=>s.actualRir)),[0,4,2],'Native/watch snapshot preserves each observation');
  assert.deepEqual(await page.evaluate(()=>testHaptics),[40,40,40],'RIR typing and copying do not signal another completion');
  for(const width of [320,412])for(const lang of ['hu','en','de','ro']){
   await page.setViewportSize({width,height:800});
   await page.evaluate(lang=>{db.set('language',lang);renderWorkout();},lang);
   const large=await page.addStyleTag({content:'.tp229-rir{font-size:150%!important}'});
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'Feature fields fit localized large text '+width);
   assert.equal(await page.locator('.tp228-copy').count(),0);await large.evaluate(el=>el.remove());
  }
  await page.evaluate(()=>{db.set('language','hu');renderWorkout();});
  const identities=await page.evaluate(()=>state.session.exercises[0].sets.map(s=>s.setId));
  await page.reload();await boot();await page.evaluate(()=>{if(!state.session)resumeDraft();else renderWorkout();});
  await page.waitForSelector('.tp229-rir input',{state:'attached'});
  assert.deepEqual(await inputs.evaluateAll(xs=>xs.map(x=>x.value)),['0','4','2']);
  assert.deepEqual(await page.evaluate(()=>state.session.exercises[0].sets.map(s=>s.setId)),identities);
  assert.deepEqual(await page.evaluate(()=>testHaptics),[],'Reload is not a new completion');
  fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/workout-features-227-229.png',fullPage:true});
  await page.locator('[data-tp-workout-next]').click();await page.getByRole('button',{name:'Most kihagyom'}).click();await page.evaluate(()=>go('history'));
  assert.deepEqual(await page.evaluate(()=>history()[0].exercises[0].sets.map(s=>s.actualRir)),[0,4,2]);
  await page.locator('details.history').first().evaluate(x=>x.open=true);
  assert.match(await page.locator('.tp197-rir-history').first().innerText(),/1\.: 0.*2\.: 4.*3\.: 2/);
  await page.evaluate(()=>{
   const saved=history()[0],other=JSON.parse(JSON.stringify(saved));other.id='other-day';other.dayId='B';other.programId='another-program';
   saved.started='2020-01-01T10:00:00Z';saved.finished='2020-01-01T11:00:00Z';
   other.started='2020-01-02T10:00:00Z';other.finished='2020-01-02T11:00:00Z';
   other.exercises[0].sets.forEach(s=>{s.weight=99;s.reps='99';});db.set('history',[other,saved]);
   go('plan');startWorkout('A',null,'features-e2e');
  });
  assert.deepEqual(await page.evaluate(()=>state.session.exercises[0].sets.map(s=>[s.weight,s.reps,s.done,s.actualRir??null])),
   [[99,'99',false,null],[99,'99',false,null],[99,'99',false,null]],'Same exercise uses most recent Journal values across day and program, without completion or RIR');
  assert.equal(await page.evaluate(old=>state.session.exercises[0].sets.every(s=>!old.includes(s.setId)),identities),true);
  assert.equal(await page.locator('.tp228-copy').count(),0);
  assert.deepEqual(await page.evaluate(()=>TrainPilotWearSync.makeHomeSnapshot().days.find(d=>d.id==='A').exercises[0].sets.map(s=>[s.weight,s.reps,s.done])),
   [[99,'99',false],[99,'99',false],[99,'99',false]],'Watch planned template uses cross-program Journal values');
  assert.deepEqual(await page.evaluate(()=>TrainPilotWearSync.makeHomeSnapshot().quickExercises.find(e=>e.id==='goblet-squat').sets.map(s=>[s.weight,s.reps])),
   [[99,'99'],[99,'99'],[99,'99']],'Watch quick-workout template includes Journal values');
  assert.deepEqual(await page.evaluate(()=>tp150QuickExercise('goblet-squat').sets.map(s=>[s.weight,s.reps,s.done])),
   [[99,'99',false],[99,'99',false],[99,'99',false]],'Phone quick exercise includes Journal values');
  await rows().nth(0).locator('input[oninput*="reps"]').fill('7');
  await page.reload();await boot();await page.evaluate(()=>{if(!state.session)resumeDraft();else renderWorkout();});
  assert.equal(await rows().nth(0).locator('input[oninput*="reps"]').inputValue(),'7','Draft reload never reapplies history');
  // Real Quick Workout flow: the inline "target reps" input is a goal and must
  // not clear previous completed reps from the same exercise in the Journal.
  await page.evaluate(()=>{
   const source=JSON.parse(JSON.stringify(history()[0]));
   source.id='quick-journal-228';source.started='2025-01-01T10:00:00Z';source.finished='2025-01-01T11:00:00Z';
   source.exercises[0].sets.forEach((set,i)=>{set.set=i+1;set.weight=11+i;set.reps=String(7+i);set.done=true;});
   db.set('history',[source]);db.set('draft',null);state.session=null;state.workout=null;state.tab='plan';render();
  });
  const pickQuick=async()=>{
   await page.locator('.tp150-quick-entry button').click();
   const card=page.locator('#tp150QuickResults [data-exercise-id="goblet-squat"]');
   await card.locator('summary').click();
   return card;
  };
  let card=await pickQuick();
  assert.equal(await card.locator('[data-tp152-qweight]').inputValue(),'11','Quick form starts from previous Journal weight');
  await card.locator('.tp150-quick-start').click();
  await page.waitForFunction(()=>state.session?.quickWorkout===true);
  assert.deepEqual(await page.evaluate(()=>state.session.exercises[0].sets.map(x=>[x.weight,x.reps,x.done,x.actualRir??null])),
   [[11,'7',false,null],[12,'8',false,null],[13,'9',false,null]],
   'Starting quick workout with unchanged target does not erase previous per-set values');
  await page.evaluate(()=>{db.set('draft',null);state.session=null;state.workout=null;state.tab='plan';render();});
  card=await pickQuick();
  await card.locator('[data-tp152-qsets]').selectOption('4');
  await card.locator('[data-tp152-qtarget]').fill('15');
  await card.locator('[data-tp152-qweight]').fill('22');
  await card.locator('.tp150-quick-start').click();
  await page.waitForFunction(()=>state.session?.quickWorkout===true&&state.session.exercises[0].sets.length===4);
  assert.deepEqual(await page.evaluate(()=>({
   sets:state.session.exercises[0].sets.map(x=>[x.weight,x.reps,x.done]),
   target:state.session.exercises[0].targetReps
  })),{sets:[[22,'7',false],[22,'8',false],[22,'9',false],[22,'',false]],target:'15'},
   'Explicit weight, set count and target edits keep Journal reps for matching sets');
  await page.evaluate(()=>{db.set('draft',null);state.session=null;state.workout=null;tp151QuickConfig('goblet-squat');});
  await page.locator('#tp151QuickSets').selectOption('4');
  await page.locator('#tp151QuickTarget').fill('12');
  const fallbackQuickWeight=await page.evaluate(()=>byId('goblet-squat')?.weight??0);
  assert.deepEqual(await page.evaluate(()=>({
   sets:tp151BuildQuick('goblet-squat').sets.map(x=>[x.weight,x.reps,x.done]),
   target:tp151BuildQuick('goblet-squat').targetReps
  })),{sets:[[11,'7',false],[12,'8',false],[13,'9',false],[fallbackQuickWeight,'',false]],target:'12'},
   'Legacy quick configuration also preserves prior Journal reps and distinct loads');
  assert.deepEqual(errors,[]);console.log('PASS #227–#229 integrated phone flow: setting reload, Journal-wide exercise prefill (planned + quick + Wear), identity/draft preservation, one haptic per manual receipt, per-set RIR/snapshot, 4 languages/large text, reload and Journal');
 }finally{await browser?.close();await new Promise(done=>server.close(done));}
})().catch(error=>{console.error(error);process.exitCode=1;});
