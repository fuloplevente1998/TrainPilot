'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const app=fs.readFileSync('www/app.js','utf8');
const source=fs.existsSync('www/wear-sync.js')?fs.readFileSync('www/wear-sync.js','utf8'):
 app.match(/\/\/ @section wear-sync\.js\n([\s\S]*?)\/\/ @endsection wear-sync\.js/)[1];
const start='2026-10-09T20:00:00Z',finish='2026-10-09T20:45:00Z';
const exercise={id:'pushup',hu:'Fekvőtámasz',en:'Push-up',loadType:'bodyweight',repUnit:'ism.',sets:2,reps:15};
const workout={
 started:start,finished:finish,syncId:'watch-001',dayId:'A',programName:'Otthoni A/B',
 exercises:[{id:'pushup',hu:'Fekvőtámasz',sets:[{set:1,reps:'15',weight:0,done:true},{set:2,reps:'',weight:null,done:false}]}],
 photos:[{secret:'should-never-go-to-watch'}],
 healthWear129:{source:'wear_health_services',workoutId:'watch-001',watchId:'watch7',revision:4,
 averageHeartRate:121.2,maxHeartRate:158,totalCalories:390,steps:0,activeDurationSeconds:2700},
 health240:{provider:'health_connect',windowStart:start,windowEnd:finish,averageHeartRate:121,
 totalCalories:49,activeCalories:1,exerciseMinutes:0,healthLedger:{sensitive:true}}
};
const program={id:'p',name:'Otthoni A/B',days:[{id:'A',name:'Alap',exercises:['pushup']}]};
const store=new Map(),rows=[workout];
const db={get:(key,fallback)=>store.has(key)?structuredClone(store.get(key)):fallback,
 set:(key,value)=>store.set(key,structuredClone(value))};
const win={Capacitor:{isNativePlatform:()=>false},addEventListener(){}};
const ctx=vm.createContext({window:win,document:{addEventListener(){}},state:{session:null},
 db,Date,console,setInterval(){},setTimeout(){},
 history:()=>rows,exercises:()=>[exercise],activeProgram:()=>program,
 programDay:(p,id)=>p?.days?.find(d=>d.id===id),programById:()=>program,
 byId:id=>id==='pushup'?exercise:null,scheduled:()=>[],nextPlanned:()=>null,nextWorkout:()=> 'A',
 settings:()=>({rest:90})});
vm.runInContext(source,ctx);
const snapshot=()=>JSON.parse(JSON.stringify(win.TrainPilotWearSync.makeHomeSnapshot()));
{
 const home=snapshot();
 assert.equal(home.recentWorkouts.length,1);
 const item=home.recentWorkouts[0];
 assert.equal(item.workoutId,'watch-001');assert.equal(item.exercises.length,1);
 assert.equal(item.exercises[0].sets.length,1,'unfinished sets do not appear as completed');
 assert.equal(item.exercises[0].sets[0].reps,'15');
 assert.equal(item.wear.totalCalories,390);assert.equal(item.wear.steps,0);
 assert.equal(item.healthConnect.totalCalories,49);
 assert.equal(item.healthConnect.activeCalories,1);
 assert.equal(item.healthConnect.exerciseMinutes,0);
 assert.equal(item.healthConnect.workoutCalories,undefined,'window total is not a workout-specific calorie');
 assert.equal(JSON.stringify(home).includes('sensitive'),false);
 assert.equal(JSON.stringify(home).includes('should-never-go-to-watch'),false);
}
{
 rows.push(structuredClone(workout));
 assert.equal(snapshot().recentWorkouts.length,1,'duplicate deliveries do not create duplicate watch history');
 rows.pop();
}
{
 rows[0].healthWear129={source:'health_connect',workoutId:'watch-001',watchId:'watch7',revision:5,totalCalories:450};
 assert.equal(snapshot().recentWorkouts[0].wear,null,'wrong Wear source is not trusted');
 delete rows[0].healthWear129;
 assert.equal(snapshot().recentWorkouts[0].wear,null,'missing measurement stays missing');
 rows[0].health240.windowEnd='outdated';
 assert.equal(snapshot().recentWorkouts[0].healthConnect,null,'stale HC attachment is excluded');
}
{
 rows.splice(0,rows.length);
 for(let i=0;i<24;i++){
  rows.push({...workout,syncId:'history-'+i,started:new Date(Date.parse(start)+i*60000).toISOString(),
   finished:new Date(Date.parse(finish)+i*60000).toISOString(),
   healthWear129:null,health240:null});
 }
 assert.equal(snapshot().recentWorkouts.length,8,'only eight finished workouts are sent');
 rows.splice(0,rows.length);
 for(let i=0;i<8;i++)rows.push({
  ...workout,syncId:'huge-'+i,started:new Date(Date.parse(start)+i*60000).toISOString(),
  finished:new Date(Date.parse(finish)+i*60000).toISOString(),
  healthWear129:null,health240:null,
  exercises:Array.from({length:20},(_,j)=>({
   id:'pushup',hu:'Fekvőtámasz '+j,sets:Array.from({length:20},(_,k)=>({set:k+1,reps:'15',done:true,weight:0}))
  }))
 });
 const home=snapshot();
 assert.ok(JSON.stringify(home.recentWorkouts).length<33000,'wear mini journal has a hard payload budget');
}
console.log('PASS Wear mini-journal: canonical history, 390 vs 49 provenance, missing/zero, exercise sets, duplicate/stale filters and payload budget');
