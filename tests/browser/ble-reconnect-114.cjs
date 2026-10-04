const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{const file=path.resolve(root,'.'+(new URL(req.url,'http://local').pathname==='/'?'/index.html':new URL(req.url,'http://local').pathname));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({args:['--no-sandbox']});const p=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];p.on('pageerror',e=>errors.push(String(e)));
 await p.addInitScript(()=>{window.__bleCalls=[];const record=async()=>{__bleCalls.push('native');return {};};window.Capacitor={isNativePlatform:()=>true,Plugins:{BleDiscovery:new Proxy({},{get:()=>record}),GoogleSync:{status:async()=>({connected:false})},HealthBridge:{getStatus:async()=>({supported:true,permissions:{}})}}};});
 await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished);
 for(const width of [320,360,393,412])for(const lang of ['hu','en','de','ro']){
  await p.setViewportSize({width,height:873});await p.evaluate(lang=>{tp155R4ClosePanel(false);db.set('language',lang);go('home');tp155R4OpenPanel('settings');},lang);
  assert.equal(await p.locator('.tp111-ble-entry, [onclick*=\"ble\"]').count(),0,'no diagnostic entry '+width+'/'+lang);
  assert.equal(await p.evaluate(()=>tp155R4OpenPanel('ble')),false,'retired panel cannot open through a route');
  assert.equal(await p.locator('[data-panel="ble"]').count(),0);await p.evaluate(()=>tp155R4RefreshPanel());
  assert.equal(await p.locator('.tp111-ble-entry').count(),0,'refresh keeps experimental UI retired');
 }
 await p.evaluate(()=>{tp155R4ClosePanel(false);const s=settings();s.theme='blue';db.set('settings',s);render();document.dispatchEvent(new Event('visibilitychange'));});
 await p.reload();await p.waitForFunction(()=>TrainPilotBoot.finished);await p.evaluate(()=>tp155R4OpenPanel('settings'));
 assert.equal(await p.locator('.tp111-ble-entry').count(),0);assert.deepEqual(await p.evaluate(()=>__bleCalls),[],'navigation, resume and reload do not request Bluetooth');
 assert.equal(await p.evaluate(()=>typeof TrainPilotBleCore), 'object','protocol code is retained');assert.deepEqual(errors,[]);console.log('PASS retired Bluetooth UI: 16 viewport/language cases, navigation/refresh/theme/reload, blocked route, no native Bluetooth calls; protocol code retained');
 }finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
