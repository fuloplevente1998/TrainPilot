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
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);await page.evaluate(()=>go('health'));await page.waitForSelector('main.rf263-health.tp168-health');await page.waitForTimeout(80);
  assert.equal(await page.locator('#rf235HealthCoach,.rf235-health-coach').count(),0,'#23 Health Coach must stay removed');
  assert.equal(await page.locator('.tp168-health-head').count(),1,'new compact Health header missing');
  assert.equal(await page.locator('.tp168-today-card .tp168-metric').count(),4,'Today status must expose four primary metrics');
  const primary=await page.locator('.tp168-today-card .tp168-metric').allInnerTexts();assert.ok(primary.some(x=>/Alvás/.test(x)));assert.ok(primary.some(x=>/HRV/.test(x)));assert.ok(primary.some(x=>/Pulzus/.test(x)));assert.ok(primary.some(x=>/Lépések/.test(x)));
  assert.equal(await page.locator('.tp168-today-card .tp168-status').count(),2,'recovery/resting pulse status row missing');
  assert.equal(await page.locator('.tp168-today-card .tp168-trend').count(),1,'pulse trend row missing');
  const body=page.locator('main.rf263-health>.tp168-body-card');assert.equal(await body.count(),1,'Body/Fitness collapsible card missing');assert.equal(await body.locator('h2').count(),1);assert.equal(await body.locator('.tp168-body-head').getAttribute('aria-expanded'),'false');await body.locator('.tp168-body-head').click();assert.equal(await body.locator('.tp168-body-head').getAttribute('aria-expanded'),'true');
  assert.equal(await page.locator('main.rf263-health>details.tp168-pulse-panel').count(),1,'Pulse panel missing');
  assert.equal(await page.locator('main.rf263-health>details.tp168-more-panel').count(),1,'More Health panel missing');
  assert.equal(await page.locator('main.rf263-health>details.tp168-connect-panel').count(),1,'Health Connect panel missing');
  assert.equal(await page.locator('main.rf263-health>.rf263-sync-card.tp168-sync-tail').count(),1,'Health sync controls must remain available');
  assert.equal(await page.locator('.tp168-health-note').count(),1,'informational disclaimer missing');
  assert.equal(await page.locator('main.rf263-health>details.tp155-health-bottom-panel').count(),3,'existing Health bottom-panel contract must remain intact');
  let overflow=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth}));assert.ok(overflow.sw<=overflow.cw+1,'393px Health dashboard overflow: '+JSON.stringify(overflow));
  await page.setViewportSize({width:320,height:640});await page.waitForTimeout(50);overflow=await page.evaluate(()=>({sw:document.documentElement.scrollWidth,cw:document.documentElement.clientWidth}));assert.ok(overflow.sw<=overflow.cw+1,'320px Health dashboard overflow: '+JSON.stringify(overflow));
  console.log('PASS TrainPilot 1.6.8 Health dashboard visual structure and responsive layout');
 }finally{if(browser)await browser.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
