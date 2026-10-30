'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve(process.env.TRAINPILOT_WEB_ROOT||'www');
const server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname==='/'?'/index.html':new URL(req.url,'http://local').pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const p=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));
 await p.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished);
 await p.evaluate(()=>{
  window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{publish:async()=>{},publishHome:async()=>{},pendingCommands:async()=>({commands:[]}),ackCommand:async()=>{},automaticCommand:async()=>{}}}};
  window.scopeFixture=(id,damaged=false)=>{
   const e={...byId(id),sets:[{set:1,setId:'set-a',done:false,reps:'5',weight:5.5},{set:2,setId:'set-b',done:false,reps:'',weight:5.5}]};
   if(damaged){e.measurementType='distance';e.sets[0].distanceMeters=0;e.sets[1].distanceMeters='';}
   state.session={syncId:'scope',started:new Date().toISOString(),timerOwner:'watch',quickWorkout:true,type:'quick',exercises:[e],automatic162:{enabled:true,owner:'watch',phase:'work'}};state.current=0;state.workout='quick';state.tab='plan';renderWorkout();
  };
  scopeFixture('db-floor-press');
  const packet={schema:1,action:'watchAutomationState',commandId:'watch-zero',workoutId:'scope',sequence:1,finalSnapshot:{active:true,workoutId:'scope',timerOwner:'watch',currentExercise:0,exercises:state.session.exercises.map(e=>({...e,sets:e.sets.map(s=>({...s,distanceMeters:0,leftSeconds:0,rightSeconds:0}))})),automation:{enabled:true,owner:'watch',phase:'rest',paused:false,key:'scope-timer',phaseNumber:1,workSeconds:30,restSeconds:120,remainingMs:120000,deadlineAt:Date.now()+120000}}};
  if(!TrainPilotWearAutomatic162.accept(packet))throw Error('watch packet rejected');renderWorkout();
 });
 assert.equal(await p.locator('#tp106GPS,#tp106Steps,#tp107Indoor').count(),0,'zero-valued Wear wire defaults must not turn a lift into a distance exercise');
 assert.equal(await p.evaluate(()=>Object.hasOwn(state.session.exercises[0].sets[0],'distanceMeters')),false,'sync does not introduce absent distance into strength sets');
 assert.deepEqual(await p.evaluate(()=>state.session.exercises[0].sets.map(s=>[s.reps,s.weight,s.done])),[['5',5.5,false],['',5.5,false]]);
 for(const width of [320,393])for(const lang of ['hu','en','de','ro']){
  await p.setViewportSize({width,height:873});await p.evaluate(lang=>{db.set('language',lang);renderWorkout();},lang);
  assert.ok(await p.evaluate(()=>{const n=document.querySelector('[data-tp162-auto-status]'),r=n.getBoundingClientRect();return r.width>40&&n.scrollWidth<=n.clientWidth+1&&n.scrollHeight<=n.clientHeight+1&&r.bottom<=innerHeight&&document.documentElement.scrollWidth<=innerWidth+1;}),'full status fits the bottom dock '+width+'/'+lang);
  if(lang==='hu')assert.ok(await p.evaluate(()=>{const n=document.querySelector('[data-tp162-auto-status]'),r=n.getBoundingClientRect(),label=document.querySelector('.tp162-automatic .tp162-auto').getBoundingClientRect();return n.textContent.trim().length>0&&Math.abs((r.top+r.bottom-label.top-label.bottom)/2)<2&&r.height<20;}),'short status shares the automatic-mode row '+width);
  assert.equal(await p.locator('#tp106GPS,#tp106Steps,#tp107Indoor').count(),0);
 }
 for(const width of [320,393]){
  await p.setViewportSize({width,height:873});await p.evaluate(()=>{db.set('language','hu');scopeFixture('db-floor-press');});
  assert.ok(await p.evaluate(()=>{const n=document.querySelector('[data-tp162-auto-status]'),r=n.getBoundingClientRect(),label=document.querySelector('.tp162-automatic .tp162-auto').getBoundingClientRect();return n.textContent==='Az óra vezérli'&&Math.abs((r.top+r.bottom-label.top-label.bottom)/2)<2&&r.height<20;}),'watch-owner text stays inline '+width);
 }
 fs.mkdirSync('ui-evidence',{recursive:true});await p.screenshot({path:'ui-evidence/automatic-strength-scope-162.png'});
 for(const id of ['db-floor-press','goblet-squat','plank']){
  await p.evaluate(id=>scopeFixture(id,true),id);
  assert.equal(await p.locator('#tp106GPS,#tp106Steps').count(),0,'existing polluted draft is repaired for '+id);
  assert.equal(await p.evaluate(()=>state.session.exercises[0].measurementType==='distance'),false);
  assert.deepEqual(await p.evaluate(()=>state.session.exercises[0].sets.map(s=>[s.reps,s.weight,s.done])),[['5',5.5,false],['',5.5,false]]);
  assert.doesNotMatch(await p.evaluate(()=>formatSet(state.session.exercises[0],state.session.exercises[0].sets[0])),/km/);
 }
 for(const id of ['running','inline-skating','walking']){
  await p.evaluate(id=>scopeFixture(id),id);
  assert.equal(await p.locator('#tp106GPS').count(),1,'real distance activities retain GPS '+id);
  assert.equal(await p.locator('#tp107Indoor').count(),1,'real distance activities retain indoor timing '+id);
 }
 assert.equal(await p.evaluate(()=>tp106IsDistance({id:'custom-unlisted-distance',measurementType:'distance',sets:[]})),true,'explicit custom distance type survives without measured kilometres');
 assert.equal(await p.evaluate(()=>tp106IsDistance({id:'legacy-cardio',sets:[{distanceMeters:2500}]})),true,'real legacy distances are retained');
 assert.deepEqual(errors,[]);console.log('PASS #162: real Wear zero defaults, repaired strength/timed drafts, unchanged sets, full multilingual bottom status and retained running/skating/custom/legacy distance controls');
}finally{await browser?.close();server.close();}})().catch(e=>{console.error(e);process.exitCode=1;server.close();});
