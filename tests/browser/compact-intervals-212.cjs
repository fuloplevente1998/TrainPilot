'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});

(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const base='http://127.0.0.1:'+server.address().port+'/';
 let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  page.on('dialog',d=>d.accept());
  await page.addInitScript(()=>{try{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'))}catch(_){}});
  await page.goto(base);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);

  // 1. Sima plank test
  await page.evaluate(()=>{
   db.set('activeProgramId','home-level2');
   startWorkout('A',null,'home-level2');
   let i=state.session.exercises.findIndex(e=>e.id==='plank');
   if(i<0){
    const src=state.session.exercises.find(e=>e.id==='side-plank');
    const c=JSON.parse(JSON.stringify(src));c.id='plank';c.name='Plank';c.repUnit='mp';c.reps='30–45 mp';
    c.sets=c.sets.map(s=>({...s,leftSeconds:undefined,rightSeconds:undefined}));
    state.session.exercises.push(c);i=state.session.exercises.length-1;
   }
   state.current=i;renderWorkout();
  });

  const plankPanel=page.locator('#tp107Indoor');
  assert.equal(await plankPanel.count(),1,'#tp107Indoor missing on plank');
  assert.equal(await plankPanel.locator('.tp110-interval').count(),1,'intervals disclosure missing on plank');
  assert.equal(await plankPanel.locator('[data-tp107-clock]').count(),1,'clock missing on plank');
  assert.equal(await plankPanel.locator('[data-tp107-clock-toggle]').count(),1,'clock toggle missing on plank');

  // Verify compact collapsed geometry on 393 and 320 viewports
  for(const width of [393,320]){
   await page.setViewportSize({width,height:873});
   const m=await page.evaluate(()=>{
    const panel=document.getElementById('tp107Indoor'),b=panel?.querySelector('.tp110-interval')?.getBoundingClientRect();
    const strong=panel?.querySelector('strong:first-child'),cs=strong?getComputedStyle(strong):null;
    const phase=panel?.querySelector('.tp110-interval-phase'),ps=phase?getComputedStyle(phase):null;
    return {h:Math.round(b?.height||0),strongVisible:cs?.display!=='none',phaseVisible:ps?.display!=='none',overflowX:document.documentElement.scrollWidth-innerWidth};
   });
   assert.ok(m.h<=65,`optional interval row must be compact (<=65px, got ${m.h}px on ${width}px)`);
   assert.equal(await page.locator('#rf110Stopwatch').count(),1,'the lower stopwatch is preserved');
   assert.equal(await plankPanel.locator('.tp107-clock').count(),0,'the upper interval stopwatch is removed');
   assert.equal(m.strongVisible,true,'the lower stopwatch title remains visible');
   assert.equal(m.phaseVisible,false,'empty interval phase must be hidden when idle');
   assert.ok(m.overflowX<=2,`no horizontal overflow on ${width}px`);
  }

  // Expand and check form controls
  await plankPanel.locator('.tp110-interval>summary').click();
  await page.waitForTimeout(150);
  assert.equal(await plankPanel.locator('input[name="work"]').count(),1,'work input present');
  assert.equal(await plankPanel.locator('input[name="rest"]').count(),1,'rest input present');
  assert.equal(await plankPanel.locator('input[name="rounds"]').count(),1,'rounds input present');
  assert.equal(await plankPanel.locator('input[name="autoAdvance"]').count(),1,'autoAdvance checkbox present');
  const minTarget=await page.evaluate(()=>{
   const inputs=[...document.querySelectorAll('#tp107Indoor input:not([type=checkbox]), #tp107Indoor button')].filter(x=>x.offsetParent);
   return Math.min(...inputs.map(x=>Math.round(x.getBoundingClientRect().height)));
  });
  assert.ok(minTarget>=44,`form fields and buttons retain 44px targets (got ${minTarget})`);

  // Collapse back
  await plankPanel.locator('.tp110-interval>summary').click();
  await page.waitForTimeout(100);

  // 2. Oldalsó plank test
  await page.evaluate(()=>{
   const i=state.session.exercises.findIndex(e=>e.id==='side-plank');
   if(i<0)throw Error('side-plank missing');
   state.current=i;renderWorkout();
  });

  const sidePanel=page.locator('#tp107Indoor');
  assert.equal(await sidePanel.count(),1,'#tp107Indoor missing on side-plank');
  assert.ok(await page.evaluate(()=>document.getElementById('tp107Indoor').classList.contains('tp212-bilateral')),'tp212-bilateral class applied');
  assert.equal(await page.locator('#rf110Stopwatch').count(),1,'bilateral stopwatch present above');
  assert.equal(await page.locator('#tp1481SideToggle-left').count(),1,'left side timer present');
  assert.equal(await page.locator('#tp1481SideToggle-right').count(),1,'right side timer present');

  // Verify compact collapsed geometry on side-plank (fitted to title/chevron)
  for(const width of [393,320]){
   await page.setViewportSize({width,height:873});
   const sm=await page.evaluate(()=>{
    const panel=document.getElementById('tp107Indoor'),b=panel?.querySelector('.tp110-interval')?.getBoundingClientRect();
    const clock=panel?.querySelector('.tp107-clock'),cs=clock?getComputedStyle(clock):null;
    const saveRow=panel?.querySelector(':scope > .tp107-save-row'),rs=saveRow?getComputedStyle(saveRow):null;
    return {h:Math.round(b?.height||0),clockVisible:!!clock&&cs.display!=='none',saveVisible:!!saveRow&&rs.display!=='none',overflowX:document.documentElement.scrollWidth-innerWidth};
   });
   assert.ok(sm.h<=65,`side-plank collapsed height must be compact (<=65px, got ${sm.h}px on ${width}px)`);
   assert.equal(sm.clockVisible,false,'redundant single clock hidden on bilateral idle');
   assert.equal(sm.saveVisible,false,'redundant single save-row hidden on bilateral idle');
   assert.ok(sm.overflowX<=2,`no horizontal overflow on ${width}px`);
  }

  // Large system font test
  await page.evaluate(()=>{
   document.documentElement.classList.add('tp110-large-text');
   document.documentElement.style.zoom='1.3';
   renderWorkout();
  });
  await page.waitForTimeout(200);
  const largeMetrics=await page.evaluate(()=>{
   const panel=document.getElementById('tp107Indoor'),b=panel?.querySelector('.tp110-interval')?.getBoundingClientRect();
   return {h:Math.round(b?.height||0),overflowX:document.documentElement.scrollWidth-innerWidth};
  });
  assert.ok(largeMetrics.overflowX<=3,'no horizontal overflow under large text mode');

  // A compact presentation must not enable the single-clock native engine
  // for bilateral sets: it cannot validate both left and right recordings.
  const nativeProbe=await page.evaluate(async()=>{
   let calls=0;
   const previous=window.Capacitor;
   window.Capacitor={isNativePlatform:()=>true,Plugins:{IntervalTimer:{start:async()=>{calls++;throw Error('bilateral native timer must not start');}}}};
   try{
    const started=await window.TrainPilotIntervals162.startConfigured({work:1,rest:0,rounds:1,sound:false,vibration:false,autoAdvance:true});
    const set=state.session.exercises[state.current].sets.find(x=>!x.done);
    return {started,calls,done:set.done,left:Number(set.leftSeconds||0),right:Number(set.rightSeconds||0)};
   }finally{window.Capacitor=previous;}
  });
  assert.equal(nativeProbe.started,false,'single-clock intervals reject bilateral sets');
  assert.equal(nativeProbe.calls,0,'no native single-clock start for bilateral sets');
  assert.equal(nativeProbe.done,false,'unmeasured bilateral set remains incomplete');
  assert.equal(nativeProbe.left,0);assert.equal(nativeProbe.right,0);

  // Both manual side clocks must persist positive seconds independently.
  const sideSetId=await page.evaluate(()=>state.session.exercises[state.current].sets.find(x=>!x.done).setId);
  for(const side of ['left','right']){
   await page.locator('#tp1481SideToggle-'+side).click();
   await page.waitForFunction(side=>{
    const set=state.session.exercises[state.current].sets.find(x=>!x.done);
    return Number(set?.[side+'Seconds']||0)>=1;
   },side);
   await page.locator('#tp1481SideToggle-'+side).click();
   const stored=await page.evaluate(id=>state.session.exercises[state.current].sets.find(x=>x.setId===id),sideSetId);
   assert.ok(Number(stored[side+'Seconds'])>=1,side+' timer persists positive seconds');
   assert.equal(stored.done,side==='right','the second explicit side stop completes the set; a single side is insufficient');
   if(side==='left')assert.equal(Number(stored.rightSeconds||0),0,'left clock leaves right side unrecorded');
  }

  console.log('PASS #212: compact intervals UI on plain and side plank; touch targets and stopwatches verified');
 }finally{
  if(browser)await browser.close();
  server.close();
 }
})().catch(e=>{
 console.error(e);
 process.exit(1);
});
