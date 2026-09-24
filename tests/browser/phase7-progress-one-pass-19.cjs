const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
const compact=s=>s&&({kind:s.kind,maxWeight:s.maxWeight,e1rm:Math.round((s.e1rm||0)*1000)/1000,maxVolume:s.maxVolume,count:s.count,sessions:s.sessions,maxReps:s.maxReps,maxSeconds:s.maxSeconds,maxLeft:s.maxLeft,maxRight:s.maxRight,best:{w:s.best?.w,r:s.best?.r,left:s.best?.left,right:s.best?.right,seconds:s.best?.seconds,date:s.best?.date},latest:{w:s.latest?.w,r:s.latest?.r,left:s.latest?.left,right:s.latest?.right,seconds:s.latest?.seconds,date:s.latest?.date}});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({headless:true,args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
 await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const result=await p.evaluate(()=>{
  const ids=['db-squat','db-floor-press','one-arm-row','rdl','db-curl','pushup','plank','side-plank'],rows=[];
  for(let i=0;i<80;i++){const finished=new Date(Date.now()-i*86400000),started=new Date(finished.getTime()-45*60000);rows.push({id:'p7-one-'+i,workout:i%2?'B':'A',dayId:i%2?'B':'A',programId:'home-basic',programName:'Phase 7 one pass',started:started.toISOString(),finished:finished.toISOString(),exercises:ids.map((id,j)=>({id,loadType:id==='side-plank'||id==='plank'||id==='pushup'?'bodyweight':'per_hand',repUnit:id==='plank'?'mp':id==='side-plank'?'mp/oldal':'ism.',sets:[1,2,3,4].map(set=>({set,weight:j%3?10+j:0,reps:id==='plank'?'45':id==='side-plank'?'30':String(8+((i+j+set)%5)),leftSeconds:id==='side-plank'?30+((i+set)%4):undefined,rightSeconds:id==='side-plank'?28+((i+set)%3):undefined,done:true}))}))})}
  db.set('history',rows);
  const progress=rf220ProgressRows(),pairs=ids.map(id=>({id,progress:progress.find(x=>x.id===id)?.s||null,single:rf220ExerciseStats(id)}));
  return {pairs,flag:window.TrainPilotPhase7Performance};
 });
 for(const x of result.pairs)assert.deepEqual(compact(x.progress),compact(x.single),'single-pass Progress stats must equal canonical per-exercise stats for '+x.id);
 assert.equal(result.flag.singlePassProgressStats,true);assert.equal(result.flag.persistentCache,false);
 console.log('PASS Phase 7 single-pass Progress aggregation matches per-exercise stats');
}finally{await b?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
