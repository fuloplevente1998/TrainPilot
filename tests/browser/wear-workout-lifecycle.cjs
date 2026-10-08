const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{const u=new URL(req.url,'http://local'),file=path.resolve(root,'.'+(u.pathname==='/'?'/index.html':u.pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});page.on('dialog',d=>d.accept());
 await page.addInitScript(()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))});
 await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const result=await page.evaluate(async()=>{
  let commands=[],acks=[];const bridge={publish:async()=>{},publishHome:async()=>{},clear:async()=>{},pendingCommands:async()=>({commands:commands.slice()}),ackCommand:async({commandId})=>{acks.push(commandId);commands=commands.filter(c=>c.commandId!==commandId)}};
  window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:bridge}};
  db.set('history',[]);db.set('draft',null);db.set('wearClosedWorkouts',[]);db.set('wearWorkoutResult',null);state.session=null;state.workout=null;
  startWorkout(activeProgram().days[0].id);const id=state.session.syncId||state.session.id||state.session.started;
  const set=state.session.exercises[0].sets[0];set.reps='12';set.done=true;state.session.scheduleId='wear-schedule';db.set('scheduled',[{id:'wear-schedule',status:'planned'}]);persistDraft();
  const finishedAt=new Date().toISOString();
  commands=[{commandId:'watch-finish',workoutId:id,action:'finishWorkout',confirmed:true,finishedAt,createdAt:Date.now(),sequence:1}];
  await window.TrainPilotWearSync.drainCommands();
  const first={history:history().length,status:db.get('wearWorkoutResult')?.status,session:!!state.session,draft:!!db.get('draft'),dialog:!!document.querySelector('#tp2628Dialog')};
  commands=[{commandId:'watch-retry',workoutId:id,action:'finishWorkout',confirmed:true,createdAt:Date.now(),sequence:2}];await window.TrainPilotWearSync.drainCommands();
  const retry=history().length,finishedMatches=history()[0].finished===finishedAt,scheduleStatus=scheduled().find(s=>s.id==='wear-schedule')?.status;
  state.session=null;db.set('draft',null);startWorkout(activeProgram().days[0].id);const draftId=state.session.syncId||state.session.id||state.session.started;persistDraft();state.session=null;state.workout=null;go('home');
  const draftSnapshot=window.TrainPilotWearSync.makeSnapshot();
  commands=[{commandId:'watch-discard',workoutId:draftId,action:'discardWorkout',confirmed:true,createdAt:Date.now(),sequence:3}];await window.TrainPilotWearSync.drainCommands();
  return {first,retry,finishedMatches,scheduleStatus,draftSnapshot:!!draftSnapshot,lastStatus:db.get('wearWorkoutResult')?.status,draft:!!db.get('draft'),history:history().length,acks};
 });
 assert.deepEqual(result.first,{history:1,status:'saved',session:false,draft:false,dialog:false},JSON.stringify(result));assert.equal(result.retry,1);assert.equal(result.finishedMatches,true);assert.equal(result.scheduleStatus,'completed');assert.equal(result.draftSnapshot,true);assert.equal(result.lastStatus,'discarded');assert.equal(result.draft,false);assert.equal(result.history,1);assert.deepEqual(result.acks,['watch-finish','watch-retry','watch-discard']);
 console.log('PASS Wear canonical phone lifecycle: partial finish without a second dialog, one history entry on retry, draft snapshot and confirmed discard.');
}finally{await browser?.close();server.close()}})().catch(e=>{console.error(e);process.exitCode=1});
