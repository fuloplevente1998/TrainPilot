const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const app=fs.readFileSync('www/app.js','utf8');
const source=fs.existsSync('www/wear-sync.js')?fs.readFileSync('www/wear-sync.js','utf8'):
 app.match(/\/\/ @section wear-sync\.js\n([\s\S]*?)\/\/ @endsection wear-sync\.js/)[1];
const id='2026-10-09T19:10:00.000Z';
const library=[
 {id:'pushup',hu:'Fekvőtámasz',en:'Push-up',loadType:'bodyweight',repUnit:'ism.',reps:'15',sets:2,weight:0},
 {id:'plank',hu:'Plank',en:'Plank',loadType:'bodyweight',repUnit:'mp',reps:'30',sets:1,weight:0},
 {id:'custom-row',hu:'Saját evezés',en:'Custom row',loadType:'weight',repUnit:'ism.',reps:'10',sets:3,weight:12}
];
const mkExercise=x=>({id:x.id,hu:x.hu,en:x.en,repUnit:x.repUnit,loadType:x.loadType,
 sets:Array.from({length:x.sets},(_,i)=>({set:i+1,weight:x.weight,reps:'',done:false}))});
function harness({draft=false}={}){
 const store=new Map(),pending=[],acks=[],state={session:null,workout:null,tab:'home',current:0};
 const db={get:(k,d)=>store.has(k)?structuredClone(store.get(k)):d,set:(k,v)=>store.set(k,structuredClone(v))};
 if(draft)db.set('draft',{session:{started:'other',exercises:[mkExercise(library[0])]},workout:'A',current:0});
 let saved=0,starts=0,added=0;
 const window={
  Capacitor:{isNativePlatform:()=>true,Plugins:{WearSync:{
   pendingCommands:async()=>({commands:pending.slice()}),ackCommand:async({commandId})=>{
    acks.push(commandId);const index=pending.findIndex(x=>x.commandId===commandId);if(index>=0)pending.splice(index,1);
   },publish:async()=>{},publishHome:async()=>{},clear:async()=>{}
  }}},
  addEventListener(){},scrollTo(){},
  TrainPilotQuickWorkout:{
   start:async exerciseId=>{
    starts++;
    const e=library.find(x=>x.id===exerciseId);if(!e||state.session)return;
    state.session={type:'quick',quickWorkout:true,programId:null,programName:'Gyors edzés',
     dayId:null,workout:'quick',started:new Date().toISOString(),exercises:[mkExercise(e)]};
    state.current=0;state.workout='quick';db.set('draft',{session:state.session,current:0,workout:'quick'});
   },
   add:exerciseId=>{
    const e=library.find(x=>x.id===exerciseId);if(!e||!state.session)return;
    const found=state.session.exercises.findIndex(x=>x.id===e.id);
    if(found>=0)state.current=found;else {state.session.exercises.push(mkExercise(e));state.current=state.session.exercises.length-1;}
    added++;
   }
  }
 };
 const active={id:'p',name:'Program',days:[{id:'A',name:'Első nap',exercises:['pushup']}]};
 const context=vm.createContext({window,document:{addEventListener(){}},state,db,Date,console,
  setInterval(){},setTimeout(){},settings:()=>({rest:75}),exercises:()=>library,
  history:()=>db.get('history',[]),activeProgram:()=>active,
  programById:()=>active,programDay:()=>active.days[0],byId:key=>library.find(e=>e.id===key),
  nextWorkout:()=> 'A',nextPlanned:()=>null,scheduled:()=>[],
  tp150IsQuick:session=>!!session&&(session.type==='quick'||session.quickWorkout===true),
  stopTimer(){state.restEndAt=null},render(){},renderWorkout(){},
  persistDraft(){if(state.session)db.set('draft',{session:state.session,current:state.current,workout:state.workout})},
  toggleSet(ei,si){const set=state.session.exercises[ei].sets[si];if(Number(set.reps)>0)set.done=!set.done},
  finishWorkout(options){assert.equal(options.wearConfirmed,true);saved++;
   db.set('history',[{...structuredClone(state.session),finished:options.wearFinishedAt||new Date().toISOString()},...db.get('history',[])]);
   db.set('draft',null);state.session=null;state.workout=null;
  }
 });
 vm.runInContext(source,context);
 return {state,db,pending,acks,sync:window.TrainPilotWearSync,stats:()=>({saved,starts,added})};
}
const command=(action,commandId,rest={})=>({schema:1,commandId,action,workoutId:id,createdAt:Date.now(),...rest});
const health=revision=>({schema:1,source:'wear_health_services',workoutId:id,watchId:'watch7',
 revision,averageHeartRate:94.6,maxHeartRate:105,totalCalories:2,steps:0});
const finalSnapshot=()=>({workoutId:id,started:id,quickWorkout:true,dayId:'quick',programId:'',
 exercises:[{id:'pushup',sets:[{set:1,reps:'15',weight:0,done:true}]},
            {id:'plank',sets:[{set:1,reps:'30',weight:0,done:true}]}]});
const finish=key=>command('finishWorkout',key,{confirmed:true,finishedAt:'2026-10-09T19:15:00Z',
 finalSnapshot:finalSnapshot(),healthSummary:health(3)});
(async()=>{
 {const h=harness();const home=h.sync.makeHomeSnapshot();
  assert.equal(home.quickExercises.length,library.length);
  assert.equal(home.quickExercises[0].name,'Fekvőtámasz');
  assert.equal(home.quickExercises[2].id,'custom-row');
  assert.equal(home.quickExercises[0].sets.length,2);
 }
 {const h=harness();
  h.pending.push(command('startQuickWorkout','start',{exerciseId:'pushup',started:id,sequence:1}));
  await h.sync.drainCommands();
  assert.deepEqual(h.acks,['start']);assert.equal(h.stats().starts,1);
  assert.equal(h.state.session?.type,'quick');assert.equal(h.state.session?.quickWorkout,true);
  assert.equal(h.state.session?.syncId,id);assert.equal(h.state.session?.started,id);
  assert.equal(h.sync.makeSnapshot().workoutId,id);
  assert.equal(h.sync.makeSnapshot().dayName,'Gyors edzés');
  assert.equal(h.sync.makeSnapshot().quickWorkout,true);
  h.pending.push(command('addQuickExercise','add',{exerciseId:'plank',exerciseIndex:1,sequence:2}));
  await h.sync.drainCommands();assert.equal(h.state.session?.exercises.length,2);
  h.pending.push(command('addQuickExercise','replay-add',{exerciseId:'plank',sequence:3}));
  await h.sync.drainCommands();assert.equal(h.state.session?.exercises.length,2);
  h.pending.push(finish('finish'));await h.sync.drainCommands();
  const row=h.db.get('history',[])[0];assert.equal(h.db.get('history',[]).length,1);
  assert.equal(row.quickWorkout,true);assert.equal(row.type,'quick');
  assert.deepEqual(row.exercises.map(e=>e.id),['pushup','plank']);
  assert.equal(row.exercises[0].sets[0].reps,'15');assert.equal(row.exercises[0].sets[0].done,true);
  assert.equal(row.healthWear129.totalCalories,2);assert.equal(row.healthWear129.steps,0);
  h.pending.push(finish('finish-again'));await h.sync.drainCommands();
  assert.equal(h.stats().saved,1);assert.equal(h.db.get('history',[]).length,1);
  h.pending.push(command('healthSummary','late-health',{healthSummary:{...health(4),totalCalories:3}}));
  await h.sync.drainCommands();assert.equal(h.db.get('history',[])[0].healthWear129.totalCalories,3);
 }
 {const h=harness();h.pending.push(finish('offline-final'));await h.sync.drainCommands();
  assert.equal(h.db.get('history',[]).length,1,'final quick snapshot reconstructs offline session');
  assert.equal(h.stats().starts,1);assert.equal(h.stats().added,1);
  assert.equal(h.db.get('history',[])[0].quickWorkout,true);
 }
 {const h=harness({draft:true});h.pending.push(command('startQuickWorkout','blocked',{exerciseId:'pushup',started:id}));
  await h.sync.drainCommands();assert.equal(h.stats().starts,0);assert.equal(h.acks.length,0);
  assert.equal(h.db.get('draft').session.started,'other');
 }
 {const h=harness();h.pending.push(command('startQuickWorkout','unknown',{exerciseId:'does-not-exist',started:id}));
  await h.sync.drainCommands();assert.equal(h.acks.length,0);assert.equal(h.stats().starts,0);
 }
 {const h=harness();h.pending.push(command('addQuickExercise','orphan-add',{exerciseId:'plank'}));
  await h.sync.drainCommands();assert.equal(h.acks.length,0);
 }
 console.log('PASS Wear Quick Workout: synced library, pushups, add exercise, offline finish, health merge, replay and draft guards');
})().catch(error=>{console.error(error);process.exitCode=1});
