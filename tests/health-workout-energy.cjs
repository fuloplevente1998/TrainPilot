'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.existsSync('www/issue79-health-active-intervals.js')?fs.readFileSync('www/issue79-health-active-intervals.js','utf8'):fs.readFileSync('www/app.js','utf8').split('// @section issue79-health-active-intervals.js\n')[1].split('// @endsection issue79-health-active-intervals.js')[0];
let saved,calls=0;
const context=vm.createContext({window:{},Date,JSON,Number,Array,Object,Set,Error,
 rf240ValidWorkout:()=>true,rf240WorkoutKey:h=>h.id,rf242HasTrainingMetric:x=>x?.maxHeartRate!=null,
 rf242SaveMatchedWorkoutHealth:(_key,_h,data)=>{saved=data;return true}});
vm.runInContext(source,context);
const h={id:'samsung-midnight',started:'2026-10-07T22:42:00Z',finished:'2026-10-08T00:08:00Z',activeIntervals:[{start:'2026-10-07T22:42:00Z',end:'2026-10-07T23:00:00Z'},{start:'2026-10-07T23:15:00Z',end:'2026-10-08T00:08:00Z'}]};
const plugin={readTrainingWindow:async()=>{const first=calls++===0;return {workoutEnergyVersion:1,workoutCalories:first?112:280,workoutEnergyCoverageMs:(first?14:40)*60000,workoutEnergyProrated:false,workoutEnergySources:['samsung'],workoutEnergySourceLabels:{samsung:'Samsung Health'},workoutEnergyTypes:['total'],workoutEnergyRecordIds:[first?'first':'second'],activeCalories:first?10:16,totalCalories:first?120:253,maxHeartRate:162,averageHeartRate:111,heartRateSamples:first?100:200,exerciseSessions:[{id:'one',source:'samsung',start:'2026-10-07T22:43:00Z',end:'2026-10-07T22:57:00Z'},{id:'two',source:'samsung',start:'2026-10-07T23:15:00Z',end:'2026-10-07T23:55:00Z'}]}}};
(async()=>{
 await context.rf240SyncWorkout(plugin,h,true);
 assert.equal(calls,2);assert.equal(saved.workoutCalories,392);assert.equal(saved.activeCalories,26);assert.equal(saved.totalCalories,373);assert.equal(saved.maxHeartRate,162);
 assert.equal(saved.workoutEnergyCoverageMs,54*60000);assert.equal(saved.exerciseMinutes,54);assert.equal(saved.exerciseSessionCount,2);assert.equal(saved.workoutEnergyVersion,1);
 assert.deepEqual(Array.from(saved.workoutEnergySources),['samsung']);assert.deepEqual(Array.from(saved.workoutEnergyRecordIds),['first','second']);
 // Pre-fix HR-only cache must be refreshed, even when less than six hours old.
 calls=0;h.health240={maxHeartRate:162,syncedAt:new Date().toISOString(),activeIntervalSignature:JSON.stringify(h.activeIntervals.map(w=>[w.start,w.end]))};
 await context.rf240SyncWorkout(plugin,h,false);assert.equal(calls,2);assert.equal(saved.workoutCalories,392);
 const missing=await context.window.TrainPilotIssue79.readWorkout({readTrainingWindow:async()=>({workoutEnergyVersion:1,workoutCalories:null,maxHeartRate:162})},h);
 assert.equal(missing.workoutCalories,null);assert.equal(missing.maxHeartRate,162);
 // A failure does not invent calories for the missing segment.
 let count=0;
 const partial=await context.window.TrainPilotIssue79.readWorkout({readTrainingWindow:async()=>{if(++count===1)throw Error('segment missing');return {workoutEnergyVersion:1,workoutCalories:280,workoutEnergyProrated:true,maxHeartRate:162}}},h);
 assert.equal(partial.workoutCalories,280);assert.equal(partial.workoutEnergyProrated,true);assert.ok(partial.warnings.includes('segment missing'));
 console.log('PASS workout calories: Samsung392/active26/total373 stay distinct, maxHR162, midnight/paused windows, migration, missing and partial reads');
})().catch(error=>{console.error(error);process.exitCode=1});
