const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');

(async()=>{
 const root=path.resolve('www');
 const server=http.createServer((req,res)=>{
  const file=path.resolve(root,'.'+new URL(req.url,'http://local').pathname.replace(/\/$/,'/index.html'));
  if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
  fs.readFile(file,(error,data)=>{if(error){res.writeHead(404);res.end();return}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(data)});
 });
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:720},locale:'hu-HU'});
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  await page.goto(`http://127.0.0.1:${server.address().port}`);
  await page.waitForFunction(()=>window.TrainPilotBoot?.finished&&window.RepForge160?.version==='1.6.0');

  await page.evaluate(()=>{db.set('language','hu');db.set('history',[]);state.session=null;go('plan');window.tp155R4OpenPanel('quick',document.activeElement)});
  const host=page.locator('#tp155R4PanelHost[data-panel="quick"]');await host.waitFor();
  assert.equal(await host.locator('.tp160-quick-context').innerText(),'Gyors edzés','Quick panel needs compact context');
  assert.equal(await host.locator('.hero').count(),0,'old duplicated Quick hero must stay removed');
  const headGeometry=await page.evaluate(()=>{
   const search=document.getElementById('tp155QuickQuery').getBoundingClientRect();
   const close=document.querySelector('#tp155R4PanelHost[data-panel="quick"] .tp155-r4-panel-close').getBoundingClientRect();
   const panel=document.querySelector('#tp155R4PanelHost .tp155-r4-panel').getBoundingClientRect();
   return {search:search.toJSON(),close:close.toJSON(),panel:panel.toJSON()};
  });
  assert.ok(Math.abs((headGeometry.search.top+headGeometry.search.height/2)-(headGeometry.close.top+headGeometry.close.height/2))<20,'Search and X must share the compact header row: '+JSON.stringify(headGeometry));
  assert.ok(headGeometry.search.right<=headGeometry.close.left+1,'Search must not overlap the X');
  assert.ok(headGeometry.search.left>=headGeometry.panel.left&&headGeometry.close.right<=headGeometry.panel.right+1,'header controls must stay inside panel');

  await host.locator('.tp155-quick-filter>summary').click();
  async function assertAttached(id){
   const trigger=page.locator(id).locator('xpath=..').locator('.tp-select-trigger');await trigger.click();
   const g=await page.evaluate(selector=>{
    const select=document.querySelector(selector),wrap=select.closest('.tp-select'),trigger=wrap.querySelector('.tp-select-trigger'),menu=wrap.querySelector('.tp-select-menu'),panel=document.querySelector('#tp155R4PanelHost .tp155-r4-panel');
    const t=trigger.getBoundingClientRect(),m=menu.getBoundingClientRect(),p=panel.getBoundingClientRect(),cs=getComputedStyle(menu);
    return {t:t.toJSON(),m:m.toJSON(),p:p.toJSON(),position:cs.position,top:cs.top,left:cs.left};
   },id);
   assert.equal(g.position,'absolute',id+' menu must be attached to its field');
   assert.ok(Math.abs(g.m.top-g.t.bottom)<=6,id+' menu must open directly under its trigger: '+JSON.stringify(g));
   assert.ok(Math.abs(g.m.left-g.t.left)<=2,id+' menu must share trigger left edge');
   assert.ok(g.m.right<=g.p.right+1&&g.m.left>=g.p.left-1,id+' menu must stay inside panel width');
   await trigger.click();
  }
  await assertAttached('#tp155QuickMuscle');
  await assertAttached('#tp155QuickGear');

  await page.evaluate(()=>{window.tp155R4ClosePanel(false);go('history')});
  const filter=page.locator('.tp155-journal-filter');await filter.waitFor();
  assert.equal(await filter.locator('.tp152-chevron').count(),0,'legacy Journal disclosure glyph must not return');
  const arrow=filter.locator('.tp160-journal-disclosure');
  assert.equal((await arrow.innerText()).trim(),'','Journal arrow must not use a text glyph');
  const closed=await arrow.evaluate(e=>getComputedStyle(e,'::before').transform);
  assert.match(await arrow.evaluate(e=>getComputedStyle(e,'::before').clipPath),/polygon/i,'Journal must use the shared CSS triangle');
  await filter.locator('summary').click();
  await page.waitForTimeout(180);
  const opened=await arrow.evaluate(e=>getComputedStyle(e,'::before').transform);
  assert.notEqual(opened,closed,'Journal CSS triangle must rotate when opened');

  await page.evaluate(()=>go('home'));
  const motion=await page.evaluate(()=>{
   const main=document.querySelector('#app main'),ms=parseFloat(getComputedStyle(main).animationDuration)*1000;
   const hostStyle=document.getElementById('tp155Round3PanelNavigationCss')?.textContent||'';
   const motionStyle=document.getElementById('tp160LightNavigationMotionCss')?.textContent||'';
   return {meta:window.RepForge160Motion,duration:ms,legacyClass:main.classList.contains('tp155-r4-page-reveal'),hostBlur:/backdrop-filter\s*:/.test(hostStyle),forced:/offsetWidth/.test(motionStyle),clip:/clip-path/.test(motionStyle)};
  });
  assert.deepEqual(motion.meta,{version:'1.6.0',forcedLayout:false,clipPath:false,backdropBlur:false});
  assert.ok(motion.duration<=110.5,'main-page reveal must remain lightweight');
  assert.equal(motion.legacyClass,false,'legacy heavy reveal class must be removed');
  assert.equal(motion.hostBlur,false,'full-screen panel backdrop blur must be removed');
  assert.equal(motion.forced,false,'motion path must not force synchronous layout');
  assert.equal(motion.clip,false,'motion path must not animate clip-path');
  assert.deepEqual(errors,[]);
  console.log('RepForge 1.6 phone release checks passed');
 }finally{await browser?.close();await new Promise(resolve=>server.close(resolve))}
})().catch(error=>{console.error(error);process.exit(1)});
