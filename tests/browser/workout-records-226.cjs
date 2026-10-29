'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),http=require('node:http');
const {chromium}=require('playwright'),root=path.resolve('www');
const server=http.createServer((req,res)=>{
 const url=new URL(req.url,'http://local'),file=path.resolve(root,'.'+(url.pathname==='/'?'/index.html':url.pathname));
 if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
 fs.readFile(file,(e,data)=>{if(e){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(data);});
});
(async()=>{
 await new Promise(done=>server.listen(0,'127.0.0.1',done));let browser;
 try{
  browser=await chromium.launch({headless:true,args:['--no-sandbox'],executablePath:process.env.TRAINPILOT_CHROMIUM||undefined});
  const page=await browser.newPage({viewport:{width:360,height:800},locale:'hu-HU'}),errors=[];
  page.on('pageerror',e=>errors.push(String(e)));page.on('dialog',d=>d.accept());
  await page.addInitScript(()=>localStorage.setItem('repforge:onboarding128',JSON.stringify('skipped')));
  const boot=()=>page.waitForFunction(()=>TrainPilotBoot.finished);
  await page.goto('http://127.0.0.1:'+server.address().port+'/');await boot();
  await page.evaluate(()=>{
   const row=(id,day,w)=>({id,started:day+'T10:00:00Z',finished:day+'T11:00:00Z',programId:'home-basic',dayId:'A',programName:'PR test',
    exercises:[{...byId('goblet-squat'),sets:[{set:1,weight:w,reps:'10',done:true}]}],photos:[]});
   db.set('history',[row('pr-base','2026-10-01',10),row('pr-new','2026-10-02',12)]);
   db.set('phase3HistoryTombstones',[]);state.session=null;go('history');
  });
  const badge=page.locator('.tp226-pr-badge');assert.equal(await badge.count(),1);
  const card=page.locator('details[data-tp152-history-key]').filter({has:page.locator('.tp226-pr-badge')});
  assert.equal(await card.evaluate(x=>x.open),false,'Workout starts collapsed');
  assert.equal(await badge.isVisible(),true,'Genuine PR badge must be visible on a collapsed journal card');
  // PR278 visual regression: a closed PR is a themed pill, not a native gray button.
  const closedStyle=await badge.evaluate(e=>{
   const s=getComputedStyle(e),r=e.getBoundingClientRect(),card=e.closest('details').getBoundingClientRect();
   return {radius:parseFloat(s.borderTopLeftRadius),background:s.backgroundColor,display:s.display,height:r.height,width:r.width,cardWidth:card.width,cardHeight:card.height};
  });
  assert.ok(closedStyle.radius>=20,'Closed PR must be visibly rounded: '+JSON.stringify(closedStyle));
  assert.notEqual(closedStyle.background,'rgb(239, 239, 239)','No browser-default gray PR button');
  assert.ok(['inline-flex','flex'].includes(closedStyle.display),'PR badge uses flex alignment');
  assert.ok(closedStyle.width<closedStyle.cardWidth*.8,'Closed PR pill must not stretch to full card width: '+JSON.stringify(closedStyle));
  assert.ok(closedStyle.height>=44,'PR remains a 44px touch target');
  assert.ok(closedStyle.cardHeight<160,'Closed short-title workout stays compact: '+JSON.stringify(closedStyle));
  fs.mkdirSync('ui-evidence',{recursive:true});
  await page.screenshot({path:'ui-evidence/pr278-journal-closed-360.png',fullPage:true});
  await card.locator(':scope > summary .history-title').click();
  assert.equal(await card.evaluate(x=>x.open),true,'Workout opens independently of its PR badge');
  await page.screenshot({path:'ui-evidence/pr278-journal-open-360.png',fullPage:true});
  const detail=card.locator(':scope > .tp3-combined-history-body > .tp226-pr-details');
  assert.equal(await detail.count(),1,'Record data exists in the hydrated workout');
  assert.equal(await detail.evaluate(x=>x.open),false,'Record disclosure defaults to closed');
  assert.equal(await detail.isVisible(),false,'No Record details title or box before tapping PR');
  assert.equal(await badge.getAttribute('aria-expanded'),'false');
  await badge.click();
  assert.equal(await badge.getAttribute('aria-expanded'),'true');
  assert.equal(await detail.evaluate(x=>x.open),true,'Tapping PR expands the record');
  assert.equal(await detail.isVisible(),true,'PR tap makes record details visible');
  assert.match(await detail.innerText(),/10 kg.*12 kg/);
  await badge.click();
  assert.equal(await detail.evaluate(x=>x.open),false,'Second PR tap collapses the record');
  assert.equal(await detail.isVisible(),false,'Second PR tap completely hides the panel');
  assert.equal(await card.evaluate(x=>x.open),true,'Closing PR leaves the workout open');
  assert.equal(await badge.getAttribute('aria-expanded'),'false');
  await badge.click();
  assert.equal(await detail.isVisible(),true,'Third PR tap opens it again');
  await detail.locator(':scope > summary').click();
  assert.equal(await detail.isVisible(),false,'Closing record details removes the panel completely');
  await page.waitForFunction(()=>document.querySelector('.tp226-pr-badge')?.getAttribute('aria-expanded')==='false');
  await badge.click();
  assert.equal(await detail.isVisible(),true,'PR tap can reveal the panel again');
  assert.equal(await detail.locator('.tp226-pr-record').count(),1,'PR count matches displayed explanation');
  const chipBounds=await card.locator(':scope > summary .history-badges > *').evaluateAll(nodes=>nodes.map(node=>{
   const {x,y,width,height}=node.getBoundingClientRect();return {x,y,width,height};
  }));
  assert.equal(chipBounds.length,3,'Time, completed sets and PR share one compact metric row');
  assert.ok(chipBounds.every(b=>Math.abs(b.y-chipBounds[0].y)<1&&Math.abs(b.height-chipBounds[0].height)<1),'Chips align to the same height');
  assert.ok(chipBounds.every(b=>Math.abs(b.width-chipBounds[0].width)<1),'Chips use equal widths');
  await page.reload();await boot();await page.evaluate(()=>go('history'));assert.equal(await badge.count(),1,'Reload does not duplicate badge');
  await page.evaluate(()=>feedbackScreen(history().find(x=>x.id==='pr-new').started));
  assert.equal(await page.locator('main.tp122-feedback .tp226-pr-feedback').count(),1,'Saved result summary shows its own records');
  for(const width of [320,393])for(const lang of ['hu','en','de','ro']){
   await page.setViewportSize({width,height:800});
   await page.evaluate(lang=>{db.set('language',lang);go('history');},lang);
   assert.equal(await badge.count(),1);
   // Route repaint may retain the expanded workout; use its actual state.
   if(!await card.evaluate(x=>x.open))await card.locator(':scope > summary .history-title').click();
   if(await detail.isVisible())await badge.click();
   assert.equal(await detail.isVisible(),false,'PR details can be fully hidden before the next localized toggle');
   assert.ok((await badge.boundingBox()).height>=44);
   await badge.click();assert.equal(await detail.count(),1);
   const large=await page.addStyleTag({content:'.tp226-pr-badge,.tp226-pr-details{font-size:150%!important}'});
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'Localized large text stays within '+width);
   await badge.click();
   assert.equal(await detail.isVisible(),false,'PR closes with localized large text');
   assert.equal(await badge.getAttribute('aria-expanded'),'false');
   await badge.click();assert.equal(await detail.isVisible(),true,'PR opens again with localized large text');
   await large.evaluate(el=>el.remove());
  }
  fs.mkdirSync('ui-evidence',{recursive:true});await page.screenshot({path:'ui-evidence/pr-badge-226.png'});
  await page.evaluate(()=>{const rows=history();rows.find(x=>x.id==='pr-new').exercises[0].sets[0].weight=10;db.set('history',rows);go('history');});
  assert.equal(await badge.count(),0,'Edited equal result removes PR badge');
  await page.evaluate(()=>{const rows=history();rows.find(x=>x.id==='pr-new').exercises[0].sets[0].weight=12;db.set('history',rows.filter(x=>x.id!=='pr-base'));go('history');});
  assert.equal(await badge.count(),0,'Removing baseline leaves first performance without a PR');
  assert.deepEqual(errors,[]);console.log('PASS #226 PR badges: saved Journal/result, identity, idempotent reload, 4 languages, small/large text, editing and removed baseline');
 }finally{await browser?.close();await new Promise(done=>server.close(done));}
})().catch(error=>{console.error(error);process.exitCode=1;});
