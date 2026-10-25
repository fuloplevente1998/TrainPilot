#!/usr/bin/env node
// Build-time catalog extraction from the exact accepted main; isolated in-memory reference runtime.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),crypto=require('node:crypto');
function pinned(file,expected){const b=fs.readFileSync(file);assert.equal(crypto.createHash('sha1').update(Buffer.concat([Buffer.from(`blob ${b.length}\0`),b])).digest('hex'),expected);return b.toString('utf8');}
const source=pinned(process.argv[2],'b5567576bed9a8f93608a6bf1ed8db1dee4723f4');
const html=pinned(process.argv[3],'a5d5011fc5c7d45815f7eb0ddf401a9e17fc9a5a');
const safety=html.match(/\/\* #18 — Drive sync safety[\s\S]*?window\.TrainPilotIssue18=\{version:'18',mergeSync:tp18MergeSync,selectSnapshots:tp18SnapshotSelection,legacyId\};\n\}\)\(\);/)[0];
const device='00000000-0000-0000-0000-000000000001',photoId='00000000-0000-0000-0000-000000000002';
function runtime(){
 const values=new Map(),timers=[],alerts=[],saved=[],deleted=[];let clock=Date.now();
 const node=()=>({innerHTML:'',textContent:'',style:{setProperty(){}},dataset:{},classList:{add(){},remove(){},contains(){return false},toggle(){}},children:[],parentNode:null,querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertBefore(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){},removeAttribute(){},closest(){return null},addEventListener(){},focus(){}});
 const root=node(),doc={hidden:false,documentElement:node(),body:node(),head:node(),querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node};
 const google={status:async()=>({profile:null}),driveList:async()=>({files:[]}),driveWrite:async()=>({id:'written',verified:true}),calendarPrepare:async()=>({id:'calendar'}),calendarSync:async()=>({}),drivePhotoWrite:async()=>({id:'photo-file',verified:true}),drivePhotoDelete:async()=>({})};
 const archive={save:async args=>{saved.push(JSON.parse(args.data));return {verified:true,name:'backup.zip',bytes:100};},open:async()=>{throw Error('restore must remain blocked');}};
 const photos={exists:async()=>({exists:true}),delete:async args=>{deleted.push(args.id);return {};}};
 class Clock extends Date {static now(){return clock;}}
 const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:google,BackupArchive:archive,WorkoutPhotos:photos}},addEventListener(){},scrollTo(){},open(){}},navigator:{onLine:true,languages:['hu-HU'],language:'hu-HU'},performance:{now:()=>1},requestAnimationFrame:fn=>{fn();return 1},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{const t={fn,ms,active:true};timers.push(t);return t;},clearTimeout:t=>{if(t)t.active=false;},Date:Clock,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert:m=>alerts.push(m),confirm:()=>true,prompt:()=>null,console:{log(){},info(){},warn(){},error(){},debug(){}}});
 const run=s=>vm.runInContext(s,ctx);run(source);run(safety);
 run(`cloudProfile={sub:'owner'};db.set('cloudDevice',${JSON.stringify(device)});db.set('cloudPrefs',{drive:true,calendar:true});calendarEvents=async()=>[];`);
 return {run,google,archive,photos,saved,deleted,alerts,timers,advance:ms=>clock+=ms};
}
const run=runtime().run;
const data=JSON.parse(run(`JSON.stringify({exercises:exercises().filter(e=>!e.custom).map(e=>{
 const raw={...e};delete raw.updatedAt;delete raw.createdAt;
 const names=Object.fromEntries(['hu','en','de','ro'].map(lang=>[lang,tp149ExerciseName(e,lang)]));
 const notes=Object.fromEntries(['hu','en','de','ro'].map(lang=>[lang,tp149ExerciseNote(e,lang)]));
 const demo=demoInfo(e.id);return {raw,names,notes,muscles:MUSCLE_MAP[e.id]||[],demo,
 measurementType:tp106IsDistance(e)?'distance':/mp|sec|second/.test(String(e.repUnit||''))?'time':'reps'};
}),programs:programs().filter(p=>p.builtin).map(p=>({raw:p,
 names:Object.fromEntries(['hu','en','de','ro'].map(lang=>[lang,tp149ProgramMeta(p,'name',lang)])),
 locations:Object.fromEntries(['hu','en','de','ro'].map(lang=>[lang,tp149ProgramMeta(p,'location',lang)])),
 levels:Object.fromEntries(['hu','en','de','ro'].map(lang=>[lang,tp149ProgramMeta(p,'level',lang)])),
 days:p.days.map(d=>({raw:d,names:Object.fromEntries(['hu','en','de','ro'].map(lang=>[lang,tp149ProgramDayName(p,d,lang)]))}))}))})`));
assert.equal(data.exercises.length,106);
assert.equal(new Set(data.exercises.map(e=>e.raw.id)).size,106);
for(const p of data.programs)for(const d of p.days)for(const id of d.raw.exercises)assert.ok(data.exercises.some(e=>e.raw.id===(typeof id==='object'?id.id:id)),id);
const planner=process.argv[5]==='planner'?JSON.parse(run(`JSON.stringify({
 roles:RF148_ROLE_PATTERNS,areas:RF148_AREA_PATTERNS,needs:NEEDS132,
 beginnerFavorites:[...RF14_BEGINNER_FAVOR],
 templates:['home-basic','home-level2'].map(id=>tp128Template(id)),
 meta:Object.fromEntries(exercises().filter(e=>!e.custom).map(e=>{const m=rf14Meta(e);return [e.id,{movementPattern:m.movementPattern,difficulty:m.difficulty,complexity:m.complexity,beginnerSafe:m.beginnerSafe,compound:m.compound,goalTags:m.goalTags}]}))
})`)):null;
const payload={source:{commit:'7473b30',appBlob:'b5567576bed9a8f93608a6bf1ed8db1dee4723f4',indexBlob:'a5d5011fc5c7d45815f7eb0ddf401a9e17fc9a5a'},...(planner||data)};
const target=process.argv[4]||'android/app/src/main/resources/native-catalog-main-7473.json';
fs.mkdirSync(require('node:path').dirname(target),{recursive:true});
fs.writeFileSync(target,JSON.stringify(payload,null,2)+'\n');
console.log(planner?`Final planner policy extracted; no Android tests executed.`:`${data.exercises.length} exercises and ${data.programs.length} programs extracted; no Android tests executed.`);

if(planner && process.argv[6]) {
 const cases=JSON.parse(run(`JSON.stringify([
  ['home-basic',90,['dumbbells','barbell','bench'],'intermediate',[]],
  ['home-level2',90,['dumbbells','barbell','bench'],'intermediate',[]],
  ['home-level2',90,['dumbbells','barbell'],'intermediate',[]],
  ['home-basic',20,['dumbbells','barbell'],'beginner',[]],
  ['home-basic',90,[],'beginner',[]],
  ['home-basic',60,['dumbbells','barbell'],'intermediate',['barbell-row']],
  ['home-level2',60,['barbell'],'intermediate',[]],
  ['home-basic',90,[],'beginner',['pushup','modified-pushup','incline-pushup','squat','air-squat']]
 ].map(([templateId,minutes,gear,experience,excluded],i)=>{
  const profile={age:30,height:175,weight:75,goal:'fitness',activity:'mixed',location:'home',cadence:'alternate',split:'auto',focus:'balanced',avoidAreas:[],minutes,gear,experience,excluded,templateId};
  try {const p=generatePersonalProgram(profile);return {name:'main-plan-'+i,profile,days:p.days.map(d=>({id:d.id,exercises:d.exercises})),estimate:p.estimatedMinutes};}
  catch(e){return {name:'main-plan-'+i,profile,rejected:true};}
 }))`));
 fs.writeFileSync(process.argv[6],JSON.stringify({source:payload.source,cases},null,2)+'\n');
 console.log(`${cases.length} actual-main planner fixtures generated; Kotlin not executed.`);
}
