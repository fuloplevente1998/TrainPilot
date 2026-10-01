const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www'),server=http.createServer((req,res)=>{const name=new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'),file=path.resolve(root,'.'+name);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);res.end();return;}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{browser=await chromium.launch({headless:true,args:['--no-sandbox']});
 for(const width of [320,360,393,412])for(const language of ['hu','en','de','ro']){
  const page=await browser.newPage({viewport:{width,height:800},locale:language});const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(lang=>{db.set('language',lang);go('settings');},language);
  await page.waitForSelector('.tp105-data');await page.locator('.tp105-data>summary').click();
  const result=await page.evaluate(()=>({text:document.querySelector('.tp105-data').textContent,checked:document.querySelector('.tp105-data input').checked,overflow:document.documentElement.scrollWidth>innerWidth+1,buttons:[...document.querySelectorAll('.tp105-data button')].map(x=>x.getAttribute('onclick'))}));
  assert.equal(result.checked,false,'health export must require consent');assert.equal(result.overflow,false,`${width}/${language} settings overflow`);assert.ok(result.buttons.includes('tp105Archive(false)'));assert.ok(result.buttons.includes('tp105Archive(true)'));assert.ok(result.text.includes(tpLabel(language)));
  await page.evaluate(()=>{window.tp155R4ClosePanel(false);go('history');go('home');rf200SetTheme('green');go('settings');});await page.waitForSelector('.tp105-data');assert.equal(await page.locator('.tp105-data input').isChecked(),false);assert.deepEqual(errors,[]);await page.close();
 }
 // Exercise the actual DOMContentLoaded Drive override with an unfinished retention request.
 const page=await browser.newPage({viewport:{width:393,height:800},locale:'hu'});
 const probeErrors=[];page.on('pageerror',e=>probeErrors.push(e.message));
 await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
 await page.evaluate(()=>{
  const device='00000000-0000-0000-0000-000000000001';let head='old';
  window.tp105SyncProbe={saved:0,reads:0,alerts:[],started:false,finished:false};
  const pending=new Promise(resolve=>window.tp105FinishPrune=resolve);
  let snapshot={app:'RepForgeSync',schema:1,owner:'test-owner',device,data:syncData()};
  window.Capacitor={isNativePlatform:()=>true,Plugins:{
   GoogleSync:{driveList:async()=>({files:[{id:head,name:'repforge-sync-'+device+'-snapshot.json',createdTime:'2026-10-01T20:00:00Z'}]}),driveRead:async()=>{tp105SyncProbe.reads++;return {data:JSON.stringify(snapshot)};},driveWrite:async x=>{snapshot=JSON.parse(x.data);head='written';return {id:head,verified:true};},drivePrune:async()=>{tp105SyncProbe.started=true;await pending;return {deleted:0};}},
   BackupArchive:{save:async()=>{tp105SyncProbe.saved++;return {verified:true,name:'TrainPilot-test.zip',bytes:100};}}
  }};
  window.alert=message=>tp105SyncProbe.alerts.push(message);
  cloudProfile={sub:'test-owner'};db.set('cloudDevice',device);db.set('cloudPrefs',{drive:false,calendar:false});rf130SyncWorkoutPhotos=async()=>{};
  go('settings');window.tp105TestSync=syncCloud(false).then(()=>{tp105SyncProbe.finished=true;});
 });
 await page.waitForFunction(()=>tp105SyncProbe.started);
 assert.ok((await page.locator('#cloudStatus').textContent()).includes('Drive-szinkron kész.'));
 assert.ok((await page.locator('#cloudStatus').textContent()).includes('A helyi mentés már használható.'));
 await page.locator('.tp105-data>summary').click();await page.locator('[onclick="tp105Archive(false)"]').click();
 await page.waitForFunction(()=>tp105SyncProbe.saved===1&&!window.TrainPilotBackupBusy);
 assert.equal(await page.evaluate(()=>tp105SyncProbe.alerts.some(x=>x.includes('Előbb fejezd be'))),false);
 await page.evaluate(()=>tp105FinishPrune());await page.waitForFunction(()=>tp105SyncProbe.finished&&!cloudBusy);
 await page.evaluate(()=>syncCloud(false));assert.equal(await page.evaluate(()=>tp105SyncProbe.reads),1);
 assert.equal(await page.evaluate(()=>cloudDriveStage),'');assert.deepEqual(probeErrors,[]);await page.close();
 console.log('PASS publication UI: four languages at 320/360/393/412px, consent off, archive controls, navigation/theme re-entry, actual sync completion and usable backup during retention');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exit(1)});
function tpLabel(lang){return {hu:'Teljes ZIP',en:'Full ZIP',de:'Vollständige ZIP',ro:'Copie ZIP'}[lang];}
