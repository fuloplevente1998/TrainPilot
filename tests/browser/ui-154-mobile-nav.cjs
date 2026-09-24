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
   const brand=document.querySelector('.tp154-brand'),tag=document.querySelector('.tp154-tagline'),brandActions=document.querySelector('.tp152-brand-actions');
   return {cells,rows,brandPresent:!!brand,tagPresent:!!tag,brandActionsPresent:!!brandActions};
  });
  assert.equal(chrome.cells.length,8,'primary navigation must contain 8 controls');
  assert.deepEqual(chrome.rows.map(x=>x.count),[4,4],'primary navigation must be two rows of four controls');
  assert.equal(chrome.cells.filter(x=>x.settings).length,1,'Settings must have one dedicated cell');
  const settings=chrome.cells.find(x=>x.settings);assert.ok(settings&&/Beállítások/.test(settings.text),'Settings must carry its visible label in the unified 2x4 grid');
  assert.equal(chrome.cells.filter(x=>x.text.length>1).length,8,'all eight primary controls must carry a visible label');
  const ordered=[...chrome.cells].sort((a,b)=>a.order-b.order).map(x=>x.text.replace(/^[^A-Za-zÁÉÍÓÖŐÚÜŰ]+/,'').trim());
  assert.deepEqual(ordered.map(x=>x.split(/\s+/)[0]),['Kezdőlap','Edzés','Egészség','Programok','Napló','Naptár','Coach','Beállítások'],'2x4 order must place Health above Calendar and Calendar beside Journal');
  assert.equal(chrome.brandPresent,false,'Home must not render the TrainPilot brand strip');
  assert.equal(chrome.tagPresent,false,'Home must not render the old planner tagline');
  assert.equal(chrome.brandActionsPresent,false,'removed brand strip must not leave duplicate Coach/Settings action chrome');

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

  const chevron=await page.locator('.tp152-chevron').first().evaluate(el=>{const s=getComputedStyle(el),b=getComputedStyle(el,'::before');return {border:s.borderTopWidth,bg:s.backgroundImage,cssWidth:parseFloat(s.width),glyph:b.content,clip:b.clipPath,color:s.color}});
  const themeColor=await page.evaluate(()=>{const el=document.createElement('i');el.style.color='var(--accent2)';document.body.appendChild(el);const v=getComputedStyle(el).color;el.remove();return v});
  assert.equal(chevron.border,'0px','shared disclosure arrows must be borderless');
  assert.equal(chevron.bg,'none','shared disclosure arrows must not use a blended box');
  assert.ok(chevron.cssWidth>=25,'shared chevron must retain its accessible touch target: '+JSON.stringify(chevron));
  assert.ok(chevron.glyph?.includes('›'),'shared chevron must show the theme-aware glyph');
  assert.equal(chevron.clip,'none','shared chevron must not use the old clipped triangle');
  assert.equal(chevron.color,themeColor,'shared chevron must follow the selected theme');

  await page.evaluate(()=>profileScreen());await page.waitForSelector('.tp152-profile');
  const group=page.locator('.tp152-profile-group').nth(1);await group.locator(':scope > summary').click();
  const select=group.locator('.tp-select-trigger').first();
  const selectArrow=await select.evaluate(el=>{const s=getComputedStyle(el,'::after');return {bg:s.backgroundImage,w:parseFloat(s.width),h:parseFloat(s.height),border:s.borderTopWidth,clip:s.clipPath,content:s.content,color:s.color}});
  assert.equal(selectArrow.bg,'none','dropdown arrow must have no gradient');
  assert.ok(selectArrow.w>=23&&selectArrow.w<=25&&selectArrow.h>=23&&selectArrow.h<=25,'dropdown chevron must fit its 24px slot');
  assert.equal(selectArrow.clip,'none','dropdown arrow must not use a clipped triangle');
  assert.ok(selectArrow.content.includes('›'),'dropdown arrow must use shared theme chevron');
  assert.equal(selectArrow.border,'0px');

  const coverage=await page.evaluate(()=>document.getElementById('tp154MobilePolishCss')?.textContent||'');
  for(const selector of ['.rf103-history-chevron','.rf-history-health-chevron','.tp1511-training summary i','.tp-library-card>summary::after','.tp-select-trigger::after'])assert.ok(coverage.includes(selector),'global blended-chevron coverage missing '+selector);
  assert.deepEqual(errors,[],'page errors: '+errors.join('\n'));
  console.log('PASS TrainPilot 1.5.4 mobile navigation compatibility under 1.5.5');
 } finally{if(browser)await browser.close();server.close()}
})().catch(e=>{console.error(e);server.close();process.exit(1)});
