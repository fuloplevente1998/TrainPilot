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
  const page=await browser.newPage({viewport:{width:360,height:800}}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));
  await page.addInitScript(()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));});
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&typeof tp197UiApprove==='function');
  await page.evaluate(()=>{state.tab='settings';render();});
  const baseline=await page.evaluate(()=>({
   modes:settings().trainingModes197||null,
   program:JSON.stringify(programs()),history:JSON.stringify(history()),draft:JSON.stringify(db.get('draft',null))
  }));
  await page.locator('#tp197-mode-global').evaluate(el=>el.open=true);
  async function choose(field,value){
   const panel=page.locator('#tp197-mode-global');if(!await panel.evaluate(el=>el.open))await panel.locator(':scope > summary').click();
   const id=field==='trainingStyle'?'tp197-mode-global-style':'tp197-mode-global-progress';
   const picker=page.locator('#tp197-mode-global .tp-select').filter({has:page.locator('#'+id)});
   await picker.locator('.tp-select-trigger').scrollIntoViewIfNeeded();
   await page.waitForTimeout(120);
   await picker.locator('.tp-select-trigger').dispatchEvent('click');
   await picker.locator('.tp-select-option[data-value="'+value+'"]').dispatchEvent('click');
  }
  await choose('trainingStyle','hit');
  await page.locator('#tp197-mode-global .tp197-actions button').first().click();
  assert.equal(await page.evaluate(()=>settings().trainingModes197||null),baseline.modes,'first click cannot save');
  await page.locator('#tp197-mode-global .tp197-confirm button').last().click(); // Cancel
  assert.equal(await page.locator('#tp197-mode-global-style').inputValue(),'normal');
  assert.equal(await page.locator('#tp197-mode-global').evaluate(el=>el.open),false,'Cancel visibly closes the editor');
  assert.equal(await page.evaluate(()=>settings().trainingModes197||null),baseline.modes);
  await choose('trainingStyle','hit');
  await choose('progressionMode','rir');
  await page.locator('#tp197-mode-global .tp197-actions button').first().click();
  await page.locator('#tp197-mode-global .tp197-confirm button').first().click(); // Explicit approval
  const saved=await page.evaluate(()=>({
   m:settings().trainingModes197,program:JSON.stringify(programs()),history:JSON.stringify(history()),draft:JSON.stringify(db.get('draft',null))
  }));
  assert.equal(saved.m.schemaVersion,1);
  assert.equal(saved.m.explicitChoice,true);
  assert.equal(saved.m.defaults.trainingStyle,'hit');
  assert.equal(saved.m.defaults.progressionMode,'rir');
  assert.equal(saved.program,baseline.program);
  assert.equal(saved.history,baseline.history);
  assert.equal(saved.draft,baseline.draft);
  await page.locator('#tp197-mode-global .tp197-actions button').last().click(); // undo
  assert.deepEqual(await page.evaluate(()=>settings().trainingModes197||null),baseline.modes,'Undo must restore legacy state');
  await page.evaluate(()=>{state.session={id:'simulated-active',status:'paused'};});
  await choose('trainingStyle','hit');
  await page.locator('#tp197-mode-global .tp197-actions button').first().click();
  assert.equal(await page.evaluate(()=>settings().trainingModes197||null),baseline.modes,'active workout must block save');
  await page.evaluate(()=>{state.session=null;});
  assert.deepEqual(errors,[]);
  console.log('PASS #197 5/7 browser: save requires separate approval, cancel, undo and active session guard');
 }finally{await browser?.close();await new Promise(done=>server.close(done));}
})().catch(e=>{console.error(e);process.exitCode=1;});
