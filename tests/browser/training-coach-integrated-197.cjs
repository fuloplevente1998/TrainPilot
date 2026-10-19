'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const loc=new URL(req.url,'http://127.0.0.1'),file=path.resolve(root,'.'+(loc.pathname==='/'?'/index.html':loc.pathname));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(err,body)=>{if(err){res.writeHead(404);return res.end();}
 res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(body);});
});
(async()=>{
 await new Promise(done=>server.listen(0,'127.0.0.1',done));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:360,height:800},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&typeof tp197CoachPanel==='function');
  const result=await page.evaluate(()=>{
   state.session={programId:'home-basic',started:new Date().toISOString(),timerOwner:'phone',
    exercises:[{id:'goblet-squat',sets:[{set:1,weight:10,reps:'',done:false},{set:2,weight:10,reps:'',done:false}]}]};
   state.current=0;window.__tp197Saves=0;
   db.set('settings',{...settings(),trainingModes197:{schemaVersion:1,defaults:{trainingStyle:'normal',progressionMode:'linear'},explicitChoice:true,byProgram:{}}});
   tp197ExercisePreview=()=>({handled:true,action:'increase',reasonCode:'linear-stable-performance',historyCount:2,baseline:{weight:10,sets:2},proposed:{weight:12,sets:2}});
   persistDraft=()=>{window.__tp197Saves++};
   // This test renders a sandboxed Coach fixture; close any app startup dialog that would intercept synthetic interaction.
   document.querySelector('#tp2628Dialog')?.remove();
   const fixture=document.createElement('div');fixture.id='tp197BrowserFixture';document.body.appendChild(fixture);
   renderWorkout=()=>{fixture.innerHTML=tp197CoachPanel()};
   renderWorkout();
   return {source:tp197TrainingModes('home-basic').source,text:fixture.innerText};
  });
  assert.equal(result.source,'global');
  assert.match(result.text,/Lineáris|lineáris/i);
  assert.match(result.text,/Javaslat ellenőrzése/);
  await page.locator('#tp197BrowserFixture button').first().click();
  assert.equal(await page.locator('#tp197BrowserFixture button').count(),2);
  assert.equal(await page.evaluate(()=>window.__tp197Saves),0);
  await page.locator('#tp197BrowserFixture button').last().click();
  assert.equal(await page.evaluate(()=>window.__tp197Saves),0);
  await page.locator('#tp197BrowserFixture button').first().click();
  await page.evaluate(()=>state.session.exercises[0].sets[0].reps='1');
  await page.locator('#tp197BrowserFixture button').first().click();
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].weight),10);
  await page.evaluate(()=>{state.session.exercises[0].sets[0].reps='';state.session.timerOwner='watch';renderWorkout()});
  assert.equal(await page.locator('#tp197BrowserFixture button').count(),0);
  await page.evaluate(()=>{state.session.timerOwner='phone';renderWorkout()});
  await page.locator('#tp197BrowserFixture button').first().click();
  await page.locator('#tp197BrowserFixture button').first().click();
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets.every(s=>s.weight===12)),true);
  assert.equal(await page.evaluate(()=>window.__tp197Saves),1);
  assert.deepEqual(errors,[]);
  console.log('PASS #197 browser: Coach approval, cancel, stale proposal and Wear ownership');
 }finally{await browser?.close();await new Promise(done=>server.close(done));}
})().catch(e=>{console.error(e);process.exitCode=1});
