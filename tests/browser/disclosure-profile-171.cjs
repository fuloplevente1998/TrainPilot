'use strict';
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve(process.env.TP171_WEB_ROOT||'www');
const output=path.resolve(process.env.TP171_OUTPUT||'ui-evidence/disclosure-profile-171');
const size=Number(process.env.TP171_ROWS||240),rate=Number(process.env.TP171_CPU||4);
const server=http.createServer((req,res)=>{
 const name=new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'),file=path.resolve(root,'.'+name);
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}
 fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data)});
});
const median=xs=>[...xs].sort((a,b)=>a-b)[Math.floor(xs.length/2)];
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  fs.mkdirSync(output,{recursive:true});browser=await chromium.launch({args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>TrainPilotBoot.finished);
  const cdp=await page.context().newCDPSession(page);await cdp.send('Emulation.setCPUThrottlingRate',{rate});
  await page.evaluate(size=>{
   const ids=['db-squat','db-floor-press','one-arm-row','rdl','db-curl','pushup','plank','side-plank'],rows=[];
   for(let i=0;i<size;i++){
    const end=new Date(Date.UTC(2026,9,11,15)-i*86400000);
    rows.push({id:'perf171-'+i,dayId:i%2?'B':'A',workout:i%2?'B':'A',programId:'home-basic',programName:'Performance 171',started:new Date(end-2700000).toISOString(),finished:end.toISOString(),
     exercises:ids.map((id,j)=>({...byId(id),id,sets:[1,2,3,4].map(set=>({set,weight:10+j,reps:'10',done:true}))})),photos:[]});
   }
   db.set('history',rows);db.set('language','hu');state.session=null;
   const names=['history','tp107FitViews','tp7EnsureProgramBody','tp3EnsureHistoryBody','tp3OpenExerciseEditor','tp107HydrateProgramRow','rfHistoryHealthEnsure','rfHistoryHealthHtml','rf130LoadHistoryPhotos','tpGlobalApplyChevrons','rf260EnhanceSelects','rf260EnhanceTemporalFields','tp168ToggleWeight','tp168TogglePulse','render'];
   window.perf171={calls:{},reads:{},historyStacks:{}};
   for(const name of names){const base=window[name];if(typeof base!=='function')continue;window[name]=function(){const start=performance.now();try{return base.apply(this,arguments)}finally{const d=performance.now()-start,s=perf171.calls[name]||(perf171.calls[name]={count:0,ms:0,max:0});s.count++;s.ms+=d;s.max=Math.max(s.max,d);}}}
   const get=db.get;db.get=function(key){perf171.reads[key]=(perf171.reads[key]||0)+1;return get.apply(this,arguments)};
  },size);
  const results=[];
  for(const [name,route,selector,prepare] of [
   ['plan-settings','plan','.tp1511-training > summary',''],
   ['plan-day','plan','.tp152-day > summary',''],
   ['plan-exercise','plan','.tp107-program-editor > summary',"document.querySelector('.tp152-day').open=true"],
   ['program-card','programs','[data-tp7-program-id] > summary',''],
   ['journal-workout','history','[data-tp152-history-key] > summary',''],
   ['journal-exercise','history','.tp3-history-ex-item > summary',"document.querySelector('[data-tp152-history-key] > summary').click()"],
   ['health-weight','health','.tp168-weight-summary',''],
   ['health-pulse','health','.tp169-pulse-summary','']
  ]){
   await page.evaluate(({route,prepare})=>{state.tp3HistoryOpenKey=null;state.tp3HistoryExerciseEdit=null;go(route);if(prepare)(0,eval)(prepare)},{route,prepare});
   await page.waitForSelector(selector,{state:'attached'});await page.waitForTimeout(80);
   await page.evaluate(()=>{perf171.calls={};perf171.reads={}});
   await cdp.send('Profiler.enable');await cdp.send('Profiler.setSamplingInterval',{interval:500});await cdp.send('Profiler.start');
   await cdp.send('Tracing.start',{categories:'devtools.timeline,v8.execute,blink.user_timing',transferMode:'ReturnAsStream'});
   const samples=[];
   for(let i=0;i<8;i++){
    samples.push(await page.evaluate(async selector=>{
     const node=document.querySelector(selector);if(!node)throw Error('Missing '+selector);
     const start=performance.now();node.click();const handler=performance.now()-start;
     await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));
     return {handler,paint:performance.now()-start};
    },selector));
    await page.waitForTimeout(60);
   }
   const stats=await page.evaluate(()=>perf171);
   const cpu=await cdp.send('Profiler.stop');fs.writeFileSync(path.join(output,name+'.cpuprofile'),JSON.stringify(cpu.profile));
   const completed=new Promise(resolve=>cdp.once('Tracing.tracingComplete',resolve));await cdp.send('Tracing.end');const {stream}=await completed;
   let trace='';for(;;){const block=await cdp.send('IO.read',{handle:stream});trace+=block.data;if(block.eof)break}await cdp.send('IO.close',{handle:stream});
   fs.writeFileSync(path.join(output,name+'.trace.json'),trace);
   const events=JSON.parse(trace).traceEvents;
   const layout=events.filter(e=>e.name==='Layout'&&e.ph==='X'),styles=events.filter(e=>e.name==='UpdateLayoutTree'&&e.ph==='X');
   const nodes=new Map(cpu.profile.nodes.map(n=>[n.id,n])),self={};
   (cpu.profile.samples||[]).forEach((id,i)=>{const n=nodes.get(id),f=n.callFrame,k=f.functionName+' '+f.url.split('/').at(-1)+':'+(f.lineNumber+1);self[k]=(self[k]||0)+(cpu.profile.timeDeltas?.[i]||0)/1000});
   const opens=samples.filter((_,i)=>i%2===0),closes=samples.filter((_,i)=>i%2===1);
   const result={name,rows:size,cpuRate:rate,firstOpen:samples[0],openMedian:{handler:median(opens.map(s=>s.handler)),paint:median(opens.map(s=>s.paint))},closeMedian:{handler:median(closes.map(s=>s.handler)),paint:median(closes.map(s=>s.paint))},calls:stats.calls,reads:stats.reads,layout:{count:layout.length,ms:layout.reduce((n,e)=>n+(e.dur||0)/1000,0),styles:styles.length,styleMs:styles.reduce((n,e)=>n+(e.dur||0)/1000,0)},cpuSelf:Object.entries(self).sort((a,b)=>b[1]-a[1]).slice(0,15)};
   results.push(result);console.log(JSON.stringify(result));
  }
  if(errors.length)throw Error(errors.join('\n'));
  fs.writeFileSync(path.join(output,'metrics.json'),JSON.stringify({rows:size,cpuRate:rate,results},null,2));
 }finally{await browser?.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exitCode=1});
