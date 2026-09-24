const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);return res.end()}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{
   const now=new Date(),days={};for(let i=6;i>=0;i--){const d=new Date(now);d.setDate(d.getDate()-i);days[rf240DayKey(d)]={averageHeartRate:70+i,restingHeartRate:56+i,steps:9000+i*100}}
   window.__tp41Ledger=rf240Ledger;rf240Ledger=()=>({version:1,days,lastSyncAt:new Date().toISOString()});
   rf200SetTheme('classicGreen');go('health');TrainPilot168Health.decorate();tpGlobalApplyChevrons(document);
  });
  await page.waitForSelector('.tp169-pulse-journal');await page.waitForTimeout(50);

  const pulseArrow=page.locator('.tp169-pulse-summary>.tp-global-chevron');assert.equal(await pulseArrow.count(),1,'Pulse summary must use global chevron');
  const themeColor=await page.evaluate(()=>{const e=document.createElement('i');e.style.color='var(--accent2)';document.body.appendChild(e);const c=getComputedStyle(e).color;e.remove();return c});
  assert.equal(await pulseArrow.evaluate(e=>getComputedStyle(e).color),themeColor,'global chevron must follow theme accent color');

  const mini=page.locator('.tp169-pulse-summary>.tp5-trend-mini');const mb=await mini.boundingBox(),ab=await pulseArrow.boundingBox();assert.ok(mb&&ab&&mb.x+mb.width<=ab.x-1,'pulse trend must keep a right-side safe zone before the chevron: '+JSON.stringify({mb,ab}));

  const matte=await page.evaluate(()=>({
   family:document.documentElement.dataset.tpThemeFamily,
   today:getComputedStyle(document.querySelector('.tp168-today-card')).boxShadow,
   pulse:getComputedStyle(document.querySelector('.tp169-pulse-journal')).boxShadow,
   weight:getComputedStyle(document.querySelector('.tp168-weight-journal')).boxShadow
  }));
  assert.equal(matte.family,'basic');assert.equal(matte.today,'none');assert.equal(matte.pulse,'none');assert.equal(matte.weight,'none');

  await page.evaluate(()=>{state.session=null;state.workout=null;state.tab='plan';render();tpGlobalApplyChevrons(document)});
  await page.waitForSelector('.tp1511-training');
  const workoutArrow=page.locator('.tp1511-training summary i.tp-global-chevron').first();assert.equal(await workoutArrow.count(),1,'Workout settings arrow must use global chevron');
  const pseudo=await workoutArrow.evaluate(e=>getComputedStyle(e,'::before').content);assert.ok(pseudo.includes('›'),'Workout arrow must render the shared chevron, not a triangle: '+pseudo);

  await page.evaluate(()=>{go('programs');tpGlobalApplyChevrons(document)});await page.waitForTimeout(50);
  const programArrows=page.locator('.tp152-program-chevron.tp-global-chevron');assert.ok(await programArrows.count()>0,'Program arrows must use global chevron');

  await page.evaluate(()=>{rf200SetTheme('green');go('health');TrainPilot168Health.decorate();tpGlobalApplyChevrons(document)});await page.waitForTimeout(60);
  const vivid=await page.evaluate(()=>({family:document.documentElement.dataset.tpThemeFamily,today:getComputedStyle(document.querySelector('.tp168-today-card')).boxShadow}));
  assert.equal(vivid.family,'vivid');assert.notEqual(vivid.today,'none','vivid theme may retain accent glow');

  for(const width of [320,360,393,412]){await page.setViewportSize({width,height:760});await page.waitForTimeout(25);const o=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth}));assert.ok(o.sw<=o.cw+1,'#41 overflow at '+width+': '+JSON.stringify(o))}
  console.log('PASS #41 browser: global theme chevrons, workout/program coverage, Health matte/vivid and pulse safe-zone.');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
