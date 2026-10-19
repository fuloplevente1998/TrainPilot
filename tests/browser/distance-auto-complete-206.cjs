'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const target=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname==='/'?'/index.html':new URL(req.url,'http://local').pathname));
 if(!target.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(target,(err,body)=>{
  if(err){res.writeHead(404);return res.end();}
  res.setHeader('Content-Type',target.endsWith('.js')?'text/javascript':target.endsWith('.css')?'text/css':'text/html');res.end(body);
 });
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));
  await page.addInitScript(()=>{
   localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
   Object.defineProperty(navigator,'onLine',{get:()=>false,configurable:true});
  });
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.tp206CompleteStoppedGps);
  const init=await page.evaluate(()=>{
   window.__p206={active:false,key:'',stopCount:0,meters:630,seconds:40,wrongKey:false};
   window.Capacitor={isNativePlatform:()=>true,Plugins:{DistanceTracker:{
    status:async()=>{const m=window.__p206;return {active:m.active,key:m.key,distanceMeters:0,elapsedSeconds:0};},
    start:async({key})=>{const m=window.__p206;m.active=true;m.key=key;return {active:true,key,distanceMeters:0,elapsedSeconds:0,hasFix:false};},
    stop:async()=>{const m=window.__p206;m.active=false;m.stopCount++;return {active:false,key:m.wrongKey?'another-owner':m.key,distanceMeters:m.meters,elapsedSeconds:m.seconds,routeSavedPoints:m.meters>0?3:0};},
    route:async({key})=>({key,partial:false,pointCount:0,segments:[]})
   }}};
   const started=new Date().toISOString();
   state.session={started,type:'quick',workout:'quick',quickWorkout:true,exercises:[
    {...byId('running'),measurementType:'distance',sets:[{set:1,setId:'gps-206',done:false,reps:'0',weight:0}]}
   ]};
   state.current=0;db.set('gpsTrip106',null);tp106GpsState={active:false};persistDraft();renderWorkout();
   return {exercise:state.session.exercises[0].id,count:document.querySelectorAll('#tp107Indoor .tp206-actions button').length,
     stepsOutside:!!document.querySelector('#tp106Steps')&&!document.querySelector('#tp106GPS #tp106Steps')};
  });
  assert.deepEqual(init,{exercise:'running',count:2,stepsOutside:true});
  for(const width of [320,393]) {
   await page.setViewportSize({width,height:873});
   const layout=await page.evaluate(()=>{
    const gps=document.querySelector('#tp106GpsButton').getBoundingClientRect(),
      indoor=document.querySelector('[data-tp107-clock-toggle]').getBoundingClientRect(),
      panel=document.querySelector('#tp107Indoor').getBoundingClientRect(),
      interval=document.querySelector('#tp107Indoor .tp110-interval').getBoundingClientRect(),
      count=document.querySelector('[data-tp106-steps-count]').getBoundingClientRect(),
      refresh=document.querySelector('[data-tp106-steps-refresh]').getBoundingClientRect(),
      note=document.querySelector('.tp106-steps-note').getBoundingClientRect();
    return {sideBySide:gps.right<=indoor.left+2&&Math.abs(gps.top-indoor.top)<3,
     fit:document.documentElement.scrollWidth<=innerWidth+1,
     idleReadout:getComputedStyle(document.querySelector('.tp206-readout')).display==='none',
     noEmptyIntervalTail:panel.bottom-interval.bottom<=16,
     compactSteps:count.right<=refresh.left&&Math.abs((count.top+count.bottom-refresh.top-refresh.bottom)/2)<2,
     refreshBeforeNote:refresh.bottom<=note.top,
     refreshTouchTarget:refresh.height>=48};
   });
   assert.deepEqual(layout,{sideBySide:true,fit:true,idleReadout:true,noEmptyIntervalTail:true,
    compactSteps:true,refreshBeforeNote:true,refreshTouchTarget:true});
  }
  await page.setViewportSize({width:393,height:873});
  await page.locator('#tp106GpsButton').click();await page.waitForSelector('.tp163-live-dialog');
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),false,'START never completes');
  await page.locator('.tp163-close').click();
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),false,'Map close never completes');
  assert.equal(await page.evaluate(()=>window.__p206.stopCount),0,'Map close never stops GPS');

  await page.locator('#tp106GpsButton').click();await page.waitForFunction(()=>window.__p206.stopCount===1);
  const success=await page.evaluate(()=>({
    done:state.session.exercises[0].sets[0].done,
    saved:db.get('draft')?.session?.exercises?.[0]?.sets?.[0]?.done,
    distance:state.session.exercises[0].sets[0].distanceMeters,
    seconds:state.session.exercises[0].sets[0].reps,
    count:state.session.exercises[0].sets.length
  }));
  assert.deepEqual(success,{done:true,saved:true,distance:630,seconds:'40',count:1},
   'Explicit stop completes exactly one recorded GPS leg and persists it');
  await page.evaluate(async()=>{await tp106ToggleGPS();});
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets.length),1,'Duplicate stop must not append a set');

  // A failed/implicit stop may save elapsed data, but must never mark done.
  const negative=await page.evaluate(async()=>{
   const set=state.session.exercises[0].sets[0];set.done=false;
   window.__p206.meters=0;window.__p206.seconds=30;
   set.reps='0';set.distanceMeters=0;set.distanceSource='gps';persistDraft();
   let snapshot={active:false,key:'gps-206-owner',distanceMeters:0,elapsedSeconds:30};
   let invalid=tp206CompleteStoppedGps({key:'gps-206-owner',started:state.session.started,
       exerciseIndex:0,exerciseId:'running',setId:set.setId},
     {session:state.session,e:state.session.exercises[0],set},snapshot);
   const noMotion=invalid===false&&!set.done;
   window.__p206.meters=400;window.__p206.seconds=60;
   snapshot={...snapshot,distanceMeters:400,elapsedSeconds:60,key:'incorrect'};
   const wrongOwner=!tp206CompleteStoppedGps({key:'gps-206-owner',started:state.session.started,
       exerciseIndex:0,exerciseId:'running',setId:set.setId},
     {session:state.session,e:state.session.exercises[0],set},snapshot)&&!set.done;
   await tp106StopGPS();
   return {noMotion,wrongOwner,implicitStopDoesNotComplete:!set.done};
  });
  assert.deepEqual(negative,{noMotion:true,wrongOwner:true,implicitStopDoesNotComplete:true});

  // Indoor timer stays independent of GPS, and shows compact controls only when active.
  await page.evaluate(()=>{window.__p206.active=false;tp106GpsState={active:false};renderWorkout();});
  await page.locator('[data-tp107-clock-toggle]').click();
  assert.equal(await page.locator('.tp206-timer-visible').count(),1);
  assert.equal(await page.locator('#tp106GpsButton').count(),1);
  await page.locator('[data-tp107-clock-toggle]').click();
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),false,
    'Indoor pause cannot auto-complete a GPS segment');

  // Strength workouts never receive a distance activity card or change manual completion.
  const strength=await page.evaluate(()=>{
    state.session.exercises=[{...byId('db-floor-press'),sets:[{set:1,setId:'lift-206',reps:'12',weight:10,done:false}]}];
    state.current=0;renderWorkout();
    return {motion:!!document.querySelector('.tp206-activity'),gps:!!document.getElementById('tp106GPS')};
  });
  assert.deepEqual(strength,{motion:false,gps:false});
  const finishedGps=await page.evaluate(async()=>{
   const session=state.session;session.exercises=[{...byId('running'),measurementType:'distance',sets:[{set:1,setId:'gps-finish-206',done:false,reps:'0',weight:0}]}];
   session.automatic162={enabled:false};state.current=0;tp106GpsState={active:false};db.set('gpsTrip106',null);
   __p206.wrongKey=false;__p206.meters=630;__p206.seconds=40;renderWorkout();
   await tp106StartGPS();await tp106ToggleGPS();
   const accepted=session.exercises[0].sets[0].done;
   await finishWorkout();
   const saved=history().find(h=>h.started===session.started);
   return {accepted,saved:!!saved,done:saved?.exercises[0].sets[0].done,meters:saved?.exercises[0].sets[0].distanceMeters};
  });
  assert.deepEqual(finishedGps,{accepted:true,saved:true,done:true,meters:630},'A successful GPS Finish is accepted and saved without a separate set OK');
  assert.deepEqual(errors,[]);
  console.log('PASS #206: 2-column compact distance controls, owned GPS stop auto-done, negative stop, indoor and strength isolation');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(e=>{console.error(e);process.exitCode=1;});
