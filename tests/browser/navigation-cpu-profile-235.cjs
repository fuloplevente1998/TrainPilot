'use strict';
// Diagnostic, not a device-latency gate. Run the same script against both roots.
const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),assert=require('node:assert/strict');
const {execFileSync}=require('node:child_process'),{chromium}=require('playwright');
const root=path.resolve(process.env.TRAINPILOT_NAV_ROOT||'www');
const output=path.resolve(process.env.TRAINPILOT_NAV_OUT||'navigation-profile/current');
const sourceCommit=execFileSync('git',['-C',path.dirname(root),'rev-parse','HEAD'],{encoding:'utf8'}).trim();
const server=http.createServer((req,res)=>{
 const name=new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'),file=path.resolve(root,'.'+name);
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});
});
function rank(profile){
 const nodes=new Map(profile.nodes.map(n=>[n.id,n])),parents=new Map(),self=new Map(),total=new Map();
 for(const n of profile.nodes)for(const id of n.children||[])parents.set(id,n.id);
 for(let i=0;i<(profile.samples||[]).length;i++){
  const id=profile.samples[i],ms=(profile.timeDeltas[i]||0)/1000;self.set(id,(self.get(id)||0)+ms);
  for(let next=id;next;next=parents.get(next))total.set(next,(total.get(next)||0)+ms);
 }
 return [...nodes.values()].map(n=>({name:n.callFrame.functionName||'(anonymous)',url:n.callFrame.url.split('/').at(-1),line:n.callFrame.lineNumber+1,
  selfMs:Math.round((self.get(n.id)||0)*10)/10,totalMs:Math.round((total.get(n.id)||0)*10)/10}))
  .filter(n=>n.selfMs>0&&!['(idle)','(root)'].includes(n.name)).sort((a,b)=>b.selfMs-a.selfMs).slice(0,30);
}
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 fs.mkdirSync(output,{recursive:true});
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox']});
  const evidence={sourceCommit,cpuThrottle:4,limits:'Chromium synthetic touchscreen input, two-RAF paint proxy and sampled CPU profile; not physical Android input or FPS.',fixtures:[]};
  for(const count of [0,200,1500]){
   const page=await browser.newPage({viewport:{width:393,height:873},hasTouch:true,isMobile:true,locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];
   page.on('pageerror',e=>errors.push(e.message));
   const cdp=await page.context().newCDPSession(page);await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});
   await cdp.send('Profiler.enable');await cdp.send('Profiler.setSamplingInterval',{interval:500});await cdp.send('Performance.enable');
   await page.addInitScript(()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));});
   const url='http://127.0.0.1:'+server.address().port+'/';await page.goto(url);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   await page.evaluate(count=>{
    const ids=['db-floor-press','db-squat','one-arm-row','rdl','db-curl','plank'];
    db.set('language','hu');db.set('draft',null);db.set('phase3HistoryTombstones',[]);
    db.set('history',Array.from({length:count},(_,i)=>({id:'nav-'+i,programId:'home-basic',dayId:i%2?'A':'B',
     started:new Date(Date.now()-(i+1)*86400000).toISOString(),finished:new Date(Date.now()-(i+1)*86400000+3600000).toISOString(),
     exercises:ids.map(id=>{const e=byId(id);return {id,loadType:e.loadType,repUnit:e.repUnit,sets:[1,2,3,4].map(set=>({set,reps:'10',weight:8,done:true}))};})})));
   },count);
   await cdp.send('Profiler.start');await page.reload({waitUntil:'domcontentloaded'});await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
   const startup=(await cdp.send('Profiler.stop')).profile;fs.writeFileSync(path.join(output,'startup-'+count+'.cpuprofile'),JSON.stringify(startup));
   const fixture={count,bootMs:await page.evaluate(()=>TrainPilotBoot.durationMs),startupRank:rank(startup),routes:[]};
   await page.locator('#tpBoot').waitFor({state:'hidden'});
   await page.evaluate(()=>{
    window.__navProbe={pending:null,rows:[]};
    document.addEventListener('pointerdown',event=>{if(__navProbe.pending&&event.target.closest('.top'))__navProbe.pending.down=event.timeStamp;},true);
    document.addEventListener('click',event=>{
     const row=__navProbe.pending;if(!row||!event.target.closest('.top'))return;
     row.clickAt=performance.now();row.inputDelayMs=row.clickAt-event.timeStamp;
     queueMicrotask(()=>{row.handlerTaskMs=performance.now()-row.clickAt;});
     requestAnimationFrame(()=>requestAnimationFrame(()=>{
      row.paintMs=performance.now()-row.clickAt;row.tapToPaintMs=performance.now()-(row.down||row.clickAt);
      row.nodes=document.querySelectorAll('*').length;row.tab=state.tab;row.panel=document.getElementById('tp155R4PanelHost')?.dataset.panel||'';
      row.animations=document.getAnimations().map(a=>a.animationName).filter(Boolean);__navProbe.rows.push(row);__navProbe.pending=null;
     }));
    },true);
   });
   for(let cycle=1;cycle<=2;cycle++)for(const route of ['plan','health','programs','history','calendar','coach','settings','home']){
    const selector=route==='coach'?'.top .tp154-coach-action':route==='settings'?'.top .tp154-settings-action':'.top [onclick="go(\''+route+'\')"]';
    await page.evaluate(({route,cycle})=>{__navProbe.pending={route,cycle};},{route,cycle});
    const before=(await cdp.send('Performance.getMetrics')).metrics;await cdp.send('Profiler.start');
    await page.locator(selector).tap();await page.waitForFunction(()=>__navProbe.pending===null);
    const profile=(await cdp.send('Profiler.stop')).profile,after=(await cdp.send('Performance.getMetrics')).metrics;
    const row=await page.evaluate(()=>__navProbe.rows.at(-1));
    assert.equal(row.panel,['calendar','coach','settings'].includes(route)?route:'');
    if(!row.panel)assert.equal(row.tab,route);
    row.metrics=Object.fromEntries(['LayoutDuration','RecalcStyleDuration','ScriptDuration','TaskDuration','LayoutCount','RecalcStyleCount'].map(name=>{
     const a=after.find(m=>m.name===name)?.value||0,b=before.find(m=>m.name===name)?.value||0;return [name,Math.round((a-b)*10000)/10];
    }));row.rank=rank(profile);fixture.routes.push(row);
    fs.writeFileSync(path.join(output,[count,cycle,route].join('-')+'.cpuprofile'),JSON.stringify(profile));
   }
   assert.deepEqual(errors,[]);evidence.fixtures.push(fixture);await page.close();
   console.log('NAVIGATION_CPU '+JSON.stringify({count,bootMs:fixture.bootMs,routes:fixture.routes.map(({route,cycle,inputDelayMs,handlerTaskMs,paintMs,nodes,metrics,rank})=>({route,cycle,inputDelayMs,handlerTaskMs,paintMs,nodes,metrics,rank:rank.slice(0,8)}))}));
  }
  fs.writeFileSync(path.join(output,'summary.json'),JSON.stringify(evidence,null,2));
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1});
