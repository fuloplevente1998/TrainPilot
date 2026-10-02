'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const route=new URL(req.url,'http://local').pathname,file=path.resolve(root,'.'+(route==='/'?'/index.html':route));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);res.end();return;}
 res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});
});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  page.on('dialog',dialog=>dialog.accept());
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  const imports=await page.evaluate(async()=>{
   const original=makeBackup(),cases=[];
   const check=async(label,mutate)=>{
    const data=JSON.parse(JSON.stringify(original));mutate(data);
    const before=JSON.stringify(Object.entries(localStorage).filter(([k])=>k.startsWith('repforge:')).sort());
    let rejected=false;try{await restoreText(JSON.stringify(data));}catch(_){rejected=true;}
    const after=JSON.stringify(Object.entries(localStorage).filter(([k])=>k.startsWith('repforge:')).sort());
    cases.push({label,rejected,unchanged:before===after});
   };
   tp2628Confirm=async()=>true;
   await check('program code',d=>{d.programs.push({...d.programs[0],builtin:false,id:"custom-review');window.__backupProbe=1;//",name:'Review'});});
   await check('day code',d=>{d.programs[0].days[0].id="A');window.__backupProbe=1;//";});
   await check('exercise code',d=>{d.exercises[0].id="x');window.__backupProbe=1;//";});
   await check('history code',d=>{d.history=[{id:"x');window.__backupProbe=1;//",workout:'A',started:'2026-10-01T10:00:00Z',finished:'2026-10-01T11:00:00Z',exercises:[]}];});
   await check('null workout',d=>{d.history=[null];});
   await check('null exercise',d=>{d.exercises=[null];});
   go('programs');return {cases,executed:window.__backupProbe===1};
  });
  for(const c of imports.cases){assert.equal(c.rejected,true,c.label+' must be rejected');assert.equal(c.unchanged,true,c.label+' must not change local data');}
  assert.equal(imports.executed,false);

  for(const width of [320,360,393,412])for(const theme of ['classicBlue','green']){
   await page.setViewportSize({width,height:873});
   await page.evaluate(t=>{
    state.session=null;db.set('draft',null);rf200SetTheme(t);go('plan');tp2628Confirm=async()=>true;
    window.__gps={active:false,distanceMeters:0,elapsedSeconds:0};window.__gpsStops=0;
    window.Capacitor={isNativePlatform:()=>true,Plugins:{DistanceTracker:{
     status:async()=>({...window.__gps}),
     start:async args=>{window.__gps={active:true,key:args.key,distanceMeters:0,elapsedSeconds:0,hasFix:true};return {...window.__gps};},
     stop:async()=>{window.__gpsStops++;window.__gps.active=false;return {...window.__gps};}
    }}};
    state.current=0;state.tab='plan';state.session={started:new Date().toISOString(),workout:'quick',type:'quick',quickWorkout:true,exercises:[{...byId('running'),sets:[{set:1,weight:0,reps:'',done:false},{set:2,weight:0,reps:'',done:false}]}]};renderWorkout();
   },theme);
   await page.locator('#tp106GpsButton').click();await page.waitForFunction(()=>tp106GpsState.active&&!tp106GpsBusy);
   await page.evaluate(async()=>{window.__gps.distanceMeters=100;window.__gps.elapsedSeconds=30;await tp106SyncGPS();});
   await page.locator('.tp-active-set-remove').first().click();
   await page.waitForFunction(()=>state.session.exercises[0].sets.length===1&&!tp106GpsBusy);
   const removed=await page.evaluate(async()=>{
    window.__gps.distanceMeters=200;window.__gps.elapsedSeconds=60;await tp106SyncGPS();
    return {active:tp106GpsState.active,stops:window.__gpsStops,set:state.session.exercises[0].sets[0],overflow:document.documentElement.scrollWidth>innerWidth+1};
   });
   assert.equal(removed.active,false,width+'/'+theme+' GPS owner deletion must stop tracking');
   assert.equal(removed.stops,1);assert.equal(removed.set.distanceMeters,undefined,'GPS totals must not migrate into the remaining set');
   assert.equal(removed.set.reps,'');assert.equal(removed.overflow,false);

   // Deleting an earlier completed set preserves the identity and totals of the measured set.
   const shifted=await page.evaluate(async()=>{
    state.session.exercises[0].sets=[{set:1,weight:0,reps:'5',done:true},{set:2,weight:0,reps:'',done:false}];
    window.__gps={active:false,distanceMeters:0,elapsedSeconds:0};renderWorkout();await tp106StartGPS();
    const id=state.session.exercises[0].sets[1].setId;
    await window.tpActiveWorkoutRemoveSet(0);window.__gps.distanceMeters=321;window.__gps.elapsedSeconds=42;await tp106SyncGPS();
    const set=state.session.exercises[0].sets[0],result={id,set,active:tp106GpsState.active};await tp106StopGPS();return result;
   });
   assert.equal(shifted.active,true);assert.equal(shifted.set.setId,shifted.id);assert.equal(shifted.set.distanceMeters,321);assert.equal(shifted.set.reps,'42');
   await page.evaluate(()=>{state.session=null;db.set('draft',null);go('programs');rf200SetTheme('yellow');go('plan');});
  }
  // Permission requests cannot outlive deletion/replacement of their target set.
  const pending=await page.evaluate(async()=>{
   state.session={started:new Date().toISOString(),workout:'quick',exercises:[{...byId('walking'),sets:[{set:1,weight:0,reps:'',done:false},{set:2,weight:0,reps:'',done:false}]}]};state.current=0;
   let resolve;window.Capacitor.Plugins.DistanceTracker.start=args=>new Promise(r=>{resolve=()=>r({key:args.key,active:true,distanceMeters:0,elapsedSeconds:0});});
   const start=tp106StartGPS();while(!resolve)await new Promise(r=>setTimeout(r,0));
   await window.tpActiveWorkoutRemoveSet(0);const count=state.session.exercises[0].sets.length;
   resolve();await start;await tp106StopGPS();return count;
  });
  assert.equal(pending,2,'deletion must wait for the GPS permission/start operation');
  const failures=await page.evaluate(async()=>{
   const session=()=>({started:new Date().toISOString(),workout:'quick',exercises:[{...byId('running'),sets:[{set:1,weight:0,reps:'10',done:false},{set:2,weight:0,reps:'',done:false}]}]});
   state.session=session();state.current=0;const owner=state.session.exercises[0].sets[0];
   // An upgrade can resume a native trip that was started before setId existed.
   const trip={key:'legacy-trip',started:state.session.started,exerciseIndex:0,exerciseId:'running',setIndex:0,baseMeters:0,baseSeconds:0};
   db.set('gpsTrip106',trip);tp106ApplyGPS({active:true,key:trip.key,distanceMeters:17,elapsedSeconds:10});
   const upgraded=!!owner.setId&&db.get('gpsTrip106').setId===owner.setId;
   const bridge=window.Capacitor.Plugins.DistanceTracker,stop=bridge.stop;
   bridge.stop=async()=>{throw Error('native stop failed');};
   await window.tpActiveWorkoutRemoveSet(0);
   const retained=state.session.exercises[0].sets.includes(owner)&&tp106GpsState.active;
   bridge.stop=async()=>({active:false,key:trip.key,distanceMeters:17,elapsedSeconds:10});await tp106StopGPS();bridge.stop=stop;
   // A confirmation belonging to a previous workout cannot delete a new one.
   let resolve;tp2628Confirm=()=>new Promise(r=>{resolve=r;});
   const deletion=window.tpActiveWorkoutRemoveSet(0);while(!resolve)await new Promise(r=>setTimeout(r,0));
   const replacement=session();state.session=replacement;resolve(true);await deletion;
   tp2628Confirm=async()=>true;
   return {upgraded,retained,replacementCount:replacement.exercises[0].sets.length};
  });
  assert.equal(failures.upgraded,true,'legacy trips must acquire stable set ownership');
  assert.equal(failures.retained,true,'failed native stop must preserve the measured set');
  assert.equal(failures.replacementCount,2,'stale confirmation must not delete from a replacement session');
  await page.close();
  console.log('PASS backup/GPS: unsafe imports write nothing; stable/legacy ownership; safe deletion; native failure and permission/confirmation races at four widths/two themes');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
