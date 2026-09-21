const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(data)});
 });
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:720},locale:'hu-HU'});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto('http://127.0.0.1:'+server.address().port+'/');
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.RepForge161?.version==='1.6.1');

  async function checkCompactPanel(type,open){
   await page.evaluate(open);
   const host=page.locator('#tp155R4PanelHost[data-panel="'+type+'"]');await host.waitFor();
   await page.waitForTimeout(180);
   const g=await page.evaluate(type=>{
    const nav=document.querySelector('.top.tp154-nav-grid').getBoundingClientRect();
    const host=document.querySelector('#tp155R4PanelHost[data-panel="'+type+'"]');
    const panel=host.querySelector('.tp155-r4-panel'),close=host.querySelector('.tp155-r4-panel-close'),content=host.querySelector('.tp155-r4-panel-content');
    const hr=host.getBoundingClientRect(),pr=panel.getBoundingClientRect(),cr=close.getBoundingClientRect(),xr=content.getBoundingClientRect();
    return {navBottom:nav.bottom,hostTop:hr.top,panelTop:pr.top,closeTop:cr.top,closeRight:cr.right,panelRight:pr.right,contentTop:xr.top,position:getComputedStyle(close).position,clear:getComputedStyle(content).clear};
   },type);
   assert.ok(g.hostTop<=g.navBottom+1&&g.hostTop>=g.navBottom-9,type+' panel should sit directly under/slightly overlap the fixed navigation: '+JSON.stringify(g));
   assert.ok(g.panelTop<=g.navBottom+3&&g.panelTop>=g.navBottom-6,type+' visible panel edge should sit directly against the navigation: '+JSON.stringify(g));
   assert.equal(g.position,'absolute',type+' close button should keep its visual position without reserving a blank row');
   assert.equal(g.clear,'none',type+' content should not clear below the close button');
   const is162=await page.evaluate(()=>!!window.TrainPilot162);
   if(is162){
    assert.ok(g.closeTop-g.panelTop>=13&&g.closeTop-g.panelTop<=18,type+' 1.6.2 X should sit slightly lower inside the panel: '+JSON.stringify(g));
    assert.ok(Math.abs((g.panelRight-g.closeRight)-14)<=3,type+' 1.6.2 X should sit further left inside the panel: '+JSON.stringify(g));
   }else{
    assert.ok(g.closeTop-g.panelTop>=7&&g.closeTop-g.panelTop<=14,type+' X vertical position changed unexpectedly: '+JSON.stringify(g));
    assert.ok(Math.abs((g.panelRight-g.closeRight)-10)<=3,type+' X should stay at the current right inset');
   }
   assert.ok(g.contentTop-g.panelTop<28,type+' panel content should start near the top instead of below a large empty gap');
   await page.evaluate(()=>window.tp155R4ClosePanel(false));
  }

  await checkCompactPanel('calendar',()=>go('calendar'));
  await checkCompactPanel('coach',()=>window.tp155R4OpenPanel('coach',document.activeElement));
  await checkCompactPanel('settings',()=>go('settings'));

  await page.evaluate(()=>{go('plan');window.tp155R4OpenPanel('quick',document.activeElement)});
  const quick=page.locator('#tp155R4PanelHost[data-panel="quick"]');await quick.waitFor();
  await quick.locator('#tp155QuickQuery').focus();
  await page.setViewportSize({width:393,height:480});
  await page.waitForTimeout(80);
  const keyboardLike=await page.evaluate(()=>{
   window.tp161AdjustPanel();
   const nav=document.querySelector('.top.tp154-nav-grid').getBoundingClientRect();
   const host=document.querySelector('#tp155R4PanelHost[data-panel="quick"]').getBoundingClientRect();
   const search=document.getElementById('tp155QuickQuery').getBoundingClientRect();
   return {navBottom:nav.bottom,hostTop:host.top,searchTop:search.top,meta:window.RepForge161};
  });
  assert.ok(keyboardLike.hostTop>=keyboardLike.navBottom-1,'Quick panel must remain below fixed navigation after viewport shrink: '+JSON.stringify(keyboardLike));
  assert.ok(keyboardLike.searchTop>=keyboardLike.navBottom-1,'Quick search/header must not slide underneath navigation after viewport shrink: '+JSON.stringify(keyboardLike));
  assert.deepEqual(keyboardLike.meta,{version:'1.6.1',visualViewportIme:true,compactPanels:true,separateSideCards:true,perSidePersistence:true});
  assert.deepEqual(errors,[]);
  console.log('PASS TrainPilot 1.6.1 Android panel / IME polish');
 }finally{await browser?.close();await new Promise(r=>server.close(r))}
})().catch(e=>{console.error(e);process.exit(1)});
