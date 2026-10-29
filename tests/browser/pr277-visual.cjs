'use strict';
// Capture the exact clean main and the working UI with identical synthetic data.
// Fixtures are labelled test evidence; they are never inserted into product data.
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),os=require('node:os');
const {execFileSync}=require('node:child_process'),assert=require('node:assert/strict'),{chromium}=require('playwright');
const base='8a36502da5832a42f1cb91708ecfed5cf4f9ed62',tmp=fs.mkdtempSync(path.join(os.tmpdir(),'tp277-'));
execFileSync('tar',['-xf','-','-C',tmp],{input:execFileSync('git',['archive',base,'www'],{maxBuffer:20*1024*1024})});
const roots={before:path.join(tmp,'www'),after:path.resolve('www')};
const out=path.resolve('ui-evidence');fs.mkdirSync(out,{recursive:true});
const report={baseline:base,source:process.env.GITHUB_SHA||execFileSync('git',['rev-parse','HEAD']).toString().trim(),fixtures:'synthetic CI only',screens:[],errors:[],checks:[]};
const server=http.createServer((req,res)=>{
 const u=new URL(req.url,'http://local'),m=u.pathname.match(/^\/(before|after)(\/.*)?$/);if(!m){res.writeHead(404);return res.end();}
 const root=roots[m[1]],file=path.resolve(root,'.'+(!m[2]||m[2]==='/'?'/index.html':m[2]));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(err,data)=>{if(err){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.woff2')?'font/woff2':'text/html');res.end(data);});
});
async function seed(page){await page.evaluate(()=>{
 const row=(i)=>({id:'277-log-'+i,programId:'home-basic',dayId:'A',workout:'A',programName:'TrainPilot CI',started:new Date(Date.UTC(2026,9,28-i,10)).toISOString(),finished:new Date(Date.UTC(2026,9,28-i,10,45)).toISOString(),exercises:[{...byId('goblet-squat'),sets:[{set:1,weight:30-i,reps:'10',done:true,actualRir:2}]}],photos:[]});
 db.set('history',Array.from({length:40},(_,i)=>row(i)));db.set('phase3HistoryTombstones',[]);db.set('draft',null);state.session=null;state.current=0;
});}
async function outdoor(page){await page.evaluate(()=>{
 const started=new Date().toISOString();state.session={started,workout:'quick',type:'quick',quickWorkout:true,exercises:[{...byId('running'),measurementType:'distance',sets:[{set:1,setId:'277-route-set',reps:'0',weight:0,done:false}]}]};state.current=0;
 db.set('draft',{session:state.session,savedAt:started});db.set('gpsTrip106',null);tp106GpsState={active:false};
 window.__277gps={active:false,key:'',starts:0,stops:0};
 const points=[{latitude:47.02,longitude:18.99,accuracyMeters:5,elapsedMillis:1000,epochMillis:1700000000000},{latitude:47.0202,longitude:18.9903,accuracyMeters:4,elapsedMillis:6000,epochMillis:1700000005000},{latitude:47.0204,longitude:18.9901,accuracyMeters:5,elapsedMillis:10000,epochMillis:1700000009000}];
 const status=()=>({active:window.__277gps.active,key:window.__277gps.key,distanceMeters:110,elapsedSeconds:55,routeSavedPoints:points.length,hasFix:true,accuracyMeters:5});
 window.Capacitor={isNativePlatform:()=>true,Plugins:{DistanceTracker:{status:async()=>status(),start:async({key})=>{Object.assign(window.__277gps,{active:true,key});window.__277gps.starts++;return status();},stop:async()=>{window.__277gps.active=false;window.__277gps.stops++;return status();},route:async({key})=>({key,partial:false,pointCount:points.length,segments:[{ordinal:1,points}]})}}};renderWorkout();
 });await page.locator('#tp106GpsButton').click();await page.waitForSelector('.tp163-live-dialog');await page.evaluate(()=>TrainPilotGpsMap163.updateLiveRoute());}
(async()=>{let browser;await new Promise(r=>server.listen(0,'127.0.0.1',r));
 try{
 browser=await chromium.launch({args:['--no-sandbox']});
 const configs=[...[320,360,393].flatMap(width=>[{width,theme:'classicBlue',lang:'hu',font:1},{width,theme:'yellow',lang:'hu',font:1}]),{width:320,theme:'classicPurple',lang:'de',font:1.3},{width:393,theme:'green',lang:'ro',font:1.3}];
 for(const version of ['before','after'])for(const config of configs){
 const page=await browser.newPage({viewport:{width:config.width,height:873},isMobile:true,hasTouch:true,locale:'hu-HU',colorScheme:config.theme==='classicBlue'?'light':'dark',reducedMotion:'reduce'});
 const errors=[];page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.accept());
 await page.addInitScript(()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));Object.defineProperty(navigator,'onLine',{get:()=>false});});
 await page.goto('http://127.0.0.1:'+server.address().port+'/'+version+'/');await page.waitForFunction(()=>TrainPilotBoot.finished);
 await seed(page);await page.evaluate(c=>{rf212SetLang(c.lang);rf200SetTheme(c.theme);if(c.font>1)document.documentElement.style.setProperty('--tp-font-scale',String(c.font));},config);
 if(config.font>1)await page.addStyleTag({content:`#app main,#tp155R4PanelHost main{font-size:${16*config.font}px} #app main .small{font-size:${12*config.font}px} #app input{font-size:${16*config.font}px}`});
 const prefix='277-'+version+'-'+config.width+'-'+config.theme+'-'+config.lang;
 async function snap(name){await page.waitForTimeout(100);const file=prefix+'-'+name+'.png';await page.screenshot({path:path.join(out,file)});report.screens.push({version,...config,name,file});const overflow=await page.evaluate(()=>document.documentElement.scrollWidth-innerWidth);if(overflow>1)report.errors.push({version,config,name,overflow});}
 await page.evaluate(()=>go('programs'));await snap('08-programs');
 const visiblePrograms=await page.locator('.tp152-program-card').evaluateAll(nodes=>nodes.filter(el=>{const r=el.getBoundingClientRect();return r.top<innerHeight&&r.bottom>0}).length);
 report.checks.push({version,config,visiblePrograms});
 if(version==='after'){
 const target=await page.locator('.rf103-program-activate:not(:disabled)').first().boundingBox();assert.ok(target.height>=44,'Program activation keeps a 44px target');
 }
await page.locator('.tp152-program-card').first().evaluate(x=>x.open=true);await snap('08-programs-open');
 await page.evaluate(()=>go('plan'));await snap('02-workout-start');await page.locator('.tp152-day').first().evaluate(x=>x.open=true);await snap('02-workout-start-open');
 if(version==='after'){assert.equal(await page.locator('.tp152-day').first().locator('.tp277-day-preview').isVisible(),false,'Open day hides the duplicate exercise preview');await page.locator('.tp152-day').first().locator(':scope > summary').click();assert.equal(await page.locator('.tp152-day').first().locator('.tp277-day-preview').isVisible(),true,'Closed day restores preview');}
 await page.evaluate(()=>go('settings'));await snap('01-settings');
 if(version==='after'){assert.equal(await page.locator('[data-settings-group]').count(),6);await page.locator('[data-settings-group="appearance"]').evaluate(x=>x.open=true);}await snap('01-settings-open');
 await page.evaluate(()=>{tp155R4ClosePanel(false);go('history');});await snap('07-journal');
 await page.locator('details.history').first().locator(':scope>summary').click();await snap('07-journal-open');
 if(version==='after'){
 if(config.width===393&&config.theme==='yellow'){
  const cost=await page.evaluate(()=>{
   const saved=history(),rows=Array.from({length:1500},(_,i)=>({...saved[i%saved.length],id:'277-volume-'+i,started:new Date(Date.UTC(2026,9,28)-i*86400000).toISOString(),finished:new Date(Date.UTC(2026,9,28,1)-i*86400000).toISOString()}));
   db.set('history',rows);state.tp277JournalKind='pr';let reads=0;const original=Storage.prototype.getItem;
   Storage.prototype.getItem=function(key){if(key==='repforge:history')reads++;return original.call(this,key);};
   let count;try{count=rf263FilteredHistory().length;}finally{Storage.prototype.getItem=original;db.set('history',saved);state.tp277JournalKind='all';}
   return {reads,count};
  });assert.ok(cost.reads<=4,'PR filtering reuses one canonical index for all 1500 rows');report.checks.push({pr1500:cost});
 }
 const mark=page.locator('.tp226-pr-badge').first(),arrow=mark.locator('..').locator('.rf103-history-chevron');
 const mr=await mark.boundingBox(),ar=await arrow.boundingBox();assert.ok(mr&&ar&&mr.y<ar.y,'PR sits above disclosure');
 await page.evaluate(()=>tp277JournalFilter('pr'));assert.ok(await page.locator('details.history').count()>0);
 await page.evaluate(()=>tp277JournalFilter('gps'));assert.equal(await page.locator('details.history').count(),0,'No invented GPS');
 await page.evaluate(()=>tp277JournalFilter('all'));await page.evaluate(()=>window.scrollTo(0,600));await snap('07-journal-sticky');
 const sticky=await page.locator('.tp177-journal-tabs').evaluate(x=>({position:getComputedStyle(x).position,y:x.getBoundingClientRect().y}));assert.equal(sticky.position,'sticky');report.checks.push({config,sticky});
 }
 await page.evaluate(()=>{window.scrollTo(0,0);go('health');});await snap('03-health');
 await page.evaluate(()=>{state.session=null;db.set('draft',null);startWorkout('A',null,'home-basic');});if(version==='after'){
  await page.evaluate(()=>{state.session.ownershipProtocol193=1;state.session.timerOwner='phone';TrainPilotWearSync.ensureSetIdentities();renderWorkout();});
  const input=page.locator('.tp277-set-row [data-inline-rir]').first();await input.fill('0');
  assert.equal(await input.getAttribute('aria-invalid'),'false');
  assert.equal(await page.evaluate(()=>db.get('draft').session.exercises[0].sets[0].actualRir),0,'RIR is persisted before OK');
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),false,'RIR does not complete a set');
  await page.evaluate(()=>renderWorkout());
  assert.equal(await page.locator('.tp277-set-row [data-inline-rir]').first().inputValue(),'0','RIR survives an active-workout repaint');
  assert.equal(await page.evaluate(()=>db.get('draft').session.exercises[0].sets[0].actualRir),0,'RIR draft survives repaint without set completion');
  await page.locator('.tp277-owner-compact summary').click();assert.equal(await page.locator('.tp277-owner-compact details').getAttribute('open'),'');await page.locator('.tp277-owner-compact summary').click();
  const dimensions=await page.evaluate(()=>({row:document.querySelector('.tp277-set-row').getBoundingClientRect().height,owner:document.querySelector('.tp277-owner-compact').getBoundingClientRect().height}));
  report.checks.push({config,compactWorkout:dimensions});assert.ok(dimensions.owner<60,'Normal owner state is a compact disclosure');
 }await snap('04-active');
 await outdoor(page);await snap('06-fullscreen-map');await page.locator('.tp163-close').click();await snap('05-outdoor');
 assert.deepEqual(await page.evaluate(()=>({active:window.__277gps.active,starts:window.__277gps.starts,stops:window.__277gps.stops})),{active:true,starts:1,stops:0},'Closing map preserves recording');
 report.errors.push(...errors.map(message=>({version,config,message})));await page.close();
 }
 fs.mkdirSync('ui-results',{recursive:true});fs.writeFileSync('ui-results/pr277-visual.json',JSON.stringify(report,null,2));
 assert.deepEqual(report.errors,[]);console.log('PASS PR277: '+report.screens.length+' before/after screenshots and archive/GPS checks');
 }finally{fs.mkdirSync('ui-results',{recursive:true});fs.writeFileSync('ui-results/pr277-visual.json',JSON.stringify(report,null,2));await browser?.close();server.close();fs.rmSync(tmp,{recursive:true,force:true});}
})().catch(e=>{console.error(e);process.exitCode=1;server.close();});
