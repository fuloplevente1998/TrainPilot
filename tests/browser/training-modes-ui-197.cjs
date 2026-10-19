'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const loc=new URL(req.url,'http://127.0.0.1'),file=path.resolve(root,'.'+(loc.pathname==='/'?'/index.html':loc.pathname));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(err,body)=>{if(err){res.writeHead(404);return res.end();}
 res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(body);});
});
(async()=>{
 await new Promise(done=>server.listen(0,'127.0.0.1',done));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:393,height:844}}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));
  await page.addInitScript(()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));});
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&typeof tp197UiSet==='function');
  await page.evaluate(()=>{state.tab='settings';render();});
  const saved=await page.evaluate(()=>JSON.stringify([settings(),programs(),history(),db.get('draft',null)]));
  await page.locator('#tp197-mode-global').evaluate(el=>el.open=true);
  await page.locator('#tp197-mode-global-style').selectOption('hit');
  await page.locator('#tp197-mode-global-progress').selectOption('rir');
  assert.equal(await page.locator('#tp197-mode-global-style').inputValue(),'hit');
  assert.equal(await page.locator('#tp197-mode-global-progress').inputValue(),'rir');
  assert.equal(await page.locator('#tp197-mode-global .tp197-ui-preview').count(),1);
  for(const [lang,word] of [['hu','Edzésstílus'],['en','Training style'],['de','Trainingsstil'],['ro','Stil de antrenament']]){
   await page.evaluate(lang=>rf212SetLang(lang),lang);
   assert.ok((await page.locator('#tp197-mode-global').innerText()).includes(word),'language '+lang);
  }
  await page.evaluate(()=>{state.tab='plan';render();});
  assert.equal(await page.locator('#tp197-mode-program').count(),1);
  await page.locator('#tp197-mode-program').evaluate(el=>el.open=true);
  assert.equal(await page.locator('#tp197-mode-program-style').inputValue(),'normal');
  await page.locator('#tp197-mode-program-style').selectOption('hit');
  assert.equal(await page.locator('#tp197-mode-program-style').inputValue(),'hit');
  const after=await page.evaluate(()=>JSON.stringify([settings(),programs(),history(),db.get('draft',null)]));
  assert.equal(after,saved,'UI preview changed persisted settings/program/history/draft');
  for(const width of [320,360,393]){
   await page.setViewportSize({width,height:844});
   const result=await page.locator('#tp197-mode-program').evaluate(el=>({
    panel:el.scrollWidth-el.clientWidth,
    controls:[...el.querySelectorAll('select')].map(x=>x.scrollWidth-x.clientWidth),
    viewport:document.documentElement.scrollWidth-document.documentElement.clientWidth
   }));
   assert.ok(result.panel<=2,'panel overflow '+width+': '+JSON.stringify(result));
   assert.ok(result.controls.every(x=>x<=2),'selector overflow '+width+': '+JSON.stringify(result));
  }
  assert.deepEqual(errors,[]);
  console.log('PASS #197 browser: HU/EN/DE/RO, preview-only selects, global/program independence, 320–393px');
 }finally{await browser?.close();await new Promise(done=>server.close(done));}
})().catch(e=>{console.error(e);process.exitCode=1;});
