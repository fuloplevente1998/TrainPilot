'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{const f=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!f.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(f,(e,b)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',f.endsWith('.js')?'text/javascript':f.endsWith('.css')?'text/css':f.endsWith('.woff2')?'font/woff2':'text/html');res.end(b);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));const browser=await chromium.launch({args:['--no-sandbox']});try{
 const url='http://127.0.0.1:'+server.address().port;
 const seed=()=>{localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped'));localStorage.setItem('repforge:language',JSON.stringify('hu'));};
 fs.mkdirSync('ui-evidence',{recursive:true});
 for(const width of [320,393,412]){
  const p=await browser.newPage({viewport:{width,height:873}});await p.addInitScript(seed);let unblock;const wait=new Promise(r=>unblock=r);
  await p.route('**/app.js',async route=>{await wait;await route.continue();});
  await p.goto(url,{waitUntil:'commit'});await p.locator('#tpBoot svg').waitFor();
  const before=await p.evaluate(()=>({width:innerWidth,overflow:document.documentElement.scrollWidth>innerWidth,app:getComputedStyle(document.querySelector('#app')).visibility,bg:getComputedStyle(document.querySelector('#tpBoot')).backgroundColor,animations:[...document.querySelectorAll('#tpBoot svg')].flatMap(g=>g.getAnimations().map(a=>({duration:a.effect.getTiming().duration,iterations:a.effect.getTiming().iterations})))}));
  assert.equal(before.overflow,false);assert.equal(before.app,'hidden');assert.equal(before.bg,'rgb(14, 16, 21)');assert.equal(before.animations.length,1);assert.ok(before.animations.every(a=>a.duration===30000&&a.iterations===1),'one continuous whole-logo loading zoom');
  // Sample the running loading animation without relying on real-time test timing.
  const loading=await p.evaluate(()=>{const logo=document.querySelector('#tpBoot svg'),a=logo.getAnimations()[0];a.pause();const values=[0,500,2000,8000].map(time=>{a.currentTime=time;return new DOMMatrixReadOnly(getComputedStyle(logo).transform).a;});a.currentTime=500;return values;});
  assert.ok(loading.every((v,i)=>i===0||v>loading[i-1]),'logo keeps growing, including slow local startup');
  if(width===393)await p.screenshot({path:'ui-evidence/startup-logo-140.png'});
  unblock();await p.waitForFunction(()=>window.TrainPilotBoot?.finished);assert.equal(await p.locator('#app').evaluate(e=>getComputedStyle(e).visibility),'visible');
  const exit=await p.evaluate(()=>{const overlay=document.querySelector('#tpBoot'),logo=overlay.querySelector('svg'),zoom=logo.getAnimations()[0],fade=overlay.getAnimations()[0];zoom.pause();fade.pause();const sample=time=>{zoom.currentTime=time;fade.currentTime=time;return {scale:new DOMMatrixReadOnly(getComputedStyle(logo).transform).a,alpha:Number(getComputedStyle(overlay).opacity)};};const initial=sample(0),middle=sample(336),last=sample(480);const card=logo.clientWidth*.5;zoom.finish();fade.finish();return {initial,middle,last,card};});
  assert.ok(Math.abs(exit.initial.scale-loading[1])<.001,'ready zoom continues without a size jump');assert.ok(exit.middle.scale>exit.initial.scale&&exit.last.scale>exit.middle.scale);assert.ok(exit.middle.alpha>0&&exit.middle.alpha<1);assert.equal(exit.last.alpha,0);assert.ok(exit.last.scale*exit.card>873,'logo grows beyond the screen before disappearing');
  await p.waitForFunction(()=>!document.querySelector('#tpBoot'));
  const saved=await p.evaluate(()=>Object.fromEntries(Object.keys(localStorage).filter(k=>/repforge:(history|programs|scheduled|draft)$/.test(k)).map(k=>[k,localStorage.getItem(k)])));
  await p.evaluate(()=>{go('history');go('home');window.dispatchEvent(new Event('pageshow'));});
  assert.equal(await p.locator('#tpBoot').count(),0,'navigation/resume does not replay splash');assert.deepEqual(await p.evaluate(()=>Object.fromEntries(Object.keys(localStorage).filter(k=>/repforge:(history|programs|scheduled|draft)$/.test(k)).map(k=>[k,localStorage.getItem(k)]))),saved);
  await p.close();
 }
 const mockNative=()=>{window.__startupCalls=[];window.Capacitor={isNativePlatform:()=>true,Plugins:{AppFeedback:{appearance:async()=>({fontScale:1}),signal:async()=>{},startupReady:async data=>{window.__startupCalls.push({failed:data.failed,hasApp:!!document.querySelector('#app')?.innerHTML,finished:!!window.TrainPilotBoot?.finished});}}},registerPlugin(name){return this.Plugins[name];}};};
 const native=await browser.newPage();await native.addInitScript(seed);await native.addInitScript(mockNative);await native.goto(url);await native.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.__startupCalls.length>0);
 assert.deepEqual(await native.evaluate(()=>window.__startupCalls),[{failed:false,hasApp:true,finished:true}],'native splash releases only after final local render');await native.evaluate(()=>TrainPilotBoot.finish());assert.equal(await native.evaluate(()=>window.__startupCalls.length),1,'readiness is idempotent');assert.equal(await native.locator('#tpBoot').count(),0);await native.close();
 const pendingNative=await browser.newPage();await pendingNative.addInitScript(seed);await pendingNative.addInitScript(()=>{
  window.__cloudChecks=0;window.__releaseExit=null;
  window.Capacitor={isNativePlatform:()=>true,Plugins:{AppFeedback:{appearance:async()=>({fontScale:1}),startupReady:()=>new Promise(resolve=>{window.__releaseExit=resolve;})},GoogleSync:{status:async()=>{window.__cloudChecks++;return {profile:null};}}},registerPlugin(name){return this.Plugins[name];}};
 });
 await pendingNative.goto(url);await pendingNative.waitForFunction(()=>window.TrainPilotBoot?.finished&&!!window.__releaseExit);
 await pendingNative.waitForTimeout(80);
 assert.equal(await pendingNative.evaluate(()=>window.__cloudChecks),0,'cloud work must not compete with the native final fade');
 await pendingNative.evaluate(()=>window.__releaseExit());await pendingNative.waitForFunction(()=>window.__cloudChecks===1);
 await pendingNative.evaluate(()=>TrainPilotBoot.finish());assert.equal(await pendingNative.evaluate(()=>window.__cloudChecks),1,'cloud starts once after native hand-off');await pendingNative.close();
 const reduced=await browser.newPage({reducedMotion:'reduce'});await reduced.addInitScript(seed);let unblock;const wait=new Promise(r=>unblock=r);await reduced.route('**/app.js',async r=>{await wait;await r.continue();});await reduced.goto(url,{waitUntil:'commit'});await reduced.locator('#tpBoot svg').waitFor();assert.equal(await reduced.evaluate(()=>[...document.querySelectorAll('#tpBoot svg')].flatMap(g=>g.getAnimations()).length),0);unblock();await reduced.waitForFunction(()=>window.TrainPilotBoot?.finished);assert.equal(await reduced.locator('#tpBoot').count(),0);await reduced.close();
 const failed=await browser.newPage();await failed.addInitScript(mockNative);await failed.route('**/app.js',r=>r.abort('failed'));await failed.goto(url);await failed.locator('#tpBoot button').waitFor();assert.match(await failed.locator('#tpBoot').textContent(),/betöltése nem sikerült/);assert.equal(await failed.locator('#tpBoot button').isVisible(),true);assert.deepEqual(await failed.evaluate(()=>window.__startupCalls),[{failed:true,hasApp:false,finished:false}],'a missing runtime releases native splash to reachable retry UI');await failed.unroute('**/app.js');await failed.locator('#tpBoot button').click();await failed.waitForFunction(()=>window.TrainPilotBoot?.finished);assert.equal(await failed.locator('#tpBoot').count(),0);await failed.close();
 console.log('PASS startup logo: immediate inline logo, 320/393/412 px, continuous loading zoom and screen-filling fade, native first-render/exit handshake and deferred cloud, reduced motion, no warm-resume replay/data edits, failed runtime and real retry.');
 }finally{await browser.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
