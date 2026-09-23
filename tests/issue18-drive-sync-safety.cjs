const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('www/index.html','utf8');
const m=html.match(/\/\* #18 — Drive sync safety[\s\S]*?window\.TrainPilotIssue18=\{version:'18',mergeSync:tp18MergeSync,selectSnapshots:tp18SnapshotSelection,legacyId\};\n\}\)\(\);/);
assert.ok(m,'#18 runtime safety block missing');
const canonical=x=>Array.isArray(x)?'['+x.map(canonical).join(',')+']':x&&typeof x==='object'?'{'+Object.keys(x).sort().map(k=>JSON.stringify(k)+':'+canonical(x[k])).join(',')+'}':JSON.stringify(x);
const ctx=vm.createContext({window:{},canonical,mergeSync(){},syncCloud(){},recordKey:(kind,x)=>kind==='scheduled'?x.id:(x.id||''),console});
vm.runInContext(m[0],ctx);
const api=ctx.window.TrainPilotIssue18;
assert.equal(api.version,'18');

const shell=(history=[],weights=[])=>({history,weights,scheduled:[],settings:{rest:90},exercises:[],plan:{A:[],B:[]},programs:[],activeProgramId:'home-basic',plannerSettings:{}});
const h=(reps,extra={})=>({workout:'A',started:'2025-01-01T10:00:00.000Z',finished:'2025-01-01T11:00:00.000Z',exercises:[{id:'sq',hu:'Guggolás',sets:[{weight:10,reps,done:true}]}],...extra});
const w=(kg,extra={})=>({kg,date:'2025-01-01T12:00:00.000Z',...extra});
let chooseCalls=0,choose=()=>{chooseCalls++;return arguments[1]};

let local=shell([h('8')],[w(80)]),remote=shell([h('10')],[w(81)]),base=shell([],[]);
let merged=api.mergeSync(local,[remote],base,choose,[]);
assert.equal(merged.history.length,2,'legacy workouts sharing workout|started must both survive');
assert.equal(merged.weights.length,2,'legacy weights sharing date must both survive');
assert.ok(merged.history.every(x=>/^tp18-h-/.test(x.id)),'legacy history rows receive stable sync IDs');
assert.ok(merged.weights.every(x=>/^tp18-w-/.test(x.id)),'legacy weight rows receive stable sync IDs');
assert.equal(chooseCalls,0,'legacy key collisions must not force destructive conflict selection');

local=shell([h('8')],[w(80)]);remote=shell([],[]);base=shell([h('6')],[w(79)]);
merged=api.mergeSync(local,[remote],base,()=>{throw Error('unexpected conflict')},[]);
assert.equal(merged.history.length,2,'legacy cloudBase-only workout must be rescued');
assert.equal(merged.weights.length,2,'legacy cloudBase-only weight must be rescued');

const archive=shell([h('12')],[w(82)]);
merged=api.mergeSync(local,[remote],shell([],[]),()=>{throw Error('unexpected conflict')},[archive]);
assert.equal(merged.history.length,2,'legacy historical snapshot workout must be rescued');
assert.equal(merged.weights.length,2,'legacy historical snapshot weight must be rescued');

const exact=h('8');
merged=api.mergeSync(shell([exact],[w(80)]),[shell([JSON.parse(JSON.stringify(exact))],[w(80)])],shell([],[]),()=>{throw Error('unexpected conflict')},[]);
assert.equal(merged.history.length,1,'exact legacy workout duplicates dedupe');
assert.equal(merged.weights.length,1,'exact legacy weight duplicates dedupe');

const id='workout-stable-1',old=h('8',{id}),changed=h('9',{id}),baseId=shell([old],[]);
chooseCalls=0;
merged=api.mergeSync(shell([old],[]),[shell([changed],[])],baseId,()=>{chooseCalls++;return old},[]);
assert.equal(merged.history.length,1);assert.equal(merged.history[0].exercises[0].sets[0].reps,'9','three-way merge must accept remote change when phone equals base');assert.equal(chooseCalls,0);


const duplicateA=h('8',{id:'tp18-h-old-a',photos:[{id:'photo-1',label:'after',updatedAt:1,driveFileId:null}],health240:{syncedAt:'2026-09-20T10:00:00Z',score:1}});
const duplicateB=h('8',{id:'tp18-h-old-b',photos:[{id:'photo-1',label:'after',updatedAt:2,driveFileId:'drive-photo-1'}],health240:{syncedAt:'2026-09-21T10:00:00Z',score:2}});
merged=api.mergeSync(shell([duplicateA],[]),[shell([duplicateB],[])],shell([],[]),()=>{throw Error('true duplicate must not conflict')},[]);
assert.equal(merged.history.length,1,'same workout performance with metadata/photo drift must collapse to one row');
assert.match(merged.history[0].id,/^tp18-h-/,'collapsed legacy duplicate keeps deterministic synthetic ID');
assert.equal(merged.history[0].photos?.length,1,'duplicate photo metadata must merge, not duplicate the workout');
assert.equal(merged.history[0].photos?.[0]?.driveFileId,'drive-photo-1','richer photo metadata must survive');
assert.equal(merged.history[0].health240?.score,2,'newer health enrichment must survive duplicate collapse');

const driftA=h('8',{id:'tp18-h-drift-a',programId:'home-basic',dayId:'A',scheduleId:'old-plan',finished:'2025-01-01T11:00:00.000Z'});
const driftB=h('8',{id:'tp18-h-drift-b',programId:'legacy-home',dayId:'workout-A',scheduleId:'new-plan',finished:'2025-01-01T11:02:30.000Z'});
merged=api.mergeSync(shell([driftA],[]),[shell([driftB],[])],shell([],[]),()=>{throw Error('metadata-only snapshot drift must not conflict')},[]);
assert.equal(merged.history.length,1,'same performed workout with historical finish/program metadata drift must collapse');


merged=api.mergeSync(shell([h('8')],[]),[shell([h('9')],[])],shell([],[]),()=>{throw Error('distinct same-time workouts must not conflict')},[]);
assert.equal(merged.history.length,2,'same timestamp with different performance must remain two distinct workouts');

const files=[];
for(let i=0;i<60;i++)files.push({id:'a'+i,name:'repforge-sync-11111111-1111-1111-1111-111111111111-'+i+'.json',createdTime:new Date(2025,0,i+1).toISOString()});
files.push({id:'b1',name:'repforge-sync-22222222-2222-2222-2222-222222222222-x.json',createdTime:'2025-01-01T00:00:00Z'});
files.push({id:'b2',name:'repforge-sync-22222222-2222-2222-2222-222222222222-y.json',createdTime:'2025-02-01T00:00:00Z'});
const sel=api.selectSnapshots(files);
assert.equal(sel.current.length,2,'latest snapshot per device must remain authoritative for scalar data');
assert.ok(sel.archive.length<=48,'historical recovery scan must stay bounded');
assert.ok(sel.archive.some(x=>x.id==='a0'),'oldest snapshot must be retained as a recovery source');
assert.ok(sel.archive.some(x=>x.id==='b1'),'oldest snapshot of each device must be retained');

assert.match(html,/archiveOnly&&x\.id/,'historical recovery must not resurrect stable-ID rows');
assert.match(html,/cloudBase and selected older snapshots are preservation-only sources for legacy records/);
console.log('PASS #18: legacy Drive merge preserves colliding history/weight rows, rescues cloudBase/archive rows and bounds historical scan.');