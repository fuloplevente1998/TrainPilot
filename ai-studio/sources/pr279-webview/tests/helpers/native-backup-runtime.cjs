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

module.exports={runtime,deferred,settle,photoId,device};
