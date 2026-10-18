'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{const url=new URL(req.url,'http://local'),file=path.resolve(root,'.'+(url.pathname==='/'?'/index.html':url.pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const p=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));p.on('dialog',d=>d.accept());
 await p.addInitScript(()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));Object.defineProperty(navigator,'onLine',{get:()=>false,configurable:true});});
 await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished&&typeof tp3EnsureHistoryBody==='function');
 await p.evaluate(()=>{
  const id='2026-10-18T10:00:00.000Z',watch='wear193:'+'a'.repeat(64);
  window.__route193={id,watch,bindings:[],reads:[],erased:[],workouts:[]};
  window.Capacitor={isNativePlatform:()=>true,Plugins:{DistanceTracker:{
   routeBindings193:async({workoutId})=>({workoutId,bindings:JSON.parse(JSON.stringify(__route193.bindings))}),
   deleteWorkoutRoutes193:async({workoutId})=>{__route193.workouts.push(workoutId);return {deleted:true};},
   deleteRoute:async({key})=>{__route193.erased.push(key);return {deleted:true};},
   route:async({key})=>{__route193.reads.push(key);return {key,source:'watch_gps',partial:true,pointCount:4,segments:[
    {ordinal:1,points:[{latitude:47,longitude:19,accuracyMeters:5,elapsedMillis:1000,epochMillis:1800000001000},{latitude:47.0001,longitude:19,accuracyMeters:5,elapsedMillis:5000,epochMillis:1800000005000}]},
    {ordinal:2,points:[{latitude:47.001,longitude:19,accuracyMeters:5,elapsedMillis:15000,epochMillis:1800000015000},{latitude:47.0011,longitude:19,accuracyMeters:5,elapsedMillis:19000,epochMillis:1800000019000}]}]};}
  }}};
  db.set('history',[{syncId:id,started:id,finished:'2026-10-18T10:05:00.000Z',workout:'A',programId:'home-basic',ownershipProtocol193:1,timerOwner:'watch',exercises:[{...byId('running'),measurementType:'distance',sets:[{set:1,setId:'route-set193',reps:'300',distanceMeters:600,done:true}]}]}]);
  state.session=null;db.set('draft',null);state.tab='history';render();
 });
 const card=p.locator('details[data-tp152-history-key]').first();await card.locator('summary').first().click();
 assert.equal(await p.locator('.tp163-route-button').count(),0,'No route is invented from a distance measurement');
 assert.equal(await p.evaluate(()=>__route193.reads.length),0);
 await p.evaluate(async()=>{
  __route193.bindings=[{workoutId:__route193.id,exerciseId:'running',setId:'route-set193',routeKey:__route193.watch,pointCount:4}];
  await TrainPilotWearRoutes193.sync(__route193.id);const el=document.querySelector('details[data-tp152-history-key]');tp3EnsureHistoryBody(el,0,el.dataset.tp152HistoryKey);
 });
 await p.locator('.tp163-route-button').waitFor({state:'visible'});assert.equal(await p.evaluate(()=>__route193.reads.length),0,'Journal hydration retrieves references, never coordinates');
 assert.equal(await p.evaluate(()=>JSON.stringify(db.get('history')).includes('latitude')),false);
 await p.locator('.tp163-route-button').click();await p.locator('.tp163-route-line').first().waitFor();
 assert.equal(await p.locator('.tp163-route-line').count(),2,'Interrupted sections must never join');
 assert.deepEqual(await p.evaluate(()=>__route193.reads),['wear193:'+'a'.repeat(64)]);
 assert.match(await p.locator('.tp193-route-source').innerText(),/Órás GPS/);assert.match(await p.locator('.tp163-route-metrics').innerText(),/0[,.]6 km/);
 for(const width of [320,393]){await p.setViewportSize({width,height:873});assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth-innerWidth)<=1);}
 fs.mkdirSync('ui-evidence',{recursive:true});await p.screenshot({path:'ui-evidence/wear-gps-route-193.png'});await p.keyboard.press('Escape');
 await p.evaluate(async()=>{const row=db.get('history')[0];row.exercises[0].sets[0].gpsRouteKey163='phone-route193';await TrainPilotGpsMap163.eraseRoutesForWorkout(row);});
 assert.deepEqual(await p.evaluate(()=>__route193.workouts),['2026-10-18T10:00:00.000Z']);
 assert.deepEqual((await p.evaluate(()=>__route193.erased)).sort(),['phone-route193','wear193:'+'a'.repeat(64)].sort());
 assert.deepEqual(errors,[]);console.log('PASS #193 watch route journal: late stable binding, no eager coordinates, source/partial metadata, gap-safe offline map, compact viewport and whole-workout tombstone');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
