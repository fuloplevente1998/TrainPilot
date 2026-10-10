const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
(async()=>{
 const root=path.resolve('www'),server=http.createServer((req,res)=>{const name=new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'),file=path.resolve(root,'.'+name);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return;}fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);res.end();return;}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{browser=await chromium.launch({headless:true,args:['--no-sandbox']});
 for(const width of [320,360,393,412])for(const language of ['hu','en','de','ro']){
  const page=await browser.newPage({viewport:{width,height:800},locale:language});const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{try{localStorage.setItem("repforge:onboarding128",JSON.stringify("skipped"))}catch(_){}});await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(lang=>{db.set('language',lang);go('settings');},language);
  await page.waitForSelector('.tp105-data');await page.locator('.tp105-data>summary').click();
  const result=await page.evaluate(()=>({text:document.querySelector('.tp105-data').textContent,checked:document.querySelector('.tp105-data input').checked,overflow:document.documentElement.scrollWidth>innerWidth+1,buttons:[...document.querySelectorAll('.tp105-data button')].map(x=>x.getAttribute('onclick'))}));
  await page.keyboard.press('Tab');
  const consentStyle=await page.evaluate(()=>{
   const input=document.querySelector('.tp105-health-consent input'),before=getComputedStyle(input);const off={appearance:before.appearance,width:before.width,radius:before.borderRadius};
   input.checked=true;input.focus();const selected=getComputedStyle(input),mark=getComputedStyle(input,'::after');
   const ref=document.createElement('span');ref.style.color='var(--accent)';document.body.appendChild(ref);
   const checked={background:selected.backgroundColor,accent:getComputedStyle(ref).color,markOpacity:mark.opacity,outline:selected.outlineStyle,labelHeight:input.closest('label').getBoundingClientRect().height};
   ref.remove();input.checked=false;return {off,checked};
  });
  assert.equal(consentStyle.off.appearance,'none');assert.equal(consentStyle.off.width,'26px');assert.equal(consentStyle.off.radius,'7px');
  assert.equal(consentStyle.checked.background,consentStyle.checked.accent);assert.equal(consentStyle.checked.markOpacity,'1');assert.equal(consentStyle.checked.outline,'solid');assert.ok(consentStyle.checked.labelHeight>=44);
  assert.equal(result.checked,false,'health export must require consent');assert.equal(result.overflow,false,`${width}/${language} settings overflow`);assert.ok(result.buttons.includes('tp105Archive(false)'));assert.ok(result.buttons.includes('tp105Archive(true)'));assert.ok(result.text.includes(tpLabel(language)));
  await page.evaluate(()=>{window.tp155R4ClosePanel(false);go('history');go('home');rf200SetTheme('green');go('settings');});await page.waitForSelector('.tp105-data');assert.equal(await page.locator('.tp105-data input').isChecked(),false);assert.deepEqual(errors,[]);await page.close();
 }
 // Exercise the actual DOMContentLoaded Drive override with an unfinished retention request.
 const page=await browser.newPage({viewport:{width:393,height:800},locale:'hu'});
 const probeErrors=[];page.on('pageerror',e=>probeErrors.push(e.message));
 await page.addInitScript(()=>{try{localStorage.setItem("repforge:onboarding128",JSON.stringify("skipped"))}catch(_){}});await page.goto(`http://127.0.0.1:${server.address().port}`);await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
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
  go('settings');window.tp105TestSync=syncCloud(false,true).then(()=>{tp105SyncProbe.finished=true;});
 });
 await page.waitForFunction(()=>tp105SyncProbe.started);
 assert.ok((await page.locator('#cloudStatus').textContent()).includes('Drive-szinkron kész.'));
 assert.ok((await page.locator('#cloudStatus').textContent()).includes('A helyi mentés már használható.'));
 await page.locator('.tp105-data>summary').click();await page.locator('[onclick="tp105Archive(false)"]').click();
 await page.waitForFunction(()=>tp105SyncProbe.saved===1&&!window.TrainPilotBackupBusy);
 assert.equal(await page.evaluate(()=>tp105SyncProbe.alerts.some(x=>x.includes('Előbb fejezd be'))),false);
 await page.evaluate(()=>tp105FinishPrune());await page.waitForFunction(()=>tp105SyncProbe.finished&&!cloudBusy);
 await page.evaluate(()=>syncCloud(false));assert.equal(await page.evaluate(()=>tp105SyncProbe.reads),1);
 // An automatic request remains pending, but the real ZIP button must stay usable.
 await page.evaluate(()=>{
  db.set('cloudPrefs',{drive:true,calendar:true});
  Capacitor.Plugins.GoogleSync.driveList=()=>new Promise((resolve,reject)=>{window.tp105FailNetwork=()=>reject(Object.assign(Error('A Google nem érhető el hálózati hiba miatt.'),{code:'GOOGLE_NETWORK'}));});
  window.tp105PendingSync=syncCloud(true);
 });
 await page.locator('.tp105-data>summary').click();
 await page.locator('[onclick="tp105Archive(false)"]').click();await page.waitForFunction(()=>tp105SyncProbe.saved===2&&!window.TrainPilotBackupBusy);
 assert.equal(await page.evaluate(()=>cloudBusy),true);assert.equal(await page.evaluate(()=>tp105SyncProbe.alerts.some(x=>x.includes('Előbb fejezd be'))),false);
 await page.evaluate(async()=>{tp105FailNetwork();await tp105PendingSync;});assert.equal(await page.evaluate(()=>cloudBusy),false);
 await page.locator('.tp105-data>summary').click();
 await page.locator('[onclick="tp105Archive(false)"]').click();await page.waitForFunction(()=>tp105SyncProbe.saved===3&&!window.TrainPilotBackupBusy);

 assert.equal(await page.evaluate(()=>cloudDriveStage),'');assert.deepEqual(probeErrors,[]);await page.close();
 // Real buttons: ordinary sync must not scan/prune earlier backups. Recovery is
 // a separate, collapsed action which still preserves legacy records.
 const normalPage=await browser.newPage({viewport:{width:393,height:800},locale:'hu'});
 await normalPage.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));await normalPage.goto(`http://127.0.0.1:${server.address().port}`);await normalPage.waitForFunction(()=>window.TrainPilotBoot?.finished);
 await normalPage.evaluate(()=>{
  const device='00000000-0000-0000-0000-000000000001',empty=JSON.parse(JSON.stringify(syncData())),files=[],snapshots=new Map();let writes=0;
  window.drive161Probe={reads:[],prunes:0};
  const add=(id,data,time)=>{files.push({id,name:'repforge-sync-'+device+'-'+id+'.json',createdTime:time,version:'1'});snapshots.set(id,{app:'RepForgeSync',schema:1,owner:'drive-test',device,data});};
  for(let i=0;i<24;i++){const data=JSON.parse(JSON.stringify(empty));data.weights=[{kg:70+i/10,date:new Date(Date.UTC(2025,0,i+1)).toISOString()}];add('archive-'+i,data,new Date(Date.UTC(2025,0,i+1)).toISOString());}add('head',empty,'2026-10-01T12:00:00Z');
  window.Capacitor={isNativePlatform:()=>true,Plugins:{GoogleSync:{driveList:async()=>({files}),driveRead:async({id})=>{drive161Probe.reads.push(id);return {data:JSON.stringify(snapshots.get(id)),version:'1'};},driveWrite:async({data})=>{const id='new-'+(++writes);add(id,JSON.parse(data).data,new Date(Date.UTC(2026,9,2+writes)).toISOString());return {id,version:'1',verified:true};},drivePrune:async()=>{drive161Probe.prunes++;return {deleted:0};}}}};
  cloudProfile={sub:'drive-test'};db.set('cloudDevice',device);db.set('cloudPrefs',{drive:false,calendar:false});rf130SyncWorkoutPhotos=async()=>{};go('settings');
  const button=document.querySelector('[onclick="syncCloud(false)"]');for(let p=button.parentElement;p;p=p.parentElement)if(p.tagName==='DETAILS')p.open=true;
 });
 await normalPage.locator('[onclick="syncCloud(false)"]').click();await normalPage.waitForFunction(()=>drive161Probe.reads.length===1&&!cloudBusy);assert.deepEqual(await normalPage.evaluate(()=>drive161Probe.reads),['head']);assert.equal(await normalPage.evaluate(()=>drive161Probe.prunes),0);
 assert.equal(await normalPage.locator('[onclick="syncCloud(false,true)"]').isVisible(),false,'earlier recovery remains collapsed');
 await normalPage.locator('details').filter({has:normalPage.locator('[onclick="syncCloud(false,true)"]')}).last().locator('summary').first().click();await normalPage.locator('[onclick="syncCloud(false,true)"]').click();await normalPage.waitForFunction(()=>!cloudBusy&&weights().length===24);assert.equal(await normalPage.evaluate(()=>drive161Probe.reads.filter(x=>x.startsWith('archive')).length),24);await normalPage.close();
 console.log('PASS publication UI: four languages at 320/360/393/412px, consent off, archive controls, navigation/theme re-entry, themed Health checkbox, actual sync completion and usable backup during pending network/retention requests');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}
})().catch(e=>{console.error(e);process.exit(1)});
function tpLabel(lang){return {hu:'Teljes ZIP',en:'Full ZIP',de:'Vollständige ZIP',ro:'Copie ZIP'}[lang];}
