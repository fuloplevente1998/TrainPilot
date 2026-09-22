const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d)})});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'});
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{db.set('weights',[{kg:66.5,date:new Date().toISOString()}]);rf200SetTheme('classicGreen');go('health')});
  await page.waitForSelector('main.rf263-health.tp168-health');await page.waitForTimeout(100);

  assert.equal(await page.locator('#rf235HealthCoach,.rf235-health-coach').count(),0,'#23 Health Coach must stay removed');
  assert.equal(await page.locator('main.rf263-health>.tp168-health-head').count(),0,'standalone Health heading/subtitle must stay removed');
  assert.equal(await page.locator('.tp168-today-card .tp168-today-date').count(),1,'Today card must contain the date');
  assert.ok((await page.locator('.tp168-today-date').innerText()).trim().length>4,'Today date must be visible');
  assert.equal(await page.locator('.tp168-today-card .tp168-metric').count(),4,'Today status must expose four primary metrics');
  assert.equal(await page.locator('.tp168-today-card .tp168-status').count(),2,'recovery/resting pulse status row missing');
  assert.equal(await page.locator('.tp168-today-card .tp168-trend').count(),1,'pulse trend row missing');
  assert.equal(await page.locator('.tp168-today-card .tp168-weight-journal').count(),1,'compact weight journal must sit below pulse trend');
  assert.equal(await page.locator('.tp168-weight-journal').evaluate(e=>e.classList.contains('tp168-open')),false,'weight journal starts compact');

  const matte=await page.evaluate(()=>({today:getComputedStyle(document.querySelector('.tp168-today-card')).boxShadow,nav:getComputedStyle(document.querySelector('.top.tp154-nav-grid .active')).boxShadow,border:getComputedStyle(document.querySelector('.tp168-today-card')).borderTopColor,family:document.documentElement.dataset.tpThemeFamily}));
  assert.equal(matte.family,'basic');assert.equal(matte.today,'none','basic Health theme must not glow');assert.equal(matte.nav,'none','basic active nav must not glow');

  await page.evaluate(()=>rf200SetTheme('classicBlue'));await page.waitForTimeout(60);
  const blueBorder=await page.locator('.tp168-today-card').evaluate(e=>getComputedStyle(e).borderTopColor);
  assert.notEqual(blueBorder,matte.border,'Health surfaces must follow the selected base theme color');

  await page.evaluate(()=>rf200SetTheme('green'));await page.waitForTimeout(60);
  const vivid=await page.evaluate(()=>({today:getComputedStyle(document.querySelector('.tp168-today-card')).boxShadow,nav:getComputedStyle(document.querySelector('.top.tp154-nav-grid .active')).boxShadow,family:document.documentElement.dataset.tpThemeFamily}));
  assert.equal(vivid.family,'vivid');assert.notEqual(vivid.today,'none','vivid Health theme should glow');assert.notEqual(vivid.nav,'none','vivid active nav should glow');

  await page.evaluate(()=>rf200SetTheme('classicGreen'));await page.waitForTimeout(50);
  await page.evaluate(()=>rf215WeightScreen());await page.waitForTimeout(80);
  assert.equal(await page.locator('main.rf263-health.tp168-health').count(),1,'weight journal must remain on the new Health dashboard');
  assert.equal(await page.locator('.tp168-weight-journal.tp168-open').count(),1,'weight shortcut must open the inline journal');
  assert.equal(await page.locator('main>button').filter({hasText:/Vissza az Egészséghez|Back to Health/i}).count(),0,'legacy body-weight subpage/back button must not be rendered');
  assert.match(await page.locator('.tp168-weight-summary').innerText(),/66[,.]5 kg/,'inline journal must show current manual weight');

  await page.evaluate(()=>{const k=rf240DayKey(new Date());window.__tp169Ledger=rf240Ledger;rf240Ledger=()=>({days:{[k]:{averageHeartRate:75,restingHeartRate:0,steps:10523}}});TrainPilot168Health.decorate()});await page.waitForTimeout(30);
  const resting=await page.locator('.tp168-resting').innerText();assert.doesNotMatch(resting,/0\s*bpm/i,'0 bpm must never be presented as a real resting heart-rate measurement');assert.match(resting,/—/,'missing resting heart rate must use a dash');

  const body=page.locator('main.rf263-health>.tp168-body-card');assert.equal(await body.count(),1,'Body/Fitness collapsible card missing');await body.locator('.tp168-body-head').click();assert.equal(await body.locator('.tp168-body-head').getAttribute('aria-expanded'),'true');
  assert.equal(await page.locator('main.rf263-health>details.tp168-pulse-panel').count(),1,'Pulse panel missing');
  assert.equal(await page.locator('main.rf263-health>details.tp168-more-panel').count(),1,'More Health panel missing');
  assert.equal(await page.locator('main.rf263-health>details.tp168-connect-panel').count(),1,'Health Connect panel missing');
  assert.equal(await page.locator('main.rf263-health>.rf263-sync-card.tp168-sync-tail').count(),1,'Health sync controls must remain available');
  assert.equal(await page.locator('.tp168-health-note').count(),1,'informational disclaimer missing');
  assert.equal(await page.locator('main.rf263-health>details.tp155-health-bottom-panel').count(),3,'existing Health bottom-panel contract must remain intact');

  let overflow=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth}));assert.ok(overflow.sw<=overflow.cw+1,'393px Health dashboard overflow: '+JSON.stringify(overflow));
  await page.setViewportSize({width:320,height:640});await page.waitForTimeout(60);overflow=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth}));assert.ok(overflow.sw<=overflow.cw+1,'320px Health dashboard overflow: '+JSON.stringify(overflow));
  console.log('PASS TrainPilot 1.6.9 themed Health, inline weight journal and responsive layout');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
