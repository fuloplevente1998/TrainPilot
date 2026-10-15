'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs'),http=require('node:http'),path=require('node:path');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const route=new URL(req.url,'http://localhost').pathname;
 const file=path.resolve(root,'.'+(route==='/'?'/index.html':route));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
 fs.readFile(file,(error,data)=>{
  if(error){res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');
  res.end(data);
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
   Object.defineProperty(navigator,'onLine',{get:()=>!!window.__tp163Online,configurable:true}); // Offline unless deliberately enabled.
  });
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotGpsMap163);
  const fixture=await page.evaluate(()=>{
   const date='2026-10-14T16:20:00Z',key='gps-163:route-one';
   const gps={id:'running',...byId('running'),measurementType:'distance',sets:[{
    set:1,setId:'gps-set-1',reps:'700',weight:0,done:true,
    distanceMeters:1500,distanceSource:'gps',gpsRouteKey163:key
   }]};
   const lift={...byId('db-floor-press'),sets:[{set:1,reps:'12',weight:5,done:true}]};
   const item={id:'gps-163-workout',workout:'A',programId:'home-basic',started:date,
    finished:'2026-10-14T16:50:00Z',exercises:[gps]};
   const strength={...item,id:'gps-163-strength',started:'2026-10-13T16:20:00Z',
    finished:'2026-10-13T16:50:00Z',exercises:[lift]};
   db.set('history',[item,strength]);state.session=null;db.set('draft',null);
   window.__routeCalls=0;window.__routeDeletes=[];
   window.Capacitor={isNativePlatform:()=>true,Plugins:{DistanceTracker:{
    route:async ({key:asked})=>{
     window.__routeCalls++;
     if(asked!==key)throw Error('Wrong GPS owner');
     return {key:asked,pointCount:4,partial:false,segments:[
      {ordinal:1,points:[{latitude:47.02,longitude:18.99,accuracyMeters:5,elapsedMillis:1000,epochMillis:100000},
       {latitude:47.021,longitude:19.00,accuracyMeters:5,elapsedMillis:5000,epochMillis:104000}]},
      {ordinal:2,points:[{latitude:47.04,longitude:19.02,accuracyMeters:4,elapsedMillis:6000,epochMillis:105000},
       {latitude:47.041,longitude:19.03,accuracyMeters:6,elapsedMillis:10000,epochMillis:109000}]}
     ]};
    },
    deleteRoute:async ({key:asked})=>{window.__routeDeletes.push(asked);return {deleted:true};}
   }}};
   go('history');
   return {hasRoute:!!TrainPilotGpsMap163,refs:TrainPilotGpsMap163.references(item).length};
  });
  assert.deepEqual(fixture,{hasRoute:true,refs:1});
  const row=page.locator('details[data-tp152-history-key="gps-163-workout"]');
  await row.locator('summary').click();
  await page.waitForTimeout(150);
  const debug=await page.evaluate(()=>({
    tab:state.tab,entries:document.querySelectorAll('details[data-tp152-history-key]').length,
    hydrated:document.querySelectorAll('.tp3-combined-history-body[data-tp3-hydrated="true"]').length,
    routes:document.querySelectorAll('.tp163-route-button').length,
    refs:TrainPilotGpsMap163.references(history().find(x=>x.id==='gps-163-workout')).length
  }));
  assert.equal(debug.routes,1,'Lazy journal route button: '+JSON.stringify(debug));
  assert.equal(await page.evaluate(()=>window.__routeCalls),0,'Never retrieve coordinates when scrolling log');

  await page.locator('.tp163-route-button').click();
  await page.waitForSelector('.tp163-route-svg .tp163-route-line');
  assert.equal(await page.locator('.tp163-route-line').count(),2,'Pauses must not become a straight-line track');
  assert.equal(await page.locator('.tp163-route-start').count(),1);
  assert.equal(await page.locator('.tp163-route-finish').count(),1);
  assert.equal(await page.evaluate(()=>window.__routeCalls),1,'Fetch only on explicit tap');
  assert.equal(await page.locator('.tp163-route-dialog').isVisible(),true);
  assert.match(await page.locator('.tp163-route-metrics').innerText(),/1[,.]5 km/);
  assert.equal(await page.locator('.tp163-map-area .leaflet-tile').count(),0,'Offline means no tile requests');
  const fit=await page.evaluate(()=>{
   const outer=document.documentElement,card=document.querySelector('.tp163-route-dialog'),rect=card.getBoundingClientRect();
   return {fit:outer.scrollWidth<=innerWidth+1,card:rect.width<=innerWidth+1,accent:!!document.querySelector('.tp163-route-line')};
  });
  assert.deepEqual(fit,{fit:true,card:true,accent:true});
  await page.keyboard.press('Escape');
  assert.equal(await page.locator('#tp163-route-overlay').count(),0);

  await page.setViewportSize({width:320,height:740});
  await row.locator('.tp163-route-button').click();
  await page.waitForSelector('.tp163-route-svg');
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true);
  await page.locator('.tp163-close').click();
  assert.equal(await page.locator('#tp163-route-overlay').count(),0);
  // Online state alone must not initiate tile/CDN requests or expose the track.
  await page.evaluate(()=>window.__tp163Online=true);
  await row.locator('.tp163-route-button').click();
  await page.waitForSelector('.tp163-route-svg');
  assert.equal(await page.locator('.tp163-online-button').count(),0,
   'Android WebView must not auto-use the community tile server');
  assert.match(await page.locator('.tp163-map-consent').innerText(),/nincsenek beállítva|not configured/);
  await page.locator('.tp163-close').click();
  await page.evaluate(()=>{
   window.TrainPilotGps163TileProvider={
     enabled:true,url:'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
     attribution:'&copy; OpenStreetMap contributors',maxZoom:16
   };
  });
  assert.equal(await page.evaluate(()=>TrainPilotGpsMap163.tileProvider()?.maxZoom),16);
  await row.locator('.tp163-route-button').click();
  await page.waitForSelector('.tp163-online-button');
  assert.equal(await page.locator('.tp163-route-svg').count(),1,'Preview stays offline until a second tap');
  assert.match(await page.locator('.tp163-map-consent').innerText(),/külső szolgáltatónak|external provider/);
  await page.locator('.tp163-close').click();
  await page.evaluate(()=>{window.__tp163Online=false;delete window.TrainPilotGps163TileProvider;});
    // A cancelled history deletion must retain native GPS routes.
  await row.locator('.tp155-history-delete').click();
  await page.locator('#tp2628Dialog [data-tp2628-cancel]').click();
  assert.deepEqual(await page.evaluate(()=>window.__routeDeletes),[]);
  assert.equal(await row.count(),1);
  // Confirmed deletion must erase only the removed workout's route owners.
  await row.locator('.tp155-history-delete').click();
  await page.locator('#tp2628Dialog [data-tp2628-confirm]').click();
  await page.waitForFunction(()=>window.__routeDeletes.length===1);
  assert.deepEqual(await page.evaluate(()=>window.__routeDeletes),['gps-163:route-one']);
  assert.equal(await page.evaluate(()=>history().some(x=>x.id==='gps-163-workout')),false);
  const xss=await page.evaluate(()=>{
   const malicious={id:'wrong',exercises:[{sets:[{gpsRouteKey163:'../private'}]}]};
   const safe=TrainPilotGpsMap163.references(malicious).length===0;
   const a=TrainPilotGpsMap163.coordinates({segments:[{points:[
    {latitude:99,longitude:1},{latitude:47,longitude:19,accuracyMeters:3}
   ]}]});
   return {safe,count:a.count};
  });
  assert.deepEqual(xss,{safe:true,count:1});

  const persisted=await page.evaluate(()=>{
   const started=new Date().toISOString(),session={
    started,workout:'quick',type:'quick',quickWorkout:true,exercises:[
     {...byId('running'),sets:[{set:1,setId:'gps-163-owned-set',done:false,reps:'',weight:0}]}
    ]};
   state.session=session;state.current=0;
   const owner=session.exercises[0].sets[0],key=started+'|running|0|abc';
   db.set('gpsTrip106',{key,started,exerciseIndex:0,exerciseId:'running',setIndex:0,setId:owner.setId,
    baseMeters:0,baseSeconds:0});
   tp106ApplyGPS({active:true,key,distanceMeters:150,elapsedSeconds:40,routeSavedPoints:2});
   const draft=db.get('draft');
   const saved=owner.gpsRouteKey163===key&&draft?.session?.exercises?.[0]?.sets?.[0]?.gpsRouteKey163===key;
   return {saved,source:owner.distanceSource,distance:owner.distanceMeters};
  });
  assert.deepEqual(persisted,{saved:true,source:'gps',distance:150});
  assert.deepEqual(errors,[],'Browser console errors');
  console.log('PASS #163: per-set saved GPS key, on-demand journal button, 2 offline segments, no tiles, mobile fit, dismiss and malformed key');
  await page.close();
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(e=>{console.error(e);process.exitCode=1;});