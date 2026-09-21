const fs=require('fs'),vm=require('vm'),assert=require('node:assert/strict');
const values=new Map(),root={innerHTML:''};
const node=()=>({innerHTML:'',style:{setProperty(){}},classList:{add(){},remove(){},toggle(){},contains(){return false}},querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){},removeAttribute(){},focus(){}});
const doc={querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node,head:node(),body:node(),documentElement:node(),activeElement:null};
const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>false},addEventListener(){},scrollTo(){},open(){},innerWidth:390,innerHeight:800,scrollY:0},navigator:{onLine:true,languages:['hu-HU'],language:'hu-HU'},setInterval:()=>1,clearInterval(){},setTimeout:()=>1,clearTimeout(){},requestAnimationFrame:fn=>fn(),Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,Event:function(){},alert(){},confirm:()=>true,prompt:()=>null,console});
for(const [,f] of fs.readFileSync('www/index.html','utf8').matchAll(/<script\b[^>]*\bsrc="([^"]+)"[^>]*>/g))vm.runInContext(fs.readFileSync('www/'+f,'utf8'),ctx,{filename:f});
const run=s=>vm.runInContext(s,ctx);
assert.equal(run('window.TrainPilotAdaptivePlanner.version'),'1.4.8');
assert.equal(run('window.TrainPilotHotfix.version'),'1.4.8.1');
assert.equal(run('exercises().length'),100,'1.4.8 built-in library must contain 100 exercises');
assert.equal(run('new Set(exercises().map(e=>e.id)).size'),100,'exercise IDs must stay unique');
for(const k of ['bands','pullupbar','dipbars','kettlebell'])assert.ok(run('Object.hasOwn(GEAR132,"'+k+'")'),'missing gear '+k);
assert.equal(run('exercises().filter(e=>demoInfo(e.id)).length'),100,'all built-in exercises need a demo/search entry');
const presetIds=['home-upper-ab','home-quick-30','calisthenics-ab','bands-ab','kettlebell-ab','gym-upper-lower','gym-ppl','gym-strength-abc'];
for(const id of presetIds)assert.ok(run("programs().some(p=>p.id==='"+id+"'&&p.builtin)"),'missing built-in preset '+id);
assert.ok(run('programs().filter(p=>p.builtin).length')>=13,'expanded built-in program library expected');
assert.equal(run('programs().flatMap(p=>p.days).flatMap(d=>d.exercises).every(id=>!!byId(id))'),true,'every preset exercise ID must exist');


ctx.base={age:28,height:178,weight:80,goal:'fitness',experience:'beginner',activity:'mixed',minutes:45,location:'home',cadence:'alternate',split:'auto',excluded:[],gear:['dumbbells','support','pullupbar','dipbars'],focus:'balanced',avoidAreas:[]};
assert.equal(run('validateProfile({...base,minutes:120}).minutes'),120);
assert.throws(()=>run('validateProfile({...base,minutes:121})'));
assert.throws(()=>run('validateProfile({...base,minutes:22})'));

ctx.noLeg={...ctx.base,avoidAreas:['legs']};
const noLeg=JSON.parse(run('JSON.stringify(generatePersonalProgram(noLeg))'));
assert.ok(noLeg.days.length>=2);
assert.ok(noLeg.days.every(d=>d.exercises.length>=3));
ctx.noLegPlan=noLeg;
assert.equal(run('noLegPlan.days.flatMap(d=>d.exercises).some(id=>RF148_AREA_PATTERNS.legs.includes(byId(id).movementPattern))'),false,'leg avoidance must remove lower-body movement patterns');
ctx.pplNoLeg={...ctx.noLeg,split:'ppl'};
assert.equal(run('generatePersonalProgram(pplNoLeg).effectiveSplit'),'upper','PPL without legs must adapt instead of throwing');

ctx.allGear={...ctx.base,gear:Array.from(run('Object.keys(GEAR132)')),avoidAreas:[],split:'full'};
ctx.shortP={...ctx.allGear,minutes:20};ctx.longP={...ctx.allGear,minutes:120};
const shortPlan=JSON.parse(run('JSON.stringify(generatePersonalProgram(shortP))')),longPlan=JSON.parse(run('JSON.stringify(generatePersonalProgram(longP))'));
assert.ok(longPlan.days[0].exercises.length>shortPlan.days[0].exercises.length,'longer workouts must add exercise capacity');
assert.ok(Math.max(...Object.values(longPlan.prescriptions).map(x=>x.sets))>Math.max(...Object.values(shortPlan.prescriptions).map(x=>x.sets)),'longer workouts must allow more work sets');

ctx.focusP={...ctx.allGear,minutes:60,focus:'chestback'};
const focused=JSON.parse(run('JSON.stringify(generatePersonalProgram(focusP))'));
ctx.focused=focused;
assert.ok(run("['horizontal-push','chest-isolation'].includes(byId(focused.days[0].exercises[0]).movementPattern)"),'chest/back focus should prioritize push/pull work');

const eligible=JSON.parse(run('JSON.stringify(exercises().filter(e=>e.beginnerSafe).map(e=>e.id))'));
ctx.twoP={...ctx.allGear,excluded:eligible.slice(2),minutes:45};
assert.throws(()=>run('generatePersonalProgram(twoP)'),/kevesebb mint 3|legalább 3/);

ctx.custom={id:'custom-test148',hu:'Teszt húzás',en:'Test Pull',equipment:'gumiszalag',target:'Hát',notes:'',sets:2,reps:'8–12',weight:0,loadType:'total',repUnit:'ism.',movementPattern:'horizontal-pull',difficulty:'beginner',beginnerSafe:true,complexity:'low',compound:true,goalTags:['fitness'],style:'calisthenics',gearNeeds:['bands'],muscleGroup:'back',custom:true};
run('db.set("exercises",[...exercises(),custom])');
ctx.customNoBand={...ctx.base,gear:[],excluded:['custom-test148']};
assert.doesNotThrow(()=>run('validateProfile(customNoBand)'),'custom exercise IDs must be valid exclusions');
ctx.customUse={...ctx.base,gear:[],excluded:[]};assert.equal(run("available132('custom-test148',customUse)"),false);
ctx.customUseBand={...ctx.base,gear:['bands'],excluded:[]};assert.equal(run("available132('custom-test148',customUseBand)"),true);
assert.equal(run("muscleGroups('custom-test148')[0]"),'back');
assert.equal(run("libraryGear(byId('band-row'))"),'bands');
assert.equal(run("libraryGear(byId('pullup'))"),'pullupbar');
assert.equal(run("libraryGear(byId('dip'))"),'dipbars');
assert.equal(run("libraryGear(byId('kb-row'))"),'kettlebell');
assert.doesNotThrow(()=>run(`(()=>{const b=makeBackup();b.settings={...b.settings,profile:{...customUseBand,excluded:['custom-test148']}};b.programs=[...b.programs,{id:'custom-backup-148',name:'Custom backup',builtin:false,days:[{id:'A',name:'A',exercises:['custom-test148']}],prescriptions:{'custom-test148':{sets:2,reps:'8–12',rest:90,weight:0}}}];return validateBackup(b)})()`),'v3 backup with a custom excluded/prescribed exercise must validate');
ctx.legacyProfile={age:28,height:178,weight:80,goal:'fitness',experience:'beginner',activity:'mixed',minutes:45,location:'home',cadence:'alternate',split:'auto',excluded:[]};
assert.ok(run('validateProfile(legacyProfile).gear.includes("dumbbells")'),'legacy location-only profiles must still resolve default equipment');

run('state.profilePreview=base;profileScreen()');
assert.ok(root.innerHTML.includes('120 perc')&&root.innerHTML.includes('Kerülendő területek')&&root.innerHTML.includes('Fókusz'),'profile UI must expose 20–120 minutes, focus and avoid areas');
assert.ok(root.innerHTML.includes('Gumiszalag')&&root.innerHTML.includes('Húzódzkodórúd')&&root.innerHTML.includes('Kettlebell'),'new equipment must be visible');
assert.ok(root.innerHTML.includes('class="setting tp152-accordion tp152-profile-group"'),'profile sections should use the 1.5.2 hierarchical accordions');
assert.ok(root.innerHTML.indexOf('Kerülendő területek')<root.innerHTML.indexOf('Egyedi gyakorlatkizárás'),'profile exclusion sections should remain separately readable');

run('muscleLibrary()');
assert.ok(root.innerHTML.includes('Gumiszalag')&&root.innerHTML.includes('Húzódzkodórúd')&&root.innerHTML.includes('Tolódzkodó')&&root.innerHTML.includes('Kettlebell'),'exercise library must expose new equipment filters');
run('customExerciseScreen()');
assert.ok(root.innerHTML.includes('Vízszintes tolás')&&root.innerHTML.includes('Törzsstabilizáció')&&root.innerHTML.includes('Kategória / stílus')&&root.innerHTML.includes('Calisthenics'),'custom exercise UI must use localized movement patterns and style');

const backupSource=fs.readFileSync('www/backup.js','utf8'),featureSource=fs.readFileSync('www/trainpilot-148-adaptive-planner.js','utf8');
assert.ok(featureSource.includes('rf148UiPolish'),'1.4.8 profile UI polish layer must exist');
assert.ok(!featureSource.includes("'.tp-brand-strip .tag{"),'1.4.8 must not override the stable 1.4.7 brand/header GUI');

assert.ok(backupSource.includes('d.exercises.length>300'),'legacy backup validator should allow expanded/custom libraries');
assert.ok(!featureSource.match(/generatePersonalProgram=function\(input\)\{[^]{0,900}RF150_HOME/),'final personal generator must not be based on RF150_HOME');
console.log('PASS TrainPilot 1.4.8: 100 exercises, adaptive exclusions/splits, 20–120 min scaling, localized custom patterns/styles and new gear.');
