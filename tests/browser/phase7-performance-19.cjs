const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
const round=n=>Math.round(n*100)/100;
const median=a=>{const s=[...a].sort((x,y)=>x-y),m=Math.floor(s.length/2);return s.length%2?s[m]:(s[m-1]+s[m])/2};
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));
  const navStart=Date.now();
  await page.goto('http://127.0.0.1:'+server.address().port+'/',{waitUntil:'domcontentloaded'});
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  const startup=await page.evaluate(()=>({bootMs:performance.now(),nodes:document.getElementsByTagName('*').length,heap:performance.memory?.usedJSHeapSize||null}));startup.wallMs=Date.now()-navStart;
  const seeded=await page.evaluate(()=>{
   db.set('language','hu');
   const ids=['db-squat','db-floor-press','one-arm-row','rdl','db-curl','pushup','plank','side-plank'];
   const rows=[];
   for(let i=0;i<240;i++){
    const finished=new Date(Date.now()-i*86400000),started=new Date(finished.getTime()-45*60000);
    rows.push({id:'p7-'+i,workout:i%2?'B':'A',dayId:i%2?'B':'A',programId:'home-basic',programName:'Phase 7 benchmark',started:started.toISOString(),finished:finished.toISOString(),exercises:ids.map((id,j)=>({id,loadType:id==='side-plank'||id==='plank'||id==='pushup'?'bodyweight':'per_hand',repUnit:id==='plank'?'mp':id==='side-plank'?'mp/oldal':'ism.',sets:[1,2,3,4].map(set=>({set,weight:j%3?10+j:0,reps:id==='plank'?'45':id==='side-plank'?'30':String(8+((i+j+set)%5)),leftSeconds:id==='side-plank'?30:undefined,rightSeconds:id==='side-plank'?28:undefined,done:true}))})),photos:[]});
   }
   db.set('history',rows);state.session=null;
   window.__p7={renderCalls:0,renderMs:0,historyCalls:0,mutations:0,added:0,removed:0,longTasks:[]};
   const renderBase=window.render;if(typeof renderBase==='function')window.render=function(){const s=performance.now();window.__p7.renderCalls++;try{return renderBase.apply(this,arguments)}finally{window.__p7.renderMs+=performance.now()-s}};
   const historyBase=window.history;if(typeof historyBase==='function')window.history=function(){window.__p7.historyCalls++;return historyBase.apply(this,arguments)};
   new MutationObserver(ms=>{for(const m of ms){window.__p7.mutations++;window.__p7.added+=m.addedNodes?.length||0;window.__p7.removed+=m.removedNodes?.length||0}}).observe(document.documentElement,{subtree:true,childList:true,attributes:true});
   try{new PerformanceObserver(list=>{for(const e of list.getEntries())window.__p7.longTasks.push({duration:e.duration,start:e.startTime})}).observe({type:'longtask',buffered:true})}catch(_){}
   return {rows:rows.length,historyType:typeof window.history,renderType:typeof window.render};
  });
  assert.equal(seeded.rows,240);assert.equal(seeded.historyType,'function');assert.equal(seeded.renderType,'function');
  const runAction=async(label,fn,runs=5)=>{
   const samples=[];
   for(let i=0;i<runs;i++){
    const x=await page.evaluate(async({label,fn})=>{
     const p=window.__p7;p.renderCalls=0;p.renderMs=0;p.historyCalls=0;p.mutations=0;p.added=0;p.removed=0;p.longTasks=[];
     const beforeNodes=document.getElementsByTagName('*').length,beforeHeap=performance.memory?.usedJSHeapSize||null,t0=performance.now();
     (0,eval)(fn);
     const feedback=performance.now()-t0;
     await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));
     const total=performance.now()-t0,afterNodes=document.getElementsByTagName('*').length,afterHeap=performance.memory?.usedJSHeapSize||null;
     return {label,feedback,total,renderCalls:p.renderCalls,renderMs:p.renderMs,historyCalls:p.historyCalls,mutations:p.mutations,added:p.added,removed:p.removed,nodes:afterNodes,nodeDelta:afterNodes-beforeNodes,longTasks:p.longTasks.length,longTaskMs:p.longTasks.reduce((a,b)=>a+b.duration,0),heapDelta:beforeHeap!=null&&afterHeap!=null?afterHeap-beforeHeap:null};
    },{label,fn:fn.toString().replace(/^\(\)=>/,'')});
    samples.push(x);
   }
   const last=samples.at(-1);
   return {label,medianTotal:round(median(samples.map(x=>x.total))),medianFeedback:round(median(samples.map(x=>x.feedback))),medianRenderMs:round(median(samples.map(x=>x.renderMs))),medianRenderCalls:median(samples.map(x=>x.renderCalls)),medianHistoryCalls:median(samples.map(x=>x.historyCalls)),medianMutations:median(samples.map(x=>x.mutations)),nodes:last.nodes,longTasks:samples.reduce((a,x)=>a+x.longTasks,0),maxTotal:round(Math.max(...samples.map(x=>x.total)))};
  };
  const routes=[];
  routes.push(await runAction('home',()=>go('home')));
  routes.push(await runAction('plan',()=>go('plan')));
  routes.push(await runAction('health',()=>go('health')));
  routes.push(await runAction('programs',()=>go('programs')));
  routes.push(await runAction('history',()=>go('history')));
  routes.push(await runAction('settings',()=>go('settings')));
  await page.evaluate(()=>{window.tp155R4ClosePanel?.(false);go('home')});
  routes.push(await runAction('coach-panel',()=>window.tp155R4OpenPanel('coach',document.activeElement)));
  await page.evaluate(()=>window.tp155R4ClosePanel?.(false));
  await page.evaluate(()=>go('history'));await page.waitForSelector('details.rf263-history');
  const workoutExpand=await runAction('journal-workout-expand',()=>document.querySelector('details.rf263-history>summary')?.click(),3);
  await page.waitForSelector('details.rf263-history[open] .tp3-history-ex-item');
  const exerciseOpen=await runAction('journal-exercise-editor',()=>document.querySelector('details.rf263-history[open] details.tp3-history-ex-item>summary')?.click(),3);
  await page.waitForSelector('details.tp3-history-ex-item[open] .tp3-exercise-inline-editor');
  const photoModal=await runAction('photo-modal',()=>{isNative=()=>true;rf130PhotoPlugin=()=>({pick:async()=>({cancelled:true}),capture:async()=>({cancelled:true})});rf130OpenAddPhoto(rf142WorkoutKey(history()[0]))},3);
  await page.evaluate(()=>document.querySelector('#rf130PhotoModal .tp6-photo-close')?.click());
  const repeat=[];
  for(let i=0;i<4;i++){repeat.push(await runAction('roundtrip-'+i,()=>{go('health');go('history');go('home')},1))}
  const final=await page.evaluate(()=>({nodes:document.getElementsByTagName('*').length,heap:performance.memory?.usedJSHeapSize||null,styleSheets:document.styleSheets.length,inlineStyles:document.querySelectorAll('style').length,importantRules:[...document.styleSheets].reduce((n,s)=>{try{return n+[...s.cssRules].filter(r=>String(r.cssText).includes('!important')).length}catch(_){return n}},0)}));
  assert.deepEqual(errors,[],'page errors: '+errors.join('\n'));
  const metrics={baselineCommit:'f064298ddbe1e7e4cf1ecdfca18b3d93899dc74a',startup:{bootMs:round(startup.bootMs),wallMs:startup.wallMs,nodes:startup.nodes,heap:startup.heap},routes,workoutExpand,exerciseOpen,photoModal,roundtrips:repeat,final};
  console.log('PHASE7_METRICS '+JSON.stringify(metrics));
  console.log('PASS Phase 7 performance benchmark instrumentation');
 }finally{await browser?.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
