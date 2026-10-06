'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{const f=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!f.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(f,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',f.endsWith('.js')?'text/javascript':f.endsWith('.css')?'text/css':'text/html');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
 page.on('pageerror',e=>errors.push(String(e)));await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
 await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot.finished);
 const generated=await page.evaluate(()=>{
  const profile={age:30,height:175,weight:70,goal:'fitness',experience:'beginner',activity:'mixed',minutes:60,location:'home',gear:Object.keys(GEAR132),cadence:'alternate',split:'full',excluded:[],focus:'balanced',avoidAreas:[]};
  const defs={A:[['goblet-squat',5,[15,15,15],'single_dumbbell'],['db-floor-press',5,[15,15],'per_hand'],['one-arm-row',5,[15,15],'single_dumbbell'],['db-ohp',5,[10,10,10],'per_hand'],['db-rdl',5,[10,8],'per_hand'],['db-curl',5,[15,12,12],'per_hand'],['side-plank',0,[20,25],'bodyweight']],B:[['bulgarian-split-squat',5,[10,10,10],'per_hand'],['db-pullover',5,[20,15,15],'single_dumbbell'],['lateral-raise',5,[16,15],'per_hand'],['oh-triceps',5,[20,15,15],'single_dumbbell'],['barbell-row',12.5,[15,15],'total'],['pushup',0,[10,8],'bodyweight'],['crunch',0,[10,10],'bodyweight']]};
  const legacy={id:'legacy-home',name:'Existing A/B',builtin:false,days:Object.entries(defs).map(([id,list])=>({id,name:id,exercises:list.map(x=>x[0])})),prescriptions:{}};
  for(const list of Object.values(defs))for(const [id,weight,reps,loadType] of list)legacy.prescriptions[id]={sets:reps.length,reps:Math.max(...reps)+(id==='side-plank'?' mp':''),weight,rest:60,loadType};
  const mk=(day,ago)=>{const finished=new Date(Date.now()-ago*86400000).toISOString();return {id:'legacy-'+day+'-'+ago,programId:legacy.id,workout:day,dayId:day,started:new Date(Date.parse(finished)-(day==='A'?49:36)*60000).toISOString(),finished,exercises:defs[day].map(([id,weight,reps,loadType])=>({...exercises().find(e=>e.id===id),loadType,prescription:{...legacy.prescriptions[id]},effort:'good',sets:reps.map((reps,i)=>({set:i+1,reps:String(reps),weight,done:true}))}))}};
  window.__profileHistory128=profile;window.__legacyHistory128=legacy;window.__rowsHistory128=[mk('B',1),mk('A',2)];
  db.set('programs',[...programs(),legacy]);db.set('activeProgramId',legacy.id);db.set('history',window.__rowsHistory128);db.set('settings',{...settings(),profile,onboarding128:'complete',progressionSteps:{single_dumbbell:0.5}});
  rf220Readiness=()=>({parts:0,score:70});
  const before=JSON.stringify([programs(),history(),activeProgramId()]);tp128BuildPreview(profile,'home-basic');
  return {p:state.programPreview,unchanged:before===JSON.stringify([programs(),history(),activeProgramId()])};
 });
 assert.equal(generated.unchanged,true,'generation remains read only');
 for(const d of generated.p.days){assert.equal(d.exercises.length,7,'established day '+d.id+' fits: '+JSON.stringify({ids:d.exercises,rx:generated.p.prescriptions,basis:generated.p.history128}));assert.equal(d.exercises.reduce((n,id)=>n+generated.p.prescriptions[id].sets,0),17,'17 established sets survive');}
 assert.equal(generated.p.prescriptions['one-arm-row'].weight,5);assert.equal(generated.p.prescriptions['one-arm-row'].trial,false);assert.equal(generated.p.prescriptions['one-arm-row'].reps,'8–15');
 assert.equal(generated.p.prescriptions['goblet-squat'].sets,3);assert.equal(generated.p.prescriptions['barbell-row'].weight,12.5,'total load is not doubled');assert.equal(generated.p.prescriptions['db-floor-press'].weight,5,'per-hand load is not doubled');
 assert.equal(await page.locator('#tp3PlannerPanelHost .tp3-planner-preview').count(),1,'direct preview uses the compact planner panel');assert.equal(await page.locator('#tp3PlannerPanelHost #rf223Today,#tp3PlannerPanelHost #rf220CoachCard').count(),0,'preview contains no dashboard cards');assert.ok(await page.locator('.tp128-history-summary').count());assert.equal(await page.locator('.tp128-history-reduction').count(),0);
 const short=await page.evaluate(()=>generatePersonalProgram({...window.__profileHistory128,minutes:20,templateId:'home-basic'}));assert.equal(short.days[0].exercises.length,3);assert.ok(short.history128.reductions.every(r=>r.reason==='time'));assert.ok(short.history128.reductions.some(r=>r.fromSets===17&&r.toSets<17));
 await page.evaluate(()=>tp128BuildPreview({...window.__profileHistory128,minutes:20},'home-basic'));assert.ok(await page.locator('.tp128-history-reduction').count());
 await page.evaluate(()=>tp128BuildPreview(window.__profileHistory128,'home-basic'));
 for(const width of [320,393])for(const lang of ['hu','en','de','ro']){await page.setViewportSize({width,height:873});await page.evaluate(lang=>{db.set('language',lang);showPersonalPreview()},lang);const geometry=await page.evaluate(()=>{const m=document.querySelector('.tp128-preview');return {overflow:m.scrollWidth-m.clientWidth,text:m.innerText}});assert.ok(geometry.overflow<=1);assert.doesNotMatch(geometry.text,/historySummary|reductionTime|planner\./);}
 await page.evaluate(()=>{db.set('language','hu');showPersonalPreview()});await page.setViewportSize({width:393,height:873});fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/profile-128-history-preview.png'});
 const stale=await page.evaluate(()=>{const before=programs().length,rows=history();rows[0].exercises[0].sets[0].reps='11';db.set('history',rows);acceptPersonalProgram();const unchanged=programs().length===before;tp2628DialogRemove();db.set('history',window.__rowsHistory128);return unchanged});assert.equal(stale,true,'editing history invalidates pending preview');
 const edges=await page.evaluate(()=>{
  const original=history(),profile=window.__profileHistory128,make=rows=>{db.set('history',rows);return generatePersonalProgram({...profile,templateId:'home-basic'})};
  const rows=JSON.parse(JSON.stringify(original)),a=rows.find(h=>h.workout==='A');a.exercises.find(e=>e.id==='db-floor-press').loadType='total';a.exercises.find(e=>e.id==='one-arm-row').prescription.variation='weighted';a.exercises.find(e=>e.id==='db-ohp').sets[0].weight=null;
  const mismatched=make(rows),old=make(original.map(h=>({...h,finished:new Date(Date.now()-120*86400000).toISOString()}))),future=make(original.map(h=>({...h,finished:new Date(Date.now()+86400000).toISOString()})));
  const painful=JSON.parse(JSON.stringify(original));painful.find(h=>h.workout==='A').exercises.find(e=>e.id==='one-arm-row').effort='pain';const pain=make(painful);
  db.set('history',original);return {unit:mismatched.prescriptions['db-floor-press'].trial,variation:mismatched.prescriptions['one-arm-row'].trial,invalid:mismatched.prescriptions['db-ohp'].trial,old:old.prescriptions['one-arm-row'].trial,future:future.prescriptions['one-arm-row'].trial,pain:pain.prescriptions['one-arm-row'].trial,painStatus:pain.history128.entries['one-arm-row'].status};
 });assert.deepEqual(edges,{unit:true,variation:true,invalid:true,old:true,future:true,pain:true,painStatus:'review'});
 const continuation=await page.evaluate(()=>{
  const rows=history(),a=rows.find(h=>h.workout==='A'),clone=(key,ago)=>({...JSON.parse(JSON.stringify(a)),id:key,finished:new Date(Date.now()-ago*86400000).toISOString(),started:new Date(Date.now()-ago*86400000-49*60000).toISOString()});
  db.set('history',[clone('stable-a-1',1),clone('stable-a-2',3),clone('stable-a-3',5),rows.find(h=>h.workout==='B')]);tp128BuildPreview(window.__profileHistory128,'home-basic');acceptPersonalProgram();tp2628DialogRemove();
  const p=activeProgram(),recommendation=rf152Recommendation('goblet-squat'),backup=makeBackup(),valid=validateBackup(JSON.parse(JSON.stringify(backup))),sync=syncData();
  const malformed=JSON.parse(JSON.stringify(backup));malformed.programs.find(x=>x.id===p.id).history128.entries['goblet-squat'].sources.push(...p.history128.entries['goblet-squat'].sources);let rejected=false;try{validateBackup(malformed)}catch(_){rejected=true}
  return {recommendation,backupSame:JSON.stringify(valid.programs)===JSON.stringify(backup.programs),syncSame:JSON.stringify(sync.programs)===JSON.stringify(backup.programs),rejected};
 });assert.equal(continuation.recommendation.historyCount,3);assert.equal(continuation.recommendation.autoApply,true);assert.equal(continuation.recommendation.weight,5.5);assert.equal(continuation.backupSame,true);assert.equal(continuation.syncSame,true);assert.equal(continuation.rejected,true);
 await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);await page.evaluate(()=>{window.__profileHistory128=trainingProfile();rf220Readiness=()=>({parts:0,score:70});startWorkout('A',null,activeProgramId())});await page.waitForFunction(()=>!!state.session);
 assert.equal(await page.evaluate(()=>state.session.exercises.find(e=>e.id==='goblet-squat').sets[0].weight),5.5,'real start applies inherited three-session progression');
 const finish=async()=>{await page.evaluate(()=>{for(const e of state.session.exercises){const t=tp140Prescription(e.id,e);e.effort='good';for(const s of e.sets){s.reps=String(t.max||10);s.done=true;if(e.id==='side-plank'){s.leftSeconds=Number(s.reps);s.rightSeconds=Number(s.reps)}}}finishWorkout()});await page.waitForFunction(()=>!state.session);await page.evaluate(()=>{tp2628DialogRemove();go('home')});};
 await finish();assert.equal(await page.evaluate(()=>history()[0].programId===activeProgramId()),true,'completed test session is logged under new program');
 assert.equal(await page.evaluate(()=>rf152Recommendation('goblet-squat').autoApply),false,'one session at the new weight does not increase again');
 for(let i=0;i<2;i++){await page.evaluate(()=>startWorkout('A',null,activeProgramId()));await page.waitForFunction(()=>!!state.session);assert.equal(await page.evaluate(()=>state.session.exercises.find(e=>e.id==='goblet-squat').sets[0].weight),5.5);await finish();}
 await page.evaluate(()=>startWorkout('A',null,activeProgramId()));await page.waitForFunction(()=>!!state.session);assert.equal(await page.evaluate(()=>state.session.exercises.find(e=>e.id==='goblet-squat').sets[0].weight),6,'three real completed sessions apply the next step');
 const guards=await page.evaluate(()=>{
  state.session=null;db.set('draft',null);const original=history(),p=activeProgram(),id='goblet-squat';
  const set=(reps,effort='good',weight=5.5,done=true,key='guard')=>({id:key,programId:p.id,started:new Date(Date.now()-60000).toISOString(),finished:new Date().toISOString(),exercises:[{...exercises().find(e=>e.id===id),effort,prescription:{...p.prescriptions[id]},sets:reps.map((reps,i)=>({set:i+1,reps,weight,done:done||i!==0}))}]});
  const read=rows=>{rows.forEach((h,i)=>{h.finished=new Date(Date.now()-i*86400000).toISOString();h.id+='-'+i});db.set('history',rows);return rf152Recommendation(id)};
  const interrupted=read([set([15,15,15]),set([12,12,12]),set([15,15,15]),set([15,15,15])]);
  const partial=read([set([15,15,15],'good',5.5,false),set([15,15,15]),set([15,15,15])]);
  const pain=read([set([15,15,15],'pain'),set([15,15,15]),set([15,15,15])]);
  const unknown=read([set([15,15,15],null),set([15,15,15],null),set([15,15,15],null)]);
  const declining=read([set([5,5,5],'hard'),set([6,6,6],'hard'),set([7,7,7],'hard')]);
  db.set('history',original);rf220Readiness=()=>({parts:2,score:38});const recovery=rf152Recommendation(id);rf220Readiness=()=>({parts:0,score:70});
  const before=JSON.stringify([history(),programs()]);tp128BuildPreview(window.__profileHistory128,'home-basic');const regenerated=state.programPreview;acceptPersonalProgram();tp2628DialogRemove();
  const next=rf152Recommendation(id);return {interrupted,partial,pain,unknown,declining,recovery,next,weight:regenerated.prescriptions[id].weight,keptHistory:JSON.parse(before)[0].length===history().length};
 });assert.equal(guards.interrupted.autoApply,false,'top-range sessions must be consecutive');assert.equal(guards.partial.autoApply,false);assert.equal(guards.pain.action,'pain');assert.equal(guards.unknown.autoApply,false);assert.equal(guards.declining.action,'reduce');assert.equal(guards.declining.autoApply,false);assert.equal(guards.recovery.action,'hold');assert.equal(guards.weight,5.5);assert.equal(guards.next.historyCount,4,'second generation retains approved continuity');assert.equal(guards.keptHistory,true);
 assert.deepEqual(errors,[]);console.log('PASS #128 history: preserved 5kg/12.5kg, 7 exercises/17 sets, range ceiling, explicit time reduction, unit/variation/partial/pain/stale guards, backup/sync, approved lineage and real start -> finish -> three-session progressive loads without a user journal.');
}finally{await browser?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exitCode=1});
