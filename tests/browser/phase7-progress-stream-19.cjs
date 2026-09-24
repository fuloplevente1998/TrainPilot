const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({headless:true,args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const result=await p.evaluate(()=>{
  const now=Date.now(),work=(id,days,exercises)=>({id,workout:'A',dayId:'A',programId:'home-basic',programName:'Phase 7 stats',started:new Date(now-days*86400000-3600000).toISOString(),finished:new Date(now-days*86400000).toISOString(),exercises,photos:[]});
  const rows=[
   work('s2',0,[
    {id:'db-squat',loadType:'per_hand',repUnit:'ism.',sets:[{set:1,weight:12,reps:'8',done:true},{set:2,weight:12,reps:'7',done:true}]},
    {id:'pushup',loadType:'bodyweight',repUnit:'ism.',sets:[{set:1,weight:0,reps:'20',done:true}]},
    {id:'plank',loadType:'bodyweight',repUnit:'mp',sets:[{set:1,weight:0,reps:'60',done:true}]},
    {id:'side-plank',loadType:'bodyweight',repUnit:'mp/oldal',sets:[{set:1,weight:0,reps:'35',leftSeconds:35,rightSeconds:32,done:true}]}
   ]),
   work('s1',2,[
    {id:'db-squat',loadType:'per_hand',repUnit:'ism.',sets:[{set:1,weight:10,reps:'10',done:true}]},
    {id:'pushup',loadType:'bodyweight',repUnit:'ism.',sets:[{set:1,weight:0,reps:'15',done:true}]},
    {id:'plank',loadType:'bodyweight',repUnit:'mp',sets:[{set:1,weight:0,reps:'45',done:true}]},
    {id:'side-plank',loadType:'bodyweight',repUnit:'mp/oldal',sets:[{set:1,weight:0,reps:'30',leftSeconds:30,rightSeconds:28,done:true}]}
   ])
  ];
  db.set('history',rows);
  const all=rf220ProgressRows(),pick=id=>all.find(x=>x.id===id)?.s||null;
  return {weighted:pick('db-squat'),reps:pick('pushup'),timed:pick('plank'),bilateral:pick('side-plank'),flag:window.TrainPilotPhase7Performance};
 });
 assert.equal(result.weighted.kind,'weighted');assert.equal(result.weighted.maxWeight,12);assert.equal(result.weighted.count,3);assert.equal(result.weighted.sessions,2);assert.equal(result.weighted.best.w,12);assert.equal(result.weighted.best.r,8);assert.equal(result.weighted.latest.w,12);assert.equal(result.weighted.maxVolume,180);
 assert.equal(result.reps.kind,'reps');assert.equal(result.reps.maxReps,20);assert.equal(result.reps.count,2);assert.equal(result.reps.sessions,2);assert.equal(result.reps.latest.r,20);
 assert.equal(result.timed.kind,'timed');assert.equal(result.timed.maxSeconds,60);assert.equal(result.timed.count,2);assert.equal(result.timed.latest.seconds,60);
 assert.equal(result.bilateral.kind,'bilateral');assert.equal(result.bilateral.maxLeft,35);assert.equal(result.bilateral.maxRight,32);assert.equal(result.bilateral.latest.left,35);assert.equal(result.bilateral.latest.right,32);
 assert.equal(result.flag.streamingProgressStats,true);assert.equal(result.flag.persistentCache,false);
 console.log('PASS Phase 7 streaming Progress stats preserve weighted/reps/timed/bilateral semantics');
}finally{await b?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
