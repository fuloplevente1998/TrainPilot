const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('www/app.js','utf8'),safety=fs.readFileSync('www/index.html','utf8').match(/\/\* #18 — Drive sync safety[\s\S]*?window\.TrainPilotIssue18=\{version:'18',mergeSync:tp18MergeSync,selectSnapshots:tp18SnapshotSelection,legacyId\};\n\}\)\(\);/)[0];
const device='00000000-0000-0000-0000-000000000001',photoId='00000000-0000-0000-0000-000000000002';
const deferred=()=>{let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b;});return {promise,resolve,reject};};
const settle=async predicate=>{const deadline=Date.now()+2000;while(!predicate()){assert.ok(Date.now()<deadline,'operation did not reach the expected checkpoint');await new Promise(setImmediate);}};
function runtime(){
 const values=new Map(),timers=[],alerts=[],saved=[],deleted=[];let clock=Date.now();
 const node=()=>({innerHTML:'',textContent:'',style:{setProperty(){}},dataset:{},classList:{add(){},remove(){},contains(){return false},toggle(){}},children:[],parentNode:null,querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertBefore(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){},removeAttribute(){},closest(){return null},addEventListener(){},focus(){}});
 const root=node(),doc={hidden:false,documentElement:node(),body:node(),head:node(),querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node};
 const google={status:async()=>({profile:null}),driveList:async()=>({files:[]}),driveWrite:async()=>({id:'written',verified:true}),calendarPrepare:async()=>({id:'calendar'}),calendarSync:async()=>({}),drivePhotoWrite:async()=>({id:'photo-file',verified:true}),drivePhotoDelete:async()=>({})};
 const archive={save:async args=>{saved.push(JSON.parse(args.data));return {verified:true,name:'backup.zip',bytes:100};},open:async()=>{throw Error('restore must remain blocked');}};
 const photos={exists:async()=>({exists:true}),delete:async args=>{deleted.push(args.id);return {};}};
 class Clock extends Date {static now(){return clock;}}
 const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:google,BackupArchive:archive,WorkoutPhotos:photos}},addEventListener(){},scrollTo(){},open(){}},navigator:{onLine:true,languages:['hu-HU'],language:'hu-HU'},performance:{now:()=>1},requestAnimationFrame:fn=>{fn();return 1},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{const t={fn,ms,active:true};timers.push(t);return t;},clearTimeout:t=>{if(t)t.active=false;},Date:Clock,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert:m=>alerts.push(m),confirm:()=>true,prompt:()=>null,console});
 const run=s=>vm.runInContext(s,ctx);run(source);run(safety);
 run(`cloudProfile={sub:'owner'};db.set('cloudDevice',${JSON.stringify(device)});db.set('cloudPrefs',{drive:true,calendar:true});calendarEvents=async()=>[];`);
 return {run,google,archive,photos,saved,deleted,alerts,timers,advance:ms=>clock+=ms};
}
(async()=>{
 const a=runtime(),run=a.run;
 assert.equal(run('exercises().length'),106);
 for(const id of ['running','jogging','inline-skating','cycling','walking','hiking']){
  assert.equal(run(`tp106IsDistance({id:${JSON.stringify(id)},sets:1})`),true,'built-in ID survives older session builders');
  assert.equal(run(`byId(${JSON.stringify(id)}).measurementType`),'distance');
  assert.equal(run(`byId(${JSON.stringify(id)}).repUnit`),'mp');
  assert.equal(run(`available132(${JSON.stringify(id)},{gear:[],excluded:[]})`),false,'strength generator excludes distance activities');
  assert.equal(run(`rf152Recommendation(${JSON.stringify(id)}).autoApply`),false);
  assert.equal(run(`window.tp155CoachShortAdvice(${JSON.stringify(id)}).label`),'Idő és távolság');
 }
 assert.equal(run('Object.values(tp149ExerciseNameAudit().missing).every(rows=>rows.length===0)'),true,'new activities have complete four-language metadata');
 run('db.set("exercises",exercises().filter(e=>!TP106_DISTANCE_IDS.has(e.id)))');
 assert.equal(run('exercises().length'),106,'upgrade adds activities to the saved old 100-entry library');
 run(`const distanceExercise={...exercises()[0],id:'custom-run',hu:'Kocogás',en:'Jogging',custom:true,measurementType:'distance',repUnit:'mp',loadType:'bodyweight',sets:1,reps:'300',style:'conditioning',movementPattern:'other',gearNeeds:[],muscleGroup:'legs'};db.set('exercises',[...exercises(),distanceExercise]);`);
 assert.equal(run(`tp106ParseKm('5,125')`),5125);assert.equal(run(`tp106ParseKm('0.001')`),1);assert.equal(run(`tp106ParseKm('')`),null);
 for(const bad of ['-1','NaN','Infinity','2000.001','5km','1e3'])assert.throws(()=>run(`tp106ParseKm(${JSON.stringify(bad)})`));
 assert.equal(run(`tp106IsDistance({...exercises()[0],sets:2})`),false,'numeric library prescriptions must not be treated as set arrays');
 run(`state.current=0;state.session={started:'2026-10-02T00:00:00Z',workout:'A',exercises:[{id:'custom-run',hu:'Kocogás',en:'Jogging',measurementType:'distance',repUnit:'mp',loadType:'bodyweight',sets:[{set:1,weight:0,reps:'',done:false}]}]};renderWorkout=()=>{};
 var gpsMock={active:false,distanceMeters:0,elapsedSeconds:0},starts=0,stops=0,denyGPS=false;
 window.Capacitor.Plugins.DistanceTracker={status:async()=>({...gpsMock}),start:async args=>{starts++;if(denyGPS)throw Error('permission denied');gpsMock={...gpsMock,key:args.key,active:true};return {...gpsMock};},stop:async()=>{stops++;gpsMock.active=false;return {...gpsMock};}};`);
 await Promise.all([run('tp106StartGPS()'),run('tp106StartGPS()')]);assert.equal(run('starts'),1,'double taps must start only one native measurement');assert.equal(run('tp106GpsState.active'),true);
 run('gpsMock.distanceMeters=5125;gpsMock.elapsedSeconds=1800;gpsMock.hasFix=true;');await run('tp106SyncGPS()');assert.equal(run('state.session.exercises[0].sets[0].distanceMeters'),5125);assert.equal(run('state.session.exercises[0].sets[0].reps'),'1800');assert.match(run('formatSet(state.session.exercises[0],state.session.exercises[0].sets[0])'),/5,125 km/);
 // Recover active polling after a WebView reload without adding the same aggregate twice.
 run('tp106GpsTimer=null;tp106GpsState={active:false};');await run('tp106SyncGPS()');assert.equal(run('tp106GpsState.active'),true);assert.ok(run('tp106GpsTimer!==null'));assert.equal(run('state.session.exercises[0].sets[0].distanceMeters'),5125);
 // A status response that overlaps a start/stop operation must not overwrite its state.
 run('var savedStatus=window.Capacitor.Plugins.DistanceTracker.status,resolveLateStatus;window.Capacitor.Plugins.DistanceTracker.status=()=>new Promise(r=>resolveLateStatus=r);');
 const latePoll=run('tp106SyncGPS()');run('tp106GpsBusy=true;resolveLateStatus({active:false});');await latePoll;assert.equal(run('tp106GpsState.active'),true);run('tp106GpsBusy=false;window.Capacitor.Plugins.DistanceTracker.status=savedStatus;');
 // Preserve totals on a native notification stop and across a WebView reload/draft resume.
 run('persistDraft();state.session=null;gpsMock.active=false;');await run('tp106SyncGPS()');assert.equal(run(`db.get('draft').session.exercises[0].sets[0].distanceMeters`),5125);assert.equal(run(`db.get('gpsTrip106',null)`),null);
 run(`state.session=db.get('draft').session;tp106SetDistance(state.session.exercises[0].sets[0],'6,25');`);assert.equal(run('state.session.exercises[0].sets[0].distanceMeters'),6250);assert.equal(run('state.session.exercises[0].sets[0].distanceSource'),'manual');
 run(`gpsMock={active:false,distanceMeters:0,elapsedSeconds:0};`);await run('tp106StartGPS()');run('gpsMock.distanceMeters=500;gpsMock.elapsedSeconds=120;');await run('tp106StopGPS()');assert.equal(run('state.session.exercises[0].sets[0].distanceMeters'),6750);assert.equal(run('state.session.exercises[0].sets[0].reps'),'1920');assert.equal(run('tp106GpsBusy'),false);
 run(`denyGPS=true;`);await run('tp106StartGPS()');assert.equal(run('tp106GpsBusy'),false);assert.equal(run(`db.get('gpsTrip106',null)`),null);assert.equal(run('state.session.exercises[0].sets[0].distanceMeters'),6750,'permission denial must preserve manual/GPS totals');
 run(`state.session.exercises[0].sets[0].done=true;db.set('history',[{id:'run-log',workout:'A',started:state.session.started,finished:'2026-10-02T00:32:00Z',exercises:state.session.exercises,photos:[]}]);`);
 const backup=run('makeBackup()');assert.equal(backup.history[0].exercises[0].sets[0].distanceMeters,6750);assert.equal(backup.history[0].exercises[0].measurementType,'distance');run('validateBackup(makeBackup())');
 for(const bad of [-1,Infinity,2000001,'5']){const d=run('makeBackup()');d.history[0].exercises[0].sets[0].distanceMeters=bad;assert.throws(()=>run(`validateBackup(${JSON.stringify(d)})`));}
 assert.ok(!JSON.stringify(backup).includes('latitude')&&!JSON.stringify(backup).includes('longitude'),'coordinate data must not enter workout exports');
 assert.ok(run('window.tp106CustomExercisePanelHtml()').includes('ceMeasure'));assert.ok(run('window.tp106CustomExercisePanelHtml()').includes('ceGear'));
 console.log('PASS custom distance: km parsing, concurrent GPS start, stop/resume/draft recovery, permission denial, manual edits, export and malformed data');
})().catch(e=>{console.error(e);process.exitCode=1;});
