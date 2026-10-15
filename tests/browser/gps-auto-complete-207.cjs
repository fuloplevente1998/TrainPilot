'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),http=require('node:http'),path=require('node:path');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const pathname=new URL(req.url,'http://localhost').pathname;
 const target=path.resolve(root,'.'+(pathname==='/'?'/index.html':pathname));
 if(!target.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
 fs.readFile(target,(error,data)=>{
  if(error){res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',target.endsWith('.js')?'text/javascript':target.endsWith('.css')?'text/css':'text/html');
  res.end(data);
 });
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',err=>errors.push(err.message));
  await page.addInitScript(()=>{
   localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
   Object.defineProperty(navigator,'onLine',{get:()=>false,configurable:true});
  });
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotGpsMap163?.openLiveRoute);
  const fixture=await page.evaluate(()=>{
   window.__gps207={active:false,key:'',meters:0,seconds:0,stops:0,fail:false};
   window.alert=()=>{};
   window.Capacitor={isNativePlatform:()=>true,Plugins:{DistanceTracker:{
    status:async()=>({active:__gps207.active,key:__gps207.key,
      distanceMeters:__gps207.meters,elapsedSeconds:__gps207.seconds,hasFix:__gps207.meters>0}),
    start:async({key})=>{
      Object.assign(__gps207,{active:true,key,meters:0,seconds:0});
      return {active:true,key,distanceMeters:0,elapsedSeconds:0};
    },
    stop:async()=>{
      __gps207.stops++;
      if(__gps207.fail)throw Error('native GPS stop failed');
      __gps207.active=false;
      return {active:false,key:__gps207.key,distanceMeters:__gps207.meters,elapsedSeconds:__gps207.seconds,
        routeSavedPoints:2};
    },
    route:async({key})=>({key,pointCount:0,partial:false,segments:[]})
   }}};
   window.__setup207=(started,source)=>{
     tp106GpsState={active:false};
     localStorage.removeItem('repforge:gpsTrip106');
     state.session={started,workout:'quick',type:'quick',quickWorkout:true,exercises:[
      {...byId('running'),measurementType:'distance',
       sets:[{set:1,setId:'set-'+started,done:false,reps:'0',weight:0,
        ...(source?{distanceMeters:100,distanceSource:source}:{})}]}
     ]};
     state.current=0;state.tab='plan';persistDraft();renderWorkout();
   };
   __setup207('2026-10-15T09:00:00Z');
   return {title:document.querySelector('.tp153-sets-title')?.textContent,
     ready:!!document.getElementById('tp106GpsButton')};
  });
  assert.deepEqual(fixture,{title:'Szakaszok',ready:true});
  assert.match(await page.locator('#tp106GPS').innerText(),/automatikusan teljesített/);
  await page.locator('#tp106GpsButton').click();
  await page.waitForSelector('.tp163-live-dialog');
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),false);
  await page.locator('.tp163-live-dialog .tp163-close').click();
  await page.evaluate(()=>Object.assign(__gps207,{meters:1250,seconds:85}));
  await page.locator('#tp106GpsButton').click();
  await page.waitForFunction(()=>state.session.exercises[0].sets[0].done);
  const completed=await page.evaluate(()=>{
   const row=state.session.exercises[0].sets[0];
   const draft=db.get('draft')?.session?.exercises?.[0]?.sets?.[0];
   return {done:row.done,source:row.distanceSource,meters:row.distanceMeters,
    seconds:row.reps,doneInDraft:draft?.done,workoutStillActive:!!state.session};
  });
  assert.deepEqual(completed,{done:true,source:'gps',meters:1250,seconds:'85',
    doneInDraft:true,workoutStillActive:true});
  assert.match(await page.locator('.tp153-set-row .check').innerText(),/✓/);
  assert.equal(await page.locator('#tp163-route-overlay').count(),0);

  // Internal GPS STOP due to navigation/error is not consent to complete the set.
  await page.evaluate(()=>__setup207('2026-10-15T10:00:00Z'));
  await page.locator('#tp106GpsButton').click();
  await page.waitForSelector('.tp163-live-dialog');
  await page.locator('.tp163-live-dialog .tp163-close').click();
  const internal=await page.evaluate(async()=>{
   Object.assign(__gps207,{meters:410,seconds:39});
   const stopped=await tp106StopGPS();
   return {stopped,done:state.session.exercises[0].sets[0].done};
  });
  assert.deepEqual(internal,{stopped:true,done:false});

  // A GPS fix without distance cannot invent a completed physical exercise.
  await page.evaluate(()=>__setup207('2026-10-15T11:00:00Z'));
  await page.locator('#tp106GpsButton').click();
  await page.waitForSelector('.tp163-live-dialog');
  await page.locator('.tp163-live-dialog .tp163-close').click();
  await page.evaluate(()=>Object.assign(__gps207,{meters:0,seconds:45}));
  await page.locator('#tp106GpsButton').click();
  await page.waitForFunction(()=>!tp106GpsState.active&&!tp106GpsBusy);
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),false);

  // A rejected native STOP keeps the set pending and the recording active.
  await page.evaluate(()=>__setup207('2026-10-15T12:00:00Z'));
  await page.locator('#tp106GpsButton').click();
  await page.waitForSelector('.tp163-live-dialog');
  await page.locator('.tp163-live-dialog .tp163-close').click();
  await page.evaluate(()=>Object.assign(__gps207,{meters:500,seconds:48,fail:true}));
  await page.locator('#tp106GpsButton').click();
  await page.waitForFunction(()=>!tp106GpsBusy);
  assert.deepEqual(await page.evaluate(()=>({
   done:state.session.exercises[0].sets[0].done,active:tp106GpsState.active
  })),{done:false,active:true});
  await page.evaluate(()=>{__gps207.fail=false;});
  await page.locator('#tp106GpsButton').click();
  await page.waitForFunction(()=>state.session.exercises[0].sets[0].done);

  // Unrelated Wear/manual distance does not complete from passive sync.
  await page.evaluate(()=>__setup207('2026-10-15T13:00:00Z','manual'));
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),false);
  assert.equal(await page.locator('.tp153-set-row .check').innerText(),'OK');
  assert.deepEqual(errors,[]);
  console.log('PASS #207: explicit GPS stop auto-completes owned valid section, draft saved; failed/internal/zero GPS stop stays pending; manual untouched');
 }finally{
  await browser?.close();
  await new Promise(resolve=>server.close(resolve));
 }
})().catch(error=>{console.error(error);process.exitCode=1;});