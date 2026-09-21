const assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright');
const root=path.resolve('www');
const server=http.createServer((req,res)=>{
 let p=new URL(req.url,'http://local').pathname;if(p==='/')p='/index.html';
 const file=path.resolve(root,'.'+p);if(!file.startsWith(root+path.sep)){res.writeHead(403);res.end();return}
 fs.readFile(file,(e,d)=>{if(e){res.writeHead(404);res.end();return}
  res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':file.endsWith('.html')?'text/html':'text/plain');res.end(d);
 });
});
(async()=>{
 await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
 try{
  browser=await chromium.launch({headless:true,executablePath:process.env.TRAINPILOT_CHROMIUM||undefined,args:['--no-sandbox']});
  const page=await browser.newPage({viewport:{width:393,height:873},locale:'hu-HU',timezoneId:'Europe/Budapest'});
  const errors=[];page.on('pageerror',e=>errors.push(e?.stack||e?.message||String(e)));page.on('dialog',d=>d.accept());
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await page.waitForFunction(()=>window.TrainPilotBoot?.finished);
  await page.evaluate(()=>{state.session=null;state.workout=null;go('home')});await page.waitForSelector('.top.tp154-nav-grid');

  const chrome=await page.evaluate(()=>{
   const cells=[...document.querySelectorAll('.top.tp154-nav-grid .tp154-nav-cell')].map(el=>{const r=el.getBoundingClientRect();return {text:(el.textContent||'').trim(),top:r.top,left:r.left,width:r.width,height:r.height,order:Number(getComputedStyle(el).order)||0,settings:el.classList.contains('tp154-settings-action')}});
   const sortedTops=[...cells].sort((a,b)=>a.top-b.top).map(x=>x.top),rows=[];
   for(const top of sortedTops){const row=rows.find(x=>Math.abs(x.top-top)<4);if(row)row.count++;else rows.push({top,count:1})}
   const brand=document.querySelector('.tp154-brand'),tag=document.querySelector('.tp154-tagline'),br=brand?.getBoundingClientRect(),tr=tag?.getBoundingClientRect();
   return {cells,rows,brand:{font:parseFloat(getComputedStyle(brand).fontSize),top:br?.top,height:br?.height},tag:{top:tr?.top,height:tr?.height},brandActions:getComputedStyle(document.querySelector('.tp152-brand-actions')).display};
  });
  assert.equal(chrome.cells.length,8,'primary navigation must contain 8 controls');
  assert.deepEqual(chrome.rows.map(x=>x.count),[4,4],'primary navigation must be two rows of four controls');
  assert.equal(chrome.cells.filter(x=>x.settings).length,1,'Settings must have one dedicated cell');
  const settings=chrome.cells.find(x=>x.settings);assert.ok(settings&&/Beállítások/.test(settings.text),'Settings must carry its visible label in the unified 2x4 grid');
  assert.equal(chrome.cells.filter(x=>x.text.length>1).length,8,'all eight primary controls must carry a visible label');
  const ordered=[...chrome.cells].sort((a,b)=>a.order-b.order).map(x=>x.text.replace(/^[^A-Za-zÁÉÍÓÖŐÚÜŰ]+/,'').trim());
  assert.deepEqual(ordered.map(x=>x.split(/\s+/)[0]),['Kezdőlap','Edzés','Egészség','Programok','Napló','Naptár','Coach','Beállítások'],'2x4 order must place Health above Calendar and Calendar beside Journal');
  assert.ok(chrome.brand.font<=20.5,'TrainPilot brand must be compact');
  assert.ok(Math.abs(chrome.brand.top-chrome.tag.top)<10,'TrainPilot and intelligent planner tagline must sit on the same line');
  assert.equal(chrome.brandActions,'none','duplicate Coach/Settings actions must be hidden from the brand strip');

  await page.evaluate(()=>go('programs'));await page.waitForSelector('.tp152-program-card');
  const first=page.locator('.tp152-program-card').first();await first.locator(':scope > summary').click();
  const firstDay=first.locator('.tp152-program-day').first();await firstDay.locator(':scope > summary').click();
  const visiblePlay=firstDay.locator('.tp152-program-exercise .tp-play-btn.tp152-play').first();await visiblePlay.waitFor({state:'visible'});
  const video=await visiblePlay.evaluate(el=>{
   const g=el.querySelector('.tp152-play-glyph'),a=el.getBoundingClientRect(),b=g.getBoundingClientRect();
   return {outer:{x:a.x,y:a.y,w:a.width,h:a.height},inner:{x:b.x,y:b.y,w:b.width,h:b.height}};
  });
  assert.ok(video.inner.w<video.outer.w&&video.inner.h<video.outer.h,'video glyph must be smaller than its frame');
  assert.ok(video.inner.x>=video.outer.x&&video.inner.y>=video.outer.y&&video.inner.x+video.inner.w<=video.outer.x+video.outer.w+0.5&&video.inner.y+video.inner.h<=video.outer.y+video.outer.h+0.5,'video glyph must remain fully inside its frame');

  const chevron=await page.locator('.tp152-chevron').first().evaluate(el=>{const s=getComputedStyle(el),b=getComputedStyle(el,'::before');return {border:s.borderTopWidth,bg:s.backgroundImage,cssWidth:parseFloat(s.width),paddingLeft:s.paddingLeft,paddingRight:s.paddingRight,font:parseFloat(s.fontSize),clip:b.clipPath,fill:b.backgroundColor}});
  assert.equal(chevron.border,'0px','disclosure arrows must not use a separate hard border');
  assert.equal(chevron.bg,'none','1.5.5 disclosure arrows must not use the legacy blended box');
  assert.ok(chevron.cssWidth<=18.6,'disclosure arrows must use the compact triangle host: '+JSON.stringify(chevron));
  assert.equal(chevron.font,0,'legacy chevron glyph must be hidden');
  assert.match(chevron.clip,/polygon/i,'disclosure arrow must be a CSS triangle');
  assert.notEqual(chevron.fill,'rgba(0, 0, 0, 0)','disclosure triangle must be visibly filled');
  assert.equal(chevron.paddingLeft,'0px');assert.equal(chevron.paddingRight,'0px');

  await page.evaluate(()=>profileScreen());await page.waitForSelector('.tp152-profile');
  const group=page.locator('.tp152-profile-group').nth(1);await group.locator(':scope > summary').click();
  const select=group.locator('.tp-select-trigger').first();
  const selectArrow=await select.evaluate(el=>{const s=getComputedStyle(el,'::after');return {bg:s.backgroundImage,w:parseFloat(s.width),h:parseFloat(s.height),border:s.borderTopWidth,clip:s.clipPath}});
  assert.equal(selectArrow.bg,'none','dropdown arrow must use the 1.5.5 plain triangle treatment');
  assert.ok(selectArrow.w<=9.6&&selectArrow.h<=12.6,'dropdown triangle must stay compact');
  assert.match(selectArrow.clip,/polygon/i,'dropdown arrow must be a CSS triangle');
  assert.equal(selectArrow.border,'0px');

  const coverage=await page.evaluate(()=>document.getElementById('tp154MobilePolishCss')?.textContent||'');
  for(const selector of ['.rf103-history-chevron','.rf-history-health-chevron','.tp1511-training summary i','.tp-library-card>summary::after','.tp-select-trigger::after'])assert.ok(coverage.includes(selector),'global blended-chevron coverage missing '+selector);
  assert.deepEqual(errors,[],'page errors: '+errors.join('\n'));
  console.log('PASS TrainPilot 1.5.4 mobile navigation compatibility under 1.5.5');
 } finally{if(browser)await browser.close();server.close()}
})().catch(e=>{console.error(e);server.close();process.exit(1)});
