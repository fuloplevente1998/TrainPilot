const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const app=fs.readFileSync('www/app.js','utf8');
const source=fs.existsSync('www/wear-sync.js')?fs.readFileSync('www/wear-sync.js','utf8'):app.match(/\/\/ @section wear-sync\.js\n([\s\S]*?)\/\/ @endsection wear-sync\.js/)[1];
function harness({normalize=false,ackFailures=0}={}){
 const storage=new Map(),acks=[],pending=[];let savedCalls=0;
 const db={get:(k,d)=>storage.has(k)?structuredClone(storage.get(k)):d,set:(k,v)=>storage.set(k,structuredClone(v))};
 const session=()=>({started:'2026-10-07T08:00:00.000Z',programId:'home-basic',programName:'Otthoni A/B',dayId:'A',scheduleId:'sched-1',exercises:[{id:'squat',hu:'Guggolás',repUnit:'ism.',loadType:'weight',sets:[{set:1,reps:'',weight:5,done:false},{set:2,reps:'',weight:5,done:false}]}]});
 const state={session:session(),current:0,workout:'A'};
 const bridge={pendingCommands:async()=>({commands:pending.slice()}),ackCommand:async({commandId})=>{if(ackFailures-->0)throw Error('temporary ACK failure');acks.push(commandId);pending.splice(pending.findIndex(c=>c.commandId===commandId),1)},publish:async()=>{},clear:async()=>{},publishHome:async()=>{}};
 const window={Capacitor:{isNativePlatform:()=>true,Plugins:{WearSync:bridge}},addEventListener(){},scrollTo(){}};
 const ctx=vm.createContext({window,document:{addEventListener(){}},state,db,Date,console,setInterval(){},setTimeout(){},settings:()=>({rest:90}),history:()=>db.get('history',[]).map(x=>normalize?{...x,started:new Date(x.started).toISOString()}:x),
  activeProgram:()=>({id:'home-basic',name:'Otthoni A/B',days:[{id:'A',name:'Alap A',exercises:['squat']}]}),programById:()=>({id:'home-basic'}),programDay:()=>({id:'A',name:'Alap A'}),byId:()=>({id:'squat',hu:'Guggolás',sets:2,repUnit:'ism.'}),
  stopTimer(){state.restEndAt=null},render(){},renderWorkout(){},persistDraft(){if(state.session)db.set('draft',{session:state.session,current:state.current,workout:state.workout})},
  startWorkout(){state.session=session();state.workout='A'},
  toggleSet(ei,si){const s=state.session.exercises[ei].sets[si];if(Number(s.reps)>0)s.done=!s.done},
  finishWorkout(options){savedCalls++;assert.equal(options.wearConfirmed,true);const saved={...structuredClone(state.session),finished:options.wearFinishedAt||new Date().toISOString()};db.set('history',[saved,...db.get('history',[])]);db.set('draft',null);state.session=null}
 });
 vm.runInContext(source,ctx);
 return {state,db,pending,acks,sync:window.TrainPilotWearSync,session,savedCalls:()=>savedCalls};
}
const finish=(id='finish')=>({commandId:id,workoutId:'2026-10-07T08:00:00.000Z',action:'finishWorkout',confirmed:true,createdAt:30,sequence:3,finishedAt:'2026-10-07T08:42:16.000Z'});
(async()=>{
 {const h=harness();h.state.session.exercises[0].sets[0]={set:1,reps:'12',weight:6,done:true};h.pending.push(finish());await h.sync.drainCommands();assert.equal(h.db.get('history',[]).length,1);assert.equal(h.db.get('history',[])[0].finished,'2026-10-07T08:42:16.000Z');assert.equal(h.db.get('wearWorkoutResult').status,'saved');assert.deepEqual(h.acks,['finish']);h.pending.push(finish('retry'));await h.sync.drainCommands();assert.equal(h.savedCalls(),1);assert.equal(h.db.get('history',[]).length,1)}
 {const h=harness();h.pending.push(finish());await h.sync.drainCommands();assert.equal(h.db.get('history',[]).length,0);assert.equal(h.db.get('wearWorkoutResult').status,'error')}
 {const h=harness();h.pending.push({...finish(),confirmed:false});await h.sync.drainCommands();assert.equal(h.acks.length,0);assert.ok(h.state.session)}
 {const h=harness();const final=h.session();final.workoutId=final.started;final.exercises[0].sets[1]={set:2,reps:'8',weight:7,done:true};h.pending.push({...finish(),finalSnapshot:final});await h.sync.drainCommands();assert.equal(h.db.get('history',[])[0].exercises[0].sets[1].weight,7)}
 {const h=harness();const s=h.session();s.exercises[0].sets[0]={set:1,reps:'10',weight:5,done:true};h.db.set('draft',{session:s,current:0});h.state.session=null;assert.ok(h.sync.makeSnapshot());h.pending.push(finish());await h.sync.drainCommands();assert.equal(h.db.get('history',[]).length,1)}
 {const h=harness();h.db.set('history',[{started:'old',finished:'old'}]);h.pending.push({...finish('discard'),action:'discardWorkout'});await h.sync.drainCommands();assert.equal(h.state.session,null);assert.equal(h.db.get('draft',null),null);assert.equal(h.db.get('history',[]).length,1);assert.equal(h.db.get('wearWorkoutResult').status,'discarded')}
 {const h=harness();h.state.session.started='another';h.pending.push({...finish('wrong'),action:'discardWorkout'});await h.sync.drainCommands();assert.equal(h.state.session.started,'another');assert.equal(h.db.get('wearWorkoutResult').status,'error')}
 {const h=harness();h.state.session=null;h.pending.push({...finish('early-delete'),action:'discardWorkout'},{...finish('late-start'),action:'startWorkout',dayId:'A',programId:'home-basic',started:finish().workoutId,createdAt:10});await h.sync.drainCommands();assert.equal(h.state.session,null);assert.equal(h.db.get('wearWorkoutResult').status,'discarded')}
 {const h=harness();h.state.session=null;const final=h.session();final.workoutId=final.started;final.exercises[0].sets[0]={set:1,reps:'12',weight:5,done:true};h.pending.push({...finish('offline-final'),finalSnapshot:final});await h.sync.drainCommands();assert.equal(h.db.get('history',[]).length,1)}
 {const h=harness();h.pending.push(finish(),{commandId:'set',workoutId:finish().workoutId,action:'completeSet',exerciseIndex:0,setIndex:0,reps:'12',weight:5,createdAt:20,sequence:2});await h.sync.drainCommands();assert.deepEqual(h.acks,['set','finish']);assert.equal(h.db.get('history',[]).length,1)}
 {const h=harness();h.state.session.exercises[0].repUnit='mp/oldal';h.pending.push({commandId:'side',workoutId:finish().workoutId,action:'completeSet',exerciseIndex:0,setIndex:0,reps:'',leftSeconds:30,rightSeconds:35,createdAt:20});await h.sync.drainCommands();assert.equal(h.state.session.exercises[0].sets[0].reps,'30');assert.equal(h.state.session.exercises[0].sets[0].done,true)}
 {const id='2026-10-07T08:00:00.123456789Z',h=harness({normalize:true,ackFailures:50});h.state.session=null;
  const final=h.session();final.started=id;final.workoutId=id;final.exercises[0].sets[0]={set:1,reps:'12',weight:5,done:true};
  h.pending.push({...finish('nano-offline'),workoutId:id,finalSnapshot:final});
  for(let i=0;i<51;i++)await h.sync.drainCommands();
  assert.equal(h.savedCalls(),1,'nanosecond offline finish remains one save through 50 failed ACKs');assert.equal(h.db.get('history',[])[0].syncId,id);assert.equal(h.db.get('wearWorkoutResult').status,'saved');assert.deepEqual(h.acks,['nano-offline']);
  h.db.set('wearClosedWorkouts',[]);h.pending.push({...finish('new-retry-id'),workoutId:id,finalSnapshot:final});await h.sync.drainCommands();assert.equal(h.savedCalls(),1,'persisted syncId survives loss of the receipt cache');
 }
 {const id='2026-10-07T08:00:00.123456789Z',h=harness({normalize:true});const old=h.session();old.started=id;old.finished=finish().finishedAt;h.db.set('history',[old]);h.state.session.started=id;h.db.set('draft',{session:h.state.session,current:0});h.pending.push({...finish('legacy-replay'),workoutId:id});await h.sync.drainCommands();assert.equal(h.savedCalls(),0,'legacy normalized timestamp is recognized');assert.equal(h.state.session,null);assert.equal(h.db.get('draft',null),null);assert.equal(h.db.get('wearWorkoutResult').status,'saved');assert.deepEqual(h.acks,['legacy-replay'])}
 {const h=harness();h.state.session.syncId='one-stable-id';h.pending.push({...finish('wrong-stable-id'),workoutId:'another-stable-id'});await h.sync.drainCommands();assert.equal(h.savedCalls(),0);assert.equal(h.state.session.syncId,'one-stable-id')}
 console.log('PASS Wear lifecycle including nanosecond timestamps, 50 failed ACKs, persisted identity and legacy save recovery.');
})().catch(e=>{console.error(e);process.exitCode=1});
