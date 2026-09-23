const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{b=await chromium.launch({headless:true,args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU'});await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.TrainPilotIssue18?.version==='18');
const result=await p.evaluate(()=>{
 const shell=(history=[],weights=[])=>({history,weights,scheduled:[],settings:{rest:90},exercises:[],plan:{A:[],B:[]},programs:[],activeProgramId:'home-basic',plannerSettings:{}});
 const h=(r)=>({workout:'A',started:'2025-01-01T10:00:00.000Z',finished:'2025-01-01T11:00:00.000Z',exercises:[{id:'sq',hu:'Guggolás',sets:[{weight:10,reps:r,done:true}]}]});
 const w=(kg)=>({kg,date:'2025-01-01T12:00:00.000Z'});
 const merged=TrainPilotIssue18.mergeSync(shell([h('8')],[w(80)]),[shell([h('10')],[w(81)])],shell([],[]),()=>{throw Error('legacy collision must not ask for destructive choice')},[]);
 const again=TrainPilotIssue18.mergeSync(merged,[merged],merged,()=>{throw Error('stable identical rows must not conflict')},[]);
 const dupA={...h('8'),id:'tp18-h-old-a',photos:[{id:'p1',updatedAt:1,driveFileId:null}]};
 const dupB={...h('8'),id:'tp18-h-old-b',photos:[{id:'p1',updatedAt:2,driveFileId:'drive-p1'}]};
 const collapsed=TrainPilotIssue18.mergeSync(shell([dupA],[]),[shell([dupB],[])],shell([],[]),()=>{throw Error('true duplicate must not conflict')},[]);
 return {history:merged.history.length,weights:merged.weights.length,historyIds:merged.history.map(x=>x.id),weightIds:merged.weights.map(x=>x.id),againHistory:again.history.length,againWeights:again.weights.length,collapsed:collapsed.history.length,collapsedPhotos:collapsed.history[0]?.photos?.length||0,collapsedDrive:collapsed.history[0]?.photos?.[0]?.driveFileId||null};
});
assert.equal(result.history,2);assert.equal(result.weights,2);assert.equal(result.againHistory,2);assert.equal(result.againWeights,2);assert.ok(result.historyIds.every(x=>/^tp18-h-/.test(x)));assert.ok(result.weightIds.every(x=>/^tp18-w-/.test(x)));assert.equal(result.collapsed,1,'same workout duplicated across snapshots must collapse');assert.equal(result.collapsedPhotos,1);assert.equal(result.collapsedDrive,'drive-p1');
console.log('PASS: #18 browser runtime preserves colliding legacy Drive rows and stays idempotent after stable IDs.');}finally{if(b)await b.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});