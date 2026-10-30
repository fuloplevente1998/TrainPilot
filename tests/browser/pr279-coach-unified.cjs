'use strict';
// PR #279 Coach UI: real HTML disclosure + original guarded approval flow.
const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const loc=new URL(req.url,'http://127.0.0.1');
 const name=path.resolve(root,'.'+(loc.pathname==='/'?'/index.html':loc.pathname));
 if(!name.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(name,(error,contents)=>{if(error){res.writeHead(404);return res.end()}
 res.setHeader('Content-Type',name.endsWith('.js')?'text/javascript':name.endsWith('.css')?'text/css':'text/html');res.end(contents)});
});
(async()=>{
 await new Promise(done=>server.listen(0,'127.0.0.1',done));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:320,height:760},locale:'hu-HU'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&typeof window.TrainPilotWorkoutCoach?.plan==='function');
  await page.evaluate(()=>{
   db.set('language','hu');
   db.set('settings',{...settings(),trainingModes197:{schemaVersion:1,defaults:{trainingStyle:'normal',progressionMode:'linear'},explicitChoice:true,byProgram:{}}});
   const sets=[1,2,3].map(n=>({set:n,weight:10,reps:'',done:false,actualRir:n===1?4:undefined}));
   state.session={programId:'home-basic',started:new Date().toISOString(),timerOwner:'phone',exercises:[{id:'goblet-squat',sets}]};
   state.current=0;tp197CoachPending=null;window.__tp279Saves=0;
   persistDraft=()=>{window.__tp279Saves++};
   tp197ExercisePreview=()=>({handled:true,action:'increase',reasonCode:'linear-stable-performance',historyCount:2,baseline:{weight:10,sets:3},proposed:{weight:12,sets:2}});
   rf233Decision=()=>({mode:'normal',text:'Normal day'});
   rf152Recommendation=()=>({action:'hold',text:'Hosszú távú cél',historyCount:2});
   document.querySelector('#tp2628Dialog')?.remove();
   const fixture=document.createElement('div');fixture.id='tp279CoachFixture';document.body.appendChild(fixture);
   window.__tp279Paint=function(){
    const se=state.session.exercises[0],w=window.TrainPilotWorkoutCoach;
    fixture.innerHTML='<details class="tp155-workout-coach">'+w.html(se,{action:'hold',label:'Súly tartása',text:'Tartsd a terhelést.'})+'</details>';
   };
   renderWorkout=window.__tp279Paint;window.__tp279Paint();
  });
  const card=page.locator('#tp279CoachFixture details.tp155-workout-coach');
  assert.equal(await card.count(),1);
  assert.equal(await card.evaluate(e=>e.open),false,'one Coach disclosure starts collapsed');
  assert.match(await card.locator('summary').innerText(),/Progresszió \/ Coach/);
  assert.match(await card.locator('summary').innerText(),/Kis súlyemelés/);
  assert.doesNotMatch(await card.innerText(),/Progresszív cél/,'no repeated generic progressive goal card');
  await card.locator('summary').click();
  assert.match(await card.locator('.tp279-coach-unified').innerText(),/Tervezett munkasorozat\s*3/);
  assert.match(await card.locator('.tp279-coach-unified').innerText(),/Javasolt munkasorozat.*2/);
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets.length),3,'proposed 2 sets must not silently edit the 3-set plan');
  assert.equal(await card.locator('.tp279-coach-basis').count(),1);
  await card.locator('.tp279-coach-basis > summary').click();
  assert.match(await card.locator('.tp279-coach-basis').innerText(),/Hosszabb távú cél/);
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].actualRir),4,'RIR survives opening details');
  assert.equal(await card.locator('button').count(),1,'original guarded Review button reused');
  await card.locator('button').first().dispatchEvent('click');
  assert.equal(await page.evaluate(()=>window.__tp279Saves),0,'Review does not edit draft');
  assert.equal(await card.locator('button').count(),2,'review still requires confirm/cancel');
  await card.locator('button').first().dispatchEvent('click');
  assert.deepEqual(await page.evaluate(()=>({
   weights:state.session.exercises[0].sets.map(x=>x.weight),
   count:state.session.exercises[0].sets.length,
   rir:state.session.exercises[0].sets[0].actualRir
  })),{weights:[12,12,12],count:3,rir:4},'only explicit approval modifies load');
  await page.evaluate(()=>{
   const e=state.session.exercises[0];e.effort='pain';e.sets.forEach(s=>{s.weight=10});window.__tp279Paint();
  });
  assert.match(await card.locator('summary').innerText(),/Fájdalomjelzés/);
  assert.equal(await card.locator('button').count(),0,'pain cannot display a load-increase action');
  await page.evaluate(()=>{
   const e=state.session.exercises[0];delete e.effort;state.session.timerOwner='watch';window.__tp279Paint();
  });
  assert.equal(await card.locator('button').count(),0,'watch-owned session has no phone apply action');
  await page.evaluate(()=>{
   state.session.timerOwner='phone';state.session.quickWorkout=true;window.__tp279Paint();
  });
  assert.equal(await card.locator('.tp279-coach-change').count(),0,'quick session has no invented program-day set proposal');
  assert.equal(await card.locator('button').count(),0);
  assert.deepEqual(errors,[]);
  console.log('PASS PR279: compact Coach, explicit set distinction, approval, pain, Wear owner and quick workout');
 }finally{await browser?.close();await new Promise(done=>server.close(done))}
})().catch(e=>{console.error(e);process.exitCode=1});