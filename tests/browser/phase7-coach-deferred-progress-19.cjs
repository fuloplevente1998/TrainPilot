const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({headless:true,args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const immediate=await p.evaluate(()=>{
  const ids=['db-squat','db-floor-press','one-arm-row','rdl','db-curl','pushup','plank','side-plank'],rows=[];
  for(let i=0;i<240;i++){const finished=new Date(Date.now()-i*86400000),started=new Date(finished.getTime()-45*60000);rows.push({id:'p7-coach-defer-'+i,workout:i%2?'B':'A',dayId:i%2?'B':'A',programId:'home-basic',programName:'Phase 7 defer',started:started.toISOString(),finished:finished.toISOString(),exercises:ids.map((id,j)=>({id,loadType:id==='side-plank'||id==='plank'||id==='pushup'?'bodyweight':'per_hand',repUnit:id==='plank'?'mp':id==='side-plank'?'mp/oldal':'ism.',sets:[1,2,3,4].map(set=>({set,weight:j%3?10+j:0,reps:id==='plank'?'45':id==='side-plank'?'30':String(8+((i+j+set)%5)),leftSeconds:id==='side-plank'?30:undefined,rightSeconds:id==='side-plank'?28:undefined,done:true}))}))})}
  db.set('history',rows);state.session=null;go('home');
  const t0=performance.now();window.tp155R4OpenPanel('coach',document.activeElement);const elapsed=performance.now()-t0,host=document.querySelector('#tp155R4PanelHost[data-panel="coach"]');
  return {elapsed,coach:!!host?.querySelector('.tp151-coach'),cards:host?.querySelectorAll('.tp155-coach-ex').length||0,pending:host?.querySelectorAll('[data-tp7-coach-progress="pending"]').length||0,stats:host?.querySelectorAll('.tp4-stat-row').length||0,link:host?.querySelectorAll('.tp67-coach-progress').length||0,flag:window.TrainPilotPhase7Performance};
 });
 assert.equal(immediate.coach,true,'Coach surface must exist immediately');
 assert.ok(immediate.cards>0,'Coach exercise recommendations must be present immediately');
 assert.equal(immediate.pending,0,'old deferred stats placeholder must be removed before the initial Coach paint');
 assert.equal(immediate.stats,0,'duplicate Progress rows must not block the Coach');
 assert.equal(immediate.link,1,'Coach uses one direct link to the Progress page');
 assert.equal(immediate.flag.deferredCoachProgress,true,'existing lazy history infrastructure remains intact');
 await p.waitForTimeout(120);
 const stable=await p.evaluate(()=>{const host=document.querySelector('#tp155R4PanelHost[data-panel="coach"]');return {pending:host?.querySelectorAll('[data-tp7-coach-progress="pending"]').length||0,stats:host?.querySelectorAll('.tp4-stat-row').length||0,link:host?.querySelectorAll('.tp67-coach-progress').length||0,cards:host?.querySelectorAll('.tp155-coach-ex').length||0}});
 assert.equal(stable.pending,0,'deferred hydration must not reintroduce the old stats placeholder');
 assert.equal(stable.stats,0,'full PR/statistics rows now belong exclusively to Progress');
 assert.equal(stable.link,1,'Coach Progress link survives the old deferred hydration callback');
 assert.ok(stable.cards>0,'exercise recommendations remain intact');
 console.log('PASS #67 Phase 7 Coach: immediate advice and no duplicate deferred Progress statistics');

}finally{await b?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
