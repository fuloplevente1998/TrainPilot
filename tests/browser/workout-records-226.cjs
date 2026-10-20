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
  await badge.click();
  const card=page.locator('details[data-tp152-history-key]').filter({has:page.locator('.tp226-pr-badge')});
  assert.equal(await card.evaluate(x=>x.open),true);
  const detail=card.locator('.tp226-pr-details');assert.equal(await detail.count(),1);assert.equal(await detail.evaluate(x=>x.open),true);
  assert.match(await detail.innerText(),/10 kg.*12 kg/);
  await page.reload();await boot();await page.evaluate(()=>go('history'));assert.equal(await badge.count(),1,'Reload does not duplicate badge');
  await page.evaluate(()=>feedbackScreen(history().find(x=>x.id==='pr-new').started));
  assert.equal(await page.locator('main.tp122-feedback .tp226-pr-feedback').count(),1,'Saved result summary shows its own records');
  for(const width of [320,393])for(const lang of ['hu','en','de','ro']){
   await page.setViewportSize({width,height:800});
   await page.evaluate(lang=>{db.set('language',lang);go('history');},lang);
   assert.equal(await badge.count(),1);assert.ok((await badge.boundingBox()).height>=44);
   await badge.click();assert.equal(await detail.count(),1);
   const large=await page.addStyleTag({content:'.tp226-pr-badge,.tp226-pr-details{font-size:150%!important}'});
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'Localized large text stays within '+width);
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
