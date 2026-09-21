const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict'),crypto=require('node:crypto').webcrypto;
const values=new Map(),root={innerHTML:''};
const ls={getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)};
// Seed a genuine 1.1.5-style state before loading the app.
values.set('repforge:plan',JSON.stringify({A:['db-squat','db-floor-press','one-arm-row','rdl','db-curl','plank'],B:['reverse-lunge','db-ohp','barbell-row','pushup','oh-triceps','crunch']}));
values.set('repforge:history',JSON.stringify([{workout:'B',started:'2026-09-06T16:00:00.000Z',finished:'2026-09-06T16:40:00.000Z',exercises:[]}]));
values.set('repforge:scheduled',JSON.stringify([{id:'12345678-old',workout:'A',start:'2026-09-07T16:00:00.000Z',end:'2026-09-07T16:45:00.000Z',updatedAt:1,cancelled:false}]));
const ctx=vm.createContext({localStorage:ls,document:{querySelector:()=>({...root,remove(){}}),querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){}},window:{Capacitor:{isNativePlatform:()=>false},scrollTo(){}},navigator:{onLine:true},setInterval:()=>1,clearInterval(){},setTimeout:()=>1,clearTimeout(){},Date,crypto,TextEncoder,Blob:function(){},URL:{createObjectURL(){return''},revokeObjectURL(){}},alert(){},confirm:()=>true,prompt:()=>null,console});
for(const f of ['backup.js','demos.js','cloud.js','app.js','v12.js','catalog131.js','v13.js','library131.js','onboarding132.js'])vm.runInContext(fs.readFileSync('www/'+f,'utf8'),ctx);
const run=s=>vm.runInContext(s,ctx);


ctx.profile132={age:28,height:180,weight:80,goal:'fitness',experience:'beginner',activity:'mixed',minutes:45,location:'home',cadence:'alternate',split:'auto',excluded:[],gear:[]};
assert.equal(run('exercises().length'),40);
assert.equal(run('generatePersonalProgram(profile132).gear.length'),0);
assert.ok(run('generatePersonalProgram(profile132).days.every(d=>d.exercises.every(id=>NEEDS132[id].length===0))'));
assert.throws(()=>run('generatePersonalProgram({...profile132,split:"ppl"})'));
assert.throws(()=>run('validateProfile({...profile132,gear:["unknown"]})'));
assert.throws(()=>run('validateProfile({...profile132,gear:["bench","bench"]})'));
const keys=Array.from(run('Object.keys(GEAR132)'));
for(let mask=0;mask<128;mask++) for(const minutes of [20,30,45,60]){
 ctx.combo=keys.filter((x,i)=>mask&(1<<i));ctx.minutes132=minutes;
 assert.ok(run('generatePersonalProgram({...profile132,gear:combo,minutes:minutes132}).days.every(d=>d.exercises.every(id=>available132(id,{...profile132,gear:combo})))'));
}
assert.equal(run('profileGear132({...profile132,gear:undefined,location:"home"}).join(",")'),'dumbbells,support');
assert.equal(run('profileGear132({...profile132,gear:undefined,location:"gym"}).length'),7);
const oldHistory=values.get('repforge:history'),oldEx=values.get('repforge:exercises');
run('state.profilePreview=profile132;state.programPreview=generatePersonalProgram(profile132);acceptPersonalProgram()');
assert.equal(values.get('repforge:history'),oldHistory);assert.equal(values.get('repforge:exercises'),oldEx);
run('restoreText(JSON.stringify(makeBackup()))');assert.equal(run('trainingProfile().gear.length'),0);
assert.equal(run('makeBackup().appVersion'),'1.3.2');
assert.ok(run('activeProgram().reasons.some(x=>x.includes("nem helyettesíti"))'));
ctx.document.querySelector=()=>root;
run('profileScreen()');assert.ok(root.innerHTML.includes('Csak saját testsúly'));assert.ok(root.innerHTML.includes('name="pfGear"'));assert.equal(root.innerHTML.includes('id="pfLocation"'),false);
console.log('PASS: 512 equipment/time combinations, bodyweight-only plan, unavailable gear rejection, legacy profile defaults, backup roundtrip, preserved history and no location selector.');

assert.equal(run("available132('db-rdl',{...profile132,gear:['dumbbell']})"),false);
assert.equal(run("available132('one-arm-row',{...profile132,gear:['dumbbell','support']})"),true);
assert.equal(run("available132('goblet-squat',{...profile132,gear:['dumbbells']})"),true);
