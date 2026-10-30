'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((q,r)=>{const name=new URL(q.url,'http://local').pathname,file=path.resolve(root,'.'+(name==='/'?'/index.html':name));if(!file.startsWith(root+path.sep)){r.writeHead(403);return r.end();}fs.readFile(file,(e,d)=>{if(e){r.writeHead(404);return r.end();}r.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');r.end(d);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;try{
 browser=await chromium.launch({args:['--no-sandbox']});
 const context=await browser.newContext({viewport:{width:393,height:873},hasTouch:true,isMobile:true,locale:'hu-HU'}),p=await context.newPage(),cdp=await context.newCDPSession(p),errors=[];
 p.on('pageerror',e=>errors.push(String(e)));await p.addInitScript(()=>{try{localStorage.setItem("repforge:onboarding128",JSON.stringify("skipped"))}catch(_){}});await p.goto('http://127.0.0.1:'+server.address().port);await p.waitForFunction(()=>TrainPilotBoot.finished);
 const menu=p.locator('#tp162ThemeMenu');
 const open=async()=>{await p.locator('.tp162-theme-dropdown .tp-select-trigger').click();await p.waitForTimeout(160);};
 const point=async column=>p.locator('.tp162-theme-column').nth(column).evaluate(e=>{const m=document.querySelector('#tp162ThemeMenu').getBoundingClientRect(),r=e.getBoundingClientRect();return {x:r.x+r.width/2,top:m.top+24,bottom:Math.min(innerHeight-24,m.bottom-24)};});
 for(const width of [320,360,393,412])for(const lang of ['hu','en','de','ro'])for(const theme of ['classicBlue','yellow']){
  const label=width+'/'+lang+'/'+theme;await p.setViewportSize({width,height:width===320?740:873});
  await p.evaluate(({lang,theme})=>{db.set('language',lang);rf200SetTheme(theme);go('home');tp155R4OpenPanel('calendar',document.activeElement);tp155R4OpenPanel('settings',document.activeElement);},{lang,theme});
  await open();
  const lastKeys=await p.evaluate(()=>[TP1511_BASIC.at(-1),TP1511_VIVID.at(-1)]);
  assert.ok(await menu.evaluate(e=>e.scrollHeight>e.clientHeight),label+' palette needs scrolling');
  for(const column of [0,1]){
   await menu.evaluate(e=>{e.scrollTop=0;});const pos=await point(column);await p.mouse.move(pos.x,(pos.top+pos.bottom)/2);await p.mouse.wheel(0,1500);
   await p.waitForTimeout(220);
   assert.ok(await menu.evaluate(e=>e.scrollTop>0),label+' wheel scroll over column '+column);
   assert.equal(await p.locator('.tp162-theme-dropdown.open').count(),1,label+' menu stays open during wheel');
   await menu.evaluate(e=>{e.scrollTop=0;});
   await cdp.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:pos.x,y:pos.bottom}]});
   for(let i=1;i<=8;i++){await cdp.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:pos.x,y:pos.bottom-(pos.bottom-pos.top)*i/8}]});await p.waitForTimeout(35);}
   await cdp.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await p.waitForTimeout(220);
   assert.ok(await menu.evaluate(e=>e.scrollTop>0),label+' touch swipe scroll over column '+column);
   assert.equal(await p.locator('.tp162-theme-dropdown.open').count(),1,label+' menu stays open during touch');
   // Re-read the hit position after touch/inertia. Reach the last row with real
   // gestures rather than assuming one synthetic wheel has already settled.
   for(let attempt=0;attempt<3;attempt++){
    const pos=await point(column);await p.mouse.move(pos.x,(pos.top+pos.bottom)/2);await p.mouse.wheel(0,1500);await p.waitForTimeout(300);
    if(await menu.evaluate(e=>e.scrollTop>=e.scrollHeight-e.clientHeight-1))break;
   }
   assert.ok(await menu.evaluate(e=>e.scrollTop>=e.scrollHeight-e.clientHeight-1),label+' wheel reaches menu bottom over column '+column);
   const choice=p.locator('.tp155-theme-option[data-theme="'+lastKeys[column]+'"]');
   // The columns have different lengths: align the requested column's final row.
   for(let attempt=0;attempt<3;attempt++){
    const delta=await choice.evaluate(e=>{const r=e.getBoundingClientRect(),m=document.querySelector('#tp162ThemeMenu').getBoundingClientRect();return r.top<m.top?r.top-m.top-12:r.bottom>m.bottom?r.bottom-m.bottom+12:0;});
    if(!delta)break;const pos=await point(column);await p.mouse.move(pos.x,(pos.top+pos.bottom)/2);await p.mouse.wheel(0,delta);await p.waitForTimeout(300);
   }
   const visible=await choice.evaluate(e=>{const r=e.getBoundingClientRect(),menu=document.querySelector('#tp162ThemeMenu'),m=menu.getBoundingClientRect();return {top:r.top,bottom:r.bottom,menuTop:m.top,menuBottom:m.bottom,scrollTop:menu.scrollTop,maxScroll:menu.scrollHeight-menu.clientHeight,hit:e.contains(document.elementFromPoint(r.x+r.width/2,r.y+r.height/2))};});assert.ok(visible.top>=visible.menuTop&&visible.bottom<=visible.menuBottom&&visible.hit,label+' last theme is visible and tappable '+JSON.stringify(visible));
   await choice.tap();assert.equal(await p.evaluate(()=>rf200ThemeKey()),lastKeys[column],label+' last palette entry saved');
   assert.equal(await p.locator('.tp162-theme-dropdown.open').count(),0,label+' selection closes menu');
   await open();
  }
  await p.keyboard.press('Escape');assert.equal(await p.locator('.tp162-theme-dropdown.open').count(),0);
  await p.evaluate(()=>tp155R4ClosePanel(false));
 }
 const saved=await p.evaluate(()=>rf200ThemeKey());await p.reload();await p.waitForFunction(()=>TrainPilotBoot.finished);assert.equal(await p.evaluate(()=>rf200ThemeKey()),saved,'bottom palette selection survives reload');
 assert.deepEqual(errors,[]);console.log('PASS #99: wheel and real touch scrolling over both theme columns, bottom entry tap/save/reload, menu remains open during scrolling, Calendar → Settings across four phone sizes/languages and matte/vivid themes');
}finally{await browser?.close();await new Promise(r=>server.close(r));}})().catch(e=>{console.error(e);process.exitCode=1;});
