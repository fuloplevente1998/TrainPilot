const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const all=fs.readFileSync('www/app.js','utf8');\nconst m=all.match(/\/\/ @section issue79-health-active-intervals\.js\\n([\\s\\S]*?)\/\/ @endsection issue79-health-active-intervals\.js/);assert.ok(m,'#79 runtime section');const src=m[1];
let saved=null;
const ctx=vm.createContext({window:{},Date,JSON,Number,Array,Object,Set,Error,
 rf240ValidWorkout:h=>Date.parse(h.finished)-Date.parse(h.started)<=86400000,
 rf240WorkoutKey:h=>h.id,
 rf242HasTrainingMetric:x=>x&&x.averageHeartRate!=null,
 rf242BestSession:()=>null,
 rf242SaveMatchedWorkoutHealth:(k,h,d,m)=>{saved={k,h,d,m};return true}
});
vm.runInContext(src,ctx);
const h={id:'overnight',started:'2026-09-28T20:00:00.000Z',finished:'2026-09-29T08:20:00.000Z',activeIntervals:[
 {start:'2026-09-28T20:00:00.000Z',end:'2026-09-28T20:10:00.000Z'},
 {start:'2026-09-29T08:00:00.000Z',end:'2026-09-29T08:20:00.000Z'}]};
assert.equal(ctx.rf240ValidWorkout(h),true);
const calls=[];
const p={readTrainingWindow:async w=>{calls.push(w);return calls.length===1?{averageHeartRate:100,maxHeartRate:130,activeCalories:40,distanceMeters:500,sources:['watch'],sourceLabels:{watch:'Watch'},warnings:[]}:{averageHeartRate:120,maxHeartRate:150,activeCalories:80,distanceMeters:1000,sources:['watch'],sourceLabels:{watch:'Watch'},warnings:[]}}};
(async()=>{
 await ctx.rf240SyncWorkout(p,h,true);
 assert.equal(calls.length,2,'only two active intervals are queried');
 assert.deepEqual(calls.map(x=>[x.start,x.end]),h.activeIntervals.map(x=>[x.start,x.end]));
 assert.equal(Math.round(saved.d.averageHeartRate),113,'HR is active-duration weighted');
 assert.equal(saved.d.maxHeartRate,150);assert.equal(saved.d.activeCalories,120);assert.equal(saved.d.distanceMeters,1500);
 assert.equal(saved.d.activeDurationMs,30*60*1000);assert.equal(saved.m.matchMode,'active-intervals');
 let n=0;const partial={readTrainingWindow:async()=>{n++;if(n===1)throw Error('bad segment');return {averageHeartRate:111,activeCalories:55,warnings:[]}}};
 await ctx.rf240SyncWorkout(partial,h,true);assert.equal(saved.d.averageHeartRate,111);assert.ok(saved.d.warnings.some(x=>x.includes('bad segment')));
 const legacy={id:'legacy',started:'2026-09-29T08:00:00.000Z',finished:'2026-09-29T08:30:00.000Z'};
 assert.equal(ctx.rf240ValidWorkout(legacy),true);
 console.log('PASS #79 active Health intervals, overnight resume, partial failure, legacy fallback');
})().catch(e=>{console.error(e);process.exitCode=1});