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
 // ZIP must not wait for DNS, a Google request or a Calendar operation to complete.
 const a=runtime(),network=deferred();a.google.driveList=()=>network.promise;
 const syncing=a.run('syncCloud(true)');await a.run('tp105Archive(false)');assert.equal(a.saved.length,1);assert.equal(a.run('cloudBusy'),true);
 await a.run('tp105Archive(true)');assert.equal(a.saved.length,1,'restore remains protected while Drive can change the dataset');
 network.reject(Object.assign(Error('A Google nem érhető el hálózati hiba miatt.'),{code:'GOOGLE_NETWORK'}));await syncing;
 assert.equal(a.run('cloudBusy'),false);assert.equal(a.run('cloudActiveOperation'),'');assert.equal(a.run('cloudBackupWaiters.length'),0);
 const retry=a.timers.filter(t=>t.active&&t.ms>=59000);assert.equal(retry.length,1,'failed automatic sync gets one delayed retry, not a 1.8-second loop');
 await a.run('tp105Archive(false)');assert.equal(a.saved.length,2,'offline backup must still work during the automatic retry cooldown');
 // A manual retry bypasses the cooldown and a successful request clears it.
 a.google.driveList=async()=>({files:[]});await a.run('syncCloud(false)');assert.equal(a.run('cloudRetryAt'),0);
 const c=runtime(),prepare=deferred(),picker=deferred();let listed=0;c.google.calendarPrepare=()=>prepare.promise;c.google.driveList=async()=>{listed++;return {files:[]};};
 c.archive.save=async args=>{c.saved.push(JSON.parse(args.data));await picker.promise;return {cancelled:true};};
 c.run('cloudChanged()');
 const auto=c.timers.filter(t=>t.active&&t.ms===1800).at(-1);assert.ok(auto);auto.active=false;const autoRun=auto.fn();await settle(()=>c.run('cloudActiveOperation')==='calendar');
 const zip=c.run('tp105Archive(false)');await settle(()=>c.saved.length===1);prepare.resolve({id:'calendar'});await autoRun;
 assert.equal(listed,0,'the Calendar-to-Drive automatic chain must yield to an open ZIP picker');assert.equal(c.run('cloudDirty'),true);
 picker.resolve();await zip;assert.equal(c.run('window.TrainPilotBackupBusy'),false);assert.equal(c.run('cloudDirty'),false);assert.ok(c.timers.some(t=>t.active&&t.ms===1800),'automatic work must resume after cancellation');
 // Remote tombstones may not delete local files before a verified snapshot/local commit.
 const b=runtime();b.run(`db.set('history',[{id:'workout',workout:'A',started:'2026-10-01T10:00:00Z',finished:'2026-10-01T11:00:00Z',exercises:[],photos:[{id:${JSON.stringify(photoId)},label:'after',createdAt:'2026-10-01T11:00:00Z',updatedAt:1,driveFileId:'photo-file',deletedAt:null}]}]);`);
 const remote=JSON.parse(JSON.stringify(b.run('syncData()')));remote.history[0].photos[0].deletedAt='2026-10-01T12:00:00Z';remote.history[0].photos[0].updatedAt=2;
 b.google.driveList=async()=>({files:[{id:'old',name:'repforge-sync-'+device+'-snapshot.json',createdTime:'2026-10-01T12:00:00Z'}]});b.google.driveRead=async()=>({data:JSON.stringify({app:'RepForgeSync',schema:1,owner:'owner',device,data:remote})});
 const write=deferred(),save=deferred();let writing=false;b.google.driveWrite=()=>{writing=true;return write.promise;};
 b.archive.save=async args=>{b.saved.push(JSON.parse(args.data));await save.promise;return {verified:true,name:'backup.zip',bytes:100};};
 const drive=b.run('syncCloud(true)');await settle(()=>writing);assert.equal(b.deleted.length,0,'remote tombstones must not remove the old local snapshot photos before commit');
 const backup=b.run('tp105Archive(false)');await settle(()=>b.saved.length===1);write.resolve({id:'new',verified:true});await settle(()=>b.run('cloudBackupWaiters.length')===1);
 assert.equal(b.run('history()[0].photos[0].deletedAt'),null);assert.equal(b.deleted.length,0);assert.equal(b.saved[0].history[0].photos[0].deletedAt,null);
 assert.equal(b.run('cloudDriveStage'),b.run('tp149T("cloud.driveBackupWait")'));
 save.resolve();await backup;await drive;assert.ok(b.run('history()[0].photos[0].deletedAt'));assert.deepEqual(b.deleted,[photoId]);assert.equal(b.run('cloudBackupWaiters.length'),0);
 // Failed/cancelled ZIP must release its reservation and recheck intervening local edits.
 const e=runtime(),write2=deferred(),save2=deferred();let writing2=false;e.google.driveWrite=()=>{writing2=true;return write2.promise;};e.archive.save=async()=>save2.promise;
 const drive2=e.run('syncCloud(true)');await settle(()=>writing2);const backup2=e.run('tp105Archive(false)');write2.resolve({id:'written',verified:true});await settle(()=>e.run('cloudBackupWaiters.length')===1);
 e.run(`db.set('weights',[{id:'weight',date:'2026-10-01T12:00:00Z',kg:71}]);`);save2.reject(Error('No space'));await backup2;await drive2;
 assert.equal(e.run('weights()[0].kg'),71,'a local edit made while waiting must not be overwritten');assert.equal(e.run('cloudBusy'),false);assert.equal(e.run('window.TrainPilotBackupBusy'),false);assert.equal(e.run('cloudBackupWaiters.length'),0);
 assert.equal(e.run('cloudMessage'),e.run('tp149T("cloud.changedLocal")'));
 console.log('PASS backup priority: pending Drive/Calendar, DNS cooldown/manual retry, queued auto-sync, photo tombstones, ZIP cancel/error and concurrent edits');
})().catch(e=>{console.error(e);process.exitCode=1;});
