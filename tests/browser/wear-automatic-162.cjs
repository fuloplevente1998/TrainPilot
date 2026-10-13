'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname==='/'?'/index.html':new URL(req.url,'http://local').pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({args:['--no-sandbox']});const p=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));p.on('dialog',d=>d.accept());
 await p.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished);
 await p.evaluate(()=>{
  window.__watchCommands=[];window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{automaticCommand:async({command})=>{__watchCommands.push(command);return {};},publish:async()=>({revision:1}),publishHome:async()=>({}),pendingCommands:async()=>({commands:[]}),ackCommand:async()=>({})}}};
  const ex=(id,ids)=>({...byId(id),sets:ids.map((setId,i)=>({set:i+1,setId,reps:'5',weight:5.5,done:false}))});
  state.session={started:new Date().toISOString(),syncId:'watch-owned',timerOwner:'watch',quickWorkout:true,type:'quick',exercises:[ex('db-floor-press',['s1','s2']),ex('goblet-squat',['s3'])],automatic162:{enabled:false,owner:'watch',phase:'work'}};state.current=0;state.workout='quick';state.tab='plan';renderWorkout();
  window.watchPacket=(sequence,values={},current=state.current)=>({schema:1,action:'watchAutomationState',commandId:'reply-'+sequence,workoutId:'watch-owned',sequence,
   finalSnapshot:{active:true,workoutId:'watch-owned',timerOwner:'watch',currentExercise:current,exercises:JSON.parse(JSON.stringify(state.session.exercises)),
    automation:{enabled:true,owner:'watch',phase:'ready',paused:false,key:'watch-run',phaseNumber:1,workSeconds:30,restSeconds:5,remainingMs:0,deadlineAt:0,rounds:1,round:1,...values}}});
  if(!TrainPilotWearAutomatic162.accept(watchPacket(1)))throw Error('initial watch state rejected');
 });
 for(const width of [320,393])for(const lang of ['hu','en','de','ro']){
  await p.setViewportSize({width,height:873});await p.evaluate(lang=>{db.set('language',lang);renderWorkout();},lang);assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth-innerWidth)<=1,'peer controls fit '+width+'/'+lang);
 }
 await p.setViewportSize({width:393,height:873});await p.evaluate(()=>{db.set('language','hu');renderWorkout();});
 await p.evaluate(()=>toggleSet(0,0));assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].done),false,'phone waits for actual owner record');
 assert.deepEqual(await p.evaluate(()=>[__watchCommands[0].action,__watchCommands[0].setId,__watchCommands[0].phaseNumber]),['record','s1',1]);
 await p.evaluate(()=>{const packet=watchPacket(2,{phase:'rest',phaseNumber:2,remainingMs:5000,deadlineAt:Date.now()+5000});packet.finalSnapshot.exercises[0].sets[0].done=true;if(!TrainPilotWearAutomatic162.accept(packet))throw Error('rest rejected');});
 assert.equal(await p.evaluate(()=>state.session.automatic162.run),null,'watch rest never starts a second phone timer');
 assert.equal(await p.evaluate(()=>state.session.interval162||null),null);
 assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].done),true);
 await p.waitForSelector('[data-tp162-countdown]');assert.ok(await p.evaluate(()=>{const clock=document.querySelector('[data-tp162-countdown]').getBoundingClientRect(),prev=document.querySelector('.tp16-workout-prev').getBoundingClientRect(),next=document.querySelector('.tp16-workout-next').getBoundingClientRect();return clock.left>=prev.right-1&&clock.right<=next.left+1;}),'watch countdown remains between navigation buttons');
 await p.evaluate(()=>{const packet=watchPacket(1);packet.finalSnapshot.exercises[0].sets[0].done=false;TrainPilotWearAutomatic162.accept(packet);});assert.equal(await p.evaluate(()=>state.session.exercises[0].sets[0].done),true,'stale mirror cannot undo a recorded set');
 await p.evaluate(()=>TrainPilotAutomatic162.dockCommand('toggle'));assert.equal(await p.evaluate(()=>__watchCommands.at(-1).action),'pause');
 await p.evaluate(()=>TrainPilotWearAutomatic162.accept(watchPacket(3,{phase:'rest',phaseNumber:2,paused:true,remainingMs:4000,deadlineAt:0})));
 const paused=await p.locator('[data-tp162-auto-clock]').textContent();await p.waitForTimeout(300);assert.equal(await p.locator('[data-tp162-auto-clock]').textContent(),paused);
 await p.evaluate(()=>TrainPilotAutomatic162.dockCommand('skipRest'));assert.equal(await p.evaluate(()=>__watchCommands.at(-1).phaseNumber),2,'remote action binds to owner phase');
 await p.evaluate(()=>TrainPilotWearAutomatic162.accept(watchPacket(4,{phaseNumber:3})));
 assert.equal(await p.locator('[data-tp162-countdown]').count(),0);
 await p.evaluate(()=>toggleSet(0,1));await p.evaluate(()=>{const packet=watchPacket(5,{phase:'rest',phaseNumber:4,remainingMs:5000,deadlineAt:Date.now()+5000});packet.finalSnapshot.exercises[0].sets[1].done=true;TrainPilotWearAutomatic162.accept(packet);});
 await p.evaluate(()=>TrainPilotWearAutomatic162.accept(watchPacket(6,{phaseNumber:5},1)));assert.equal(await p.evaluate(()=>state.current),1,'owner alone advances exercise');
 const invalid=await p.evaluate(()=>{const packet=watchPacket(7);packet.finalSnapshot.currentExercise=99;return TrainPilotWearAutomatic162.accept(packet);});assert.equal(invalid,false);
 assert.equal(await p.evaluate(()=>TrainPilotWearSync.makeSnapshot().automation.owner),'watch');assert.equal(await p.evaluate(()=>TrainPilotWearSync.makeHomeSnapshot().automaticProtocol),1);
 await p.evaluate(()=>prevExercise());assert.equal(await p.evaluate(()=>__watchCommands.at(-1).action),'selectExercise');
 assert.equal(await p.evaluate(()=>__watchCommands.at(-1).exerciseIndex),0);assert.equal(await p.evaluate(()=>state.current),1,'navigation waits for watch owner');
 await p.evaluate(()=>TrainPilotWearAutomatic162.accept(watchPacket(8,{enabled:false,phaseNumber:6},0)));assert.equal(await p.evaluate(()=>state.current),0);
 fs.mkdirSync('ui-evidence',{recursive:true});await p.screenshot({path:'ui-evidence/wear-automatic-phone-162.png'});assert.deepEqual(errors,[]);
 console.log('PASS watch-owned phone controls: 8 layouts, authoritative record/rest, paused clock, phase-bound remote commands, no second timer, stale/invalid mirror and protocol support');
}finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
