const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'}),errors=[];page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.accept());
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  const sightings={},bad=[];
  const scan=async tab=>{
   const r=await page.evaluate(()=>{
    const checks=[['select','.tp-select-trigger','::after'],['journal','.tp160-journal-disclosure','::before'],['profile','.rf148-profile-accordion>summary','::after'],['library','.tp-library-card>summary','::after'],['global','.tp-global-chevron','::before']];
    const bad=[],seen={};const theme=(()=>{const e=document.createElement('i');e.style.color='var(--accent2)';document.body.appendChild(e);const color=getComputedStyle(e).color;e.remove();return color})();
    for(const [name,sel,pseudo] of checks)for(const e of document.querySelectorAll(sel)){
     if(e.closest('.tp-play-btn,[data-play]'))continue;
     const s=getComputedStyle(e),box=e.getBoundingClientRect();if(s.display==='none'||s.visibility==='hidden'||!box.width||!box.height)continue;
     const c=getComputedStyle(e,pseudo);seen[name]=(seen[name]||0)+1;
     if(c.clipPath!=='none'||!c.content.includes('›')||c.color!==theme)bad.push(name+': '+sel+' '+JSON.stringify({clip:c.clipPath,content:c.content,color:c.color,theme}));
    }
    // Investigate other visible disclosure triangles without mistaking video play,
    // chart decorations, progress icons, or checkbox tick masks for old carets.
    for(const e of document.querySelectorAll('summary,.tp-exercise-actions,.rf-history-health-toggle,button[aria-expanded],.tp-select-trigger')){
     if(e.closest('.tp-play-btn,[data-play]'))continue;
     const s=getComputedStyle(e),r=e.getBoundingClientRect();if(s.display==='none'||s.visibility==='hidden'||!r.width||!r.height)continue;
     for(const pseudo of ['::before','::after']){
      const c=getComputedStyle(e,pseudo),content=c.content||'';
      if((/[▶▸►▾▼▲◂◀]/.test(content)||/polygon\(/i.test(c.clipPath))&&c.display!=='none'){
       if(!e.matches('.tp-select-trigger,.rf148-profile-accordion>summary,.tp-library-card>summary,.tp160-journal-disclosure'))bad.push('unexpected '+e.tagName+'.'+e.className+' '+pseudo+' '+content+' '+c.clipPath);
      }
     }
    }
    return {seen,bad};
   });for(const [key,count] of Object.entries(r.seen))sightings[key]=(sightings[key]||0)+count;bad.push(...r.bad.map(x=>tab+': '+x));
  };
  for(const tab of ['home','plan','programs','health','history','calendar','settings']){
   await page.evaluate(t=>go(t),tab);await page.waitForTimeout(90);await scan(tab);
  }
  await page.evaluate(()=>{go('programs');muscleLibrary()});await page.waitForSelector('#libraryResults .tp-library-card');await scan('exercise-library');
  await page.evaluate(()=>profileScreen());await page.waitForSelector('.rf148-profile-accordion');await scan('profile');

  // #41 phone-reported exact locations (2026-09-24): these four surfaces previously
  // still exposed the white CSS triangle even though the generic audit passed.
  const exactChevron=async(selector,pseudo,label)=>{
   const x=await page.locator(selector).first().evaluate((e,pseudo)=>{const c=getComputedStyle(e,pseudo),probe=document.createElement('i');probe.style.color='var(--accent2)';document.body.appendChild(probe);const theme=getComputedStyle(probe).color;probe.remove();return {content:c.content,clip:c.clipPath,color:c.color,theme}},pseudo);
   assert.equal(x.clip,'none',label+' must not use triangle clipping: '+JSON.stringify(x));
   assert.ok(x.content.includes('›'),label+' must render the shared chevron: '+JSON.stringify(x));
   assert.equal(x.color,x.theme,label+' must follow the active theme accent: '+JSON.stringify(x));
  };
  await page.evaluate(()=>{window.tp155R4ClosePanel?.(false);go('history')});await page.waitForSelector('.tp155-journal-filter .tp160-journal-disclosure');
  await exactChevron('.tp155-journal-filter .tp160-journal-disclosure','::before','Journal date-filter arrow');
  await page.evaluate(()=>{go('plan');window.tp155R4OpenPanel('quick',document.activeElement)});await page.waitForSelector('#tp155R4PanelHost[data-panel="quick"] .tp155-quick-filter');
  await exactChevron('#tp155R4PanelHost[data-panel="quick"] .tp155-quick-filter>summary','::after','Quick-workout filter arrow');
  await exactChevron('#tp155R4PanelHost[data-panel="quick"] .tp152-quick-row .tp152-chevron','::before','Quick-workout exercise-row arrow');
  await page.evaluate(()=>{window.tp155R4ClosePanel(false);go('calendar')});await page.waitForSelector('#tp155R4PanelHost[data-panel="calendar"] #rf230Mode');
  {
   const trigger=page.locator('#tp155R4PanelHost[data-panel="calendar"] #rf230Mode').locator('xpath=..').locator('.tp-select-trigger');
   const x=await trigger.evaluate(e=>{const c=getComputedStyle(e,'::after'),probe=document.createElement('i');probe.style.color='var(--accent2)';document.body.appendChild(probe);const theme=getComputedStyle(probe).color;probe.remove();return {content:c.content,clip:c.clipPath,color:c.color,theme}});
   assert.equal(x.clip,'none','Calendar planning-rhythm select must not use triangle clipping: '+JSON.stringify(x));
   assert.ok(x.content.includes('›'),'Calendar planning-rhythm select must render the shared chevron: '+JSON.stringify(x));
   assert.equal(x.color,x.theme,'Calendar planning-rhythm select must follow theme accent: '+JSON.stringify(x));
  }
  await page.evaluate(()=>{window.tp155R4ClosePanel(false);go('programs');window.tp155R4OpenPanel('exercises',document.activeElement)});await page.waitForSelector('#tp155R4PanelHost[data-panel="exercises"] #libraryMuscle');
  for(const id of ['libraryMuscle','libraryGear']){
   const sel='#tp155R4PanelHost[data-panel="exercises"] #'+id;
   const trigger=page.locator(sel).locator('xpath=..').locator('.tp-select-trigger');
   const x=await trigger.evaluate(e=>{const c=getComputedStyle(e,'::after'),probe=document.createElement('i');probe.style.color='var(--accent2)';document.body.appendChild(probe);const theme=getComputedStyle(probe).color;probe.remove();return {content:c.content,clip:c.clipPath,color:c.color,theme}});
   assert.equal(x.clip,'none',id+' filter must not use triangle clipping: '+JSON.stringify(x));
   assert.ok(x.content.includes('›'),id+' filter must render the shared chevron: '+JSON.stringify(x));
   assert.equal(x.color,x.theme,id+' filter must follow theme accent: '+JSON.stringify(x));
  }
  await page.evaluate(()=>go('health'));await page.waitForSelector('.tp5-today-full');
  for(const width of [320,360,393,412]){
   await page.setViewportSize({width,height:760});
   const h=await page.locator('main.tp168-health .tp5-today-full>.tp5-today-metric').evaluateAll(es=>es.map(e=>e.getBoundingClientRect().height));
   assert.equal(h.length,6,'Health still needs six tiles');
   assert.ok(h.every(n=>n<=86),'Health tiles should be flatter than the old 100px square layout: '+width+' '+h);
   const o=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth}));assert.ok(o.sw<=o.cw+1,'horizontal overflow at '+width);
  }
  for(const key of ['select','journal','profile','library','global'])assert.ok(sightings[key]>0,'no coverage for '+key+': '+JSON.stringify(sightings));
  assert.deepEqual([...new Set(bad)],[],'remaining disclosure triangles or theme mismatch:\n'+[...new Set(bad)].join('\n'));
  assert.deepEqual(errors,[],'page errors');
  console.log('PASS #41 disclosure audit + #44 flatter Health tiles (320/360/393/412px)');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
