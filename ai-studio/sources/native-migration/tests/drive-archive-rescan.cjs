const assert=require('node:assert/strict'),fs=require('node:fs'),runtime=require('./helpers/app-runtime.cjs');
const safety=fs.readFileSync('www/index.html','utf8').match(/\/\* #18 — Drive sync safety[\s\S]*?window\.TrainPilotIssue18=\{version:'18',mergeSync:tp18MergeSync,selectSnapshots:tp18SnapshotSelection,legacyId\};\n\}\)\(\);/)[0];
const device='00000000-0000-0000-0000-000000000001',clone=x=>JSON.parse(JSON.stringify(x));
function fixture(count=24){
 const a=runtime();a.run(safety);const empty=clone(a.run('syncData()')),files=[],snapshots=new Map(),reads=[],stages=[];
 const add=(id,data,time)=>{const f={id,name:'repforge-sync-'+device+'-'+id+'.json',createdTime:time,version:'1'};files.push(f);snapshots.set(id,{app:'RepForgeSync',schema:1,owner:'owner',device,data:clone(data)});return f;};
 for(let i=0;i<count;i++){const data=clone(empty);data.weights=[{kg:70+i/10,date:new Date(Date.UTC(2025,0,i+1)).toISOString()}];add('archive-'+i,data,new Date(Date.UTC(2025,0,i+1)).toISOString());}
 add('head',empty,'2026-10-01T12:00:00Z');let writes=0,fail=null,mismatch=false,afterWrite=null;
 const bridge={driveList:async()=>({files:clone(files)}),driveRead:async({id})=>{reads.push(id);stages.push(a.run('cloudDriveStage'));if(id===fail)throw Error('offline');return {data:JSON.stringify(snapshots.get(id)),version:mismatch?'999':files.find(f=>f.id===id).version};},driveWrite:async({data})=>{const id='new-'+(++writes),parsed=JSON.parse(data);add(id,parsed.data,new Date(Date.UTC(2026,9,2+writes)).toISOString());await afterWrite?.();return {id,version:'1',verified:true};}};
 a.context.window.Capacitor={isNativePlatform:()=>true,Plugins:{GoogleSync:bridge}};
 a.run(`cloudProfile={sub:'owner'};db.set('cloudDevice',${JSON.stringify(device)});db.set('cloudPrefs',{drive:false,calendar:false});rf130SyncWorkoutPhotos=async()=>{};`);
 return {...a,files,snapshots,reads,stages,bridge,get writes(){return writes;},set fail(id){fail=id;},set mismatch(value){mismatch=value;},set afterWrite(fn){afterWrite=fn;},edit(kg=85){a.run(`db.set('weights',[...weights(),{id:'local-${kg}',kg:${kg},date:'2026-10-03T12:00:00Z'}]);`);}};
}
(async()=>{
 const normal=fixture();let prunes=0;normal.bridge.drivePrune=async()=>{prunes++;return {};};await normal.run('syncCloud(false)');assert.deepEqual(normal.reads,['head'],'normal first sync reads only the latest device snapshot, not 24 old backups');normal.edit();normal.reads.length=0;await normal.run('syncCloud(false)');assert.equal(normal.reads.length,1);assert.equal(prunes,0,'ordinary sync must not inspect old snapshots during automatic cleanup either');
 const a=fixture();await a.run('syncCloud(false,true)');assert.equal(a.reads.length,25,'explicit recovery must read all selected archives');assert.equal(a.run('weights().length'),24,'all unique legacy weights must survive');assert.ok(a.stages.some(s=>s.includes('Korábbi mentés')));
 a.reads.length=0;await a.run('syncCloud(false)');assert.equal(a.reads.length,0,'unchanged data skips all snapshots');
 a.edit();await a.run('syncCloud(false)');assert.equal(a.reads.length,1,'local change reads the latest head, not 24 already committed archives');assert.equal(a.run('weights().length'),25);
 a.reads.length=0;const changed=a.files.find(f=>f.id==='archive-0');changed.version='2';a.snapshots.get(changed.id).data.weights.push({kg:92,date:'2025-02-15T12:00:00Z'});await a.run('syncCloud(false,true)');assert.deepEqual(a.reads.sort(),['archive-0','new-2']);assert.equal(a.run('weights().length'),26,'explicit recovery reads in-place archive changes');
 // Restores/consent changes remove the base; receipts cannot bypass that scan.
 a.reads.length=0;a.run(`localStorage.removeItem('repforge:cloudBase:owner');`);await a.run('syncCloud(false,true)');assert.ok(a.reads.length>=25);
 // An incomplete archive read must remain retryable even with unchanged heads.
 const b=fixture(2);b.fail='archive-0';await b.run('syncCloud(false,true)');assert.equal(b.run(`db.get('cloudArchiveScan161:owner').versions['archive-0']`),undefined);b.reads.length=0;b.fail=null;await b.run('syncCloud(false,true)');assert.ok(b.reads.includes('archive-0'));assert.equal(b.run('weights().length'),2);
 // Never trust version metadata which changed between listing and reading.
 const c=fixture(2);c.mismatch=true;await c.run('syncCloud(false,true)');c.reads.length=0;c.edit();await c.run('syncCloud(false,true)');assert.ok(c.reads.includes('archive-0')&&c.reads.includes('archive-1'));
 // A failed final commit must not record successful reads as incorporated data.
 const d=fixture(2);d.afterWrite=async()=>d.run(`db.set('weights',[{id:'intervening',kg:95,date:'2026-10-03T12:00:00Z'}]);`);await d.run('syncCloud(false)');assert.equal(d.run(`db.get('cloudArchiveScan161:owner',null)`),null);assert.equal(d.run('weights()[0].kg'),95);
 // Old bridges without file versions remain conservative; failed retry is safe.
 const e=fixture(2);e.files.forEach(f=>delete f.version);await e.run('syncCloud(false,true)');e.reads.length=0;e.edit();await e.run('syncCloud(false,true)');assert.ok(e.reads.includes('archive-0')&&e.reads.includes('archive-1'));
 const f=fixture(1);await f.run('syncCloud(false,true)');f.reads.length=0;f.files.at(-1).version='2';f.snapshots.get(f.files.at(-1).id).data.weights.push({id:'remote-change',kg:96,date:'2026-10-04T12:00:00Z'});await f.run('syncCloud(false)');assert.ok(f.reads.includes(f.files.at(-2).id),'same-ID head version change must not use the fast path');assert.equal(f.run('weights().length'),2);
 assert.equal(a.run(`driveHasPendingPhotos([{photos:[{id:'deleted',deletedAt:'2026-10-01',driveFileId:null}]}])`),false,'completed deletion is not pending forever');assert.equal(a.run(`driveHasPendingPhotos([{photos:[{id:'deleted',deletedAt:'2026-10-01',driveFileId:'pending'}]}])`),true);assert.equal(a.run(`driveHasPendingPhotos([{photos:[{id:'new',driveFileId:null}]}])`),true);
 assert.deepEqual(a.alerts,[]);assert.equal(a.run('cloudBusy'),false);assert.equal(a.run('cloudDriveStage'),'');
 console.log('PASS Drive: ordinary sync reads only latest device snapshots, no archive reads/pruning; explicit recovery, version changes, retry, restore, concurrent edits and deleted photos');
})().catch(e=>{console.error(e);process.exitCode=1});
