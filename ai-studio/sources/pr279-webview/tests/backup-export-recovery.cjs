const assert=require('node:assert/strict');
const {runtime,photoId}=require('./helpers/native-backup-runtime.cjs');
const photo={id:photoId,label:'utána',createdAt:'2026-10-13T08:00:00Z',driveFileId:'drive-photo',deletedAt:null};
const workout={id:'logged-workout',workout:'Alap A',started:'2026-10-13T08:00:00.000Z',finished:'2026-10-13T08:30:00.000Z',exercises:[],photos:[photo]};
const install=a=>a.run(`db.set('history',${JSON.stringify([workout])});`);
(async()=>{
 // Same complete Unicode snapshot fits compact JSON, but not the former pretty export.
 const a=runtime();let file;
 a.run(`window.Capacitor.Plugins.NativeFiles={save:async args=>{window.fileProbe=args;return {cancelled:true}}};window.TrainPilotHealthJournal={backup:async()=>window.largeSnapshot};`);
 const data={history:[],values:Array.from({length:700000},()=>({value:'árvíz'}))};
 assert.ok(Buffer.byteLength(JSON.stringify(data,null,2))>20*1024*1024);
 assert.ok(Buffer.byteLength(JSON.stringify(data))<20*1024*1024);
 a.run(`window.largeSnapshot=${JSON.stringify(data)};`);await a.run('exportData()');file=a.run('window.fileProbe');assert.deepEqual(JSON.parse(file.data),data);assert.equal(a.alerts.length,0);
 a.run(`window.largeSnapshot={value:'ő'.repeat(11*1024*1024)};window.fileProbe=null;`);await a.run('exportData()');assert.equal(a.run('window.fileProbe'),null,'UTF-8 limit is checked before opening the picker');assert.ok(a.alerts.at(-1).includes('20 MB'));
 const b=runtime();install(b);const before=b.run('JSON.stringify(history())');let downloaded=0,present=false;
 b.photos.exists=async()=>({exists:present,bytes:present?1000:0});b.google.drivePhotoRead=async args=>{assert.equal(args.id,'drive-photo');assert.equal(args.photoId,photoId);assert.equal(args.silent,true);downloaded++;present=true;};
 await b.run('tp105Archive(false)');assert.equal(downloaded,1);assert.equal(b.saved.length,1);assert.deepEqual(b.saved[0].history,[workout]);assert.equal(b.run('JSON.stringify(history())'),before);
 await b.run('tp105Archive(false)');assert.equal(downloaded,1,'available local files do not need network');
 for(const [status,message]of [[{exists:false,bytes:0},'hiányzik'],[{exists:true,bytes:0},'üres'],[{exists:true,bytes:6*1024*1024+1},'6 MB']]){
  const c=runtime();install(c);c.run('cloudProfile=null');c.photos.exists=async()=>status;await c.run('tp105Archive(false)');assert.equal(c.saved.length,0,'incomplete archive never reaches the picker');assert.ok(c.alerts[0].includes(message));assert.ok(c.alerts[0].includes('Alap A'));assert.equal(c.run('window.TrainPilotBackupBusy'),false);assert.equal(c.run('JSON.stringify(history())'),before);
 }
 const d=runtime();install(d);d.photos.exists=async()=>({exists:false,bytes:0});d.google.drivePhotoRead=async()=>{throw Error('Hálózati hiba')};await d.run('tp105Archive(false)');assert.equal(d.saved.length,0);assert.ok(d.alerts[0].includes('Hálózati hiba'));assert.equal(d.run('window.TrainPilotBackupBusy'),false);
 const e=runtime();e.run(`db.set('history',${JSON.stringify([{...workout,photos:[{...photo,deletedAt:'2026-10-13T09:00:00Z'}]}])});`);e.photos.exists=async()=>{throw Error('deleted photos must not be read')};await e.run('tp105Archive(false)');assert.equal(e.saved.length,1,'deleted photo metadata is retained without requiring the deleted file');
 const f=runtime();install(f);let calls=0;f.photos.exists=async()=>{calls++;return {exists:true,bytes:1000}};f.run(`db.set('history',${JSON.stringify([workout,{...workout,id:'other'}])});`);await f.run('tp105Archive(false)');assert.equal(calls,1,'duplicate references are checked once');assert.equal(f.saved[0].history.length,2);
 console.log('PASS backup recovery: lossless large/Unicode compact JSON, UTF-8 limit, local/Drive photos, missing/empty/oversize/download failure, tombstones, deduplication and lock release');
})().catch(error=>{console.error(error);process.exitCode=1;});
