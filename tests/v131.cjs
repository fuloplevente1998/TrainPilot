const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict'),crypto=require('node:crypto').webcrypto;
const values=new Map(),root={innerHTML:''};
const ls={getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)};
// Seed a genuine 1.1.5-style state before loading the app.
values.set('repforge:plan',JSON.stringify({A:['db-squat','db-floor-press','one-arm-row','rdl','db-curl','plank'],B:['reverse-lunge','db-ohp','barbell-row','pushup','oh-triceps','crunch']}));
values.set('repforge:history',JSON.stringify([{workout:'B',started:'2026-09-06T16:00:00.000Z',finished:'2026-09-06T16:40:00.000Z',exercises:[]}]));
values.set('repforge:scheduled',JSON.stringify([{id:'12345678-old',workout:'A',start:'2026-09-07T16:00:00.000Z',end:'2026-09-07T16:45:00.000Z',updatedAt:1,cancelled:false}]));
const ctx=vm.createContext({localStorage:ls,document:{querySelector:()=>({...root,remove(){}}),querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){}},window:{Capacitor:{isNativePlatform:()=>false},scrollTo(){}},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:()=>1,clearTimeout(){},Date,crypto,TextEncoder,Blob:function(){},URL:{createObjectURL(){return''},revokeObjectURL(){}},alert(){},confirm:()=>true,prompt:()=>null,console});
for(const f of ['backup.js','demos.js','cloud.js','app.js','v12.js','catalog131.js','v13.js','library131.js'])vm.runInContext(fs.readFileSync('www/'+f,'utf8'),ctx);
const run=s=>vm.runInContext(s,ctx);

assert.equal(run('exercises().length'),40);
assert.equal(run('new Set(exercises().map(e=>e.id)).size'),40);
assert.equal(run('exercises().filter(e=>!demoInfo(e.id)||!muscleGroups(e.id).length).length'),0);
assert.equal(run("libraryGear(byId('bodyweight-squat'))"),'bodyweight');
assert.equal(run("libraryGear(byId('db-rdl'))"),'dumbbells');
assert.equal(run("libraryGear(byId('rdl'))"),'barbell');
assert.equal(run("libraryGear(byId('face-pull'))"),'machine');
assert.ok(run("libraryMatch(byId('db-rdl'),'legs','dumbbells','roman')"));
assert.equal(run("libraryMatch(byId('rdl'),'legs','dumbbells','')"),false);
const rawHistory=values.get('repforge:history');
run("const input131={age:28,height:180,weight:80,goal:'fitness',experience:'beginner',activity:'mixed',minutes:45,location:'home',cadence:'alternate',split:'full',excluded:[]};const personal131=generatePersonalProgram(input131);db.set('programs',[...programs(),personal131]);");
assert.ok(run("personal131.days.flatMap(d=>d.exercises).includes('db-rdl')||personal131.days.flatMap(d=>d.exercises).includes('glute-bridge')"));
assert.equal(run("personal131.days.flatMap(d=>d.exercises).includes('rdl')"),false);
run("appendLibraryExercise(personal131.id,'A','side-plank')");
assert.equal(run("programById(personal131.id).prescriptions['side-plank'].reps"),'15–30 mp / oldal');
assert.throws(()=>run("appendLibraryExercise(personal131.id,'A','side-plank')"));
assert.throws(()=>run("appendLibraryExercise('home-basic','A','side-plank')"));
assert.equal(values.get('repforge:history'),rawHistory);
run('restoreText(JSON.stringify(makeBackup()))');
assert.equal(run('exercises().length'),40);
assert.ok(run("programById(personal131.id).days[0].exercises.includes('side-plank')"));
assert.equal(run("byId('side-plank').repUnit"),'mp/oldal');
assert.equal(run('makeBackup().appVersion'),'1.3.1');
console.log('PASS: 40 unique exercises, muscle/video coverage, accent-insensitive search, gear filters, program placement and duplicate protection, dumbbell-only generator and backup roundtrip.');

run("const sideHistory={started:'2026-09-10T10:00:00Z',programId:personal131.id,exercises:[{id:'side-plank',repUnit:'mp/oldal',sets:[{done:true,reps:20}]}],feedback:{suggestion:{action:'reps'}}};db.set('history',[sideHistory]);state.feedbackStarted=sideHistory.started;applyProgression()");
assert.equal(run("programById(personal131.id).prescriptions['side-plank'].reps"),'25 mp');
