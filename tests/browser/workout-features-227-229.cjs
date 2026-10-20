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
    prescriptions:{'goblet-squat':{sets:2,reps:'8–12',rir:2,rest:30,weight:10,loadType:byId('goblet-squat').loadType}}};
   db.set('programs',[...programs().filter(x=>x.id!==p.id),p]);db.set('activeProgramId',p.id);db.set('history',[]);db.set('draft',null);
   db.set('settings',{...settings(),trainingModes197:{schemaVersion:1,defaults:{trainingStyle:'normal',progressionMode:'rir'},explicitChoice:true,byProgram:{}}});go('plan');
  });
  const toggle=page.locator('.tp227-haptic-setting input');assert.equal(await toggle.count(),1);
  await page.locator('.tp1511-training').evaluate(x=>x.open=true);
  await toggle.uncheck();assert.equal(await page.evaluate(()=>settings().setHaptic227),false);
  await page.reload();await boot();await page.evaluate(()=>go('plan'));await page.locator('.tp1511-training').evaluate(x=>x.open=true);
  assert.equal(await toggle.isChecked(),false,'Haptic preference survives reload');await toggle.check();
  await page.evaluate(()=>startWorkout('A',null,'features-e2e'));
  const rows=()=>page.locator('.tp153-set-row');
  await rows().nth(0).locator('input[oninput*="weight"]').fill('12.5');await rows().nth(0).locator('input[oninput*="reps"]').fill('10');
  await rows().nth(0).locator('.check').click();
  assert.deepEqual(await page.evaluate(()=>testHaptics),[40],'Accepted manual set has one short cue');
  await page.locator('.tp228-copy').first().click();
  assert.equal(await rows().count(),3);
  const cloned=await page.evaluate(()=>state.session.exercises[0].sets.map(s=>({...s})));
  assert.equal(cloned[2].weight,12.5);assert.equal(cloned[2].reps,'10');assert.equal(cloned[2].done,false);
  assert.notEqual(cloned[2].setId,cloned[0].setId);assert.equal(Object.hasOwn(cloned[2],'actualRir'),false);
  const failure=await page.evaluate(()=>{
   const e=state.session.exercises[0],before=JSON.stringify(e.sets),base=persistDraft;
   persistDraft=()=>{throw Error('injected storage failure');};
   try{return {result:TrainPilotSetCopy228.copy(state.session,e,e.sets[0]),same:JSON.stringify(e.sets)===before};}finally{persistDraft=base;}
  });assert.deepEqual(failure,{result:false,same:true});
  await rows().nth(1).locator('input[oninput*="reps"]').fill('10');await rows().nth(1).locator('.check').click();
  await rows().nth(2).locator('.check').click();
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
   const large=await page.addStyleTag({content:'.tp229-rir,.tp228-copy{font-size:150%!important}'});
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'Feature fields fit localized large text '+width);
   assert.ok((await page.locator('.tp228-copy').first().boundingBox()).height>=44);await large.evaluate(el=>el.remove());
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
  assert.deepEqual(errors,[]);console.log('PASS #227–#229 integrated phone flow: setting reload, explicit copy rollback/identity, one haptic per manual receipt, per-set RIR/snapshot, 4 languages/large text, reload and Journal');
 }finally{await browser?.close();await new Promise(done=>server.close(done));}
})().catch(error=>{console.error(error);process.exitCode=1;});
