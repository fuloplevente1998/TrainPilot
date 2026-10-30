'use strict';
const assert=require('node:assert/strict');
const {run,context,windowEvents,documentEvents}=require('./helpers/app-runtime.cjs')();
const plain=x=>JSON.parse(JSON.stringify(x));
const flush=()=>new Promise(resolve=>setImmediate(resolve));
const fixture={id:'phone-stable-id',started:'2026-10-10T08:00:00.123456789Z',finished:'2026-10-10T08:01:00.000Z',
 type:'quick',quickWorkout:true,exercises:[{id:'pushup',hu:'Fekvőtámasz',sets:[{set:1,reps:10,done:true,weight:0}]}],
 healthWear129:{schema:1,source:'wear_health_services',workoutId:'phone-stable-id',watchId:'watch7',revision:4,totalCalories:4},
 health240:{windowStart:'2026-10-10T08:00:00.123456789Z',windowEnd:'2026-10-10T08:01:00.000Z',activeCalories:1,totalCalories:2}};
run('db.set("history",'+JSON.stringify([fixture])+');db.set("draft",null);state.session=null');
let commands=[],acks=[],sent=[],fail=false,held=null;
const bridge={clear:async()=>{},publish:async()=>{},pendingCommands:async()=>({commands:commands.slice()}),
 ackCommand:async({commandId})=>{acks.push(commandId);commands=commands.filter(c=>c.commandId!==commandId)},
 publishHome:async({snapshot})=>{if(fail)throw Error('temporary transport failure');sent.push(plain(snapshot));if(held)await held.promise;}};
context.window.Capacitor={isNativePlatform:()=>true,Plugins:{WearSync:bridge}};
const sync=context.window.TrainPilotWearSync;
(async()=>{
 const home=plain(sync.makeHomeSnapshot());
 assert.equal(home.recentWorkouts.length,1,'the full phone runtime must project a saved workout');
 assert.equal(home.recentWorkouts[0].workoutId,'phone-stable-id');
 assert.equal(home.recentWorkouts[0].wear.totalCalories,4,'same identity as the Data Layer session');
 assert.equal(home.recentWorkouts[0].healthConnect.totalCalories,2,'equivalent timestamp precision keeps the separate HC source');
 assert.match(run('rfHistoryHealthHtml(0)'),/4 kcal/,'show measured Wear 4 kcal even when HC has total 2 and active 1');
 assert.doesNotMatch(run('rfHistoryHealthHtml(0)'),/nem érkezett külön órás/);
 await sync.syncHomeNow();assert.equal(sent.length,1);
 await sync.syncHomeNow();assert.equal(sent.length,1,'routine polling still avoids unchanged transfers');
 sent=[]; // Simulate reinstall/cache loss on the watch while the phone stays alive.
 for(const listener of windowEvents.get('focus')||[])listener();await flush();
 assert.equal(sent.length,1,'reopening the phone resends unchanged history to a watch with no cache');
 sent=[];
 for(const listener of documentEvents.get('visibilitychange')||[])listener();await flush();
 assert.equal(sent.length,1,'returning the WebView to foreground also resends unchanged history');
 const before=run("localStorage.getItem('repforge:history')");
 commands=[{commandId:'journal-refresh',action:'requestHome',createdAt:1}];
 sent=[];await sync.drainCommands();
 assert.equal(sent.length,1,'an explicit watch request bypasses the unchanged-payload cache');
 assert.deepEqual(acks,['journal-refresh']);
 assert.equal(run("localStorage.getItem('repforge:history')"),before,'refresh is read-only');
 commands=[{commandId:'retry-refresh',action:'requestHome',createdAt:2}];fail=true;
 await sync.drainCommands();assert.equal(commands.length,1,'failed delivery must not acknowledge the watch request');
 fail=false;await sync.drainCommands();assert.equal(commands.length,0);
 assert.deepEqual(acks,['journal-refresh','retry-refresh']);
 run("activeProgram=()=>null");
 assert.equal(sync.makeHomeSnapshot().recentWorkouts.length,1,'saved history also syncs without an active program');
 run("exercises=()=>Array.from({length:240},(_,i)=>({id:'custom-'+i,hu:'Árvíztűrő tükörfúrógép '.repeat(100),sets:10,reps:'10',loadType:'weight'}))");
 const large=plain(sync.makeHomeSnapshot());
 assert.ok(Buffer.byteLength(JSON.stringify(large),'utf8')<100000,'full accented payload fits one DataItem');
 assert.equal(large.recentWorkouts.length,1,'an oversized catalog cannot hide saved workouts');
 const tasks=[];context.setTimeout=fn=>tasks.push(fn);
 let release;held={promise:new Promise(r=>release=r)};
 const first=sync.syncHomeNow({force:true}),count=sent.length;
 assert.equal(await sync.syncHomeNow({force:true}),false);
 held=null;release();await first;
 assert.equal(tasks.length,1,'a foreground request during an in-flight send is retained');
 await tasks.shift()();assert.equal(sent.length,count+1);
 console.log('PASS full runtime Wear Journal: phone foreground/cache loss, explicit request, failed transport retry, identity-matched 4 vs 2 kcal, no active program and UTF-8 payload limits');
})().catch(error=>{console.error(error);process.exitCode=1});
