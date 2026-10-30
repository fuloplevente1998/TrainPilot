'use strict';
// PR279: RIR can be entered before OK, but Coach and Journal count completed sets only.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const pathname=new URL(req.url,'http://localhost').pathname;
 const file=path.resolve(root,'.'+(pathname==='/'?'/index.html':pathname));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(file,(error,body)=>{if(error){res.writeHead(404);return res.end()}
 res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(body)});
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:320,height:760},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  const address='http://127.0.0.1:'+server.address().port+'/';
  await page.goto(address);
  const boot=()=>page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await boot();
  await page.evaluate(()=>{
   const p={id:'pr279-rir-preok',name:'RIR pending set test',location:'home',level:'all',builtin:false,
    days:[{id:'A',name:'A',exercises:['goblet-squat']}],
    prescriptions:{'goblet-squat':{sets:2,reps:'8–12',rir:2,rest:30,weight:10,loadType:byId('goblet-squat').loadType}}};
   db.set('programs',[...programs().filter(x=>x.id!==p.id),p]);
   db.set('activeProgramId',p.id);db.set('history',[]);db.set('draft',null);
   db.set('settings',{...settings(),trainingModes197:{schemaVersion:1,defaults:{trainingStyle:'normal',progressionMode:'rir'},explicitChoice:true,byProgram:{}}});
   startWorkout('A',null,'pr279-rir-preok');
  });
  const first=page.locator('.tp277-set-row').nth(0);
  const pending=first.locator('input[data-inline-rir="true"]');
  assert.equal(await pending.count(),1,'before-OK RIR input must be visible');
  assert.equal(await first.locator('.check').innerText(),'OK');
  await pending.fill('0');
  assert.deepEqual(await page.evaluate(()=>({
   done:state.session.exercises[0].sets[0].done,
   actual:db.get('draft',null).session.exercises[0].sets[0].actualRir,
   observation:tp229RirObservation(state.session.exercises[0])
  })),{done:false,actual:0,observation:null},'pending zero is saved but not a Coach observation');
  await pending.fill('5');
  await page.reload();await boot();
  await page.evaluate(()=>{if(!state.session)resumeDraft();else renderWorkout()});
  const restored=page.locator('.tp277-set-row').nth(0).locator('input[data-inline-rir="true"]');
  assert.equal(await restored.inputValue(),'5','pending actual RIR persists across reload');
  await restored.fill('11');
  assert.equal(await restored.getAttribute('aria-invalid'),'true','invalid RIR must be marked');
  assert.equal(await page.evaluate(()=>Object.hasOwn(db.get('draft',null).session.exercises[0].sets[0],'actualRir')),false,'invalid RIR clears stale draft observation');
  await restored.fill('4');assert.equal(await restored.getAttribute('aria-invalid'),'false');
  const guard=await page.evaluate(()=>{
   const session=state.session,e=session.exercises[0],s=e.sets[0];
   return {
    wrongWorkout:tp197RirSave('9',false,s.setId,true,0,e.id,'stale-started'),
    wrongIndex:tp197RirSave('9',false,s.setId,true,1,e.id,session.started),
    wrongExercise:tp197RirSave('9',false,s.setId,true,0,'stale-exercise',session.started),
    pending:s.actualRir
   };
  });
  assert.deepEqual(guard,{wrongWorkout:false,wrongIndex:false,wrongExercise:false,pending:4},'stale UI cannot overwrite another set');
  await first.locator('input[oninput*="weight"]').fill('10');
  await first.locator('input[oninput*="reps"]').fill('10');
  await first.locator('.check').click();
  assert.deepEqual(await page.evaluate(()=>({
   done:state.session.exercises[0].sets[0].done,actual:state.session.exercises[0].sets[0].actualRir,
   coach:tp229RirObservation(state.session.exercises[0])
  })),{done:true,actual:4,coach:4},'OK promotes pending RIR to a completed observation');
  const second=page.locator('.tp277-set-row').nth(1);
  await second.locator('input[data-inline-rir="true"]').fill('0');
  assert.equal(await page.evaluate(()=>tp229RirObservation(state.session.exercises[0])),4,'unfinished second set RIR cannot change Coach');
  await page.locator('.tp277-set-row').first().locator('.check').click();
  assert.equal(await page.evaluate(()=>Object.hasOwn(state.session.exercises[0].sets[0],'actualRir')),false,'unchecking OK removes completed RIR');
  assert.equal(await page.evaluate(()=>tp229RirObservation(state.session.exercises[0])),null,'unfinished set entries are not used in Coach');
  await page.locator('.tp277-set-row').first().locator('input[data-inline-rir="true"]').fill('3');
  await page.locator('.tp277-set-row').first().locator('.check').click();
  assert.equal(await page.evaluate(()=>tp229RirObservation(state.session.exercises[0])),3);
  const preview=await page.evaluate(()=>{
   const ids=exercises().slice(0,5).map(e=>e.id),p={id:'pr279-multi',days:[{id:'A',exercises:ids}]};
   const outer=document.createElement('details');outer.className='tp277-day';outer.innerHTML='<summary>'+tp277DayOverview(p,p.days[0])+'</summary>';
   document.body.appendChild(outer);const more=outer.querySelector('.tp279-preview-more');
   if(!more)return {markup:false};
   const closed=getComputedStyle(more).display;
   outer.open=true;const opened=getComputedStyle(more).display;
   outer.remove();return {markup:true,closed,opened};
  });
  assert.deepEqual(preview,{markup:true,closed:'inline',opened:'none'},'expanded day hides redundant +N preview only');
  assert.deepEqual(errors,[]);
  console.log('PASS PR279: RIR before OK, 0/invalid/draft reload, identity safety, completed Coach only, undo and expanded day preview');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve))}
})().catch(e=>{console.error(e);process.exitCode=1});