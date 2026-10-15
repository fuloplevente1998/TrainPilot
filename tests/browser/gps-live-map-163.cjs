'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const p=new URL(req.url,'http://localhost').pathname;
 const file=path.resolve(root,'.'+(p==='/'?'/index.html':p));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
 fs.readFile(file,(error,buffer)=>{
  if(error){res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');
  res.end(buffer);
 });
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
 let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',error=>errors.push(String(error)));
  await page.addInitScript(()=>{
   localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
   Object.defineProperty(navigator,'onLine',{get:()=>false,configurable:true});
  });
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotGpsMap163?.openLiveRoute);
  const setup=await page.evaluate(()=>{
   const started='2026-10-15T09:00:00Z';
   state.session={started,workout:'quick',type:'quick',quickWorkout:true,exercises:[
    {...byId('running'),measurementType:'distance',sets:[{set:1,setId:'163-live-1',done:false,reps:'0',weight:0}]}
   ]};state.current=0;
   db.set('draft',{session:state.session,savedAt:started});
   db.set('gpsTrip106',null);
   tp106GpsState={active:false};
   window.__gpsMock={
    active:false,key:'',stops:0,starts:0,statusCalls:0,routeCalls:0,points:[],trip:null
   };
   window.Capacitor={isNativePlatform:()=>true,Plugins:{DistanceTracker:{
    status:async()=>{
     window.__gpsMock.statusCalls++;
     const m=window.__gpsMock;
     return {active:m.active,paused:false,key:m.key,distanceMeters:110,
      elapsedSeconds:55,routeSavedPoints:m.points.length,hasFix:m.points.length>0,accuracyMeters:5};
    },
    start:async ({key})=>{
     const m=window.__gpsMock;m.starts++;m.active=true;m.key=key;
     return {active:true,key,distanceMeters:0,elapsedSeconds:0,routeSavedPoints:0,hasFix:false};
    },
    stop:async()=>{
     const m=window.__gpsMock;m.stops++;m.active=false;
     return {active:false,key:m.key,distanceMeters:110,elapsedSeconds:55,routeSavedPoints:m.points.length};
    },
    route:async ({key})=>{
     const m=window.__gpsMock;m.routeCalls++;
     if(key!==m.key)throw Error('Incorrect workout GPS owner');
     return {key,partial:false,pointCount:m.points.length,
       segments:m.points.length?[{ordinal:1,points:m.points}]:[]};
    }
   }}};
   renderWorkout();
   return {native:isNative(),exercise:state.session.exercises[0].id,gpsButton:!!document.getElementById('tp106GpsButton')};
  });
  assert.deepEqual(setup,{native:true,exercise:'running',gpsButton:true});
  assert.equal(await page.locator('#tp163-route-overlay').count(),0,'Do not auto-open before successful start');
  await page.locator('#tp106GpsButton').click();
  await page.waitForSelector('.tp163-live-dialog');
  await page.waitForFunction(()=>document.querySelector('.tp163-live-wait'));
  assert.match(await page.locator('.tp163-route-hint').innerText(),/GPS-jelet keresünk/);
  assert.equal(await page.locator('#tp163LiveMapButton').count(),1,'Can reopen the map during GPS tracking');
  assert.deepEqual(await page.evaluate(()=>({
   starts:window.__gpsMock.starts,active:window.__gpsMock.active,stops:window.__gpsMock.stops
  })),{starts:1,active:true,stops:0});
  assert.equal(await page.locator('.leaflet-tile').count(),0,'No online map without consent');

  await page.evaluate(()=>{
   const m=window.__gpsMock,base=1700000000000;
   m.points=[{latitude:47.02,longitude:18.99,accuracyMeters:5,elapsedMillis:1000,epochMillis:base},
    {latitude:47.0202,longitude:18.9903,accuracyMeters:4,elapsedMillis:6000,epochMillis:base+5000}];
  });
  await page.evaluate(async()=>{await TrainPilotGpsMap163.updateLiveRoute();});
  await page.waitForSelector('.tp163-live-dialog .tp163-route-line');
  assert.equal(await page.locator('.tp163-route-line').count(),1,'Exactly one measured segment, no invented points');
  assert.match(await page.locator('.tp163-route-metrics').innerText(),/00:00:55/);
  assert.match(await page.locator('.tp163-route-metrics').innerText(),/7[,.]2 km\/h/);
  assert.equal(await page.locator('.tp163-route-start').count(),1);
  assert.equal(await page.locator('.tp163-route-finish').count(),1);

  // The X is UI-only; GPS keeps running in the foreground service.
  await page.locator('.tp163-close').click();
  assert.equal(await page.locator('#tp163-route-overlay').count(),0);
  assert.deepEqual(await page.evaluate(()=>({active:window.__gpsMock.active,stops:window.__gpsMock.stops})),
   {active:true,stops:0});
  await page.locator('#tp163LiveMapButton').click();
  await page.waitForSelector('.tp163-live-dialog');
  await page.setViewportSize({width:320,height:740});
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,
   'Live map must fit 320px phones');
  await page.locator('.tp163-close').click();

  // Wear-only totals cannot invent a phone route or open the local phone GPS modal.
  await page.evaluate(()=>{
   state.session.exercises[0].sets[0].distanceSource='wear';
   state.session.exercises[0].sets[0].distanceMeters=150;
  });
  assert.equal(await page.locator('#tp163-route-overlay').count(),0);
  assert.equal(await page.evaluate(()=>window.__gpsMock.stops),0);

  await page.locator('#tp106GpsButton').click();
  await page.waitForFunction(()=>window.__gpsMock.stops===1);
  assert.equal(await page.locator('#tp163-route-overlay').count(),0);
  assert.equal(await page.locator('#tp163LiveMapButton').count(),0);
  assert.deepEqual(errors,[]);
  console.log('PASS #163: confirmed GPS start opens offline live map; new fixes update; close/reopen never stop; mobile and Wear source isolation');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
