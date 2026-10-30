'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
process.env.TZ='Europe/Budapest';
const bundled=!fs.existsSync('www/features-110.js'),raw=fs.readFileSync(bundled?'www/app.js':'www/features-110.js','utf8');
const section=bundled?raw.match(/\/\/ @section features-110.js\n([\s\S]*?)\/\/ @endsection features-110.js/)[1]:raw;
const context=vm.createContext({window:{addEventListener(){}},Date});vm.runInContext(section,context);
const {planWeeks}=context.window.TrainPilot110Core,plain=x=>JSON.parse(JSON.stringify(x));
const row=(id,date,programId='old-program',status='planned')=>({id,programId,dayId:'A',workout:'A',start:new Date(date+'T18:00:00').toISOString(),end:new Date(date+'T18:45:00').toISOString(),status,cancelled:status==='cancelled',updatedAt:1});
const rows=[row('keep-date','2026-10-10'),row('move-date','2026-10-11'),row('remove-date','2026-10-13'),row('other-program','2026-10-20','other'),row('done','2026-10-12','old-program','completed'),row('logged','2026-10-14'),row('active','2026-10-16'),row('draft','2026-10-18'),row('deleted','2026-10-22','old-program','cancelled'),row('outside-before','2026-10-09'),row('outside-after','2026-10-24')];
const logs=[{scheduleId:'logged',started:rows[5].start,exercises:[{sets:[{done:true,reps:10}]}]}];
let serial=0;const options={programId:'personalized',programDays:[{id:'A'},{id:'B'}],startDate:'2026-10-10',weeks:2,mode:'alternate',time:'19:20',minutes:60,firstIndex:1,activeScheduleIds:['active','draft'],now:new Date('2026-10-09T12:00:00+02:00'),newId:()=> 'new-'+(++serial)};
const snapshot=JSON.stringify([rows,logs]),result=planWeeks(rows,logs,options);
assert.equal(JSON.stringify([rows,logs]),snapshot,'planning is pure until storage commit');
assert.deepEqual(plain(result.moved.map(x=>[x.after.id,x.after.programId,x.after.dayId])),[['keep-date','personalized','B'],['other-program','personalized','A'],['move-date','personalized','B']]);
assert.deepEqual(plain(result.moved.map(x=>new Date(x.after.start).getDate())),[10,20,22]);
for(const {after} of result.moved){assert.equal(new Date(after.start).getHours(),19);assert.equal(new Date(after.start).getMinutes(),20);assert.equal(Date.parse(after.end)-Date.parse(after.start),3600000);}
assert.deepEqual(plain(result.removed.map(x=>x.id)),['remove-date']);assert.equal(result.rows[2].cancelled,true);assert.equal(result.rows[2].status,'cancelled');
for(let i=4;i<rows.length;i++)assert.deepEqual(plain(result.rows[i]),rows[i],'protected/tombstones/outside rows remain unchanged');
assert.deepEqual(plain(planWeeks(result.rows,logs,options).rows),plain(result.rows),'repeat planning does not add duplicates or churn timestamps');
const onlyActive=planWeeks(rows,logs,{...options,programId:'old-program',replaceOtherPrograms:false});assert.deepEqual(plain(onlyActive.rows[3]),rows[3]);
for(const mode of ['daily','alternate','weekly']){
 const planned=planWeeks([],[],{...options,mode,weekdays:[1,3,5],startDate:'2026-10-20',weeks:2});
 assert.equal(planned.moved.length,mode==='daily'?14:mode==='alternate'?7:6);
 assert.deepEqual(plain(planWeeks(planned.rows,[],{...options,mode,weekdays:[1,3,5],startDate:'2026-10-20',weeks:2}).rows),plain(planned.rows));
 for(const {after} of planned.moved)assert.equal(new Date(after.start).getHours(),19,'local time survives DST');
}
const full=Array.from({length:7},(_,i)=>row('blocked-'+i,'2026-10-'+String(10+i).padStart(2,'0'),'old-program','completed'));
assert.deepEqual(plain(planWeeks(full,[],{...options,weeks:1}).rows),full,'no available slot cannot destroy the old calendar');
for(const extra of [{startDate:'2026-02-30'},{weeks:0},{minutes:0},{time:'25:00'},{programDays:[]},{mode:'weekly',weekdays:[]}])assert.throws(()=>planWeeks(rows,logs,{...options,...extra}));
assert.throws(()=>planWeeks([],[],{...options,newId:()=> 'duplicate'}),/saveFailed/);
console.log('PASS multiweek replacement: old/all programs become the active plan, stable/moved IDs, surplus tombstones, protected history/active/draft, outside window, repeat saves, daily/alternate/weekly and local DST times, invalid input and ID collisions');
