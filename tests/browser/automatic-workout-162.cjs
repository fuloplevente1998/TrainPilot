'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname==='/'?'/index.html':new URL(req.url,'http://local').pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});const p=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));p.on('dialog',d=>d.accept());
 await p.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished);
 await p.evaluate(()=>{
  const m=window.__auto162={plan:null,elapsed:0,running:false,stopped:false,interrupted:false,starts:0};
  const status=()=>m.plan?{...TrainPilotInterval162Core.phase(m.plan.steps,m.plan.autoAdvance,m.elapsed,m.plan.initialRest||0,!!m.plan.restOnly),key:m.plan.key,steps:m.plan.steps,running:m.running,stopped:m.stopped,interrupted:m.interrupted,gps:{key:'',distanceMeters:0,active:false}}:{};
  window.Capacitor={isNativePlatform:()=>true,Plugins:{IntervalTimer:{start:async plan=>{m.starts++;if(m.wait)await new Promise(r=>m.release=r);m.plan=JSON.parse(JSON.stringify(plan));m.elapsed=0;m.running=true;m.stopped=false;m.interrupted=false;return status();},status:async()=>status(),pause:async({key})=>{assertKey(key);m.running=false;return status();},resume:async({key})=>{assertKey(key);m.running=true;return status();},stop:async({key})=>{assertKey(key);m.running=false;m.stopped=true;return status();},skipRest:async({key})=>{assertKey(key);const phase=status();if(phase.phase!=='rest')throw Error('not resting');m.elapsed+=phase.remainingMs;m.running=true;return status();}}}};
  function assertKey(key){if(key!==m.plan?.key)throw Error('owner changed');}
  window.fixtureAuto162=()=>{const ex=(id,count)=>({...byId(id),sets:Array.from({length:count},(_,i)=>({set:i+1,setId:crypto.randomUUID(),reps:i===0?'5':'0',weight:5.5,done:false}))});state.session={started:new Date().toISOString(),syncId:crypto.randomUUID(),quickWorkout:true,type:'quick',exercises:[ex('db-floor-press',2),ex('goblet-squat',1),ex('plank',1)]};state.session.exercises[2].sets[0].reps='0';state.current=0;state.workout='quick';state.tab='plan';db.set('settings',{...settings(),rest:30,automaticWorkout162:false,intervalPreset110:{work:2,rest:1,rounds:1,sound:false,vibration:true,autoAdvance:true}});persistDraft();renderWorkout();};fixtureAuto162();
 });
 for(const width of [320,393])for(const lang of ['hu','en','de','ro'])for(const theme of ['classicBlue','blue']){
  await p.setViewportSize({width,height:873});await p.evaluate(({lang,theme})=>{db.set('language',lang);rf200SetTheme(theme);renderWorkout();},{lang,theme});assert.equal(await p.locator('[data-tp162-automatic]').count(),1);assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth-innerWidth)<=1,'automatic controls fit '+width+'/'+lang+'/'+theme);
 }
 await p.setViewportSize({width:393,height:873});await p.evaluate(async()=>{db.set('language','hu');await toggleSet(0,0);});await p.waitForFunction(()=>document.querySelector('.timer.tp16-rest-timer')?.closest('.tp153-workout-actions'));
 assert.ok(await p.evaluate(()=>document.querySelector('.timer').getBoundingClientRect().top>innerHeight*.6),'manual countdown stays in the bottom dock');
 await p.evaluate(()=>{skipTimer();fixtureAuto162();});
 await p.evaluate(async()=>{await TrainPilotAutomatic162.toggle(true);await toggleSet(0,0);});await p.waitForFunction(()=>state.session.automatic162.run?.running);
 await p.waitForFunction(()=>document.querySelector('[data-tp162-countdown]')?.closest('.tp153-workout-actions'));
 assert.ok(await p.evaluate(()=>document.querySelector('[data-tp162-countdown]').getBoundingClientRect().top>innerHeight*.6),'automatic countdown stays in the bottom dock');
 assert.ok(await p.evaluate(()=>{const clock=document.querySelector('[data-tp162-countdown]').getBoundingClientRect(),prev=document.querySelector('.tp16-workout-prev').getBoundingClientRect(),next=document.querySelector('.tp16-workout-next').getBoundingClientRect();return prev.right<=clock.left+1&&clock.right<=next.left+1&&Math.abs(prev.top-next.top)<=1;}),'active countdown remains between Previous and Next');
 assert.ok(await p.evaluate(()=>document.querySelector('[data-tp162-enabled]').closest('label').getBoundingClientRect().height<document.querySelector('[data-tp-workout-next]').getBoundingClientRect().height),'automatic mode is smaller than primary navigation buttons');
 assert.deepEqual(await p.evaluate(()=>[state.session.exercises[0].sets[0].done,state.session.exercises[0].sets[1].done,state.session.exercises[0].sets[1].reps,__auto162.plan.restOnly]),[true,false,'0',true]);
 for(const width of [320,393])for(const lang of ['hu','en','de','ro'])for(const theme of ['classicBlue','blue']){
  await p.setViewportSize({width,height:873});await p.evaluate(({lang,theme})=>{db.set('language',lang);rf200SetTheme(theme);renderWorkout();},{lang,theme});
  const label=p.locator('[data-tp162-dock-label]');assert.equal(await label.isVisible(),true,'rest phase survives compact timer decoration '+width+'/'+lang+'/'+theme);
  assert.equal(await label.textContent(),({hu:'Pihenő',en:'Rest',de:'Pause',ro:'Odihnă'})[lang]);
  assert.ok(await p.evaluate(()=>{const dock=document.querySelector('[data-tp162-countdown]').getBoundingClientRect(),prev=document.querySelector('.tp16-workout-prev').getBoundingClientRect(),next=document.querySelector('.tp16-workout-next').getBoundingClientRect();return prev.right<=dock.left+1&&dock.right<=next.left+1&&document.documentElement.scrollWidth<=innerWidth+1;}),'named countdown stays between bottom buttons');
 }
 await p.setViewportSize({width:393,height:873});await p.evaluate(()=>{db.set('language','hu');renderWorkout();});
 assert.equal(await p.evaluate(()=>TrainPilotWearSync.makeSnapshot().automation.restSeconds),30,'snapshot preserves this rest phase duration');
 fs.mkdirSync('ui-evidence',{recursive:true});await p.screenshot({path:'ui-evidence/automatic-rest-label-162.png'});

 await p.evaluate(async()=>{__auto162.elapsed=5000;await TrainPilotAutomatic162.sync();await TrainPilotAutomatic162.command('pause');});const paused=await p.evaluate(()=>state.session.automatic162.run.remainingMs);await p.waitForTimeout(300);assert.equal(await p.evaluate(()=>state.session.automatic162.run.remainingMs),paused);
 await p.evaluate(()=>TrainPilotAutomatic162.command('resume'));await p.evaluate(()=>TrainPilotAutomatic162.command('skipRest'));assert.equal(await p.evaluate(()=>state.session.automatic162.phase),'ready');assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[1].done),false,'rest never fabricates a recorded set');
 await p.evaluate(async()=>{state.session.exercises[0].sets[1].reps='6';await toggleSet(0,1);});await p.waitForFunction(()=>state.session.automatic162.run?.running);await p.evaluate(async()=>{__auto162.elapsed=30000;await TrainPilotAutomatic162.sync();});assert.equal(await p.evaluate(()=>state.current),1,'rest advances to the next exercise');assert.equal(await p.evaluate(()=>state.session.exercises[1].sets[0].done),false);await p.evaluate(()=>TrainPilotAutomatic162.sync());assert.equal(await p.evaluate(()=>state.current),1,'duplicate callbacks do not advance twice');
 // Strength-to-timed transition includes its rest in the native plan; no extra Start.
 await p.evaluate(()=>toggleSet(1,0));await p.waitForFunction(()=>state.session.interval162?.running);assert.equal(await p.evaluate(()=>__auto162.plan.initialRest),30);assert.equal(await p.evaluate(()=>state.current),2);assert.equal(await p.evaluate(()=>state.session.exercises[2].sets[0].reps),'0');
 await p.evaluate(async()=>{__auto162.elapsed=30000;await TrainPilotIntervals162.sync();});assert.equal(await p.evaluate(()=>state.session.interval162.phase.phase),'work');assert.equal(await p.evaluate(()=>state.session.exercises[2].sets[0].done),false);
 await p.evaluate(async()=>{__auto162.elapsed=32000;await TrainPilotIntervals162.sync();});assert.equal(await p.evaluate(()=>state.session.exercises[2].sets[0].done),true);assert.equal(await p.evaluate(()=>history().length),0,'workout is not saved automatically');
 await p.evaluate(()=>renderWorkout());assert.equal(await p.locator('#tp106GPS,#tp106Steps').count(),0,'empty GPS service state cannot turn a timed plank into a distance exercise');
 // Undo, interruption and draft restore retain the exact unfinished owner.
 await p.evaluate(async()=>{fixtureAuto162();await TrainPilotAutomatic162.toggle(true);await toggleSet(0,0);});await p.waitForFunction(()=>state.session.automatic162.run?.running);await p.evaluate(()=>toggleSet(0,0));assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].done),false);assert.equal(await p.evaluate(()=>state.session.automatic162.run),null);assert.equal(await p.evaluate(()=>state.session.automatic162.enabled),true);
 await p.evaluate(()=>toggleSet(0,0));await p.waitForFunction(()=>state.session.automatic162.run?.running);await p.evaluate(async()=>{persistDraft();state.session=null;state.workout=null;resumeDraft();__auto162.running=false;__auto162.interrupted=true;await TrainPilotAutomatic162.sync();});await p.waitForFunction(()=>state.session.automatic162.phase==='interrupted');assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[1].done),false);await p.evaluate(()=>TrainPilotAutomatic162.toggle(false));assert.equal(await p.evaluate(()=>state.session.automatic162.enabled),false);
 // A delayed native start cannot change a replacement workout.
 await p.evaluate(async()=>{fixtureAuto162();await TrainPilotAutomatic162.toggle(true);__auto162.wait=true;void toggleSet(0,0);});await p.waitForFunction(()=>!!__auto162.release);await p.evaluate(()=>{fixtureAuto162();state.session.syncId='replacement';state.session.exercises[0].sets[0].reps='77';__auto162.wait=false;__auto162.release();});await p.waitForTimeout(350);assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].reps),'77');assert.equal(await p.evaluate(()=>__auto162.stopped),true);
 // A plain browser does not promise background timing or resume lost elapsed time.
 await p.evaluate(async()=>{window.Capacitor={isNativePlatform:()=>false};fixtureAuto162();await TrainPilotAutomatic162.toggle(true);await toggleSet(0,0);});assert.equal(await p.evaluate(()=>state.session.automatic162.run.native),false);await p.locator('[data-tp162-automatic]').scrollIntoViewIfNeeded();await p.locator('.tp153-toast.show').waitFor({state:'hidden',timeout:5000});fs.mkdirSync('ui-evidence',{recursive:true});await p.screenshot({path:'ui-evidence/automatic-workout-162.png'});assert.deepEqual(errors,[]);
 const hapticSignals=await p.evaluate(async()=>{
  const signals=[];Object.defineProperty(navigator,'vibrate',{configurable:true,value:ms=>{signals.push(ms);return true;}});
  await TrainPilotAutomatic162.sync();signals.length=0;
  state.session.automatic162.run.elapsedMs=27000;await TrainPilotAutomatic162.sync();await TrainPilotAutomatic162.sync();
  await TrainPilotAutomatic162.command('pause');await TrainPilotAutomatic162.sync();
  await TrainPilotAutomatic162.command('resume');
  state.session.automatic162.run.elapsedMs=28000;await TrainPilotAutomatic162.sync();
  state.session.automatic162.run.elapsedMs=29000;await TrainPilotAutomatic162.sync();
  state.session.automatic162.run.elapsedMs=30000;await TrainPilotAutomatic162.sync();await TrainPilotAutomatic162.sync();
  return signals;
 });
 assert.deepEqual(hapticSignals,[80,0,80,80,1000],'browser rest has three short cues, cancels on pause, and finishes once with one second');
 console.log('PASS automatic workout: 16 layouts, manual repetition records, native rest pause/skip, next exercise, timed transition, undo/draft/interruption, stale-owner protection, browser fallback and 3–2–1 haptics');
}finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
