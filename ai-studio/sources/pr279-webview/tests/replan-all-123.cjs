'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
process.env.TZ='Europe/Budapest';
const bundled=!fs.existsSync('www/features-110.js'),source=fs.readFileSync(bundled?'www/app.js':'www/features-110.js','utf8');
const section=bundled?source.match(/\/\/ @section features-110.js\n([\s\S]*?)\/\/ @endsection features-110.js/)[1]:source;
const context=vm.createContext({window:{addEventListener(){}},Date});vm.runInContext(section,context);
const {replan}=context.window.TrainPilot110Core,plain=x=>JSON.parse(JSON.stringify(x)),now=new Date('2026-10-06T13:31:00+02:00');
let serial=0;const opts={programId:'personal-123',programDays:[{id:'A'},{id:'B'}],startDate:'2026-10-07',mode:'alternate',time:'18:00',minutes:45,now,newId:()=> 'new123-'+(++serial)};
const item=(id,date,dayId='A',status='planned')=>({id,programId:opts.programId,dayId,workout:dayId,start:new Date(date+'T08:00').toISOString(),end:new Date(date+'T08:45').toISOString(),status,cancelled:status==='cancelled',updatedAt:1});
const empty=replan([],[],opts);assert.equal(empty.moved.length,2);assert.deepEqual(plain(empty.rows.map(x=>x.dayId)),['A','B']);assert.equal(new Date(empty.rows[0].start).getHours(),18);
const snapshot=JSON.stringify(empty.rows),again=replan(empty.rows,[],opts);assert.equal(JSON.stringify(again.rows),snapshot,'saving the same future plan is idempotent');assert.equal(again.moved.length,2,'future preview always remains available');
const future=[item('future-a','2026-11-01'),item('future-b','2026-11-03','B')];assert.equal(replan(future,[],opts).moved.length,2);
const past=replan(future,[],{...opts,startDate:'2026-09-01',firstDayId:'B'});assert.deepEqual(plain(past.rows.map(x=>x.dayId)),['B','A']);assert.equal(new Date(past.rows[0].start).getMonth(),8);assert.ok(past.rows.every(x=>x.status==='planned'),'backdated planning never invents completed history');
const deleted=[item('deleted-a','2026-10-01','A','cancelled'),item('live-b','2026-10-03','B')],deletedBefore=JSON.stringify(deleted);
const restored=replan(deleted,[],opts);assert.equal(JSON.stringify(deleted),deletedBefore);assert.deepEqual(restored.rows[0],deleted[0],'original tombstone is retained');assert.equal(restored.rows.length,3);assert.equal(restored.moved[0].after.replannedFromId,'deleted-a');assert.notEqual(restored.moved[0].after.id,'deleted-a');
assert.equal(replan(restored.rows,[],opts).rows.length,3,'a restored tombstone cannot add duplicate sessions');
const done=item('done','2026-10-01','B','completed'),logs=[{id:'log',scheduleId:'logged',programId:opts.programId,started:done.start,finished:done.end,exercises:[{sets:[{done:true,reps:10}]}]}],logged=item('logged','2026-10-02');
const completed=replan([done,logged],logs,opts);assert.deepEqual(plain(completed.rows.slice(0,2)),[done,logged]);assert.equal(completed.rows.length,4);assert.ok(completed.rows.slice(2).every(x=>!['done','logged'].includes(x.id)));assert.equal(logs[0].scheduleId,'logged');
const logOnly=replan([],logs,{...opts,firstDayId:'B'});assert.deepEqual(plain(logOnly.rows.map(x=>x.dayId)),['B','A']);
const live=[item('active','2026-10-02'),item('draft','2026-10-04','B'),{...item('other','2026-10-07'),programId:'other-program'}],protectedPlan=replan(live,[],{...opts,activeScheduleIds:['active','draft']});
assert.deepEqual(plain(protectedPlan.rows.slice(0,3)),live);assert.equal(protectedPlan.moved.length,2,'an active-only plan can create a separate cycle without editing the workout');
for(const status of ['planned','skipped','cancelled','completed']){
 const original=[item('status-'+status,'2026-10-01','A',status)];const result=replan(original,[],opts);
 assert.ok(result.moved.length>0,status+' always has a reviewed plan');assert.ok(result.moved.every(x=>x.after.status==='planned'));
 if(status==='completed'||status==='cancelled')assert.deepEqual(result.rows[0],original[0]);
}
for(const mode of ['alternate','daily','custom','weekly']){
 const result=replan([],[],{...opts,mode,weekdays:[2,4]});assert.equal(result.rows.length,2);
 if(mode==='weekly')assert.ok(result.rows.every(x=>[2,4].includes(new Date(x.start).getDay())));
}
const dst=replan([],[],{...opts,startDate:'2026-10-24',time:'18:00'});assert.deepEqual(plain(dst.rows.map(x=>new Date(x.start).getHours())),[18,18]);
assert.throws(()=>replan([],[],{...opts,firstDayId:'missing'}),/programDay/);
assert.throws(()=>replan([],[],{...opts,time:'25:00'}),/time/);
assert.throws(()=>replan([],[],{...opts,startDate:'2026-02-31'}),/date/);
assert.throws(()=>replan([],[],{...opts,newId:()=> 'duplicate'}),/saveFailed/);
assert.throws(()=>replan(deleted,[],{...opts,newId:()=> 'live-b'}),/saveFailed/);
console.log('PASS #123 general replanning: empty/deleted/future/elapsed/completed/log-only plans, arbitrary date and first day, immutable history/tombstones, separate active/draft protection, unique IDs, idempotence, cadence/DST and validation');
