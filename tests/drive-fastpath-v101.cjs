const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync('www/app.js','utf8');
const begin=source.indexOf('var driveLatestHeads=function');
const end=source.indexOf('workoutDescription=function',begin);
assert.ok(begin>0&&end>begin);
const base={history:[],weights:[],settings:{rest:90},exercises:[],plan:{A:[],B:[]},scheduled:[]};
const values=new Map([['cloudBase:owner',base],['cloudHeads:owner',{'00000000-0000-0000-0000-000000000001':'old'}]]);
let local=JSON.parse(JSON.stringify(base)),remoteId='old',reads=0,writes=0,photos=0;
const device='00000000-0000-0000-0000-000000000001';
const bridge={driveList:async()=>({files:[{id:remoteId,name:'repforge-sync-'+device+'-snapshot.json',createdTime:'2026-09-27T10:00:00Z'}]}),driveRead:async()=>{reads++;return {data:JSON.stringify({app:'RepForgeSync',schema:1,owner:'owner',data:base})}},driveWrite:async()=>{writes++;return {verified:true,id:'new'}}};
const ctx={cloudMessage:'',cloudBusy:false,cloudActiveOperation:'',cloudDriveStage:'',cloudProfile:{sub:'owner'},state:{session:null},navigator:{onLine:true},tp149T:k=>k,showCloudMessage:x=>{ctx.cloudMessage=x},validateSync:x=>x,syncData:()=>local,canonicalSyncData:x=>JSON.stringify(x),googleBridge:()=>bridge,db:{get:(k,f)=>values.get(k)??f,set:(k,v)=>values.set(k,v)},mergeSync:x=>x,storeMerged:()=>{},rf130SyncWorkoutPhotos:async()=>{photos++},render:()=>{},finishCloud:()=>{ctx.cloudBusy=false},alert:()=>{},crypto:{randomUUID:()=>device}};
vm.createContext(ctx);vm.runInContext(source.slice(begin,end),ctx);
(async()=>{
 await ctx.syncCloud();assert.equal(reads,0,'unchanged Drive head and local data should skip download');assert.equal(photos,0);assert.equal(writes,0);
 remoteId='changed';await ctx.syncCloud();assert.equal(reads,1,'new remote head must be read');assert.equal(photos,1);assert.equal(ctx.cloudMessage,'cloud.driveDone');
 remoteId='old';local=JSON.parse(JSON.stringify(base));local.settings.rest=120;await ctx.syncCloud();assert.equal(reads,2,'local changes must use the full merge path');assert.equal(writes,1);
 console.log('PASS Drive fast path skips unchanged snapshots while remote and local changes use full merge');
})().catch(e=>{console.error(e);process.exitCode=1});
