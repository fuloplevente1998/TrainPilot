'use strict';
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),assert=require('node:assert/strict'),{chromium}=require('playwright');
const root=path.resolve(__dirname,'../../www'),server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
try{
 browser=await chromium.launch({executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,headless:true,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873}}),errors=[];page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.accept());
 await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot?.finished);await page.locator('#tpBoot').waitFor({state:'hidden'});
 for(const lang of ['hu','en','de','ro'])for(const width of [320,393]){
  await page.setViewportSize({width,height:873});
  await page.evaluate(({lang})=>{
   db.set('language',lang);state.session={started:new Date().toISOString(),workout:'quick',quickWorkout:true,type:'quick',exercises:[{...byId('plank'),sets:[{set:1,setId:'timed-one',weight:0,reps:'',done:false},{set:2,setId:'timed-two',weight:0,reps:'',done:false}]}]};state.workout='quick';state.current=0;state.tab='plan';persistDraft();renderWorkout();
  },{lang});
  assert.equal(await page.locator('#rf110Stopwatch').count(),1,'keep the lower stopwatch');assert.equal(await page.locator('#tp107Indoor .tp107-clock').count(),0,'remove the upper interval clock');assert.equal(await page.locator('#rf110Stopwatch .tp110-interval').count(),1,'interval settings are optional inside the lower card');assert.equal(await page.locator('.tp110-interval').evaluate(e=>e.open),false);
  await page.locator('#rf110StopwatchToggle').click();await page.evaluate(()=>{rf110Stopwatch.elapsedMs=0;rf110Stopwatch.startedAt=Date.now();});await page.locator('#rf110StopwatchToggle').click();
  assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[0].done),false,'zero seconds is not a completed set');
  await page.locator('#rf110StopwatchToggle').click();await page.evaluate(()=>{rf110Stopwatch.startedAt-=8100;});await page.locator('#rf110StopwatchToggle').click();
  await page.waitForFunction(()=>state.session.exercises[0].sets[0].done);
  const set=await page.evaluate(()=>state.session.exercises[0].sets[0]);assert.ok(Number(set.reps)>=8);assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[1].done),false,'only the measured set completes');assert.equal(await page.evaluate(()=>history().length),0,'stopping a set does not save the whole workout');
  await page.locator('#rf110StopwatchToggle').click();await page.evaluate(()=>{rf110Stopwatch.startedAt-=4100;rf110StopwatchHalt(true);});assert.equal(await page.evaluate(()=>state.session.exercises[0].sets[1].done),false,'implicit halt must not complete a set');
 }
 fs.mkdirSync('ui-evidence',{recursive:true});
 await page.setViewportSize({width:393,height:873});
 for(const id of ['plank','side-plank']){
  await page.evaluate(id=>{rf110StopwatchHalt(true);tp1481HaltSide(true);db.set('language','hu');state.session={started:new Date().toISOString(),workout:'quick',quickWorkout:true,type:'quick',exercises:[{...byId(id),sets:[{set:1,setId:'visual-'+id,weight:0,reps:'',done:false}]}]};state.workout='quick';state.current=0;state.tab='plan';renderWorkout();},id);
  await page.locator('#rf110Stopwatch').screenshot({path:'ui-evidence/lower-stopwatch-'+id+'.png'});
 }
 const performance=[];
 await page.evaluate(()=>{rf110StopwatchHalt(true);state.session=null;state.workout=null;db.set('draft',null);db.set('language','hu');go('home');});
 for(const count of [200,1500]){
  await page.evaluate(count=>{
   const ids=['db-floor-press','db-squat','one-arm-row','rdl','db-curl','plank'];
   db.set('phase3HistoryTombstones',[]);db.set('history',Array.from({length:count},(_,i)=>({id:'perf-'+i,syncId:'perf-'+i,started:new Date(Date.now()-(i+1)*86400000).toISOString(),finished:new Date(Date.now()-(i+1)*86400000+3600000).toISOString(),exercises:ids.map(id=>{const e=byId(id);return {id,loadType:e.loadType,repUnit:e.repUnit,sets:Array.from({length:4},(_,j)=>({set:j+1,weight:8,reps:'10',done:true}))};})})));
  },count);
  const probe=await page.evaluate(()=>{
   const get=Storage.prototype.getItem,deleted=window.tp3HistoryIsDeleted;let reads=0,checks=0;
   Storage.prototype.getItem=function(k){if(k==='repforge:history')reads++;return get.call(this,k);};window.tp3HistoryIsDeleted=function(row){checks++;return deleted(row);};
   try{const start=performance.now(),snapshot=TrainPilotWearSync.makeHomeSnapshot();return {ms:performance.now()-start,reads,checks,catalog:snapshot.quickExercises.length};}finally{Storage.prototype.getItem=get;window.tp3HistoryIsDeleted=deleted;}
  });
  assert.ok(probe.reads<=4,'one shared history batch, not a read for every exercise');assert.ok(probe.checks<=count*5,'deletion checks are bounded by history size, not catalog × history');assert.ok(probe.catalog>50);performance.push({count,...probe});
  for(const panel of ['calendar','coach','settings']){
   await page.evaluate(panel=>{tp155R4ClosePanel(false);tp155R4OpenPanel(panel,document.activeElement);},panel);
   await page.locator('.tp155-r4-panel').evaluate(el=>{el.scrollTop=200;});
   const before=await page.locator('.tp155-r4-panel').evaluate(el=>el.scrollTop);
   await page.evaluate(()=>TrainPilotWearSync.makeHomeSnapshot());
   assert.equal(await page.locator('.tp155-r4-panel').evaluate(el=>el.scrollTop),before,'background snapshot preserves '+panel+' scroll and content');
   await page.evaluate(()=>tp155R4ClosePanel(false));
  }
 }
 // Equal-sized edits and a newly written deletion tombstone must be visible on
 // the very next publication; there is no stale process-global catalog cache.
 const fresh=await page.evaluate(()=>{
  let rows=history();const id=rows[0].id;rows[0].exercises.find(e=>e.id==='db-floor-press').sets[0].weight=33;db.set('history',rows);
  const before=TrainPilotWearSync.makeHomeSnapshot().quickExercises.find(e=>e.id==='db-floor-press').sets[0].weight;
  db.set('phase3HistoryTombstones',[{ids:[id]}]);const after=TrainPilotWearSync.makeHomeSnapshot();
  return {before,after:after.quickExercises.find(e=>e.id==='db-floor-press').sets[0].weight,deleted:after.recentWorkouts.some(h=>h.workoutId===id)};
 });assert.equal(fresh.before,33);assert.equal(fresh.after,8);assert.equal(fresh.deleted,false,'deleted workout is not republished');
 const deferred=await page.evaluate(async()=>{
  tp155R4OpenPanel('settings',document.activeElement);const previous=window.Capacitor;let publishes=0;
  window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{publishHome:async()=>{publishes++;}}}};
  try{
   document.querySelector('.tp155-r4-panel').dispatchEvent(new Event('scroll'));
   const pending=TrainPilotWearSync.syncHomeNow();await new Promise(r=>setTimeout(r,50));const during=publishes;
   await pending;return {during,after:publishes};
  }finally{window.Capacitor=previous;tp155R4ClosePanel(false);}
 });assert.equal(deferred.during,0,'ordinary home sync yields during panel scrolling');assert.equal(deferred.after,1,'publication resumes after the gesture without a button press');
 const commandsDuringScroll=await page.evaluate(async()=>{
  tp155R4OpenPanel('settings',document.activeElement);const previous=window.Capacitor;let pending=true,acked=[],draining;
  const panel=document.querySelector('.tp155-r4-panel'),gesture=()=>panel.dispatchEvent(new Event('scroll'));
  window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:{publishHome:async()=>{},publish:async()=>{},clear:async()=>{},pendingCommands:async()=>({commands:pending?[{commandId:'scroll-command',action:'saveWorkoutFeedback',feedback:{}}]:[]}),ackCommand:async({commandId})=>{acked.push(commandId);pending=false;}}}};
  gesture();const timer=setInterval(gesture,20);
  try{
   draining=TrainPilotWearSync.drainCommands();
   const completed=await Promise.race([draining.then(()=>true),new Promise(r=>setTimeout(()=>r(false),500))]);
   return {completed,acked,busy:TrainPilotWearSync.isApplyingCommand()};
  }finally{clearInterval(timer);await draining;window.Capacitor=previous;tp155R4ClosePanel(false);}
 });assert.equal(commandsDuringScroll.completed,true,'continuous scrolling cannot hold the command drain behind a catalog refresh');assert.deepEqual(commandsDuringScroll.acked,['scroll-command']);assert.equal(commandsDuringScroll.busy,false);
 const readiness=await page.evaluate(()=>{
  const now=new Date(),day=rf240DayKey(now);state.health={recovery:{}};
  window.TrainPilotEnergy184={hasResult:()=>true,reading:()=>({energyScore:87,energyScoreTime:now.toISOString(),energyScoreSource:'samsung_health',day})};
  const effort=['hard','hard','hard','hard','hard','hard','challenging','challenging','light'];
  db.set('history',[{started:new Date(Date.now()-3600000).toISOString(),finished:new Date(Date.now()-1800000).toISOString(),exercises:effort.map((effort,i)=>({id:'effort-'+i,effort,sets:[]}))}]);
  const r=rf220Readiness();return {...r,html:TrainPilot110.coachReasonsHtml({readiness:r})};
 });
 assert.equal(readiness.load,45);assert.equal(readiness.energyScore,87);assert.equal(readiness.score,54);assert.equal(readiness.trainpilotScore,61);assert.match(readiness.html,/87 → 54\/100/);assert.match(readiness.html,/Miért alacsonyabb/);
 fs.mkdirSync('ui-evidence',{recursive:true});fs.writeFileSync('ui-evidence/panel-snapshot-performance.json',JSON.stringify(performance,null,2));
 assert.deepEqual(errors,[]);console.log('PASS lower stopwatch auto-completion: 8 layouts, zero/implicit-stop guards; panel snapshots 200/1500 rows; fresh edits/deletions and Samsung 87→54 explanation. '+JSON.stringify(performance));
}finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1});
