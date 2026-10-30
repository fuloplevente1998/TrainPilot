'use strict';
const assert=require('node:assert/strict'),runtime=require('./helpers/app-runtime.cjs');
const watchKey='wear193:'+'a'.repeat(64);
const workout=()=>({syncId:'w193',started:'2026-10-18T10:00:00Z',ownershipProtocol193:1,timerOwner:'watch',notes:'retain',exercises:[{id:'running',sets:[{setId:'set193',reps:'42',done:false,gpsRouteKey163:'phone-gps193',note:'keep'}]}]});
const binding=()=>({workoutId:'w193',exerciseId:'running',setId:'set193',routeKey:watchKey,pointCount:3});
const f=runtime(),core=f.context.window.TrainPilotWearRoutes193Core;
const initial=workout(),text=JSON.stringify(initial),next=core.merge(initial,[binding()]);
assert.equal(JSON.stringify(initial),text);assert.equal(next.exercises[0].sets[0].gpsRouteKey193,watchKey);
assert.equal(next.exercises[0].sets[0].gpsRouteKey163,'phone-gps193');assert.equal(next.exercises[0].sets[0].reps,'42');assert.equal(next.exercises[0].sets[0].note,'keep');
assert.equal(core.merge(next,[binding()]),null);
for(const bad of [{...binding(),workoutId:'another'},{...binding(),setId:'another'},{...binding(),exerciseId:'another'},{...binding(),routeKey:'../private'},{...binding(),pointCount:0},{...binding(),pointCount:12001}])assert.equal(core.merge(initial,[bad]),null);
assert.equal(core.merge({...initial,ownershipProtocol193:0},[binding()]),null);
function fixture(){
 const f=runtime();f.context.input=workout();f.run('state.session=input;state.workout="quick";state.current=0;persistDraft();originalSet193=state.session.exercises[0].sets[0]');
 const calls=[];f.context.window.Capacitor={isNativePlatform:()=>true,Plugins:{DistanceTracker:{routeBindings193:async({workoutId})=>{calls.push(workoutId);return {workoutId,bindings:[binding()]};}}}};
 return {...f,calls};
}
(async()=>{
 const live=fixture();assert.equal(await live.context.window.TrainPilotWearRoutes193.sync('w193'),true);
 assert.equal(live.run('state.session.exercises[0].sets[0].gpsRouteKey193'),watchKey);assert.equal(live.run('originalSet193===state.session.exercises[0].sets[0]'),true);
 assert.equal(live.run('db.get("draft").session.exercises[0].sets[0].gpsRouteKey193'),watchKey);
 const quota=fixture(),set=quota.context.localStorage.setItem;quota.context.localStorage.setItem=(k,v)=>{if(k==='repforge:draft')throw Error('quota');set(k,v);};
 assert.equal(await quota.context.window.TrainPilotWearRoutes193.sync('w193'),false);assert.equal(quota.run('state.session.exercises[0].sets[0].gpsRouteKey193'),undefined);
 const archived=fixture();archived.run('db.set("history",[{...state.session,finished:"2026-10-18T10:10:00Z"}]);state.session=null;db.set("draft",null);');
 assert.equal(await archived.context.window.TrainPilotWearRoutes193.sync('w193'),true);assert.equal(archived.run('db.get("history")[0].exercises[0].sets[0].gpsRouteKey193'),watchKey);assert.equal(archived.run('state.session'),null);
 const removed=fixture();let release;removed.context.window.Capacitor.Plugins.DistanceTracker.routeBindings193=()=>new Promise(r=>release=r);
 const flight=removed.context.window.TrainPilotWearRoutes193.sync('w193');removed.run('state.session=null;db.set("draft",null);db.set("history",[]);');release({workoutId:'w193',bindings:[binding()]});
 assert.equal(await flight,false);assert.equal(removed.run('db.get("history").length'),0);
 const legacy=fixture();legacy.run('state.session.ownershipProtocol193=0');assert.equal(await legacy.context.window.TrainPilotWearRoutes193.sync('w193'),false);assert.equal(legacy.calls.length,0);
 const discard=fixture(),erased=[];discard.context.window.Capacitor.Plugins.DistanceTracker.deleteWorkoutRoutes193=async({workoutId})=>{erased.push(workoutId);return {deleted:true};};
 discard.context.window.TrainPilotPhoneExecution162.discardFence=async(session,before,work)=>await before()&&work();
 discard.context.window.TrainPilotAutomatic162={command:async()=>true};discard.context.window.TrainPilotIntervals162={stop:async()=>true};
 discard.run('render=function(){};tp107StopIndoor=function(){};rf110StopwatchHalt=function(){};tp1481HaltSide=function(){};');
 assert.equal(await discard.context.window.TrainPilotWearSync.discardDraft(discard.run('state.session')),true);
 assert.deepEqual(erased,['w193']);assert.equal(discard.run('state.session'),null);
 console.log('PASS #193 route bindings: stable IDs, phone/watch references coexist, coordinate-free atomic draft/history import, failed storage and removed workout fencing');
})().catch(e=>{console.error(e);process.exitCode=1;});
