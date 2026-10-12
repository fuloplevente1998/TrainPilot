'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),runtime=require('./helpers/app-runtime.cjs');
const safety=fs.readFileSync('www/index.html','utf8').match(/\/\* #18 — Drive sync safety[\s\S]*?window\.TrainPilotIssue18=\{version:'18',mergeSync:tp18MergeSync,selectSnapshots:tp18SnapshotSelection,legacyId\};\n\}\)\(\);/)[0];
const clone=x=>JSON.parse(JSON.stringify(x)),localDevice='11111111-1111-1111-1111-111111111111',remoteDevice='22222222-2222-2222-2222-222222222222';
function fixture(){
 const app=runtime();app.run(safety);const base=clone(app.run('syncData()'));base.themeAccent='classicBlue';
 const files=[],snapshots=new Map(),writes=[],choices=[];let failWrite=false,readVersion=null,afterWrite=null,phone=true;
 const add=(id,device,data)=>{files.push({id,name:'repforge-sync-'+device+'-'+id+'.json',createdTime:new Date(Date.UTC(2026,9,11,10,files.length)).toISOString(),version:'1'});snapshots.set(id,{app:'RepForgeSync',schema:1,owner:'theme-owner',device,data:clone(data)});};
 add('old-blue',remoteDevice,base);
 const bridge={driveList:async()=>({files:clone(files)}),driveRead:async({id})=>({version:readVersion||files.find(f=>f.id===id).version,data:JSON.stringify(snapshots.get(id))}),driveWrite:async({data})=>{const snapshot=JSON.parse(data),id='write-'+(writes.length+1);writes.push(snapshot);add(id,localDevice,snapshot.data);await afterWrite?.();return {id,version:'1',verified:!failWrite}}};
 app.context.window.Capacitor={isNativePlatform:()=>true,Plugins:{GoogleSync:bridge}};
 app.context.themeChoice=async()=>{choices.push(phone);return phone};
 app.run(`cloudProfile={sub:'theme-owner'};db.set('cloudDevice',${JSON.stringify(localDevice)});db.set('cloudPrefs',{drive:false,calendar:false});db.set('cloudBase:theme-owner',${JSON.stringify(base)});rf130SyncWorkoutPhotos=async()=>{};tp2628Confirm=themeChoice;rf200SetTheme('green');`);
 return {...app,base,files,snapshots,writes,choices,bridge,add,
  edit(){app.run(`db.set('weights',[...weights(),{id:'theme-weight-'+weights().length,kg:79,date:'2026-10-11T12:00:00Z'}]);`);},
  remote(theme='classicBlue'){const data=clone(base);data.themeAccent=theme;data.weights=[{id:'remote-weight',kg:81,date:'2026-10-11T12:30:00Z'}];add('remote-'+files.length,remoteDevice,data);},
  get receipt(){return app.values.get('repforge:cloudArchiveScan161:theme-owner');},
  set phone(value){phone=value;},set failWrite(value){failWrite=value;},set readVersion(value){readVersion=value;},set afterWrite(value){afterWrite=value;}
 };
}
(async()=>{
 const a=fixture();await a.run('syncCloud(true)');assert.equal(a.run('rf200ThemeKey()'),'green');
 for(const silent of [true,false,true]){a.edit();await a.run(`syncCloud(${silent})`);assert.equal(a.run('rf200ThemeKey()'),'green','unrelated save/sync must not replay an old blue theme');}
 a.remote();await a.run('syncCloud(true)');assert.equal(a.run('rf200ThemeKey()'),'green','new remote data with unchanged old theme must not revert the color');
 assert.equal(a.run(`weights().some(w=>w.id==='remote-weight')`),true,'remote weight data still imports');
 a.remote('red');await a.run('syncCloud(true)');assert.equal(a.run('rf200ThemeKey()'),'red','genuinely changed remote theme remains syncable');
 a.edit();await a.run('syncCloud(true)');assert.equal(a.run('rf200ThemeKey()'),'red');assert.deepEqual(a.choices,[]);assert.deepEqual(a.alerts,[]);
 assert.equal(JSON.parse(a.receipt).themeByDevice[remoteDevice],'red','receipt records the actual observed remote color');
 for(const phone of [true,false]){
  const f=fixture();await f.run('syncCloud(true)');f.run(`rf200SetTheme('pink');`);f.remote('red');f.phone=phone;
  await f.run('syncCloud(false)');assert.equal(f.run('rf200ThemeKey()'),phone?'pink':'red','concurrent preference changes retain explicit conflict choice');assert.equal(f.choices.length,1);
 }
 const old=fixture();await old.run('syncCloud(true)');old.run(`const oldReceipt=db.get('cloudArchiveScan161:theme-owner');delete oldReceipt.themeByDevice;db.set('cloudArchiveScan161:theme-owner',oldReceipt);`);
 old.edit();await old.run('syncCloud(true)');assert.equal(old.run('rf200ThemeKey()'),'green','successful verified heads migrate without previous theme receipts');
 const mismatch=fixture();await mismatch.run('syncCloud(true)');mismatch.run(`const receipt=db.get('cloudArchiveScan161:theme-owner');delete receipt.themeByDevice;db.set('cloudArchiveScan161:theme-owner',receipt);`);
 mismatch.snapshots.get('old-blue').data.themeAccent='purple';mismatch.readVersion='2';mismatch.edit();await mismatch.run('syncCloud(true)');
 assert.equal(mismatch.run('rf200ThemeKey()'),'purple','mismatched read versions cannot hide a changed preference');
 const legacy=fixture();legacy.files.forEach(f=>delete f.version);await legacy.run('syncCloud(true)');legacy.edit();await legacy.run('syncCloud(true)');assert.equal(legacy.run('rf200ThemeKey()'),'green','observed colors work with older bridges lacking versions');
 const changed=fixture();await changed.run('syncCloud(true)');const receipt=changed.receipt;changed.edit();changed.afterWrite=()=>changed.run(`rf200SetTheme('pink');`);
 await changed.run('syncCloud(true)');assert.equal(changed.run('rf200ThemeKey()'),'pink','local selection during upload survives');assert.equal(changed.receipt,receipt,'concurrent failed commit does not record observations');
 const failed=fixture();await failed.run('syncCloud(true)');const failedReceipt=failed.receipt;failed.remote('purple');failed.failWrite=true;failed.edit();await failed.run('syncCloud(true)');assert.equal(failed.run('rf200ThemeKey()'),'green');assert.equal(failed.receipt,failedReceipt,'unverified uploads cannot advance receipts');
 const invalid=fixture();await invalid.run('syncCloud(true)');const invalidReceipt=invalid.receipt;invalid.snapshots.get('old-blue').data.themeAccent='invalid-theme';invalid.edit();await invalid.run('syncCloud(true)');assert.equal(invalid.run('rf200ThemeKey()'),'green');assert.equal(invalid.receipt,invalidReceipt,'invalid remote preferences are rejected before masking');
 const account=fixture();await account.run('syncCloud(true)');account.files.splice(1);account.run(`cloudProfile={sub:'other-owner'};`);account.snapshots.get('old-blue').owner='other-owner';account.snapshots.get('write-1').owner='other-owner';account.snapshots.get('old-blue').data.themeAccent='purple';account.phone=false;await account.run('syncCloud(false)');assert.equal(account.run('rf200ThemeKey()'),'purple','observations from another account cannot hide preferences');
 const restore=fixture();await restore.run('syncCloud(true)');const backup=clone(restore.run('makeBackup()'));assert.equal(backup.themeAccent,'green');backup.themeAccent='purple';
 assert.equal(await restore.run(`restoreText(${JSON.stringify(JSON.stringify(backup))})`),true);assert.equal(restore.run('rf200ThemeKey()'),'purple','explicit file restore still restores its chosen theme');
 const queue=fixture();queue.run(`db.set('cloudPrefs',{drive:true,calendar:false});cloudBusy=true;cloudDirty=false;rf200SetTheme('purple');`);assert.equal(queue.run('cloudDirty'),true,'theme choice queues sync while busy');
 console.log('PASS #173: repeated save/sync, old/new heads, real remote/concurrent themes, migrated/legacy receipts, version mismatch, account isolation, failed commits, file restore and queued theme sync');
})().catch(error=>{console.error(error);process.exitCode=1});
