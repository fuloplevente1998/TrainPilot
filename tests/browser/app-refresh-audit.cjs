const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}
  fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);res.end();return;}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',error=>errors.push(error.message));page.on('dialog',dialog=>dialog.dismiss());
  await page.addInitScript(()=>{
   localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));
   window.auditProjection={version:1,days:{},preferences:{autoOnOpen:false}};
   window.Capacitor={isNativePlatform:()=>true,Plugins:{HealthJournal:{
    pendingRestore:async()=>({}),initialize:()=>new Promise(resolve=>window.auditFinishInitialization=()=>resolve(window.auditProjection)),getProjection:async()=>window.auditProjection
   }}};
  });
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.auditFinishInitialization);
  await page.evaluate(()=>go('plan'));
  await page.locator('.tp150-quick-entry button').click();
  await page.waitForSelector('#tp155R4PanelHost[data-panel="quick"]');
  const floor=page.locator('#tp150QuickResults [data-exercise-id="db-floor-press"]');
  await floor.locator('summary').click();
  await floor.locator('[data-tp152-qsets]').selectOption('4');await floor.locator('[data-tp152-qweight]').fill('20');
  await page.evaluate(()=>auditFinishInitialization());
  await page.evaluate(()=>TrainPilotHealthJournal.ready());await page.waitForTimeout(100);
  assert.equal(await page.locator('#tp155R4PanelHost[data-panel="quick"]').count(),1,'late native Health Journal initialization must not dismiss the real Quick Workout panel');
  assert.equal(await floor.locator('[data-tp152-qsets]').inputValue(),'4','late initialization must retain configured sets');
  assert.equal(await floor.locator('[data-tp152-qweight]').inputValue(),'20','late initialization must retain configured weight');
  assert.equal(await floor.evaluate(node=>node.open),true,'expanded exercise must remain open');
  fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/app-refresh-quick.png'});
  assert.deepEqual(errors,[]);
  console.log('PASS native initialization keeps the real Quick Workout panel and configured exercise intact');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve));}
})().catch(error=>{console.error(error);process.exitCode=1;});
