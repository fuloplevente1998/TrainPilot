const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict'),crypto=require('node:crypto').webcrypto;
const values=new Map(),root={innerHTML:''};
const ls={getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)};
// Seed a genuine 1.1.5-style state before loading the app.
values.set('repforge:plan',JSON.stringify({A:['db-squat','db-floor-press','one-arm-row','rdl','db-curl','plank'],B:['reverse-lunge','db-ohp','barbell-row','pushup','oh-triceps','crunch']}));
values.set('repforge:history',JSON.stringify([{workout:'B',started:'2026-09-06T16:00:00.000Z',finished:'2026-09-06T16:40:00.000Z',exercises:[]}]));
values.set('repforge:scheduled',JSON.stringify([{id:'12345678-old',workout:'A',start:'2026-09-07T16:00:00.000Z',end:'2026-09-07T16:45:00.000Z',updatedAt:1,cancelled:false}]));
const ctx=vm.createContext({localStorage:ls,document:{querySelector:()=>({...root,remove(){}}),querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){}},window:{Capacitor:{isNativePlatform:()=>false},scrollTo(){}},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:()=>1,clearTimeout(){},Date,crypto,TextEncoder,Blob:function(){},URL:{createObjectURL(){return''},revokeObjectURL(){}},alert(){},confirm:()=>true,prompt:()=>null,console});
for(const f of ['backup.js','demos.js','cloud.js','app.js','v12.js'])vm.runInContext(fs.readFileSync('www/'+f,'utf8'),ctx);
const run=s=>vm.runInContext(s,ctx);
assert.equal(run("db.get('schemaVersion',0)"),3);
assert.equal(run("activeProgram().name"),'Otthoni A/B – Alap');
assert.equal(run("programs().length"),5);
assert.ok(run("exercises().some(e=>e.id==='lat-pulldown')"));
assert.equal(run("scheduled()[0].programId"),'home-basic');
assert.equal(run("scheduled()[0].status"),'planned');
// Friday -> Sunday -> Tuesday proves the alternate rhythm crosses weekends without a reset.
const dates=run("buildAlternateSchedule('2026-09-11','18:00',3,45,'A','home-basic').map(x=>localDateKey(new Date(x.start))).join(',')");
assert.equal(dates,'2026-09-11,2026-09-13,2026-09-15');
assert.equal(run("buildAlternateSchedule('2026-09-11','18:00',4,45,'A','home-basic').map(x=>x.dayId).join('')"),'ABAB');
assert.equal(run("buildWeeklySchedule('2026-09-14','18:00',[1,3,5],2,45,'A','home-basic').map(x=>x.dayId).join('')"),'ABABAB');
// Arbitrary cycle length is supported.
run("let ps=programs();ps.push({id:'test-abc',name:'ABC',location:'Egyéni',level:'Egyéni',builtin:false,days:[{id:'A',name:'A',exercises:['db-squat']},{id:'B',name:'B',exercises:['pushup']},{id:'C',name:'C',exercises:['plank']}]});db.set('programs',ps)");
assert.equal(run("buildAlternateSchedule('2026-09-11','18:00',5,45,'A','test-abc').map(x=>x.dayId).join('')"),'ABCAB');
const backup=run('makeBackup()');assert.equal(backup.version,3);assert.ok(Array.isArray(backup.programs));assert.equal(backup.appVersion,'1.2.1');
run("db.set('activeProgramId','test-abc');startWorkout('C')");assert.equal(run("state.session.programId"),'test-abc');assert.equal(run("state.session.exercises[0].id"),'plank');
run("state.session=null;state.workout=null;state.tab='home';resumeDraft()");assert.equal(run("state.tab"),'plan');assert.equal(run("state.session.programId"),'test-abc');
console.log('PASS: 1.1.5→1.2 migration, every-other-day weekend rollover, weekly planner, arbitrary A/B/C cycles, program library and v3 backup.');
// Every built-in exercise must expose a real demo mapping, including new programs.
assert.equal(run('exercises().length'),26);
assert.equal(run('exercises().filter(e=>!demoInfo(e.id)||!demoCard(e.id)).length'),0);
assert.equal(run("exercises().filter(e=>demoInfo(e.id).provider!=='youtube').length"),0);
assert.match(run("demoInfo('reverse-lunge').source"),/sjlsISvHyZs/);
console.log('PASS: all 26 built-in exercises have demo cards; YouTube providers and two-dumbbell lunge preserved. Playback not tested.');

const beforeHistory=values.get('repforge:history'),beforeExercises=values.get('repforge:exercises');
run("let pl=programs();pl.find(p=>p.id==='home-basic').days[0].exercises=DEFAULT_PLAN.A.filter(x=>x!=='plank');db.set('programs',pl);db.set('plank121',false);migratePlank121();migratePlank121()");
assert.equal(run("programById('home-basic').days[0].exercises[5]"),'plank');
assert.equal(run("programById('home-basic').days[0].exercises.filter(x=>x==='plank').length"),1);
assert.equal(values.get('repforge:history'),beforeHistory);assert.equal(values.get('repforge:exercises'),beforeExercises);
console.log('PASS: sixth plank restored once, custom exercise settings and history preserved.');
