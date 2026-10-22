'use strict';
// Regression for #236 P1.1: panel refresh preserves navigation, focusability,
// and clamped scroll position without forcing scrollHeight/clientHeight reads.
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http'),{chromium}=require('playwright');
const source=fs.readFileSync('www/app.js','utf8');
const start=source.indexOf('window.tp155R4RefreshPanel=function(){'),end=source.indexOf('window.tp155R4ClosePanel=function',start);
assert.ok(start>=0&&end>start,'canonical panel refresh exists');
const refresh=source.slice(start,end);
assert.match(refresh,/positionPanel\(\);\s*const builder=/,'navigation position read precedes content writes');
assert.match(refresh,/panel\.scrollTop=y;/,'native scrolling clamp restores position');
assert.doesNotMatch(refresh,/panel\.(scrollHeight|clientHeight)/,'no forced post-write panel size reads');
const root=path.resolve('www');
const server=http.createServer((q,r)=>{const file=path.resolve(root,'.'+new URL(q.url,'http://local').pathname.replace(/\/$/,'/index.html'));if(!file.startsWith(root+path.sep)){r.writeHead(403);return r.end();}fs.readFile(file,(e,b)=>{if(e){r.writeHead(404);return r.end();}r.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');r.end(b)});});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU'}),errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>TrainPilotBoot?.finished);
  for(const width of [320,393])for(const route of ['calendar','coach','settings']){
   await page.setViewportSize({width,height:700});
   const info=await page.evaluate(route=>{
    window.tp155R4ClosePanel?.(false);go(route);
    const host=document.querySelector('#tp155R4PanelHost'),panel=host?.querySelector('.tp155-r4-panel'),content=host?.querySelector('.tp155-r4-panel-content');
    if(!host||!panel||!content)return {error:'Missing '+route+' panel'};
    // Force an overlong panel, refresh (removing the spacer) and check native
    // clamping to the new content length, regardless of whether that is shorter.
    const spacer=document.createElement('div');spacer.style.height='2800px';spacer.id='tp236ProbeSpacer';content.appendChild(spacer);
    panel.scrollTop=650;const before=panel.scrollTop;
    window.tp155R4RefreshPanel();
    const after=panel.scrollTop,max=Math.max(0,panel.scrollHeight-panel.clientHeight);
    return {type:host.dataset.panel,before,after,max,spacer:!!host.querySelector('#tp236ProbeSpacer'),nav:!!document.querySelector('.top'),close:!!host.querySelector('.tp155-r4-panel-close'),overflow:document.documentElement.scrollWidth-innerWidth};
   },route);
   assert.ok(!info.error,JSON.stringify(info));assert.equal(info.type,route);assert.equal(info.spacer,false);
   assert.ok(info.after>=0&&info.after<=info.max+2,JSON.stringify(info));
   if(info.max>=info.before-2)assert.ok(Math.abs(info.after-info.before)<=2,JSON.stringify(info));
   assert.equal(info.close,true);assert.equal(info.nav,true);assert.ok(info.overflow<=1,JSON.stringify(info));
   await page.evaluate(()=>tp155R4ClosePanel(false));assert.equal(await page.locator('#tp155R4PanelHost').count(),0);
  }
  assert.deepEqual(errors,[]);console.log('PASS #236 P1.1: pre-write nav read, native scroll clamp, calendar/coach/settings across 320/393px, close/overflow/no page errors');
 }finally{await browser?.close();server.close();}
})().catch(e=>{console.error(e);process.exitCode=1;server.close();});
