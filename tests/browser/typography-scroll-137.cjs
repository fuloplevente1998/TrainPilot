'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const root=path.resolve('www'),server=http.createServer((req,res)=>{
 const f=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
 if(!f.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(f,(e,d)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',f.endsWith('.js')?'text/javascript':f.endsWith('.css')?'text/css':f.endsWith('.woff2')?'font/woff2':f.endsWith('.html')?'text/html':'application/octet-stream');res.end(d);});
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',reducedMotion:'reduce'}),errors=[],external=[];
  page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
  const origin='http://127.0.0.1:'+server.address().port;
  page.on('request',r=>{if(/^https?:/.test(r.url())&&!r.url().startsWith(origin))external.push(r.url());});
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto(origin);await page.waitForFunction(()=>TrainPilotBoot.finished);await page.evaluate(()=>document.fonts.ready);
  const initial=await page.evaluate(()=>JSON.stringify({programs:programs(),history:history(),scheduled:scheduled()}));
  const cdp=await page.context().newCDPSession(page);await cdp.send('DOM.enable');await cdp.send('CSS.enable');
  async function glyphs(){
   await page.evaluate(()=>{document.querySelector('#tp137FontProbe')?.remove();const e=document.createElement('span');e.id='tp137FontProbe';e.textContent='Árvíztűrő tükörfúrógép. Știință, înot, înălțime.';document.querySelector('#app main').append(e);});
   const {root}=await cdp.send('DOM.getDocument');const {nodeId}=await cdp.send('DOM.querySelector',{nodeId:root.nodeId,selector:'#tp137FontProbe'});
   const {fonts}=await cdp.send('CSS.getPlatformFontsForNode',{nodeId});
   assert.ok(fonts.length>0,'actual accent glyphs rendered');
   assert.ok(fonts.every(f=>f.isCustomFont&&/Inter/.test(f.familyName)&&f.glyphCount>0),'Hungarian/Romanian glyphs use the bundled font '+JSON.stringify(fonts));
   await page.evaluate(()=>document.querySelector('#tp137FontProbe').remove());
  }
  await glyphs();
  fs.mkdirSync('ui-evidence',{recursive:true});
  for(const [width,height,lang] of [[320,740,'hu'],[393,873,'hu'],[412,915,'de'],[393,873,'en'],[873,393,'ro']])for(const theme of ['classicBlue','yellow']){
   await page.setViewportSize({width,height});await page.evaluate(({lang,theme})=>{tp155R4ClosePanel(false);db.set('language',lang);rf200SetTheme(theme)},{lang,theme});
   for(const route of ['home','plan','programs','health','history','coach','calendar','settings']){
    await page.evaluate(r=>{tp155R4ClosePanel(false);if(['coach','calendar','settings'].includes(r)){go('home');tp155R4OpenPanel(r,document.activeElement)}else go(r)},route);
    const fonts=await page.evaluate(()=>{
     const main=document.querySelector('#tp155R4PanelHost main')||document.querySelector('#app main');
     return [...main.querySelectorAll('h1,h2,h3,p,.ex-name,.btn,input')].filter(e=>e.getClientRects().length).map(e=>{const s=getComputedStyle(e);return {text:e.textContent.slice(0,30),family:s.fontFamily,size:parseFloat(s.fontSize),weight:Number(s.fontWeight),stretch:s.fontStretch};});
    });
    assert.ok(fonts.length,route+': real text controls');
    assert.ok(fonts.every(f=>f.family.startsWith('"TrainPilot Inter"')),route+': consistent font '+JSON.stringify(fonts));
    assert.ok(fonts.every(f=>f.stretch==='100%'||f.stretch==='normal'),route+': normal width glyphs');
    const heading=await page.evaluate(()=>{
     const main=document.querySelector('#tp155R4PanelHost main')||document.querySelector('#app main'),e=main.querySelector('h1');if(!e)return null;const s=getComputedStyle(e);return {size:parseFloat(s.fontSize),weight:Number(s.fontWeight)};
    });
    if(heading){
     assert.equal(heading.weight,800,route+': strong title');
     // Home's active-program h1 is a compact card heading. The accepted
     // fixed overview deliberately reduces it on short/narrow phones.
     if(route==='home')assert.ok([16,18,24].includes(heading.size),'responsive Home card title');
     else assert.equal(heading.size,24,route+': page title hierarchy');
    }
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,[width,lang,theme,route]+' no horizontal clipping');
   }
   await page.evaluate(()=>{tp155R4ClosePanel(false);go('programs')});
   for(const y of [0,28,44,120,44,12,0]){
    await page.evaluate(y=>scrollTo(0,y),y);await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
    const audit=await page.locator('.top').evaluate(e=>{const s=getComputedStyle(e),r=e.getBoundingClientRect(),m=e.querySelector('.tp137-selection'),b=e.querySelector('.tp137-selected'),a=m.getBoundingClientRect(),t=b.getBoundingClientRect();return {bg:s.backgroundColor,blur:s.backdropFilter,top:r.top,padding:s.padding,compact:document.documentElement.classList.contains('tp-nav-compact'),marker:{x:a.x,y:a.y,w:a.width,h:a.height},button:{x:t.x,y:t.y,w:t.width,h:t.height},delta:Math.max(Math.abs(a.x-t.x),Math.abs(a.y-t.y),Math.abs(a.width-t.width),Math.abs(a.height-t.height))};});
    assert.equal(audit.bg,'rgb(14, 16, 21)','opaque menu while scrolling');assert.equal(audit.blur,'none');assert.ok(Math.abs(audit.top)<1);assert.ok(audit.delta<1,'selection tracks the same button while scrolling '+JSON.stringify({width,height,lang,theme,y,audit}));
    // Change the real full-width action passing UNDER the sticky menu. Its
    // color must not affect a single captured menu pixel (the reported video).
    const rect=await page.locator('.top').boundingBox(),clip={x:rect.x+2,y:rect.y+2,width:rect.width-12,height:rect.height-4};
    await page.locator('main .btn').first().evaluate(e=>e.style.setProperty('background','#ff00ff','important'));
    const magenta=await page.screenshot({clip});
    await page.locator('main .btn').first().evaluate(e=>e.style.setProperty('background','#00ff00','important'));
    const green=await page.screenshot({clip});
    assert.deepEqual(green,magenta,[width,lang,theme,y]+': page actions do not shine through main-menu pixels');
    await page.locator('main .btn').first().evaluate(e=>e.style.removeProperty('background'));
   }
   if(width===393&&lang==='hu'&&theme==='yellow'){
    await page.evaluate(()=>go('home'));await page.screenshot({path:'ui-evidence/typography-home-137.png'});
    await page.evaluate(()=>tp155R4OpenPanel('coach',document.activeElement));await page.screenshot({path:'ui-evidence/typography-coach-137.png'});
   }
  }
  await page.evaluate(()=>{tp155R4ClosePanel(false);go('home')});
  assert.equal(await page.evaluate(()=>JSON.stringify({programs:programs(),history:history(),scheduled:scheduled()})),initial,'visual changes preserve user data');
  // Verify the shipped service-worker cache includes the local font too.
  await page.evaluate(()=>navigator.serviceWorker.register('sw.js'));await page.waitForFunction(()=>navigator.serviceWorker.controller);
  await page.context().setOffline(true);await page.reload();await page.waitForFunction(()=>TrainPilotBoot.finished);await page.evaluate(()=>document.fonts.ready);await glyphs();
  assert.deepEqual(external,[],'no remote font/CDN dependency');assert.deepEqual(errors,[]);
  console.log('PASS #137 typography/scroll: real Inter Hungarian/Romanian glyphs, offline font restart, shared page/body hierarchy, 4 languages and phone/landscape widths, both theme families; 70 real scroll positions and pixel-identical opaque menu over contrasting actions; immutable data');
 }finally{await browser?.close();server.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
