const assert=require('node:assert/strict');
const {run,values}=require('./helpers/app-runtime.cjs')();
const plain=value=>JSON.parse(JSON.stringify(value));
(async()=>{
 run(`
  db.set('history',[{id:'legacy-h-1',workout:'A',programId:'home-basic',dayId:'A',started:'2026-10-01T10:00:00Z',finished:'2026-10-01T11:00:00Z',exercises:[{id:'running',hu:'Futás',repUnit:'mp',loadType:'bodyweight',measurementType:'distance',sets:[{set:1,setId:'set-1',weight:0,reps:'1800',done:true,distanceMeters:5000,distanceSource:'manual'}]}],health240:{steps:1234},photos:[{id:'00000000-0000-0000-0000-000000000001',label:'after',driveFileId:null}]}]);
  db.set('weights',[{id:'weight-1',date:'2026-10-01',kg:80}]);
  db.set('exerciseFavorites',['running']);db.set('themeAccent','green');db.set('language','ro');
  db.set('recoveryHistory',[{day:'2026-10-01',sleepHours:8}]);db.set('wellnessLatest',{weightKg:80});
 `);
 const original=new Map(values),backup=plain(run('makeBackup()'));
 assert.equal(backup.appVersion,require('../package.json').version);
 assert.equal(backup.version,3);assert.equal(backup.healthIncluded,false);
 assert.deepEqual(backup.history[0].exercises[0].sets[0],{set:1,setId:'set-1',weight:0,reps:'1800',done:true,distanceMeters:5000,distanceSource:'manual'});
 assert.equal(backup.history[0].photos.length,1);assert.equal(backup.history[0].health240,undefined);
 assert.deepEqual(backup.recoveryHistory,[]);assert.deepEqual(backup.exerciseFavorites,['running']);assert.equal(backup.language,'ro');assert.equal(backup.themeAccent,'green');
 for(const [field,read] of Object.entries({weights:'weights()',programs:'programs()',activeProgramId:'activeProgramId()',plannerSettings:'plannerSettings()',settings:'settings()',plan:"db.get('plan',DEFAULT_PLAN)",exercises:'exercises()',scheduled:'scheduled()'}))assert.deepEqual(backup[field],plain(run(read)),field+' must preserve its complete dataset');
 // Catalog merging may write during the first snapshot. Further exports must not
 // mutate persistent Health data or consume/change photo references.
 const before=new Map(values);run('makeBackup()');assert.deepEqual(values,before);assert.equal(run('history()[0].health240.steps'),1234);
 assert.equal(backup.history[0].photos[0].driveFileId,null);assert.equal(original.get('repforge:history'),values.get('repforge:history'));
 run("db.set('privacyPrefs',{includeHealth:true})");
 const inclusive=plain(run('makeBackup()')),sync=plain(run('syncData()'));
 assert.equal(inclusive.history[0].health240.steps,1234);assert.equal(inclusive.recoveryHistory[0].sleepHours,8);assert.ok(inclusive.healthLedger);assert.deepEqual(inclusive.wellnessLatest,{weightKg:80});
 assert.equal(sync.healthLedger,undefined);assert.equal(sync.wellnessLatest,undefined);assert.equal(sync.history[0].health240.steps,1234);
 run("validateBackup(makeBackup());tp2628Confirm=async()=>true");
 for(const mutate of [d=>d.programs[0].id="bad');alert(1);//",d=>d.programs[0].days[0].id='bad\\id',d=>d.exercises[0].id='<bad>',d=>d.history[0].id="bad'",d=>d.history[0].exercises[0].sets[0].setId='constructor',d=>d.exerciseFavorites=['bad\nname']]){
  const data=plain(run('makeBackup()'));mutate(data);const state=new Map(values);
  await assert.rejects(run(`restoreText(${JSON.stringify(JSON.stringify(data))})`));assert.deepEqual(values,state,'unsafe import must leave every local key unchanged');
 }
 const legacy={version:1,exercises:[{id:'old-squat',hu:'Guggolás',en:'Squat',reps:'8',sets:2,weight:10}],settings:{rest:90},plan:{A:['old-squat'],B:['old-squat']},weights:[{date:'2025-01-01',kg:80}],history:[8,10].map(reps=>({workout:'A',started:'2025-01-01T10:00:00Z',finished:'2025-01-01T11:00:00Z',exercises:[{id:'old-squat',hu:'Guggolás',sets:[{set:1,weight:10,reps:String(reps),done:true}]}]}))};
 assert.equal(await run(`restoreText(${JSON.stringify(JSON.stringify(legacy))})`),true);
 assert.equal(run('history().length'),2,'distinct same-time ID-less legacy workouts must survive import');
 assert.deepEqual(plain(run('history().map(h=>h.exercises[0].sets[0].reps)')),['8','10']);
 assert.equal(run('weights()[0].kg'),80);
 console.log('PASS consolidated backup: complete dataset, Health consent, read-only export, distance/photos, safe identifiers and v1 legacy import');
})().catch(error=>{console.error(error);process.exitCode=1;});
