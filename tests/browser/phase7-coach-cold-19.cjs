const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let b;try{
 b=await chromium.launch({headless:true,args:['--no-sandbox']});const p=await b.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
 await p.goto('http://127.0.0.1:'+server.address().port+'/');await p.waitForFunction(()=>window.TrainPilotBoot?.finished);
 const result=await p.evaluate(async()=>{
  const ids=['db-squat','db-floor-press','one-arm-row','rdl','db-curl','pushup','plank','side-plank'],rows=[];
  for(let i=0;i<240;i++){const finished=new Date(Date.now()-i*86400000),started=new Date(finished.getTime()-45*60000);rows.push({id:'p7-coach-'+i,workout:i%2?'B':'A',dayId:i%2?'B':'A',programId:'home-basic',programName:'Phase 7 coach',started:started.toISOString(),finished:finished.toISOString(),exercises:ids.map((id,j)=>({id,loadType:id==='side-plank'||id==='plank'||id==='pushup'?'bodyweight':'per_hand',repUnit:id==='plank'?'mp':id==='side-plank'?'mp/oldal':'ism.',sets:[1,2,3,4].map(set=>({set,weight:j%3?10+j:0,reps:id==='plank'?'45':id==='side-plank'?'30':String(8+((i+j+set)%5)),leftSeconds:id==='side-plank'?30:undefined,rightSeconds:id==='side-plank'?28:undefined,done:true}))}))});}
  db.set('history',rows);state.session=null;go('home');window.tp155R4ClosePanel?.(false);
  const names=['rf233CoachPlan','rf220Readiness','rf233TargetWorkout','rf233ExerciseAdvice','rf152Recommendation','rf220ExerciseStats','rf220ProgressRows','rf220CoachScreen','rf233CoachScreen','tp155R4OpenPanel'],stats={};
  for(const name of names){const base=window[name];if(typeof base!=='function')continue;stats[name]={calls:0,ms:0,maxMs:0};window[name]=function(){const t=performance.now();try{return base.apply(this,arguments)}finally{const d=performance.now()-t,s=stats[name];s.calls++;s.ms+=d;if(d>s.maxMs)s.maxMs=d}}}
  let mutations=0,added=0;new MutationObserver(ms=>{for(const m of ms){mutations++;added+=m.addedNodes?.length||0}}).observe(document.documentElement,{subtree:true,childList:true});
  const long=[];try{new PerformanceObserver(list=>{for(const e of list.getEntries())long.push(e.duration)}).observe({type:'longtask',buffered:true})}catch(_){}
  const before=document.getElementsByTagName('*').length,t0=performance.now();window.tp155R4OpenPanel('coach',document.activeElement);const feedback=performance.now()-t0;
  await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));const total=performance.now()-t0,after=document.getElementsByTagName('*').length;
  const ranked=Object.entries(stats).map(([name,s])=>({name,calls:s.calls,ms:Math.round(s.ms*100)/100,maxMs:Math.round(s.maxMs*100)/100})).sort((a,b)=>b.ms-a.ms);
  return {feedback:Math.round(feedback*100)/100,total:Math.round(total*100)/100,beforeNodes:before,afterNodes:after,nodeDelta:after-before,mutations,added,longTasks:long.map(x=>Math.round(x*100)/100),ranked,coachCards:document.querySelectorAll('#tp155R4PanelHost .tp155-coach-ex').length,coachText:(document.querySelector('#tp155R4PanelHost .tp151-coach')?.innerText||'').slice(0,120)};
 });
 console.log('PHASE7_COACH_COLD '+JSON.stringify(result));
 console.log('PASS Phase 7 cold Coach profiling');
}finally{await b?.close();await new Promise(r=>server.close(r))}})().catch(e=>{console.error(e);process.exit(1)});
