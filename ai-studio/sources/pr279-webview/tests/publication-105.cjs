const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const app=fs.readFileSync('www/app.js','utf8');
const values=new Map();let writes=0,html='';
const root={get innerHTML(){return html},set innerHTML(v){writes++;html=v}};
const classList={add(){},remove(){},contains(){return false},toggle(){}};
const node=()=>({innerHTML:'',textContent:'',style:{setProperty(){}},dataset:{},classList:{...classList},children:[],parentNode:null,querySelector:()=>null,querySelectorAll:()=>[],appendChild(){},prepend(){},insertBefore(){},insertAdjacentElement(){},insertAdjacentHTML(){},remove(){},setAttribute(){},removeAttribute(){},closest(){return null},addEventListener(){},focus(){}});
const documentElement={...node(),classList:{...classList},scrollTop:0};
const body=node(),head=node();
const doc={hidden:false,documentElement,body,head,querySelector:s=>s==='#app'?root:null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener(){},createElement:node};
let tasks=[],cloudChecks=0;
const ctx=vm.createContext({localStorage:{getItem:k=>values.get(k)??null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)},document:doc,window:{Capacitor:{isNativePlatform:()=>true,Plugins:{GoogleSync:{status:async()=>{cloudChecks++;return {profile:null}}}}},addEventListener(){},scrollTo(){},open(){}},navigator:{onLine:true,languages:['hu-HU'],language:'hu-HU'},performance:{now:()=>1},requestAnimationFrame:fn=>{fn();return 1},setInterval:()=>1,clearInterval(){},setTimeout:(fn,ms)=>{if(!ms)tasks.push(fn);return 1},clearTimeout(){},Date,crypto:require('node:crypto').webcrypto,TextEncoder,Blob,URL,alert(){},confirm:()=>true,prompt:()=>null,console});
vm.runInContext(app,ctx,{filename:'app.js'});
const run=s=>vm.runInContext(s,ctx);

const safety=fs.readFileSync('www/index.html','utf8').match(/\/\* #18 — Drive sync safety[\s\S]*?window\.TrainPilotIssue18=\{version:'18',mergeSync:tp18MergeSync,selectSnapshots:tp18SnapshotSelection,legacyId\};\n\}\)\(\);/)[0];
vm.runInContext(safety,ctx);
(async()=>{
 run(`db.set('history',[{id:'keep',workout:'A',started:'2026-09-30T10:00:00Z',finished:'2026-09-30T11:00:00Z',exercises:[],photos:[],health240:{averageHeartRate:120}}]);db.set('recoveryHistory',[{day:'2026-09-30',sleepHours:8}]);`);
 let backup=run('makeBackup()');
 assert.equal(backup.appVersion,require('../package.json').version);assert.equal(backup.healthIncluded,false);assert.equal(backup.history[0].health240,undefined);assert.equal(backup.recoveryHistory.length,0);assert.equal(backup.healthLedger,undefined);
 assert.equal(run('history()[0].health240.averageHeartRate'),120,'export must not mutate local Health data');
 run(`let local105=syncData(),remote105=JSON.parse(JSON.stringify(local105));remote105.programs.push({...remote105.programs[0],id:'custom-test',name:'My program',builtin:false});remote105.activeProgramId='custom-test';remote105.plannerSettings={mode:'daily',time:'19:00',minutes:50};remote105.exerciseFavorites=['db-squat'];remote105.themeAccent='classicBlue';remote105.language='de';let merged105=mergeSync(local105,[remote105],local105,()=>{throw Error('unexpected conflict')});storeMerged(merged105);`);
 assert.equal(run('activeProgramId()'),'custom-test');assert.equal(run("programById('custom-test').name"),'My program');assert.equal(run('plannerSettings().mode'),'daily');assert.equal(run("rf144Favorites().join(',')"),'db-squat');assert.equal(run('rf212LangSetting()'),'de');assert.equal(run('history()[0].health240.averageHeartRate'),120,'redacted cloud payload must preserve local Health enrichment');assert.equal(run('rf229RecoveryHistory().length'),1);
 run(`db.set('privacyPrefs',{includeHealth:true});`);backup=run('makeBackup()');assert.equal(backup.healthIncluded,true);assert.equal(backup.history[0].health240.averageHeartRate,120);assert.equal(backup.recoveryHistory.length,1);assert.ok(backup.healthLedger);
 run(`let healthRemote=JSON.parse(JSON.stringify(syncData()));healthRemote.recoveryHistory=[{day:'2026-10-01',sleepHours:7,syncedAt:'2026-10-01T10:00:00Z'}];storeMerged(mergeSync(syncData(),[healthRemote],syncData(),()=>true));`);
 assert.equal(run('rf229RecoveryHistory().length'),2,'opted-in recovery history must merge across days');
 const before=values.get('repforge:programs');let failOnce=true;const set=ctx.localStorage.setItem;
 ctx.localStorage.setItem=(k,v)=>{if(failOnce&&k==='repforge:programs'){failOnce=false;throw Error('quota');}set(k,v);};
 assert.throws(()=>run(`let bad105=syncData();bad105.programs[0].name='BROKEN';storeMerged(bad105);`));assert.equal(values.get('repforge:programs'),before,'failed cloud apply must restore original programs');ctx.localStorage.setItem=set;
 run(`tp2628Confirm=async()=>false;`);const text=JSON.stringify(run('makeBackup()'));const unchanged=new Map(values);assert.equal(await run(`restoreText(${JSON.stringify(text)})`),false);assert.deepEqual(values,unchanged,'cancelled restore must write nothing');
 run(`tp2628Confirm=async()=>true;`);backup=JSON.parse(text);backup.exerciseFavorites=['db-curl'];backup.language='ro';backup.themeAccent='green';backup.activeProgramId='home-basic';backup.recoveryHistory=[];
 assert.equal(await run(`restoreText(${JSON.stringify(JSON.stringify(backup))})`),true);assert.equal(run('activeProgramId()'),'home-basic');assert.equal(run('rf212LangSetting()'),'ro');assert.equal(run('rf144Favorites()[0]'),'db-curl');assert.equal(run('rf229RecoveryHistory().length'),0,'an empty restored recovery history must replace old rows');
 backup=run('makeBackup()');backup.history=[];const originalHistory=values.get('repforge:history');failOnce=true;ctx.localStorage.setItem=(k,v)=>{if(failOnce&&k==='repforge:programs'){failOnce=false;throw Error('quota');}set(k,v);};
 await assert.rejects(run(`restoreText(${JSON.stringify(JSON.stringify(backup))})`));assert.equal(values.get('repforge:history'),originalHistory,'failed manual restore must roll back earlier writes');ctx.localStorage.setItem=set;
 assert.throws(()=>run(`validateBackup({...makeBackup(),language:'bad'})`));assert.throws(()=>run(`validateBackup({...makeBackup(),recoveryHistory:[{day:'bad'}]})`));
 run(`db.set('privacyPrefs',{includeHealth:false});`);assert.equal(run('syncData().history[0].health240'),undefined);assert.equal(run('syncData().recoveryHistory.length'),0);

 let rollbacks=0;ctx.window.Capacitor.Plugins.BackupArchive={install:async()=>{throw Error('install failed');},rollback:async()=>{rollbacks++;}};
 const restoreInput=JSON.stringify(run('makeBackup()')),restoreBefore=new Map(values);
 await assert.rejects(run(`restoreText(${JSON.stringify(restoreInput)},'00000000-0000-0000-0000-000000000001')`));assert.equal(rollbacks,1,'even a partially failed native install requires rollback');assert.deepEqual(values,restoreBefore);
 ctx.window.Capacitor.Plugins.BackupArchive.rollback=async()=>{throw Error('rollback interrupted');};
 await assert.rejects(run(`restoreText(${JSON.stringify(restoreInput)},'00000000-0000-0000-0000-000000000001')`));assert.ok(values.has('repforge:archiveRestore105'),'failed rollback must preserve its durable recovery journal');
 // Execute the real startup recovery entry point with an interrupted local dataset.
 const recovered=new Map([['repforge:history','new'],['repforge:archiveRestore105',JSON.stringify({token:'restore-token',old:[['history','old'],['draft',null]]})]]);let loaded,rollbackToken;
 const recovery={localStorage:{getItem:k=>recovered.get(k)||null,setItem:(k,v)=>recovered.set(k,v),removeItem:k=>recovered.delete(k)},window:{Capacitor:{isNativePlatform:()=>true,Plugins:{BackupArchive:{recover:async x=>{rollbackToken=x.rollbackToken;assert.equal(recovery.window.TrainPilotBackupBusy,true);}}}},addEventListener:(event,fn)=>{if(event==='DOMContentLoaded')loaded=fn;}},alert:e=>{throw Error(e);}};
 vm.runInNewContext(app.slice(0,app.indexOf('// @section')),recovery);assert.equal(recovered.get('repforge:history'),'old');await loaded();assert.equal(rollbackToken,'restore-token');assert.equal(recovered.has('repforge:archiveRestore105'),false);assert.equal(recovery.window.TrainPilotBackupBusy,false);
 values.delete('repforge:archiveRestore105');
 console.log('PASS publication: complete cloud apply, Health consent/privacy, recovery merge, async restore, cancellation and quota rollback');
})().catch(e=>{console.error(e);process.exitCode=1});
