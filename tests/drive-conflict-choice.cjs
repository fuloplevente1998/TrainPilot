const assert=require('node:assert/strict'),fs=require('node:fs'),runtime=require('./helpers/app-runtime.cjs');
const safety=fs.readFileSync('www/index.html','utf8').match(/\/\* #18 — Drive sync safety[\s\S]*?window\.TrainPilotIssue18=\{version:'18',mergeSync:tp18MergeSync,selectSnapshots:tp18SnapshotSelection,legacyId\};\n\}\)\(\);/)[0];
const clone=x=>JSON.parse(JSON.stringify(x)),device='00000000-0000-0000-0000-000000000001';
function fixture(phoneChoice){
 const app=runtime();app.run(safety);
 const phone=clone(app.run('syncData()'));
 for(const id of ['phone-program','cloud-program'])phone.programs.push({...clone(phone.programs[0]),id,name:id,builtin:false});
 phone.activeProgramId='phone-program';phone.settings.rest=60;phone.exercises[0].reps='8';
 phone.plannerSettings={mode:'weekly',time:'18:00',minutes:30};phone.exerciseFavorites=['db-squat'];phone.themeAccent='classicBlue';phone.language='hu';
 const workout=(id,reps,started)=>({id,workout:'A',programId:'home-basic',dayId:'A',started:new Date(started).toISOString(),finished:new Date(Date.parse(started)+600000).toISOString(),exercises:[{...clone(phone.exercises[0]),sets:[{set:1,setId:'set-'+id,weight:0,reps,done:true}]}],photos:[{id:'photo-'+id,driveFileId:'drive-'+id,createdAt:started,updatedAt:1}]});
 phone.history=[workout('workout-shared','8','2026-10-01T10:00:00Z'),workout('phone-only','10','2026-10-02T10:00:00Z')];
 phone.weights=[{id:'weight-shared',kg:65,date:'2026-10-01T10:00:00Z'},{id:'phone-weight',kg:66,date:'2026-10-02T10:00:00Z'}];
 phone.scheduled=[{id:'schedule-shared',workout:'A',programId:'home-basic',dayId:'A',start:'2026-10-12T10:00:00Z',end:'2026-10-12T10:30:00Z',updatedAt:1,cancelled:false,status:'planned'}];
 const cloud=clone(phone);cloud.activeProgramId='cloud-program';cloud.settings.rest=90;cloud.exercises[0].reps='12';cloud.programs[0].name='Cloud program';cloud.plan.A=[...cloud.plan.A].reverse();
 cloud.plannerSettings={mode:'daily',time:'19:00',minutes:45};cloud.exerciseFavorites=[];cloud.themeAccent='green';cloud.language='en';
 cloud.history=[workout('workout-shared','12','2026-10-01T10:00:00Z'),workout('cloud-only','14','2026-10-03T10:00:00Z')];
 cloud.weights=[{id:'weight-shared',kg:70,date:'2026-10-01T10:00:00Z'},{id:'cloud-weight',kg:71,date:'2026-10-03T10:00:00Z'}];cloud.scheduled[0].end='2026-10-12T10:45:00Z';
 app.run(`validateSync(${JSON.stringify(phone)});validateSync(${JSON.stringify(cloud)});storeMerged(${JSON.stringify(phone)});`);
 // Health data omitted from cloud export must survive applying the chosen rows.
 app.run(`db.set('history',history().map(h=>({...h,health240:{averageHeartRate:120},healthWear129:{schema:1,source:'wear_health_services',workoutId:h.id,watchId:'watch7',revision:4,totalCalories:4}})));cloudProfile={sub:'conflict-owner'};db.set('cloudDevice',${JSON.stringify(device)});db.set('cloudPrefs',{drive:false,calendar:false});rf130SyncWorkoutPhotos=async()=>{};`);
 const reads=[],writes=[],choices=[];let onChoice=null,head='head',snapshot=cloud;
 const bridge={driveList:async()=>({files:[{id:head,name:'repforge-sync-'+device+'-head.json',createdTime:'2026-10-10T12:00:00Z',version:'1'}]}),driveRead:async({id})=>{reads.push(id);return {version:'1',data:JSON.stringify({app:'RepForgeSync',schema:1,owner:'conflict-owner',device,data:snapshot})};},driveWrite:async({data})=>{const parsed=JSON.parse(data);writes.push(parsed);snapshot=parsed.data;head='written';return {id:head,version:'1',verified:true};}};
 app.context.window.Capacitor={isNativePlatform:()=>true,Plugins:{GoogleSync:bridge}};
 app.context.choosePhone=async(message,options)=>{choices.push({message,options});await onChoice?.();return phoneChoice;};app.run('tp2628Confirm=choosePhone;');
 return {...app,phone,cloud,reads,writes,choices,set onChoice(fn){onChoice=fn;}};
}
(async()=>{
 for(const phoneChoice of [true,false]){
  const app=fixture(phoneChoice),expected=phoneChoice?app.phone:app.cloud;
  await app.run('syncCloud(false)');
  assert.deepEqual(app.alerts,[],`${phoneChoice?'Telefon':'Felhő'} must complete without invalid-identifier/structure errors`);
  assert.equal(app.choices.length,1);assert.equal(app.writes.length,1);assert.deepEqual(app.reads,['head']);
  const merged=app.writes[0].data;
  for(const key of ['settings','exercises','plan','programs','activeProgramId','plannerSettings','exerciseFavorites','themeAccent','language'])assert.deepEqual(merged[key],expected[key],key);
  assert.equal(merged.history.find(h=>h.id==='workout-shared').exercises[0].sets[0].reps,phoneChoice?'8':'12');
  assert.deepEqual(merged.history.map(h=>h.id).sort(),['cloud-only','phone-only','workout-shared']);
  assert.ok(merged.history.every(h=>h.photos.length===1),'unique workout photo metadata survives');
  assert.equal(merged.weights.find(w=>w.id==='weight-shared').kg,phoneChoice?65:70);assert.equal(merged.weights.length,3);
  assert.deepEqual(merged.scheduled,expected.scheduled);
  app.run(`validateSync(${JSON.stringify(merged)});validateBackup(makeBackup());`);
  assert.equal(app.run(`history().find(h=>h.id==='phone-only').health240.averageHeartRate`),120);
  assert.equal(app.run(`history().find(h=>h.id==='workout-shared').healthWear129.totalCalories`),4);
  assert.equal(app.run('cloudBusy'),false);assert.equal(app.run('cloudDriveStage'),'');
  await app.run('syncCloud(false)');assert.equal(app.choices.length,1,'successful choice must not prompt again');assert.deepEqual(app.alerts,[]);
  if(phoneChoice){assert.equal(app.writes.length,1);assert.deepEqual(app.reads,['head'],'unchanged phone choice uses the latest-only fast path');}
  // Existing callers may return the selected value instead of a boolean.
  for(const selectPhone of [true,false]){
   const chosen=selectPhone?app.phone:app.cloud;
   const result=clone(app.run(`validateSync(mergeSync(${JSON.stringify(app.phone)},[${JSON.stringify(app.cloud)}],null,(key,a,b)=>${selectPhone?'a':'b'}))`));
   for(const key of ['settings','exercises','activeProgramId','exerciseFavorites','themeAccent','language'])assert.deepEqual(result[key],chosen[key],`value-returning chooser: ${key}`);
  }
 }
 const silent=fixture(true),beforeSilent=new Map(silent.values);await silent.run('syncCloud(true)');
 assert.equal(silent.choices.length,0);assert.equal(silent.writes.length,0);assert.deepEqual(silent.values,beforeSilent,'silent conflicts must not apply changes');
 const changed=fixture(true);changed.onChoice=()=>changed.run(`db.set('weights',[...weights(),{id:'during-dialog',kg:72,date:'2026-10-10T12:00:00Z'}]);`);await changed.run('syncCloud(false)');
 assert.equal(changed.writes.length,0,'edits made while choosing must not be overwritten or uploaded');assert.equal(changed.run(`weights().some(w=>w.id==='during-dialog')`),true);
 const invalid=fixture(true),beforeInvalid=new Map(invalid.values);invalid.cloud.exercises[0].id='bad\"identifier';await invalid.run('syncCloud(false)');
 assert.equal(invalid.choices.length,0);assert.equal(invalid.writes.length,0);assert.deepEqual(invalid.values,beforeInvalid,'invalid remote backups remain rejected before any data writes');
 console.log('PASS Drive conflict: Telefon/Felhő choices produce valid selected data, unique records/photos and local Health survive; value-returning choices, silent conflicts, concurrent edits and malformed input stay safe');
})().catch(e=>{console.error(e);process.exitCode=1});
