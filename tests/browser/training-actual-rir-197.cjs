'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright'),root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const name=new URL(req.url,'http://local').pathname,file=path.resolve(root,'.'+(name==='/'?'/index.html':name));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(err,data)=>{if(err){res.writeHead(404);return res.end();}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});
});
(async()=>{
 await new Promise(done=>server.listen(0,'127.0.0.1',done));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:360,height:800},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',err=>errors.push(String(err)));page.on('dialog',dialog=>dialog.accept());
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  const boot=()=>page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await boot();
  await page.evaluate(()=>{
   const p={id:'rir-e2e',name:'RIR E2E',location:'home',level:'all',builtin:false,
    days:[{id:'A',name:'A',exercises:['goblet-squat']}],
    prescriptions:{'goblet-squat':{sets:1,reps:'8–12',rir:2,rest:30,weight:10,loadType:byId('goblet-squat').loadType}}};
   db.set('programs',[...programs().filter(x=>x.id!==p.id),p]);db.set('activeProgramId',p.id);
   db.set('history',[]);db.set('draft',null);
   db.set('settings',{...settings(),trainingModes197:{schemaVersion:1,defaults:{trainingStyle:'normal',progressionMode:'rir'},explicitChoice:true,byProgram:{}}});
  });
  const begin=()=>page.evaluate(()=>startWorkout('A',null,'rir-e2e'));
  async function completeSet(){
   assert.equal(await page.locator('#tp197ActualRir').count(),0,'No actual RIR before a completed strength set');
   await page.locator('.tp153-set-row input[oninput*="weight"]').fill('10');
   await page.locator('.tp153-set-row input[oninput*="reps"]').fill('10');
   await page.locator('.tp153-set-row .check').click();
   await page.locator('#rf152Feedback button[onclick*="good"]').click();
   assert.equal(await page.locator('#tp197ActualRir').inputValue(),'','Target RIR must not prefill actual RIR');
  }
  async function choose(value){
   const trigger=page.locator('.tp197-rir-field .tp-select-trigger');
   await trigger.scrollIntoViewIfNeeded();await page.waitForTimeout(120);await trigger.click();
   await page.locator('.tp197-rir-field .tp-select-option[data-value="'+value+'"]').click();
   assert.equal(await page.locator('#tp197ActualRir').inputValue(),value);
  }
  async function finish(){
   await page.locator('[data-tp-workout-next]').click();
   await page.getByRole('button',{name:'Most kihagyom'}).click();
   await page.evaluate(()=>go('history'));
   await page.locator('details.history').first().evaluate(node=>node.open=true);
  }
  await begin();await completeSet();
  await choose('0');assert.equal(await page.evaluate(()=>db.get('draft',null).session.exercises[0].actualRir),0);
  await choose('');assert.equal(await page.evaluate(()=>Object.hasOwn(db.get('draft',null).session.exercises[0],'actualRir')),false);
  await choose('6');
  await page.reload();await boot();
  await page.evaluate(()=>{if(!state.session)resumeDraft();else renderWorkout();});
  await page.waitForSelector('#tp197ActualRir',{state:'attached'});
  assert.equal(await page.locator('#tp197ActualRir').inputValue(),'6','Draft restored after a real page reload');
  await finish();
  assert.equal(await page.evaluate(()=>history()[0].exercises[0].actualRir),6,'Actual RIR survives workout finalization');
  assert.match(await page.locator('details.history .tp197-rir-history').first().innerText(),/Tényleges RIR.*6/);
  await page.reload();await boot();
  const first=await page.evaluate(()=>tp197ExercisePreview('goblet-squat','rir-e2e'));
  assert.equal(first.reasonCode,'rir-await-stability');assert.equal(first.historyCount,1);
  await begin();await completeSet();await choose('6');await finish();
  const stable=await page.evaluate(()=>tp197ExercisePreview('goblet-squat','rir-e2e'));
  assert.equal(stable.reasonCode,'rir-stable-reserve');assert.equal(stable.action,'increase');
  assert.equal(stable.autoApply,false);assert.ok(stable.proposed.weight>10);
  assert.equal(await page.evaluate(()=>programById('rir-e2e').prescriptions['goblet-squat'].weight),10,'Recommendation never silently changes the program');
  await begin();await completeSet();await choose('0');await finish();
  assert.equal(await page.evaluate(()=>tp197ExercisePreview('goblet-squat','rir-e2e').reasonCode),'rir-overexertion');
  await begin();await completeSet();await finish();
  assert.equal(await page.evaluate(()=>tp197ExercisePreview('goblet-squat','rir-e2e').reasonCode),'rir-feedback-missing','Optional skipped feedback is never inferred from the target');
  assert.deepEqual(errors,[]);
  console.log('PASS #197 actual RIR E2E: real selector, zero/clear, draft reload, finish, visible Journal, persisted history and conservative next Coach recommendation without auto-apply');
 }finally{await browser?.close();await new Promise(done=>server.close(done));}
})().catch(err=>{console.error(err);process.exitCode=1});
