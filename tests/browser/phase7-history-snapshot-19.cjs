const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({headless:true,args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await p.addInitScript(()=>{try{localStorage.setItem("repforge:onboarding128",JSON.stringify("skipped"))}catch(_){}});await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const makeRows=n=>p.evaluate(n=>{const rows=[];for(let i=0;i<n;i++){const end=new Date(Date.now()-i*86400000),start=new Date(end.getTime()-3600000);rows.push({id:'p7-snap-'+i,workout:i%2?'B':'A',dayId:i%2?'B':'A',programId:'home-basic',programName:'Snapshot',started:start.toISOString(),finished:end.toISOString(),exercises:[{id:'db-squat',loadType:'per_hand',repUnit:'ism.',effort:i===0?'easy':null,sets:[{set:1,weight:10,reps:'10',done:true}]}],photos:[]})}db.set('history',rows);state.session=null},n);
 await makeRows(120);
 const homeCalls=await p.evaluate(()=>{let n=0;const base=window.history;window.history=function(){n++;return base.apply(this,arguments)};go('home');return n});
 assert.ok(homeCalls<=4,'Home should use one operation-scoped history snapshot, calls='+homeCalls);
 const coach=await p.evaluate(()=>{let n=0;const live=window.history;window.history=function(){n++;return live.apply(this,arguments)};window.tp155R4OpenPanel('coach',document.activeElement);return {calls:n,text:document.querySelector('#tp155R4PanelHost .tp151-coach')?.innerText||'',flag:window.TrainPilotPhase7Performance}});
 assert.ok(coach.calls<=4,'Coach open should reuse one history snapshot, calls='+coach.calls);assert.match(coach.text,/TrainPilot Coach/);assert.equal(coach.flag.historySnapshot,'operation-scoped');assert.equal(coach.flag.persistentCache,false);
 await p.evaluate(()=>window.tp155R4ClosePanel?.(false));
 await makeRows(2);const after=await p.evaluate(()=>{go('home');return history().map(x=>x.id)});
 assert.deepEqual(after,['p7-snap-0','p7-snap-1'],'a new operation must observe newly stored history; no persistent cache allowed');
 // A long real dataset must not rebuild the library per set or reread history per calendar cell.
 await p.evaluate(()=>{
  const ids=['db-squat','db-floor-press','one-arm-row','rdl','db-curl','pushup','plank','side-plank'];
  const rows=Array.from({length:240},(_,i)=>({id:'budget-'+i,programId:'home-basic',dayId:i%2?'A':'B',started:new Date(Date.now()-i*86400000-1800000).toISOString(),finished:new Date(Date.now()-i*86400000).toISOString(),exercises:ids.map(id=>({id,loadType:'per_hand',repUnit:'ism.',sets:[1,2,3,4].map(set=>({set,weight:10,reps:'10',done:true}))}))}));
  db.set('history',rows);
 });
 const saved=await p.evaluate(()=>localStorage.getItem('repforge:history'));
 const actions=[['history',"go('history')"],['health',"go('health')"],['settings',"go('settings')"],['home',"go('home')"],['coach',"go('coach')"],['home-return',"go('home')"],['calendar',"go('calendar')"],['plan',"go('plan')"],['programs',"go('programs')"],['progress','tp177OpenProgress()'],['statistics','tp177OpenStatistics()']];
 for(const [name,action] of actions){
  const budget=await p.evaluate(action=>{
   const counts={library:0,history:0,programs:0,target:0,readiness:0},lib=mergeExerciseLibrary,get=db.get,target=rf233TargetWorkout,load=rf220EffortLoad;
   mergeExerciseLibrary=function(){counts.library++;return lib.apply(this,arguments)};
   db.get=function(key){if(key==='history')counts.history++;if(key==='programs')counts.programs++;return get.apply(this,arguments)};
   rf233TargetWorkout=function(){counts.target++;return target.apply(this,arguments)};
   rf220EffortLoad=function(){counts.readiness++;return load.apply(this,arguments)};
   try{(0,eval)(action);return counts;}finally{mergeExerciseLibrary=lib;db.get=get;rf233TargetWorkout=target;rf220EffortLoad=load;}
  },action);
  assert.ok(budget.library<=1,name+' builds the exercise library once: '+JSON.stringify(budget));
  assert.ok(budget.programs<=1,name+' reads programs at most once: '+JSON.stringify(budget));
  assert.ok(budget.history<=1,name+' reads history at most once: '+JSON.stringify(budget));
  assert.ok(budget.target<=1,name+' computes one Coach target: '+JSON.stringify(budget));
  assert.ok(budget.readiness<=1,name+' computes readiness once: '+JSON.stringify(budget));
 }
 assert.equal(await p.evaluate(()=>localStorage.getItem('repforge:history')),saved,'navigation must preserve every workout');
 const validity=await p.evaluate(()=>{
  const expected=rf233CoachPlan(),inside=tp7WithHistorySnapshot(()=>rf233CoachPlan());
  const originalExercises=db.get('exercises',null),originalLanguage=db.get('language','system');
  const changed=tp7WithHistorySnapshot(()=>{
   const oldCount=rf220Hist().length;db.set('history',history().slice(0,2));const newCount=rf220Hist().length;
   const oldName=exercises().find(e=>e.id==='db-squat').hu;
   db.set('exercises',exercises().map(e=>e.id==='db-squat'?{...e,hu:'Frissített saját név'}:e));
   const newName=exercises().find(e=>e.id==='db-squat').hu;
   rf212Lang();db.set('language','en');const language=rf212Lang();
   return {oldCount,newCount,oldName,newName,language};
  });
  db.set('exercises',originalExercises);db.set('language',originalLanguage);
  let threw=false;try{tp7WithHistorySnapshot(()=>{throw Error('scope test')})}catch(_){threw=true}
  db.set('history',[]);
  return {expected,inside,changed,threw,afterError:history().length};
 });
 assert.deepEqual(validity.inside,validity.expected,'scoped recommendations match the uncached calculation');
 assert.equal(validity.changed.oldCount,240);assert.equal(validity.changed.newCount,2,'writes invalidate the current snapshot immediately');
 assert.equal(validity.changed.newName,'Frissített saját név','exercise edits invalidate the library during the same operation');
 assert.equal(validity.changed.language,'en','language writes invalidate localization during the same operation');
 assert.equal(validity.threw,true);assert.equal(validity.afterError,0,'exceptions release scope and later reads see new data');
 console.log('PASS #137 runtime budgets: Home/Coach/calendar/plan/programs/progress/statistics; one library/history/readiness/target per operation; recommendation parity, in-scope edits, exception cleanup, immutable navigation');
 console.log('PASS Phase 7 operation-scoped history snapshot, Coach/Home freshness preserved');
}finally{await b?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
