const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname==='/'?'/index.html':new URL(req.url,'http://local').pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({args:['--no-sandbox']});const p=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));fs.mkdirSync('ui-evidence',{recursive:true});
 await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished);
 await p.evaluate(()=>go('settings'));await p.locator('.tp111-ble-entry button').click();assert.match(await p.locator('[data-ble-status]').innerText(),/Android APK/);assert.equal(await p.locator('[data-ble-action="scan"]').isDisabled(),true);await p.evaluate(()=>tp155R4ClosePanel(false));
 await p.evaluate(()=>{
  window.__ble={listeners:new Set(),calls:[],status:{state:'idle',supported:true,sdk:35,services:[]},failure:null,pending:null,saves:[],saveFailure:false,saveCancel:false};
  const b=window.__ble;b.emit=e=>{for(const fn of b.listeners)fn(e);};
  const setStatus=(s,extra={})=>{b.status={...b.status,state:s,...extra};b.emit({kind:'state',...b.status});return {...b.status};};
  const plugin={
   addListener:async(_,fn)=>{b.listeners.add(fn);return {remove:async()=>b.listeners.delete(fn)};},getStatus:async()=>({...b.status}),
   startScan:async()=>{b.calls.push('scan');if(b.failure){const e=new Error('denied');e.code=b.failure;throw e;}return setStatus('scanning',{services:[],heartSupported:false,code:''});},
   stopScan:async()=>{b.calls.push('stop');return setStatus('idle',{code:'SCAN_FINISHED'});},
   connect:async({id})=>{b.calls.push('connect:'+id);setStatus('connecting');if(b.pending==='connect')return new Promise((resolve,reject)=>b.pendingReject=reject);return setStatus('connected',{deviceName:'GT4Pro+',heartSupported:b.noHeart!==true,services:[{uuid:'0000180d-0000-1000-8000-00805f9b34fb',type:0,characteristics:[{uuid:'00002a37-0000-1000-8000-00805f9b34fb',properties:16,descriptors:['00002902-0000-1000-8000-00805f9b34fb']}]}]});},
   startHeartRate:async()=>{b.calls.push('heart');return setStatus('monitoring');},
   disconnect:async()=>{b.calls.push('disconnect');if(b.pendingReject){const e=new Error('cancelled');e.code='DISCONNECTED';b.pendingReject(e);b.pendingReject=null;}return setStatus('disconnected',{code:'DISCONNECTED'});}
  };
  window.Capacitor={isNativePlatform:()=>true,Plugins:{BleDiscovery:plugin,NativeFiles:{save:async args=>{b.saves.push(JSON.parse(args.data));if(b.saveFailure)throw Error('quota');if(b.saveCancel)return {cancelled:true};setStatus('disconnected',{code:'BACKGROUND'});return {verified:true,name:args.name,bytes:args.data.length};}}}};
  b.device=id=>b.emit({kind:'device',device:{id,name:id==='watch'?'GT4Pro+':'Other device',rssi:-50,advertisedServices:['0000180d-0000-1000-8000-00805f9b34fb'],manufacturers:[{companyId:4660,length:8}]}});
  b.history=JSON.stringify(history());b.health=JSON.stringify(state.health);
 });
 await p.evaluate(()=>go('settings'));
 await p.locator('.tp111-ble-entry button').click();await p.waitForFunction(()=>__ble.listeners.size===1);
 await p.locator('[data-ble-action="scan"]').click();
 await p.evaluate(()=>{for(let i=0;i<42;i++)__ble.emit({kind:'device',device:{id:'anonymous-'+i,name:'',rssi:-63-i,displayAddress:'AA:BB:CC:DD:EE:'+i.toString(16).padStart(2,'0'),advertisedServices:i===4?['6e40ab01-b5a3-f393-e0a9-e50e24dcca9e']:[],manufacturers:[]}});});
 await p.waitForFunction(()=>document.querySelectorAll('[data-ble-device]').length===40);
 assert.equal(await p.locator('[data-ble-device]').first().getAttribute('data-ble-device'),'anonymous-4','known protocol candidate appears before unnamed neighbours');
 for(const width of [320,360,393,412])for(const lang of ['hu','en','de','ro']){
  await p.setViewportSize({width,height:width===320?740:873});await p.evaluate(lang=>{db.set('language',lang);tp155R4RefreshPanel();},lang);
  assert.match(await p.locator('[data-ble-device="anonymous-0"]').innerText(),/#1.*\n.*AA:BB:CC:DD:EE:00/);
  assert.match(await p.locator('[data-ble-device="anonymous-4"]').innerText(),/Realtek/);
  const visibleText=await p.locator('.tp111-ble').innerText();for(const key of ['identify','deviceNumber','macAddress','candidate','scanExport'])assert.ok(!new RegExp('\\b'+key+'\\b').test(visibleText),'no untranslated BLE key '+lang+'/'+key);
  const inputColors=await p.locator('[data-ble-filter]').evaluate(e=>({background:getComputedStyle(e).backgroundColor,color:getComputedStyle(e).color}));assert.notEqual(inputColors.background,'rgb(255, 255, 255)','MAC input keeps dark app surface');assert.notEqual(inputColors.color,'rgb(0, 0, 0)','MAC input remains readable');
  assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth-innerWidth)<=1,'40 unnamed devices fit '+width+'/'+lang);
  if(width===393&&lang==='hu')await p.screenshot({path:'ui-evidence/ble-unnamed-2700.png'});
 }
 await p.evaluate(()=>__ble.emit({kind:'device',device:{id:'anonymous-0',name:'',rssi:-99,displayAddress:'AA:BB:CC:DD:EE:00',advertisedServices:[],manufacturers:[]}}));
 await p.waitForFunction(()=>document.querySelector('[data-ble-device="anonymous-0"]').textContent.includes('-99'));
 assert.match(await p.locator('[data-ble-device="anonymous-0"]').innerText(),/#1/,'RSSI sorting must not renumber anonymous devices');
 await p.locator('[data-ble-filter]').fill('aa-bb-cc-dd-ee-04');assert.equal(await p.locator('[data-ble-device]').count(),1);assert.equal(await p.locator('[data-ble-device]').getAttribute('data-ble-device'),'anonymous-4','exact MAC filter finds the known RDFit watch');
 await p.locator('[data-ble-filter]').fill('00:00:00:00:00:00');assert.equal(await p.locator('[data-ble-device]').count(),0,'unmatched MAC must not guess an identity');
 await p.locator('[data-ble-filter]').fill('');assert.equal(await p.locator('[data-ble-device]').count(),40);
 assert.equal(await p.locator('[data-ble-action="exportReport"]').isDisabled(),true,'finish scanning before opening the file picker');
 await p.locator('[data-ble-action="stop"]').click();assert.equal(await p.locator('[data-ble-action="exportReport"]').isEnabled(),true,'diagnostics without connecting');
 await p.locator('[data-ble-action="exportReport"]').click();await p.waitForFunction(()=>__ble.saves.length===1);
 const scanReport=await p.evaluate(()=>__ble.saves[0]);assert.equal(scanReport.mode,'scan');assert.equal(scanReport.scanDevices.length,40);assert.equal(scanReport.services.length,0);
 for(const secret of ['AA:BB:CC:DD:EE:','anonymous-','displayAddress'])assert.ok(!JSON.stringify(scanReport).includes(secret),'saved scan excludes '+secret);
 await p.evaluate(()=>{tp155R4ClosePanel(false);__ble.saves=[];});
 for(const width of [320,360,393,412])for(const lang of ['hu','en','de','ro'])for(const theme of ['classicBlue','blue']){
  await p.setViewportSize({width,height:width===320?740:873});await p.evaluate(({lang,theme})=>{tp155R4ClosePanel(false);db.set('language',lang);rf200SetTheme(theme);go('settings');},{lang,theme});
  await p.locator('.tp111-ble-entry button').click();await p.waitForFunction(()=>__ble.listeners.size===1);assert.equal(await p.locator('#tp155R4PanelHost').getAttribute('data-panel'),'ble');
  await p.locator('[data-ble-action="scan"]').click();await p.evaluate(()=>{__ble.device('other');__ble.device('watch');__ble.device('watch');});await p.waitForFunction(()=>document.querySelectorAll('[data-ble-device]').length===2);
  assert.equal(await p.locator('[data-ble-device]').first().getAttribute('data-ble-device'),'watch');assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth-innerWidth)<=1,'BLE horizontal fit '+width+'/'+lang+'/'+theme);
  await p.locator('[data-ble-device="watch"]').click();await p.waitForFunction(()=>document.querySelector('[data-ble-action="heart"]').disabled===false);await p.locator('[data-ble-action="heart"]').click();
  await p.evaluate(()=>__ble.emit({kind:'pulse',bpm:72,measuredAt:new Date().toISOString()}));await p.waitForFunction(()=>document.querySelector('[data-ble-bpm]').textContent==='72 bpm');
  assert.equal(await p.evaluate(()=>__ble.listeners.size),1,'exactly one BLE listener after repeated navigation');
  assert.equal(await p.evaluate(()=>JSON.stringify(history())===__ble.history),true);assert.equal(await p.evaluate(()=>JSON.stringify(state.health)===__ble.health),true,'live BLE must not overwrite Health Connect');
  if(width===393&&lang==='hu'&&theme==='blue')await p.screenshot({path:'ui-evidence/ble-live-2700.png'});
 }
 await p.evaluate(()=>{db.set('language','hu');tp155R4RefreshPanel();});
 await p.evaluate(()=>__ble.emit({kind:'pulse',bpm:85,measuredAt:new Date(Date.now()-20000).toISOString()}));await p.waitForFunction(()=>document.querySelector('[data-ble-bpm]').textContent==='—');
 await p.locator('[data-ble-action="exportReport"]').click();await p.waitForFunction(()=>__ble.saves.length===1);const report=await p.evaluate(()=>__ble.saves[0]);assert.equal(report.format,'TrainPilot-BLE-diagnostic');assert.equal(report.device.name,'GT4Pro+');assert.ok(!JSON.stringify(report).includes('bpm'));assert.ok(!JSON.stringify(report).includes('"id"'));assert.equal(await p.locator('[data-ble-pulse]').isVisible(),false);
 await p.evaluate(()=>{__ble.saveFailure=true;});await p.locator('[data-ble-action="exportReport"]').click();await p.waitForFunction(()=>document.querySelector('[data-ble-export-status]').textContent.includes('sikerült'));
 await p.evaluate(()=>{__ble.saveFailure=false;__ble.saveCancel=true;});const before=await p.locator('[data-ble-export-status]').innerText();await p.locator('[data-ble-action="exportReport"]').click();assert.equal(await p.locator('[data-ble-export-status]').innerText(),before,'cancel does not report success');
 await p.evaluate(()=>{tp155R4ClosePanel(false);__ble.failure='PERMISSION_DENIED';go('settings');});await p.locator('.tp111-ble-entry button').click();await p.locator('[data-ble-action="scan"]').click();await p.waitForFunction(()=>document.querySelector('[data-ble-status]').textContent.includes('Közeli eszközök'));assert.equal(await p.locator('[data-ble-action="scan"]').isEnabled(),true);
 await p.evaluate(()=>{__ble.failure=null;__ble.noHeart=true;});await p.locator('[data-ble-action="scan"]').click();await p.evaluate(()=>__ble.device('watch'));await p.locator('[data-ble-device="watch"]').click();await p.waitForFunction(()=>document.querySelector('[data-ble-capability]').textContent.includes('Nem található'));assert.equal(await p.locator('[data-ble-action="heart"]').isDisabled(),true);assert.equal(await p.locator('[data-ble-action="exportReport"]').isEnabled(),true);
 await p.evaluate(()=>{__ble.emit({kind:'state',...__ble.status,state:'disconnected',code:'BACKGROUND'});});assert.equal(await p.locator('[data-ble-device]').count(),0,'background invalidates scanned devices');assert.equal(await p.locator('[data-ble-pulse]').isVisible(),false);
 await p.evaluate(()=>{__ble.noHeart=false;__ble.pending='connect';});await p.locator('[data-ble-action="scan"]').click();await p.evaluate(()=>__ble.device('watch'));await p.locator('[data-ble-device="watch"]').click();await p.waitForFunction(()=>__ble.status.state==='connecting');await p.evaluate(()=>go('home'));await p.waitForFunction(()=>__ble.listeners.size===0);assert.equal(await p.locator('#tp155R4PanelHost').count(),0);assert.equal(await p.evaluate(()=>__ble.calls.at(-1)),'disconnect');
 await p.reload();await p.waitForFunction(()=>TrainPilotBoot.finished);assert.equal(await p.evaluate(()=>window.__ble),undefined,'BLE sessions do not persist across app restart');
 assert.deepEqual(errors,[]);console.log('PASS #105 BLE UI: 32 phone/language/theme cases, web fallback, scan deduplication, live/old pulse, separate Health data, diagnostic export/failure/cancel, permission denial, missing standard service, background cleanup and cancellation after navigation');
}finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
