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
   await page.addInitScript(()=>{
    localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
    window.__startupProbe={startedAt:performance.now(),reads:{},rootWrites:0,beforeDCL:null,domContentLoadedAt:null};
    const get=Storage.prototype.getItem,html=Object.getOwnPropertyDescriptor(Element.prototype,'innerHTML');
    Storage.prototype.getItem=function(key){__startupProbe.reads[key]=(__startupProbe.reads[key]||0)+1;return get.call(this,key);};
    Object.defineProperty(Element.prototype,'innerHTML',{...html,set(value){if(this.id==='app')__startupProbe.rootWrites++;return html.set.call(this,value);}});
    document.addEventListener('DOMContentLoaded',()=>{__startupProbe.domContentLoadedAt=performance.now();__startupProbe.beforeDCL={reads:{...__startupProbe.reads},rootWrites:__startupProbe.rootWrites};},{once:true});
   });
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
   const fixture={count,...await page.evaluate(()=>({bootMs:TrainPilotBoot.durationMs,documentBootMs:performance.now()-__startupProbe.startedAt,
    domContentLoadedMs:__startupProbe.domContentLoadedAt-__startupProbe.startedAt})),startupReads:await page.evaluate(()=>__startupProbe),startupRank:rank(startup),routes:[]};
   await page.locator('#tpBoot').waitFor({state:'hidden'});
   await page.evaluate(()=>{
    window.__navProbe={pending:null,rows:[]};
    // Measurement only: count page-driven geometry/style queries per navigation.
    window.__navGeometry={boundingRects:0,clientRects:0,computedStyles:0};
    const bound=Element.prototype.getBoundingClientRect,clients=Element.prototype.getClientRects,style=window.getComputedStyle;
    Element.prototype.getBoundingClientRect=function(...args){__navGeometry.boundingRects++;return bound.apply(this,args);};
    Element.prototype.getClientRects=function(...args){__navGeometry.clientRects++;return clients.apply(this,args);};
    window.getComputedStyle=function(...args){__navGeometry.computedStyles++;return style.apply(this,args);};
    document.addEventListener('pointerdown',event=>{if(__navProbe.pending&&event.target.closest('.top'))__navProbe.pending.down=event.timeStamp;},true);
    document.addEventListener('click',event=>{
     const row=__navProbe.pending;if(!row||!event.target.closest('.top'))return;
     row.clickAt=performance.now();row.inputDelayMs=row.clickAt-event.timeStamp;
     requestAnimationFrame(()=>requestAnimationFrame(()=>{
      row.paintMs=performance.now()-row.clickAt;row.tapToPaintMs=performance.now()-(row.down||row.clickAt);
      row.nodes=document.querySelectorAll('*').length;row.tab=state.tab;row.panel=document.getElementById('tp155R4PanelHost')?.dataset.panel||'';
      row.animations=document.getAnimations().map(a=>a.animationName).filter(Boolean);__navProbe.rows.push(row);__navProbe.pending=null;
     }));
    },true);
    document.addEventListener('click',()=>{const row=__navProbe.pending;if(row?.clickAt!=null)row.handlerTaskMs=performance.now()-row.clickAt;});
   });
   for(let cycle=1;cycle<=2;cycle++)for(const route of ['plan','health','programs','history','calendar','coach','settings','home']){
    const selector=route==='coach'?'.top .tp154-coach-action':route==='settings'?'.top .tp154-settings-action':'.top [onclick="go(\''+route+'\')"]';
    await page.evaluate(({route,cycle})=>{__navProbe.pending={route,cycle};__navGeometry.boundingRects=0;__navGeometry.clientRects=0;__navGeometry.computedStyles=0;},{route,cycle});
    const before=(await cdp.send('Performance.getMetrics')).metrics;await cdp.send('Profiler.start');
    await page.locator(selector).tap();await page.waitForFunction(()=>__navProbe.pending===null);
    const profile=(await cdp.send('Profiler.stop')).profile,after=(await cdp.send('Performance.getMetrics')).metrics;
    const row=await page.evaluate(()=>__navProbe.rows.at(-1));
    assert.equal(row.panel,['calendar','coach','settings'].includes(route)?route:'');
    if(!row.panel)assert.equal(row.tab,route);
    // Sample DOM after the timed profile: diagnostics must not inflate paint/layout metrics.
    row.geometry=await page.evaluate(()=>({...window.__navGeometry}));
    row.dom=await page.evaluate(()=>{
     const subtree=el=>el?el.getElementsByTagName('*').length+1:0;
     const app=document.getElementById('app'),panel=document.getElementById('tp155R4PanelHost');
     const largestAppBranches=[...(app?.children||[])].map((el,index)=>({
      index,tag:el.tagName.toLowerCase(),id:el.id||'',
      className:typeof el.className==='string'?el.className.slice(0,100):'',nodes:subtree(el)
     })).sort((a,b)=>b.nodes-a.nodes).slice(0,8);
     return {
      totalNodes:document.getElementsByTagName('*').length,
      appNodes:subtree(app),panelNodes:subtree(panel),
      journalCards:document.querySelectorAll('details.rf263-history').length,
      journalExpanded:document.querySelectorAll('details.rf263-history[open]').length,
      journalHydrated:document.querySelectorAll('.tp3-combined-history-body[data-tp3-hydrated="true"]').length,
      journalExerciseRows:document.querySelectorAll('details.tp3-history-ex-item').length,
      journalSummaries:document.querySelectorAll('details.rf263-history>summary').length,
      largestAppBranches
     };
    });
    row.metrics=Object.fromEntries(['LayoutDuration','RecalcStyleDuration','ScriptDuration','TaskDuration','LayoutCount','RecalcStyleCount'].map(name=>{
     const a=after.find(m=>m.name===name)?.value||0,b=before.find(m=>m.name===name)?.value||0;return [name,name.endsWith('Count')?a-b:Math.round((a-b)*10000)/10];
    }));row.rank=rank(profile);fixture.routes.push(row);
    fs.writeFileSync(path.join(output,[count,cycle,route].join('-')+'.cpuprofile'),JSON.stringify(profile));
   }
   assert.deepEqual(errors,[]);evidence.fixtures.push(fixture);await page.close();
   console.log('NAVIGATION_CPU '+JSON.stringify({count,bootMs:fixture.bootMs,documentBootMs:fixture.documentBootMs,domContentLoadedMs:fixture.domContentLoadedMs,routes:fixture.routes.map(({route,cycle,inputDelayMs,handlerTaskMs,paintMs,nodes,metrics,geometry,dom,rank})=>({route,cycle,inputDelayMs,handlerTaskMs,paintMs,nodes,metrics,geometry,dom,rank:rank.slice(0,8)}))}));
  }
  fs.writeFileSync(path.join(output,'summary.json'),JSON.stringify(evidence,null,2));
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1});
