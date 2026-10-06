'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{const f=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html')));if(!f.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(f,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',f.endsWith('.js')?'text/javascript':f.endsWith('.css')?'text/css':'text/html');res.end(d)})});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
  const url='http://127.0.0.1:'+server.address().port;
  await page.goto(url);await page.waitForFunction(()=>TrainPilotBoot.finished);
  assert.equal(await page.locator('.tp128-welcome').count(),1,'fresh install starts onboarding');
  await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);
  assert.equal(await page.locator('.tp128-welcome').count(),1,'unfinished onboarding survives relaunch');
  await page.locator('[onclick="tp128BeginOnboarding()"]').click();
  assert.equal(await page.locator('#pfAge').count(),1,'reuse existing compact profile panel');
  assert.equal(await page.locator('#pfTemplate128').count(),1);
  await page.evaluate(()=>{document.querySelector('#pfAge').value='30';document.querySelector('#pfHeight').value='175';document.querySelector('#pfWeight').value='70';document.querySelector('#pfMinutes').value='45';document.querySelector('input[name="pfGear"][value="dumbbells"]')?.click();previewProfile()});
  const preview=await page.evaluate(()=>({p:state.programPreview,profile:state.profilePreview,gear:Object.keys(GEAR132),active:activeProgramId()}));
  assert.ok(preview.p,'valid profile creates preview');
  assert.equal(preview.p.sourceTemplateId,'home-basic');assert.equal(preview.active,'home-basic','preview does not activate');
  assert.equal(await page.locator('.tp128-preview-meta').count(),1);
  await page.evaluate(()=>acceptPersonalProgram());
  assert.equal(await page.evaluate(()=>activeProgram().sourceType),'profile-template');
  assert.equal(await page.evaluate(()=>settings().onboarding128),'complete');
  await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);assert.equal(await page.locator('.tp128-welcome').count(),0);
  const manual=await page.evaluate(()=>{const before=activeProgramId();activateProgram('home-level2');return {before,after:activeProgramId(),template:state.programPreview?.sourceTemplateId}});
  assert.equal(manual.after,manual.before,'manual built-in selection waits for preview acceptance');assert.equal(manual.template,'home-level2');
  await page.evaluate(()=>{tp3ClosePlannerPanel(false);state.programPreview=null;state.profilePreview=null;state.tp128PreviewToken=null;go('home')});

  const logic=await page.evaluate(()=>{
   const gear=Object.keys(GEAR132),dumb=gear.find(x=>x==='dumbbells')||gear.find(x=>/dumbbell/.test(x));
   const profile={age:30,height:175,weight:70,goal:'fitness',experience:'beginner',activity:'mixed',minutes:45,location:'home',gear:[dumb],cadence:'alternate',split:'full',excluded:[],focus:'balanced',avoidAreas:[]};
   window.__profile128=profile;
   const before=JSON.stringify([programs(),history(),activeProgramId()]);
   const versions=[20,45,60,90,120].flatMap(minutes=>['beginner','intermediate'].map(experience=>{
    const p=generatePersonalProgram({...profile,minutes,experience,templateId:'recommended'});
    return {minutes,experience,p,available:p.days.every(d=>d.exercises.every(id=>available132(id,{...profile,minutes,experience}))),compoundsFirst:p.days.every(d=>{let isolated=false;return d.exercises.every(id=>{const e=byId(id);if(!e.compound)isolated=true;return !isolated||!e.compound})})};
   }));
   const bw=generatePersonalProgram({...profile,gear:[],templateId:'recommended'}),custom=generatePersonalProgram({...profile,templateId:'custom'});
   const base=tp128Template('home-basic'),advanced=tp128Template('home-level2');
   const unchanged=before===JSON.stringify([programs(),history(),activeProgramId()]);
   return {versions,bw,custom,base,advanced,unchanged};
  });
  assert.equal(logic.unchanged,true,'generation is read only');
  assert.deepEqual(logic.base.days[1].exercises,['bulgarian-split-squat','pushup','barbell-row','db-pullover','lateral-raise','oh-triceps','crunch']);
  assert.notDeepEqual(logic.base.days,logic.advanced.days);
  assert.equal(logic.advanced.prescriptions.crunch.variation,'weighted');
  assert.equal(logic.advanced.prescriptions.crunch.loadType,'single_dumbbell');
  assert.equal(logic.custom.sourceType,'personal','standalone generator remains');
  for(const v of logic.versions){assert.equal(v.available,true);assert.equal(v.compoundsFirst,true);assert.equal(v.p.templateVersion,1);assert.ok(v.p.profileSnapshot);for(const [id,r] of Object.entries(v.p.prescriptions)){assert.ok(r.sets>=2&&r.sets<=3);assert.ok(r.rest>=60);assert.equal(r.rir,2)}}
  assert.ok(logic.bw.days.every(d=>d.exercises.length>=3));

  const trends=await page.evaluate(()=>{
   const profile=window.__profile128,p=generatePersonalProgram({...profile,templateId:'home-basic'});p.id='test-128';p.prescriptions['db-floor-press']={sets:2,reps:'8–12',weight:0,rest:120,rir:2};p.prescriptions.pushup={sets:2,reps:'8–15',weight:0,rest:120,rir:2};
   db.set('programs',[...programs(),p]);db.set('activeProgramId',p.id);db.set('settings',{...settings(),profile,onboarding128:'complete'});
   const mk=(id,reps,effort='good',weight=10,pid=p.id,variation='')=>({id:crypto.randomUUID(),programId:pid,started:new Date().toISOString(),finished:new Date().toISOString(),exercises:[{id,effort,loadType:byId(id).loadType,prescription:{variation},sets:reps.map((reps,i)=>({set:i+1,reps,weight,done:true}))}]});
   const read=rows=>{db.set('history',rows);return rf152Recommendation(rows[0].exercises[0].id)};
   const oldReadiness=rf220Readiness;rf220Readiness=()=>({parts:0,score:70});
   const a={
    improving:read([mk('db-floor-press',[12,10]),mk('db-floor-press',[11,9]),mk('db-floor-press',[10,8])]),
    singleWeak:read([mk('db-floor-press',[7,6],'hard'),mk('db-floor-press',[12,10]),mk('db-floor-press',[11,9])]),
    declining:read([mk('db-floor-press',[5,5],'hard'),mk('db-floor-press',[6,6],'hard'),mk('db-floor-press',[7,7],'hard')]),
    stable:read([mk('db-floor-press',[12,12]),mk('db-floor-press',[12,12]),mk('db-floor-press',[12,12])]),
    pain:read([mk('db-floor-press',[12,12]),mk('db-floor-press',[12,12],'pain'),mk('db-floor-press',[12,12])]),
    foreign:read([mk('db-floor-press',[12,12],'good',10,'other'),mk('db-floor-press',[12,12],'good',10,'other')]),
    body:read([mk('pushup',[15,15],'good',0),mk('pushup',[15,15],'good',0),mk('pushup',[15,15],'good',0)])
   };
   rf220Readiness=()=>({parts:2,score:38});a.recovery=read([mk('db-floor-press',[12,12]),mk('db-floor-press',[12,12]),mk('db-floor-press',[12,12])]);
   rf220Readiness=oldReadiness;db.set('history',[]);return a;
  });
  assert.equal(trends.improving.action,'reps');assert.equal(trends.singleWeak.action,'hold');assert.equal(trends.declining.action,'reduce');
  assert.equal(trends.stable.action,'increase');assert.equal(trends.stable.autoApply,true);assert.equal(trends.pain.action,'pain');assert.equal(trends.foreign.historyCount,0);assert.equal(trends.foreign.autoApply,false);
  assert.equal(trends.body.action,'variation');assert.equal(trends.body.autoApply,false);assert.equal(trends.body.nextVariation.id,'decline-pushup');assert.equal(trends.recovery.action,'hold');

  const migration=await page.evaluate(()=>{
   const before=JSON.stringify([activeProgram(),history()]);tp128BuildPreview({...window.__profile128,goal:'muscle',experience:'intermediate',minutes:90},'home-level2');
   const previewOnly=before===JSON.stringify([activeProgram(),history()]);
   const oldSet=db.set;db.set=function(k,v){if(k==='settings')throw Error('quota');return oldSet(k,v)};acceptPersonalProgram();db.set=oldSet;
   const rolledBack=before===JSON.stringify([activeProgram(),history()]);
   tp128BuildPreview({...window.__profile128,goal:'muscle',experience:'intermediate',minutes:90},'home-level2');acceptPersonalProgram();
   const backup=makeBackup(),roundtrip=validateBackup(JSON.parse(JSON.stringify(backup))),sync=syncData();
   return {previewOnly,rolledBack,p:activeProgram(),same:JSON.stringify(roundtrip.programs)===JSON.stringify(backup.programs),syncSame:JSON.stringify(sync.programs)===JSON.stringify(backup.programs)};
  });
  assert.equal(migration.previewOnly,true);assert.equal(migration.rolledBack,true);assert.equal(migration.p.sourceTemplateId,'home-level2');assert.equal(migration.same,true);assert.equal(migration.syncSame,true);assert.equal(migration.p.prescriptions.crunch.variation,'weighted');
  await page.evaluate(()=>startWorkout('A',null,activeProgramId()));await page.waitForFunction(()=>!!state.session);
  const weightedSession=await page.evaluate(()=>{const e=state.session.exercises.find(e=>e.id==='side-plank');const out={loadType:e?.loadType,variation:e?.prescription?.variation,sets:e?.sets?.length};state.session=null;db.set('draft',null);go('home');return out});
  assert.equal(weightedSession.loadType,'single_dumbbell');assert.equal(weightedSession.variation,'weighted');assert.equal(weightedSession.sets,3);

  for(const width of [320,360,393,412])for(const lang of ['hu','en','de','ro']){
   await page.setViewportSize({width,height:873});await page.evaluate(lang=>{tp3ClosePlannerPanel(false);db.set('language',lang);go('plan')},lang);
   const layout=await page.evaluate(()=>({icons:document.querySelectorAll('.top .tp128-nav-icon').length,navTop:document.querySelector('.top').getBoundingClientRect().top,overflow:document.documentElement.scrollWidth-innerWidth,docked:document.querySelectorAll('.tp127-page-tools,.tp127-edge-switch').length}));
   assert.equal(layout.icons,8);assert.equal(layout.docked,0);assert.ok(layout.navTop>=0&&layout.navTop<100);assert.ok(layout.overflow<=1,JSON.stringify({width,lang,layout}));
   await page.evaluate(()=>{state.tp128TemplateChoice='recommended';profileScreen()});assert.equal(await page.locator('#pfTemplate128').count(),1);
   const overflow=await page.evaluate(()=>document.documentElement.scrollWidth-innerWidth);assert.ok(overflow<=1);
  }
  await page.evaluate(()=>{tp2628DialogRemove();tp3ClosePlannerPanel(false);db.set('language','hu');go('plan')});await page.setViewportSize({width:393,height:873});
  fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/profile-128-original-training.png'});
  await page.setViewportSize({width:873,height:393});assert.equal(await page.locator('.tp127-edge-switch').count(),0);await page.screenshot({path:'ui-evidence/profile-128-original-landscape.png'});
  assert.deepEqual(errors,[]);
  const skip=await browser.newPage({viewport:{width:360,height:800}});await skip.goto(url);await skip.waitForFunction(()=>TrainPilotBoot.finished);await skip.evaluate(()=>tp128SkipOnboarding());await skip.reload();await skip.waitForFunction(()=>TrainPilotBoot.finished);assert.equal(await skip.locator('.tp128-welcome').count(),0);assert.equal(await skip.evaluate(()=>activeProgramId()),'home-basic');await skip.close();
  console.log('PASS #128: onboarding/relaunch/skip, shared templates and personal generation, 20–120 minutes, program-scoped trends/pain/readiness, preview/rollback, backup/sync and original 8-icon layout in four languages.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exitCode=1});
